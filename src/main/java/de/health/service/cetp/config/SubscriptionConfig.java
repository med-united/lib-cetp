package de.health.service.cetp.config;

import java.util.Optional;

public interface SubscriptionConfig {

    int getSubscriptionsMaintenanceRetryIntervalMs();

    int getCetpSubscriptionsRenewalSafePeriodMs();

    int getForceResubscribePeriodSeconds();

    Optional<String> getCardLinkServer(); // default -> used when no config/konnektoren/{port} is absent

    Optional<String> getEventToHost();

    int getCetpPort(); // default -> used when no config/konnektoren/{port} is absent
}
