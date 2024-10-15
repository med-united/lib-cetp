package de.health.service.cetp.cardlink;

import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public interface CardlinkClient {

    void connect();

    Supplier<Boolean> connected();

    void sendJson(
        String correlationId,
        String iccsn,
        String type,
        Map<String, Object> payloadMap
    );

    void close();
}
