package de.health.service.check;

import de.health.service.cetp.CETPServer;
import de.health.service.config.api.IFeatureConfig;
import de.health.service.config.api.IRuntimeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@SuppressWarnings("CdiInjectionPointsInspection")
@ApplicationScoped
public class CetpServerCheck implements Check {

    @Inject
    CETPServer cetpServer;

    @Inject
    IFeatureConfig featureConfig;

    @Override
    public String getName() {
        return CETP_SERVER_CHECK;
    }

    @Override
    public Status getStatus(IRuntimeConfig runtimeConfig) {
        if (featureConfig.isCetpEnabled()) {
            Map<String, Object> startedOnPorts = cetpServer.getStartedOnPorts();
            boolean someFailed = startedOnPorts.values().stream().anyMatch(s -> String.valueOf(s).startsWith("FAILED"));
            return someFailed || startedOnPorts.isEmpty() ? Status.Down503 : Status.Up200;
        } else {
            return Status.Down503;
        }
    }

    @Override
    public Map<String, Object> getData(IRuntimeConfig runtimeConfig) {
        if (featureConfig.isCetpEnabled()) {
            return cetpServer.getStartedOnPorts();
        } else {
            return Map.of("No port bind", "CETP feature is disabled");
        }
    }
}
