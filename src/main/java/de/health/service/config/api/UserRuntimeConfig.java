package de.health.service.config.api;

@SuppressWarnings("unused")
public interface UserRuntimeConfig extends IKonnektorUrlConfig {

    String getConnectorVersion();

    @Deprecated
    String getMandantId();

    @Deprecated
    String getWorkplaceId();

    @Deprecated
    String getClientSystemId();

    @Deprecated
    String getUserId();

    IUserConfigurations getUserConfigurations();

    IRuntimeConfig getRuntimeConfig();

    UserRuntimeConfig copy();

    void updateProperties(IUserConfigurations userConfigurations);
}
