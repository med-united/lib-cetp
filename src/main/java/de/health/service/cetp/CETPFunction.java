package de.health.service.cetp;

import de.health.service.config.api.UserRuntimeConfig;

@FunctionalInterface
public interface CETPFunction<K, V> {
    V apply(K key, UserRuntimeConfig userRuntimeConfig) throws Exception;
}
