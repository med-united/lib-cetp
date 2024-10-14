package de.health.service.cetp;

import javax.net.ssl.KeyManagerFactory;

public interface FallbackSecretsManager {

    KeyManagerFactory getKeyManagerFactory();
}
