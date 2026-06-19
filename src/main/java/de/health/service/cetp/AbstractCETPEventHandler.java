package de.health.service.cetp;

import de.health.service.cetp.domain.eventservice.event.CetpEvent;
import de.health.service.cetp.domain.eventservice.event.CetpParameter;
import de.health.service.cetp.domain.eventservice.event.DecodeResult;
import de.health.service.config.api.IUserConfigurations;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractCETPEventHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
    }

    protected abstract Logger getLog();

    protected abstract String getTopicName();

    protected abstract void processEvent(IUserConfigurations configurations, Map<String, String> paramsMap, String eventXml);

    protected void logCardInsertedEvent(Map<String, String> paramsMap, String correlationId) {
        String paramsStr = paramsMap.entrySet().stream()
            .filter(p -> !p.getKey().equals("CardHolderName"))
            .map(p -> String.format("key=%s value=%s", p.getKey(), p.getValue())).collect(Collectors.joining(", "));

        getLog().info(String.format("[%s] Card inserted: params: %s", correlationId, paramsStr));
    }

    protected Map<String, String> getParams(CetpEvent event) {
        return event.getParameters().stream().collect(Collectors.toMap(CetpParameter::getKey, CetpParameter::getValue));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        DecodeResult decodeResult = (DecodeResult) msg;
        CetpEvent event = decodeResult.getEvent();
        if (event.getTopic().equals(getTopicName())) {
            processEvent(decodeResult.getConfigurations(), getParams(event), decodeResult.getEventXml());
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (getLog().isDebugEnabled()) {
            String port = "unknown";
            if (ctx.channel().localAddress() instanceof InetSocketAddress inetSocketAddress) {
                port = String.valueOf(inetSocketAddress.getPort());
            }
            getLog().debug(String.format("New CETP connection established (on port %s)", port));
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (getLog().isDebugEnabled()) {
            String port = "unknown";
            if (ctx.channel().localAddress() instanceof InetSocketAddress inetSocketAddress) {
                port = String.valueOf(inetSocketAddress.getPort());
            }
            getLog().debug(String.format("CETP connection was closed (on port %s)", port));
        }
        super.channelUnregistered(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        getLog().error("Caught an exception handling CETP input", cause);
        ctx.close();
    }
}