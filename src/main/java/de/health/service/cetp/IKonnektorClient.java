package de.health.service.cetp;

import de.health.service.cetp.config.UserRuntimeConfig;
import de.health.service.cetp.domain.CetpStatus;
import de.health.service.cetp.domain.SubscriptionResult;
import de.health.service.cetp.domain.eventservice.Subscription;
import de.health.service.cetp.domain.fault.CetpFault;

import java.util.List;

@SuppressWarnings("unused")
public interface IKonnektorClient {

    String CARD_INSERTED_TOPIC = "CARD/INSERTED";

    List<Subscription> getSubscriptions(UserRuntimeConfig runtimeConfig) throws CetpFault;

    SubscriptionResult renewSubscription(
        UserRuntimeConfig runtimeConfig, String subscriptionId
    ) throws CetpFault;

    SubscriptionResult subscribe(UserRuntimeConfig runtimeConfig, String cetpHost) throws CetpFault;

    CetpStatus unsubscribe(
        UserRuntimeConfig runtimeConfig, String subscriptionId, String cetpHost, boolean forceCetp
    ) throws CetpFault;
}
