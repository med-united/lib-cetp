package de.health.service.cetp.codec;

import de.servicehealth.config.api.IUserConfigurations;
import io.netty.channel.ChannelInboundHandler;

public interface CETPEventDecoderFactory {

    ChannelInboundHandler build(IUserConfigurations configurations);
}
