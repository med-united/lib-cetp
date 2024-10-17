package de.health.service.cetp.domain.eventservice.event;

import lombok.Getter;

@Getter
public enum CetpSeverityType {

    INFO("Info"),

    WARNING("Warning"),

    ERROR("Error"),
    
    FATAL("Fatal");

    private final String value;

    CetpSeverityType(String value) {
        this.value = value;
    }
}
