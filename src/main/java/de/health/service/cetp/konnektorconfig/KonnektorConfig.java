package de.health.service.cetp.konnektorconfig;

import de.health.service.cetp.config.IUserConfigurations;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.concurrent.Semaphore;

@Getter
public class KonnektorConfig {

    File folder;
    Integer cetpPort;
    URI cardlinkEndpoint;
    IUserConfigurations userConfigurations;

    @Setter
    String subscriptionId;
    @Setter
    OffsetDateTime subscriptionTime;

    private final Semaphore semaphore = new Semaphore(1);

    public KonnektorConfig() {
    }

    public KonnektorConfig(
        File folder,
        Integer cetpPort,
        IUserConfigurations userConfigurations,
        URI cardlinkEndpoint
    ) {
        this.folder = folder;
        this.cetpPort = cetpPort;
        this.userConfigurations = userConfigurations;
        this.cardlinkEndpoint = cardlinkEndpoint;

        subscriptionId = null;
        subscriptionTime = OffsetDateTime.now().minusDays(30);
    }

    public String getHost() {
        String connectorBaseURL = userConfigurations.getConnectorBaseURL();
        return connectorBaseURL == null ? null : connectorBaseURL.split("//")[1];
    }
}

