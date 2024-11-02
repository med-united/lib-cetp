package de.health.service.cetp;

import de.health.service.cetp.domain.CetpStatus;
import de.health.service.cetp.domain.SubscriptionResult;
import de.health.service.cetp.domain.eventservice.Subscription;
import de.health.service.cetp.domain.fault.CetpFault;
import de.health.service.cetp.domain.fault.Error;
import de.health.service.cetp.konnektorconfig.KonnektorConfigService;
import de.health.service.cetp.retry.Retrier;
import de.health.service.cetp.utils.LocalAddressInSameSubnetFinder;
import de.health.service.cetp.config.KonnektorConfig;
import de.health.service.config.api.ISubscriptionConfig;
import de.health.service.config.api.UserRuntimeConfig;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.xml.ws.Holder;
import org.apache.commons.lang3.tuple.Pair;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static de.health.service.cetp.utils.Utils.printException;

@SuppressWarnings({"CdiInjectionPointsInspection", "unused"})
@ApplicationScoped
public class SubscriptionManager {

    private static final DateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    private final static Logger log = Logger.getLogger(SubscriptionManager.class.getName());
    
    public static final String FAILED = "failed";

    private final Map<String, KonnektorConfig> hostToKonnektorConfig = new ConcurrentHashMap<>();

    ISubscriptionConfig subscriptionConfig;
    UserRuntimeConfig userRuntimeConfig;
    IKonnektorClient konnektorClient;
    KonnektorConfigService kcService;

    private ExecutorService threadPool;

    @Inject
    public SubscriptionManager(
        ISubscriptionConfig subscriptionConfig,
        UserRuntimeConfig userRuntimeConfig,
        IKonnektorClient konnektorClient,
        KonnektorConfigService kcService
    ) {
        this.subscriptionConfig = subscriptionConfig;
        this.userRuntimeConfig = userRuntimeConfig;
        this.konnektorClient = konnektorClient;
        this.kcService = kcService;
    }

    public void onStart(@Observes StartupEvent ev) {
        log.info("SubscriptionManager starting..");
        hostToKonnektorConfig.putAll(kcService.loadConfigs());
        threadPool = Executors.newFixedThreadPool(hostToKonnektorConfig.size());
        log.info("SubscriptionManager started");
    }

    @SuppressWarnings("unused")
    @Scheduled(
        every = "${cetp.subscriptions.maintenance.interval.sec:3s}", // TODO naming
        delay = 5,
        delayUnit = TimeUnit.SECONDS,
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP
    )
    void subscriptionsMaintenance() {
        String defaultSender = subscriptionConfig.getDefaultEventToHost();
        if (defaultSender == null) {
            log.log(Level.WARNING, "You did not set 'cetp.subscriptions.event-to-host' property. Will have no fallback if Konnektor is not found to be in the same subnet");
        }
        List<Integer> retryMillis = List.of(200);
        int intervalMs = subscriptionConfig.getCetpSubscriptionsMaintenanceRetryIntervalMs();
        List<Future<Boolean>> futures = getKonnektorConfigs(null).stream().map(kc -> threadPool.submit(() -> {
            Inet4Address meInSameSubnet = LocalAddressInSameSubnetFinder.findLocalIPinSameSubnet(konnektorToIp4(kc.getHost()));
            String eventToHost = (meInSameSubnet != null) ? meInSameSubnet.getHostAddress() : defaultSender;
            if (eventToHost == null) {
                log.log(Level.INFO, "Can't maintain subscription. Don't know my own address to tell konnektor about it");
                return false;
            }
            Boolean result = Retrier.callAndRetry(
                retryMillis,
                intervalMs,
                () -> renewSubscriptions(eventToHost, kc),
                bool -> bool
            );
            if (!result) {
                String msg = String.format(
                    "[%s] Subscriptions maintenance is failed within %d ms retry", kc.getHost(), intervalMs);
                log.warning(msg);
            }
            return result;
        })).toList();
        for (Future<Boolean> future : futures) {
            try {
                future.get();
            } catch (Throwable e) {
                log.log(Level.SEVERE, "Subscriptions maintenance error", e);
            }
        }
    }

