package de.health.service.cetp.konnektorconfig;

import de.health.service.cetp.KonnektorsConfigs;
import de.health.service.cetp.config.KonnektorConfig;
import de.health.service.config.api.ISubscriptionConfig;
import de.health.service.config.api.IUserConfigurations;
import de.health.service.config.api.UserRuntimeConfig;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.Setter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static de.health.service.cetp.SubscriptionManager.FAILED;

@SuppressWarnings("CdiInjectionPointsInspection")
@ApplicationScoped
public class FSConfigService implements KonnektorConfigService {

    private final static Logger log = Logger.getLogger(FSConfigService.class.getName());

    public static final String PROPERTIES_EXT = ".properties";
    public static final String CONFIG_DELIMETER = "<sep>";

    @Setter
    @ConfigProperty(name = "ere.per.konnektor.config.folder")
    String configFolder;

    private final Map<String, KonnektorConfig> hostToKonnektorConfig;
    private final ISubscriptionConfig subscriptionConfig;
    private final UserRuntimeConfig userRuntimeConfig;

    @Inject
    public FSConfigService(
        ISubscriptionConfig subscriptionConfig,
        UserRuntimeConfig userRuntimeConfig
    ) {
        this.subscriptionConfig = subscriptionConfig;
        this.userRuntimeConfig = userRuntimeConfig;

        hostToKonnektorConfig = new ConcurrentHashMap<>();
    }

    public void onStart(@Observes @Priority(10) StartupEvent ev) {
        hostToKonnektorConfig.putAll(loadConfigs());
    }

    @Produces
    @KonnektorsConfigs
    public Map<String, KonnektorConfig> configMap() {
        return hostToKonnektorConfig;
    }

    @Override
    public Map<String, KonnektorConfig> loadConfigs() {
        List<KonnektorConfig> configs = new ArrayList<>();
        var konnektorConfigFolder = new File(configFolder);
        if (konnektorConfigFolder.exists()) {
            configs = readFromPath(konnektorConfigFolder.getAbsolutePath());
        }
        if (configs.isEmpty()) {
            int cetpServerDefaultPort = subscriptionConfig.getDefaultCetpServerPort();
            String cardlinkServer = subscriptionConfig.getDefaultCardLinkServer();
            configs.add(
                new KonnektorConfig(
                    konnektorConfigFolder,
                    cetpServerDefaultPort,
                    URI.create(cardlinkServer),
                    userRuntimeConfig.getUserConfigurations()
                )
            );
        }
        return configs.stream().collect(Collectors.toMap(this::getKonnectorKey, config -> config));
    }

    private String getKonnectorKey(KonnektorConfig config) {
        String konnectorHost = config.getHost();
        if (konnectorHost == null) {
            konnectorHost = userRuntimeConfig.getKonnektorHost();
        }
        String workplaceId = config.getUserConfigurations().getWorkplaceId();
        if (workplaceId == null) {
            workplaceId = userRuntimeConfig.getWorkplaceId();
        }
        return String.format("%s" + CONFIG_DELIMETER + "%s", konnectorHost, workplaceId);
    }

    public List<KonnektorConfig> readFromPath(String path) {
        File folderFile = new File(path);
        if (folderFile.exists() && folderFile.isDirectory()) {
            File[] files = folderFile.listFiles();
            if (files != null) {
                return Arrays.stream(files)
                    .filter(File::isDirectory)
                    .map(this::fromFolder)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(KonnektorConfig::getCetpPort))
                    .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private KonnektorConfig fromFolder(File folder) {
        File[] files = folder.listFiles();
        Optional<File> userPropertiesOpt = Optional.ofNullable(files).flatMap(list -> Arrays.stream(list)
            .filter(f -> f.getName().endsWith(PROPERTIES_EXT))
            .max(Comparator.comparingLong(File::lastModified)));

        Optional<File> subscriptionFileOpt = Optional.ofNullable(files).flatMap(list -> Arrays.stream(list)
            .filter(f -> !f.getName().startsWith(FAILED) && !f.getName().endsWith(PROPERTIES_EXT))
            .max(Comparator.comparingLong(File::lastModified)));

        if (userPropertiesOpt.isPresent()) {
            File actualSubscription = userPropertiesOpt.get();
            if (actualSubscription.exists()) {
                String subscriptionId = subscriptionFileOpt.map(File::getName).orElse(null);
                OffsetDateTime subscriptionTime = subscriptionFileOpt
                    .map(f -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault()))
                    .orElse(OffsetDateTime.now().minusDays(30)); // force subscribe if no subscription is found
                try (var fis = new FileInputStream(actualSubscription)) {
                    Properties properties = new Properties();
                    properties.load(fis);
                    KonnektorConfig konnektorConfig = new KonnektorConfig();
                    konnektorConfig.setCetpPort(Integer.parseInt(folder.getName()));
                    konnektorConfig.setUserConfigurations(new KCUserConfigurations(properties));
                    konnektorConfig.setCardlinkEndpoint(new URI(properties.getProperty("cardlinkServerURL")));
                    konnektorConfig.setSubscriptionId(subscriptionId);
                    konnektorConfig.setSubscriptionTime(subscriptionTime);
                    konnektorConfig.setFolder(folder);
                    return konnektorConfig;
                } catch (URISyntaxException | IOException e) {
                    String msg = String.format(
                        "Could not read konnektor config: folder=%s, subscriptionId=%s", folder.getName(), subscriptionId
                    );
                    log.log(Level.WARNING, msg, e);
                }
            }
        }
        return null;
    }

    @Override
    public void trackSubscriptionFile(KonnektorConfig konnektorConfig, String subscriptionId, String error) {
        try {
            writeFile(konnektorConfig.getFolder().getAbsolutePath() + "/" + subscriptionId, error);
            cleanUpSubscriptionFiles(konnektorConfig, subscriptionId);
            konnektorConfig.setSubscriptionId(subscriptionId);
            konnektorConfig.setSubscriptionTime(OffsetDateTime.now());
        } catch (IOException e) {
            String msg = String.format(
                "Error while recreating subscription properties in folder: %s",
                konnektorConfig.getFolder().getAbsolutePath()
            );
            log.log(Level.SEVERE, msg, e);
        }
    }

    @Override
    public void cleanUpSubscriptionFiles(KonnektorConfig konnektorConfig, String subscriptionId) {
        Optional.ofNullable(konnektorConfig.getFolder().listFiles()).ifPresent(files -> Arrays.stream(files)
            .filter(file -> !file.getName().equals(subscriptionId) && !file.getName().endsWith(PROPERTIES_EXT))
            .forEach(file -> {
                boolean deleted = file.delete();
                if (!deleted) {
                    String msg = String.format("Unable to delete previous subscription file: %s", file.getName());
                    log.log(Level.SEVERE, msg);
                    file.renameTo(new File(String.format("%s_DELETING", file.getAbsolutePath())));
                }
            }));
    }

    private static void writeFile(String absolutePath, String content) throws IOException {
        try (FileOutputStream os = new FileOutputStream(absolutePath)) {
            if (content != null) {
                os.write(content.getBytes());
            }
            os.flush();
        }
    }
}
