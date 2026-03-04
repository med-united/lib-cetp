package de.health.service.config.api;

@SuppressWarnings("unused")
public interface IRuntimeConfig {

    String getIdpClientId();

    String getIdpAuthRequestRedirectURL();

    String getEHBAHandle();

    String getSMCBHandle();

    void setEHBAHandle(String eHBAHandle);

    void setSMCBHandle(String smcbHandle);

    boolean isSendPreview();
}
