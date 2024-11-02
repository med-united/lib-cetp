package de.health.service.cetp.konnektorconfig;

import de.health.service.cetp.config.KonnektorConfig;

import java.util.Map;

public interface KonnektorConfigService {

    Map<String, KonnektorConfig> loadConfigs();

    void trackSubscriptionFile(KonnektorConfig konnektorConfig, String subscriptionId, String error);

    void cleanUpSubscriptionFiles(KonnektorConfig konnektorConfig, String subscriptionId);
}