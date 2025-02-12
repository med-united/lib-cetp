package de.health.service.check;

import de.health.service.config.api.IRuntimeConfig;

import java.util.Map;

public interface Check {

    String CARDLINK_WEBSOCKET_CHECK = "CardlinkWebsocketCheck";
    String CETP_SERVER_CHECK = "CETPServerCheck";
    String STATUS_CHECK = "StatusCheck";
    String GIT_CHECK = "GitCheck";

    String getName();

    Status getStatus(IRuntimeConfig runtimeConfig);

    default Status getSafeStatus(IRuntimeConfig runtimeConfig) {
        try {
            return getStatus(runtimeConfig);
        } catch (Throwable t) {
            return Status.Down500;
        }
    }

    Map<String, String> getData(IRuntimeConfig runtimeConfig);
}
