package de.health.service.cetp.config;

@SuppressWarnings("unused")
public interface IUserConfigurations {

    String getBasicAuthUsername();

    String getBasicAuthPassword();

    String getClientCertificate();

    String getClientCertificatePassword();

    String getErixaHotfolder();

    String getErixaDrugstoreEmail();

    String getErixaUserEmail();

    String getErixaApiKey();

    String getMuster16TemplateProfile();

    String getConnectorBaseURL();

    String getMandantId();

    String getWorkplaceId();

    String getClientSystemId();

    String getUserId();

    String getVersion();

    void setVersion(String version);

    String getTvMode();
}
