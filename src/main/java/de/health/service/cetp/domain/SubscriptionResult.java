package de.health.service.cetp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class SubscriptionResult {

    CetpStatus status;

    String subscriptionId;
    
    Date terminationTime;
}