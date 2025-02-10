package de.health.service.cetp.domain.fault;

import lombok.Getter;

import java.io.Serial;

@Getter
public class CetpFault extends Exception {

    @Serial
    private static final long serialVersionUID = -1636108370897458787L;
    
    private Error faultInfo;

    public CetpFault() {
        super();
    }

    public CetpFault(String message) {
        super(message);
    }

    public CetpFault(String message, java.lang.Throwable cause) {
        super(message, cause);
    }

    public CetpFault(String message, Error error) {
        super(message);
        this.faultInfo = error;
    }

    public CetpFault(String message, Error error, java.lang.Throwable cause) {
        super(message, cause);
        this.faultInfo = error;
    }
}
