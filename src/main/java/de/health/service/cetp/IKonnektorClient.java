package de.health.service.cetp;

import de.health.service.cetp.domain.CetpStatus;
import de.health.service.cetp.domain.SubscriptionResult;
import de.health.service.cetp.domain.eventservice.Subscription;
import de.health.service.cetp.domain.eventservice.card.Card;
import de.health.service.cetp.domain.eventservice.card.CardType;
import de.health.service.cetp.domain.eventservice.cardTerminal.CardTerminal;
import de.health.service.cetp.domain.fault.CetpFault;
import de.health.service.config.api.UserRuntimeConfig;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public interface IKonnektorClient {

    String CARD_INSERTED_TOPIC = "CARD/INSERTED";

    List<Subscription> getSubscriptions(UserRuntimeConfig runtimeConfig) throws CetpFault;

    SubscriptionResult renewSubscription(UserRuntimeConfig runtimeConfig, String subscriptionId) throws CetpFault;

    SubscriptionResult subscribe(UserRuntimeConfig runtimeConfig, String cetpHost) throws CetpFault;

    CetpStatus unsubscribe(
        UserRuntimeConfig runtimeConfig,
        String subscriptionId,
        String cetpHost,
        boolean forceCetp
    ) throws CetpFault;

    List<Card> getCards(UserRuntimeConfig runtimeConfig, CardType cardType) throws CetpFault;

    List<CardTerminal> getCardTerminals(UserRuntimeConfig runtimeConfig) throws CetpFault;

    CertificateInfo getSmcbX509Certificate(UserRuntimeConfig userRuntimeConfig, String smcbHandle) throws CetpFault;

    String getTelematikId(UserRuntimeConfig userRuntimeConfig, String smcbHandle);

    String getSmcbHandle(UserRuntimeConfig userRuntimeConfig) throws CetpFault;

    String getKvnr(UserRuntimeConfig userRuntimeConfig, String egkHandle) throws CetpFault;

    String getEgkHandle(UserRuntimeConfig userRuntimeConfig, String insurantId) throws CetpFault;

    default <K, V> V computeIfAbsentCetpEx(
        Map<K, V> map,
        K key,
        UserRuntimeConfig userRuntimeConfig,
        CETPFunction<? super K, ? extends V> mappingFunction
    ) throws CetpFault {
        try {
            return map.computeIfAbsent(key, k -> {
                try {
                    return mappingFunction.apply(k, userRuntimeConfig);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (IllegalStateException e) {
            if (e.getCause() instanceof CetpFault cetpFault) {
                throw cetpFault;
            } else {
                throw new CetpFault(e.getMessage());
            }
        }
    }
}
