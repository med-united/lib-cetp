package de.health.service.cetp.config;

public enum KonnektorAuth {

    BASIC, CERTIFICATE;

    public static KonnektorAuth from(String value) {
        try {
            return KonnektorAuth.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return CERTIFICATE;
        }
    }
}