package de.health.service.config.api;

public interface IFeatureConfig {

    boolean isMutualTlsEnabled();

    boolean isCetpEnabled();

    boolean isCardlinkEnabled();

    boolean isNativeFhirEnabled();
    
    boolean isExternalPnwEnabled();
}
