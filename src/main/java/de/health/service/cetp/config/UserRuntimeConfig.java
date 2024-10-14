package de.health.service.cetp.config;

@SuppressWarnings("unused")
public interface UserRuntimeConfig {

    String getConnectorBaseURL();

    String getConnectorVersion();

    String getMandantId();

    String getWorkplaceId();

    String getClientSystemId();

    String getUserId();

    IUserConfigurations getConfigurations();

    IRuntimeConfig getRuntimeConfig();

    UserRuntimeConfig copy();

    void updateProperties(IUserConfigurations userConfigurations);
}
