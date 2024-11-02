package de.health.service.config.api;

public interface ISubscriptionConfig {

    int getCetpSubscriptionsMaintenanceRetryIntervalMs();

    int getCetpSubscriptionsRenewalSafePeriodMs();

    int getForceResubscribePeriodSeconds();

    String getDefaultCardLinkServer();

    String getDefaultEventToHost();

    int getDefaultCetpServerPort();
}