    private UserRuntimeConfig modifyRuntimeConfig(UserRuntimeConfig runtimeConfig, KonnektorConfig konnektorConfig) {
        if (runtimeConfig == null) {
            UserRuntimeConfig userRuntimeConfigCopy = userRuntimeConfig.copy();
            userRuntimeConfigCopy.updateProperties(konnektorConfig.getUserConfigurations());
            return userRuntimeConfigCopy;
        } else {
            runtimeConfig.updateProperties(konnektorConfig.getUserConfigurations());
            return runtimeConfig;
        }
    }

    public boolean renewSubscriptions(String eventToHost, KonnektorConfig kc) {
        Semaphore semaphore = kc.getSemaphore();
        if (semaphore.tryAcquire()) {
            try {
                UserRuntimeConfig runtimeConfig = modifyRuntimeConfig(null, kc);
                String cetpHost = "cetp://" + eventToHost + ":" + kc.getCetpPort();
                List<Subscription> subscriptions = konnektorClient.getSubscriptions(runtimeConfig)
                    .stream().filter(st -> st.getEventTo().contains(eventToHost)).toList();

                Holder<String> resultHolder = new Holder<>();
                if (subscriptions.isEmpty()) {
                    return subscribe(kc, runtimeConfig, cetpHost, resultHolder);
                } else {
                    Optional<Subscription> newestOpt = subscriptions.stream().max(
                        Comparator.comparing(Subscription::getTerminationTime)
                    );
                    Subscription newest = newestOpt.get();
                    Date expireDate = newest.getTerminationTime();
                    Date now = new Date();
                    boolean expired = now.getTime() >= expireDate.getTime();

                    // force re-subscribe every 12 hours
                    int periodSeconds = subscriptionConfig.getForceResubscribePeriodSeconds();
                    boolean forceSubscribe = kc.getSubscriptionTime().plusSeconds(periodSeconds).isBefore(OffsetDateTime.now());
                    if (expired || forceSubscribe) {
                        boolean subscribed = subscribe(kc, runtimeConfig, cetpHost, resultHolder);
                        if (forceSubscribe && subscribed) {
                            log.info(String.format("Force subscribed to %s: %s", kc.getHost(), resultHolder.value));
                        }
                        // all are expired, drop them
                        boolean dropped = drop(runtimeConfig, subscriptions).isEmpty();
                        return subscribed && dropped;
                    } else {
                        boolean renewed = renew(runtimeConfig, kc, newest, now, expireDate);
                        List<Subscription> olderSubscriptions = subscriptions.stream()
                            .filter(st -> !st.getSubscriptionId().equals(newest.getSubscriptionId()))
                            .toList();
                        boolean dropped = drop(runtimeConfig, olderSubscriptions).isEmpty();
                        return renewed && dropped;
                    }
                }
            } catch (CetpFault fm) {
                log.log(Level.SEVERE, String.format("[%s] Subscriptions maintenance error", kc.getHost()), fm);
                return false;
            } finally {
                semaphore.release();
            }
        } else {
            log.warning(String.format("[%s] Subscription maintenance is in progress, try later", kc.getHost()));
            return true;
        }
    }

