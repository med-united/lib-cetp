package de.health.service.check;

import lombok.Getter;

@Getter
public enum Status {

    Up200(200),
    Down503(503),
    Down500(500);

    Status(int code) {
        this.code = code;
    }

    private final int code;

    public String toSingleStatusString() {
        return this.equals(Up200) ? "UP" : "DOWN";
    }
}
