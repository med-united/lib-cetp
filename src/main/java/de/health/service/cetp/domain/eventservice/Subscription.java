package de.health.service.cetp.domain.eventservice;

import lombok.Data;

import java.util.Date;

@Data
public class Subscription {

    private String subscriptionId;

    private Date terminationTime;

    private String eventTo;

    private String topic;
}
