package de.health.service.cetp;

import de.health.service.cetp.codec.CETPEventDecoderFactory;
import de.health.service.cetp.config.KonnektorConfig;
import de.health.service.config.api.IFeatureConfig;
import de.health.service.config.api.ISubscriptionConfig;
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
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    IFeatureConfig featureConfig;
    ISubscriptionConfig subscriptionConfig;
    SubscriptionManager subscriptionManager;
    ISecretsManager secretsManager;

    CETPEventDecoderFactory eventDecoderFactory;
    CETPEventHandlerFactory eventHandlerFactory;

    @Inject
    public CETPServer(
        IFeatureConfig featureConfig,
        ISubscriptionConfig subscriptionConfig,
        SubscriptionManager subscriptionManager,
        CETPEventDecoderFactory eventDecoderFactory,
        CETPEventHandlerFactory eventHandlerFactory,
        ISecretsManager secretsManager
    ) {
        this.featureConfig = featureConfig;
        this.subscriptionConfig = subscriptionConfig;
        this.subscriptionManager = subscriptionManager;
        this.eventDecoderFactory = eventDecoderFactory;
        this.eventHandlerFactory = eventHandlerFactory;
        this.secretsManager = secretsManager;
    }

    // Make sure subscription manager get onStart first, before CETPServer at least!
    void onStart(@Observes @Priority(5200) StartupEvent ev) {
        if (featureConfig.isCetpEnabled()) {
            StopWatch watch = StopWatch.createStarted();
            try {
                run();
            } finally {
                watch.stop();
                log.info(String.format("%s %s took %s", "CETPServer", "run", watch.formatTime()));
            }
        } else {
            log.warning("CETP feature is disabled, please check 'feature.cetp.enabled' property");
        }
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

    private void run() {
        for (KonnektorConfig config : subscriptionManager.getKonnektorConfigs(null, null)) {
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
                                .forServer(secretsManager.getKeyManagerFactory(config))
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
}

