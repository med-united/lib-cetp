# CETP Library (lib-cetp)

Manages subscriptions for Konnektor CETP events. Introduces general adapter interfaces for:
- Konnektor client which is used for different telematik-api SOAP interfaces versions
- Runtime/User/App configurations

> [!IMPORTANT]
> lib-cetp consumer services must implement configuration interfaces from the following [package](src/main/java/de/health/service/cetp/config) and [IKonnektorClient](src/main/java/de/health/service/cetp/IKonnektorClient.java) with proper @Mappers from request/response XSD objects to the domain [model](src/main/java/de/health/service/cetp/domain).

## Additional interfaces

There are several of additional interfaces which must be implemented by lib-cetp consumer services:
- [CETPEventDecoderFactory](src/main/java/de/health/service/cetp/codec/CETPEventDecoderFactory.java) - service must know how to decode CETP event based on telematik-api version
- [CETPEventHandlerFactory](src/main/java/de/health/service/cetp/CETPEventHandlerFactory.java) - service must provide handling logic of the CETP event
- [FallbackSecretsManager](src/main/java/de/health/service/cetp/FallbackSecretsManager.java) - if konnektor config doesn't contain client certificate properties then fallback KeyManagerFactory can be used. 