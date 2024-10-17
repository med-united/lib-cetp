package de.health.service.cetp.domain.eventservice.event;

import lombok.Data;

import java.util.List;

@Data
public class CetpEvent {

    private String topic;

    private CetpEventType type;

    private CetpSeverityType severity;

    private String subscriptionId;

    private List<CetpParameter> parameters;
}
