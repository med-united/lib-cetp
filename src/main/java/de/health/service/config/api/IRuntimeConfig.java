package de.health.service.config.api;

@SuppressWarnings("unused")
public interface IRuntimeConfig {

    String getIccsn();

    void setIccsn(String iccsn);

    String getEHBAHandle();

    String getSMCBHandle();

    void setEHBAHandle(String eHBAHandle);

    void setSMCBHandle(String smcbHandle);

    boolean isSendPreview();

    String getIdpAuthRequestRedirectURL();

    String getIdpClientId();
}