    public boolean renew(
        UserRuntimeConfig runtimeConfig,
        KonnektorConfig konnektorConfig,
        Subscription type,
        Date now,
        Date expireDate
    ) throws CetpFault {
        String newestSubscriptionId = type.getSubscriptionId();
        boolean sameSubscription = newestSubscriptionId.equals(konnektorConfig.getSubscriptionId());
        if (!sameSubscription) {
            String msg = String.format(
                "Found subscriptions discrepancy: CONFIG=%s, REAL=%s, REAL_EXPIRATION=%s, updating config",
                konnektorConfig.getSubscriptionId(),
                newestSubscriptionId,
                expireDate
            );
            log.warning(msg);
            kcService.trackSubscriptionFile(konnektorConfig, newestSubscriptionId, null);
        }
        int safePeriod = subscriptionConfig.getCetpSubscriptionsRenewalSafePeriodMs();
        if (now.getTime() + safePeriod >= expireDate.getTime()) {
            String msg = String.format(
                "Subscription %s is about to expire after %d seconds, renew", newestSubscriptionId, safePeriod / 1000
            );
            log.info(msg);
            SubscriptionResult subscriptionResult = konnektorClient.renewSubscription(runtimeConfig, newestSubscriptionId);
            Error error = subscriptionResult.getStatus().getError();
            String renewedSubscriptionId = subscriptionResult.getSubscriptionId();
            if (error == null && !renewedSubscriptionId.equals(newestSubscriptionId)) {
                msg = String.format(
                    "Subscription ID has changed after renew: OLD=%s, NEW=%s, updating config",
                    newestSubscriptionId,
                    renewedSubscriptionId
                );
                log.fine(msg);
                kcService.trackSubscriptionFile(konnektorConfig, renewedSubscriptionId, null);
            }
            return error == null;
        } else {
            return true;
        }
    }

    private String printError(Error error) {
        return String.format(
            "[%s] Gematik ERROR at %s: %s ",
            error.getMessageId(),
            error.getTimestamp(),
            error.getTrace().stream().map(t ->
                String.format("Code=%s ErrorText=%s Detail=%s", t.getCode(), t.getErrorText(), t.getDetail().getValue())
            ).collect(Collectors.joining(", "))
        );
    }

    public List<String> drop(
        UserRuntimeConfig runtimeConfig,
        List<Subscription> subscriptions
    ) throws CetpFault {
        return subscriptions.stream().map(s -> {
            try {
                CetpStatus status = konnektorClient.unsubscribe(runtimeConfig, s.getSubscriptionId(), null, false);
                Error error = status.getError();
                if (error == null) {
                    return Pair.of(true, s.getSubscriptionId());
                } else {
                    String msg = String.format("Failed to unsubscribe %s", s.getSubscriptionId());
                    log.log(Level.SEVERE, msg, printError(error));
                    return Pair.of(false, s.getSubscriptionId());
                }
            } catch (CetpFault f) {
                String msg = String.format("Failed to unsubscribe %s", s.getSubscriptionId());
                log.log(Level.SEVERE, msg, f);
                return Pair.of(false, s.getSubscriptionId());
            }
        }).filter(p -> !p.getKey()).map(Pair::getValue).toList();
    }

    public Collection<KonnektorConfig> getKonnektorConfigs(String host) {
        return host == null
            ? hostToKonnektorConfig.values()
            : hostToKonnektorConfig.entrySet().stream().filter(entry -> entry.getKey().contains(host)).map(Map.Entry::getValue).toList();
    }

    private boolean subscribe(
        KonnektorConfig konnektorConfig,
        UserRuntimeConfig runtimeConfig,
        String cetpHost,
        Holder<String> resultHolder
    ) throws CetpFault {
        SubscriptionResult subscriptionResult = konnektorClient.subscribe(runtimeConfig, cetpHost);
        CetpStatus status = subscriptionResult.getStatus();
        Error error = status.getError();
        if (error == null) {
            String newSubscriptionId = subscriptionResult.getSubscriptionId();
            Date terminationTime = subscriptionResult.getTerminationTime();
            resultHolder.value = status.getResult() + " " + newSubscriptionId + " " + SIMPLE_DATE_FORMAT.format(terminationTime);
            kcService.trackSubscriptionFile(konnektorConfig, newSubscriptionId, null);
            log.info(String.format("Subscribe status for subscriptionId=%s: %s", newSubscriptionId, status.getResult()));
            return true;
        } else {
            String statusResult = printError(error);
            resultHolder.value = statusResult;
            String subscriptionId = konnektorConfig.getSubscriptionId();
            String fileName = subscriptionId != null && subscriptionId.startsWith(FAILED)
                ? subscriptionId
                : String.format("%s-%s", FAILED, subscriptionId);

            kcService.trackSubscriptionFile(konnektorConfig, fileName, statusResult);
            log.log(Level.WARNING, String.format("Could not subscribe -> %s", statusResult));
            return false;
        }
    }

