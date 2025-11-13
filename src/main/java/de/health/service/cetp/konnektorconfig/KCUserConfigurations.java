package de.health.service.cetp.konnektorconfig;

import de.health.service.cetp.config.KonnektorAuth;
import de.health.service.config.api.IUserConfigurations;
import lombok.Data;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"unused"})
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

    static BeanInfo beanInfo;

    static {
        try {
            beanInfo = Introspector.getBeanInfo(KCUserConfigurations.class);
        } catch (IntrospectionException e) {
            log.log(Level.SEVERE, "Could not process konnektor user configurations", e);
        }
    }

    public KCUserConfigurations(Properties properties) {
        fillValues(properties::getProperty);
    }

    private void fillValues(Function<String, Object> getValue) {
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            try {
                Method writeMethod = pd.getWriteMethod();
                if (writeMethod != null) {
                    Object value = getValue.apply(pd.getName());
                    if (value instanceof String stringValue) {
                        writeMethod.invoke(this, stringValue.isBlank() ? null : stringValue);
                    } else {
                        writeMethod.invoke(this, value);
                    }
                } else {
                    if (!"class".equals(pd.getName())) {
                        log.warning("No write method for: " + pd.getName());
                    }
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "Could not process konnektor user configurations", e);
            }
        }
    }

    @Override
    public KonnektorAuth getKonnektorAuth() {
        return KonnektorAuth.from(auth);
    }

    public Properties properties() {
        Properties properties = new Properties();
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            try {
                if (pd.getReadMethod().invoke(this) instanceof String str) {
                    properties.setProperty(pd.getName(), str);
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "Could not process konnektor user configurations", e);
            }
        }
        return properties;
    }
}
