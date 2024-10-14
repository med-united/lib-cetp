package de.health.service.cetp.domain.fault;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class Error {

    String messageId;

    Date timestamp;

    List<Trace> trace;
}