    public List<String> manage(
        UserRuntimeConfig runtimeConfig,
        String host,
        String eventToHost,
        boolean forceCetp,
        boolean subscribe
    ) {
        Collection<KonnektorConfig> konnektorConfigs = getKonnektorConfigs(host);
        List<String> statuses = konnektorConfigs.stream().map(kc -> {
                Semaphore semaphore = kc.getSemaphore();
                if (semaphore.tryAcquire()) {
                    try {
                        String cetpHost = "cetp://" + eventToHost + ":" + kc.getCetpPort();
                        return process(kc, modifyRuntimeConfig(runtimeConfig, kc), cetpHost, forceCetp, subscribe);
                    } finally {
                        semaphore.release();
                    }
                } else {
                    try {
                        String h = host == null ? kc.getHost() : host;
                        String s = subscribe ? "subscription" : "unsubscription";
                        return String.format("[%s] Host %s is in progress, try later", h, s);
                    } catch (Exception e) {
                        return e.getMessage();
                    }
                }
            })
            .filter(Objects::nonNull)
            .toList();

        if (statuses.isEmpty()) {
            return List.of(String.format("No configuration is found for the given host: %s", host));
        }
        return statuses;
    }

    private String process(
        KonnektorConfig konnektorConfig,
        UserRuntimeConfig runtimeConfig,
        String cetpHost,
        boolean forceCetp,
        boolean subscribe
    ) {
        String subscriptionId = konnektorConfig.getSubscriptionId();
        String failedUnsubscriptionFileName = subscriptionId != null && subscriptionId.startsWith(FAILED)
            ? subscriptionId
            : String.format("%s-unsubscription-%s", FAILED, subscriptionId);

        String failedSubscriptionFileName = String.format("%s-subscription", FAILED);

        String statusResult;
        boolean unsubscribed = false;
        try {
            CetpStatus status = konnektorClient.unsubscribe(runtimeConfig, subscriptionId, cetpHost, forceCetp);
            Error error = status.getError();
            if (error == null) {
                unsubscribed = true;
                statusResult = status.getResult();
                log.info(String.format("Unsubscribe status for subscriptionId=%s: %s", subscriptionId, statusResult));
                if (subscribe) {
                    Holder<String> resultHolder = new Holder<>();
                    subscribe(konnektorConfig, runtimeConfig, cetpHost, resultHolder);
                    statusResult = resultHolder.value;
                } else {
                    kcService.cleanUpSubscriptionFiles(konnektorConfig, null);
                }
            } else {
                statusResult = printError(error);
                kcService.trackSubscriptionFile(konnektorConfig, failedUnsubscriptionFileName, statusResult);
                String msg = String.format("Could not unsubscribe from %s -> %s", subscriptionId, printError(error));
                log.log(Level.WARNING, msg);
            }
        } catch (Exception e) {
            String fileName = unsubscribed ? failedSubscriptionFileName : failedUnsubscriptionFileName;
            kcService.trackSubscriptionFile(konnektorConfig, fileName, printException(e));
            log.log(Level.WARNING, "Error: " + fileName, e);
            statusResult = e.getMessage();
        }
        return statusResult;
    }

    private Inet4Address konnektorToIp4(String host) {
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            if (inetAddress instanceof Inet4Address addr) {
                return addr;
            } else {
                return null;
            }
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
