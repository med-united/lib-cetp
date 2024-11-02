package de.health.service.cetp;

import de.health.service.cetp.config.KonnektorConfig;

import javax.net.ssl.KeyManagerFactory;

public interface ISecretsManager {

    KeyManagerFactory getKeyManagerFactory(KonnektorConfig config);
}
