package de.health.service.cetp;

import de.health.service.cetp.domain.CetpStatus;
import de.health.service.cetp.domain.SubscriptionResult;
import de.health.service.cetp.domain.cardterminal.EgkHandle;
import de.health.service.cetp.domain.eventservice.Subscription;
import de.health.service.cetp.domain.eventservice.card.Card;
import de.health.service.cetp.domain.eventservice.card.CardType;
import de.health.service.cetp.domain.eventservice.cardTerminal.CardTerminal;
import de.health.service.cetp.domain.fault.CetpFault;
import de.health.service.config.api.UserRuntimeConfig;

import java.security.cert.X509Certificate;
import java.util.List;

@SuppressWarnings("unused")
public interface IKonnektorClient {

    String CARD_INSERTED_TOPIC = "CARD/INSERTED";

    boolean isReady();

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

    X509Certificate getHbaX509Certificate(UserRuntimeConfig userRuntimeConfig, String hbaHandle) throws CetpFault;

    String getTelematikId(UserRuntimeConfig userRuntimeConfig, String smcbHandle);

    String getSmcbHandle(UserRuntimeConfig userRuntimeConfig) throws CetpFault;

    String getKvnr(UserRuntimeConfig userRuntimeConfig, String egkHandle) throws CetpFault;

    EgkHandle getEgkHandle(UserRuntimeConfig userRuntimeConfig, String insurantId) throws CetpFault;

    String ejectEgkCard(UserRuntimeConfig userRuntimeConfig, EgkHandle egkHandle) throws CetpFault;
}
