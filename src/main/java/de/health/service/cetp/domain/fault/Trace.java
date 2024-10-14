package de.health.service.cetp.domain.fault;

import lombok.Data;

import java.math.BigInteger;

@Data
public class Trace {

    String eventId;

    String instance;

    String logReference;

    String compType;

    BigInteger code;

    String severity;

    String errorType;

    String errorText;

    Detail detail;
}
