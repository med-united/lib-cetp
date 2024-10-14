package de.health.service.cetp.codec;

import de.health.service.cetp.config.IUserConfigurations;
import io.netty.channel.ChannelInboundHandler;

public interface CETPEventDecoderFactory {

    ChannelInboundHandler build(IUserConfigurations userConfigurations);
}
