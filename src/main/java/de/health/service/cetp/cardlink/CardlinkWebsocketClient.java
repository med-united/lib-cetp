package de.health.service.cetp.cardlink;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.xml.bind.DatatypeConverter;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
@ClientEndpoint
public class CardlinkWebsocketClient extends Endpoint implements CardlinkClient {

    private static final Logger log = Logger.getLogger(CardlinkWebsocketClient.class.getName());

    URI endpointURI;
    Session userSession;
    WebSocketContainer container;
    JwtConfigurator jwtConfigurator;

    private final AtomicBoolean connected = new AtomicBoolean(false);

    public CardlinkWebsocketClient() {
    }

    public CardlinkWebsocketClient(URI endpointURI, JwtConfigurator jwtConfigurator) {
        try {
            this.endpointURI = endpointURI;
            this.jwtConfigurator = jwtConfigurator;
            container = ContainerProvider.getWebSocketContainer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Supplier<Boolean> connected() {
        return connected::get;
    }

    @OnOpen
    @Override
    public void onOpen(Session userSession, EndpointConfig config) {
        log.info("opening websocket to " + endpointURI);
        this.userSession = userSession;
        connected.set(true);
    }

    @SuppressWarnings("resource")
    public void connect() {
        try {
            ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
                .configurator(jwtConfigurator)
                .build();
            container.connectToServer(this, endpointConfig, endpointURI);
        } catch (DeploymentException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason      the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        log.info("closing websocket " + endpointURI);
        connected.set(false);
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        log.fine(message);
    }

    public void sendJson(String correlationId, String iccsn, String type, Map<String, Object> payloadMap) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder().add("type", type);
        if (!payloadMap.isEmpty()) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            for (Map.Entry<String, ?> entry : payloadMap.entrySet()) {
                if (entry.getValue() instanceof Integer intValue) {
                    builder.add(entry.getKey(), intValue);
                } else if (entry.getValue() instanceof Long longValue) {
                    builder.add(entry.getKey(), longValue);
                } else if (entry.getValue() instanceof String stringValue) {
                    if (stringValue.equalsIgnoreCase("null")) {
                        builder.add(entry.getKey(), JsonValue.NULL);
                    } else {
                        builder.add(entry.getKey(), stringValue);
                    }
                } else if (entry.getValue() instanceof JsonArrayBuilder jsonArrayBuilderValue) {
                    builder.add(entry.getKey(), jsonArrayBuilderValue);
                }
            }
            String payload = builder.build().toString();
            objectBuilder.add("payload", DatatypeConverter.printBase64Binary(payload.getBytes()));
        }
        JsonObject jsonObject = objectBuilder.build();
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder()
            .add(jsonObject)
            .add(JsonValue.NULL)
            .add(correlationId);
        if (iccsn != null) {
            jsonArrayBuilder.add(iccsn);
        }
        JsonArray jsonArray = jsonArrayBuilder.build();
        sendMessage(jsonArray.toString(), correlationId);
    }

    public void sendMessage(String message, String correlationId) {
        try {
            this.userSession.getBasicRemote().sendText(message);
            log.fine(String.format("[%s] WS message is sent to cardlink: %s", correlationId, message));
        } catch (Throwable e) {
            log.log(Level.WARNING, String.format("[%s] Could not send WS message to cardlink: %s", correlationId, message), e);
        }
    }

    public void close() {
        try {
            // Close might be called even before @onOpen was called, hence userSession might be null.
            if (this.userSession != null) {
                userSession.close();
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not close websocket session", e);
        }
    }
}