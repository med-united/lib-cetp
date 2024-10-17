package de.health.service.cetp.domain.eventservice.event;

import de.servicehealth.config.api.IUserConfigurations;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DecodeResult {

    private CetpEvent event;

    private IUserConfigurations configurations;
}
