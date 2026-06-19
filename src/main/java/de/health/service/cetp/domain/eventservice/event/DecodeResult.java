package de.health.service.cetp.domain.eventservice.event;

import de.health.service.config.api.IUserConfigurations;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DecodeResult {

    private CetpEvent event;

    private IUserConfigurations configurations;

    // raw CETP Event XML source, nullable for backward compatibility
    private String eventXml;

    public DecodeResult(CetpEvent event, IUserConfigurations configurations) {
        this(event, configurations, null);
    }
}
