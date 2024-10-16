package de.health.service.cetp.cardlink;

import de.servicehealth.epa4all.config.api.UserRuntimeConfig;
import jakarta.websocket.ClientEndpointConfig;

public abstract class JwtConfigurator extends ClientEndpointConfig.Configurator {

    protected UserRuntimeConfig userRuntimeConfig;

    public JwtConfigurator(UserRuntimeConfig userRuntimeConfig) {
        this.userRuntimeConfig = userRuntimeConfig;
    }
}