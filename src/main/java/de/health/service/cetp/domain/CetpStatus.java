package de.health.service.cetp.domain;

import de.health.service.cetp.domain.fault.Error;
import lombok.Data;

@Data
public class CetpStatus {

    private String result;
    
    private Error error;
}
