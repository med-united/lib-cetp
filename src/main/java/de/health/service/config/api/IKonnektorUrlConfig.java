package de.health.service.config.api;

import de.health.service.cetp.beaninfo.Synthetic;

import java.net.URI;

public interface IKonnektorUrlConfig {

    String getConnectorBaseURL();

    @Synthetic
    default String getKonnektorHost() {
        String connectorBaseURL = getConnectorBaseURL();
        if (connectorBaseURL == null) {
            return null;
        }
        try {
            String host = URI.create(connectorBaseURL).getHost();
            return host != null ? host : getHostFromProperty(connectorBaseURL);
        } catch (Exception e) {
            return getHostFromProperty(connectorBaseURL);
        }
    }

    default String getHostFromProperty(String propertyUrl) {
        String host = propertyUrl;
        if (propertyUrl.contains("//")) {
            host = propertyUrl.split("//")[1].trim();
        }
        if (host.contains(":")) {
            host = host.split(":")[0].trim();
        }
        if (host.contains("/")) {
            host = host.split("/")[0].trim();
        }
        return host;
    }
}