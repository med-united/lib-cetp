package de.health.service.cetp;

import de.health.service.cetp.codec.CETPEventDecoderFactory;
import de.servicehealth.config.KonnektorConfig;
import de.servicehealth.config.api.ISubscriptionConfig;
import de.servicehealth.config.api.IUserConfigurations;
import de.servicehealth.utils.SSLResult;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.EventExecutorGroup;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.Getter;

import javax.net.ssl.KeyManagerFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.servicehealth.utils.SSLUtils.getClientCertificateBytes;
import static de.servicehealth.utils.SSLUtils.initSSLContext;

@SuppressWarnings({"CdiInjectionPointsInspection", "unused"})
@ApplicationScoped
@Startup
public class CETPServer {

    public static final int DEFAULT_PORT = 8585;

    private static final Logger log = Logger.getLogger(CETPServer.class.getName());

    List<EventLoopGroup> bossGroups = new ArrayList<>();
    List<EventLoopGroup> workerGroups = new ArrayList<>();

    @Getter
    private final Map<String, String> startedOnPorts = new HashMap<>();

    ISubscriptionConfig subscriptionConfig;
    SubscriptionManager subscriptionManager;
    FallbackSecretsManager fallbackSecretsManager;

    CETPEventDecoderFactory eventDecoderFactory;
    CETPEventHandlerFactory eventHandlerFactory;

    @Inject
    public CETPServer(
        ISubscriptionConfig subscriptionConfig,
        SubscriptionManager subscriptionManager,
        CETPEventDecoderFactory eventDecoderFactory,
        CETPEventHandlerFactory eventHandlerFactory,
        FallbackSecretsManager fallbackSecretsManager
    ) {
        this.subscriptionConfig = subscriptionConfig;
        this.subscriptionManager = subscriptionManager;
        this.eventDecoderFactory = eventDecoderFactory;
        this.eventHandlerFactory = eventHandlerFactory;
        this.fallbackSecretsManager = fallbackSecretsManager;
    }

    // Make sure subscription manager get onStart first, before CETPServer at least!
    void onStart(@Observes @Priority(5200) StartupEvent ev) {
        run();
    }

    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("Shutdown CETP Server on port " + subscriptionConfig.getDefaultCetpServerPort());
        if (workerGroups != null) {
            workerGroups.stream().filter(Objects::nonNull).forEach(EventExecutorGroup::shutdownGracefully);
        }
        if (bossGroups != null) {
            bossGroups.stream().filter(Objects::nonNull).forEach(EventExecutorGroup::shutdownGracefully);
        }
    }

    public void run() {
        for (KonnektorConfig config : subscriptionManager.getKonnektorConfigs(null)) {
            log.info("Running CETP Server on port " + config.getCetpPort() + " for cardlink server: " + config.getCardlinkEndpoint());
            runServer(config);
        }
    }

    private void runServer(KonnektorConfig config) {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        bossGroups.add(bossGroup);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        workerGroups.add(workerGroup);
        Integer port = config.getCetpPort();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class) // (3)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                    @Override
                    public void initChannel(SocketChannel ch) {
                        try {
                            SslContext sslContext = SslContextBuilder
                                .forServer(getKeyFactoryManager(config))
                                .clientAuth(ClientAuth.NONE)
                                .build();

                            ch.pipeline()
                                .addLast("ssl", sslContext.newHandler(ch.alloc()))
                                .addLast("logging", new LoggingHandler(LogLevel.DEBUG))
                                .addLast(new LengthFieldBasedFrameDecoder(65536, 4, 4, 0, 0))
                                .addLast(eventDecoderFactory.build(config.getUserConfigurations()))
                                .addLast(eventHandlerFactory.build(config));
                        } catch (Exception e) {
                            log.log(Level.WARNING, "Failed to create SSL context", e);
                        }
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.

            ChannelFuture f = b.bind(port).sync(); // (7)
            startedOnPorts.put(port.toString(), "STARTED");

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture(); //.sync();
        } catch (InterruptedException e) {
            startedOnPorts.put(port.toString(), String.format("FAILED: %s", e.getMessage()));
            log.log(Level.WARNING, "CETP Server interrupted", e);
        }
    }

    private KeyManagerFactory getKeyFactoryManager(KonnektorConfig config) {
        IUserConfigurations userConfigurations = config.getUserConfigurations();
        if (userConfigurations.getClientCertificate() == null) {
            return fallbackSecretsManager.getKeyManagerFactory();
        } else {
            byte[] clientCertificateBytes = getClientCertificateBytes(userConfigurations);
            try (ByteArrayInputStream certInputStream = new ByteArrayInputStream(clientCertificateBytes)) {
                SSLResult sslResult = initSSLContext(certInputStream, userConfigurations.getClientCertificatePassword());
                return sslResult.getKeyManagerFactory();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Could not create keyManagerFactory", e);
            }
        }
        return null;
    }
}

