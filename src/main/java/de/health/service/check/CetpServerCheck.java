package de.health.service.check;

import de.health.service.cetp.CETPServer;
import de.health.service.config.api.IRuntimeConfig;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
@IfBuildProperty(name = "feature.cetp.enabled", stringValue = "true", enableIfMissing = true)
public class CetpServerCheck implements Check {

    @Inject
    CETPServer cetpServer;

    @Override
    public String getName() {
        return CETP_SERVER_CHECK;
    }

    @Override
    public Status getStatus(IRuntimeConfig runtimeConfig) {
        Map<String, String> startedOnPorts = cetpServer.getStartedOnPorts();
        boolean someFailed = startedOnPorts.values().stream().anyMatch(s -> s.startsWith("FAILED"));
        return someFailed || startedOnPorts.isEmpty() ? Status.Down503 : Status.Up200;
    }

    @Override
    public Map<String, String> getData(IRuntimeConfig runtimeConfig) {
        return cetpServer.getStartedOnPorts();
    }
}
