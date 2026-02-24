package de.health.service.cetp.konnektorconfig;

import de.health.service.cetp.beaninfo.BeanInfoHelper;
import de.health.service.cetp.beaninfo.Synthetic;
import de.health.service.cetp.config.KonnektorAuth;
import de.health.service.config.api.IUserConfigurations;
import lombok.Data;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
public class KCUserConfigurations implements IUserConfigurations {

    private static final Logger log = Logger.getLogger(KCUserConfigurations.class.getName());

    private String erixaHotfolder;
    private String erixaDrugstoreEmail;
    private String erixaUserEmail;
    private String erixaUserPassword;
    private String erixaApiKey;
    private String muster16TemplateProfile;
    private String connectorBaseURL;
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
            beanInfoHelper = new BeanInfoHelper(Introspector.getBeanInfo(KCUserConfigurations.class));
        } catch (IntrospectionException e) {
            log.log(Level.SEVERE, "Could not process konnektor user configurations", e);
        }
    }

    public KCUserConfigurations(Properties properties) {
        beanInfoHelper.fillValues(this, properties::getProperty);
    }

    public Properties properties() {
        return beanInfoHelper.properties(this);
    }

    @Override
    @Synthetic
    public KonnektorAuth getKonnektorAuth() {
        return KonnektorAuth.from(auth);
    }
}
