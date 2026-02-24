package de.health.service.cetp.beaninfo;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BeanInfoHelper {

    private static final Logger log = Logger.getLogger(BeanInfoHelper.class.getName());

    private final BeanInfo beanInfo;

    public BeanInfoHelper(BeanInfo beanInfo) {
        this.beanInfo = beanInfo;
    }

    public void fillValues(Object target, Function<String, Object> getValue) {
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            try {
                Method writeMethod = pd.getWriteMethod();
                if (writeMethod != null) {
                    Object value = getValue.apply(pd.getName());
                    if (value instanceof String stringValue) {
                        writeMethod.invoke(target, stringValue.isBlank() ? null : stringValue);
                    } else {
                        writeMethod.invoke(target, value);
                    }
                } else {
                    if (!"class".equals(pd.getName())) {
                        Method readMethod = pd.getReadMethod();
                        if (readMethod != null && readMethod.getAnnotation(Synthetic.class) == null) {
                            log.warning("No write method for: " + pd.getName());
                        }
                    }
                }
            } catch (Exception e) {
                String beanClassName = beanInfo.getBeanDescriptor().getBeanClass().getSimpleName();
                log.log(Level.SEVERE, String.format("Could not fill values for %s properties", beanClassName), e);
            }
        }
    }

    public Properties properties(Object target) {
        Properties properties = new Properties();
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            try {
                if (pd.getReadMethod().invoke(target) instanceof String str) {
                    properties.setProperty(pd.getName(), str);
                }
            } catch (Exception e) {
                String beanClassName = beanInfo.getBeanDescriptor().getBeanClass().getSimpleName();
                log.log(Level.SEVERE, String.format("Could not read values %s properties", beanClassName), e);
            }
        }
        return properties;
    }
}
