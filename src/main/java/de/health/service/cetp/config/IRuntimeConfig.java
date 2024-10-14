package de.health.service.cetp.config;

@SuppressWarnings("unused")
public interface IRuntimeConfig {

    String getEHBAHandle();

    String getSMCBHandle();

    boolean isSendPreview();

    String getIdpAuthRequestRedirectURL();

    String getIdpClientId();
}
