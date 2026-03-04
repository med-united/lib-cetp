package de.health.service.cetp.konnektorconfig;

import de.health.service.cetp.beaninfo.BeanInfoHelper;
import de.health.service.config.api.IUserConfigurations;
import lombok.Data;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.Properties;

@Data
public class UserConfigurations implements IUserConfigurations {

    private String erixaHotfolder;
    private String erixaDrugstoreEmail;
    private String erixaUserEmail;
    private String erixaUserPassword;
    private String erixaApiKey;
    private String muster16TemplateProfile;
    private String connectorBaseURL;
    private String iccsn;
    private String mandantId;
    private String workplaceId;
    private String clientSystemId;
    private String userId;
    private String tvMode;
    private String clientCertificate;
    private String clientCertificatePassword;
    private String auth;
    private String basicAuthUsername;
    private String basicAuthPassword;
    private String pruefnummer;
    private String version;

    static BeanInfoHelper beanInfoHelper;

    static {
        try {
            beanInfoHelper = new BeanInfoHelper(Introspector.getBeanInfo(UserConfigurations.class));
        } catch (IntrospectionException ignored) {
        }
    }

    public UserConfigurations() {
    }

    public UserConfigurations(Properties properties) {
        beanInfoHelper.fillValues(this, properties::getProperty);
    }

    public Properties properties() {
        return beanInfoHelper.properties(this);
    }
}
