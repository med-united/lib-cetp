package de.health.service.cetp.domain.eventservice.event;

import lombok.Getter;

@Getter
public enum CetpEventType {

    OPERATION("Operation"),

    SECURITY("Security"),

    INFRASTRUCTURE("Infrastructure"),

    BUSINESS("Business"),

    OTHER("Other");

    private final String value;

    CetpEventType(String value) {
        this.value = value;
    }
}
