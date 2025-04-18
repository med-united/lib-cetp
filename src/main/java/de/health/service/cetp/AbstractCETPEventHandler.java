package de.health.service.cetp;

import de.health.service.cetp.cardlink.CardlinkClient;
import de.health.service.cetp.domain.eventservice.event.CetpEvent;
import de.health.service.cetp.domain.eventservice.event.CetpParameter;
import de.health.service.cetp.domain.eventservice.event.DecodeResult;
import de.health.service.config.api.IUserConfigurations;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractCETPEventHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = Logger.getLogger(AbstractCETPEventHandler.class.getName());

    protected CardlinkClient cardlinkClient;

    public AbstractCETPEventHandler(CardlinkClient cardlinkClient) {
        this.cardlinkClient = cardlinkClient;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
    }

    protected abstract String getTopicName();

    protected abstract void processEvent(IUserConfigurations configurations, Map<String, String> paramsMap);

    protected Map<String, String> getParams(CetpEvent event) {
        return event.getParameters().stream().collect(Collectors.toMap(CetpParameter::getKey, CetpParameter::getValue));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        DecodeResult decodeResult = (DecodeResult) msg;
        CetpEvent event = decodeResult.getEvent();
        if (event.getTopic().equals(getTopicName())) {
            try {
                cardlinkClient.connect();
                processEvent(decodeResult.getConfigurations(), getParams(event));
            } finally {
                cardlinkClient.close();
            }
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (log.isLoggable(Level.FINE)) {
            String port = "unknown";
            if (ctx.channel().localAddress() instanceof InetSocketAddress inetSocketAddress) {
                port = String.valueOf(inetSocketAddress.getPort());
            }
            log.fine(String.format("New CETP connection established (on port %s)", port));
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (log.isLoggable(Level.FINE)) {
            String port = "unknown";
            if (ctx.channel().localAddress() instanceof InetSocketAddress inetSocketAddress) {
                port = String.valueOf(inetSocketAddress.getPort());
            }
            log.fine(String.format("CETP connection was closed (on port %s)", port));
        }
        super.channelUnregistered(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.log(Level.SEVERE, "Caught an exception handling CETP input", cause);
        ctx.close();
    }
}