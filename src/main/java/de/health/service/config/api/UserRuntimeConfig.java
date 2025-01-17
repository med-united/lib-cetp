package de.health.service.config.api;

@SuppressWarnings("unused")
public interface UserRuntimeConfig extends IKonnektorUrlConfig {

    String getConnectorVersion();

    String getMandantId();

    String getWorkplaceId();

    String getClientSystemId();

    String getUserId();

    IUserConfigurations getUserConfigurations();

    IRuntimeConfig getRuntimeConfig();

    UserRuntimeConfig copy();

    void updateProperties(IUserConfigurations userConfigurations);
}
