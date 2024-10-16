package de.health.service.cetp.codec;

import de.servicehealth.epa4all.config.api.IUserConfigurations;
import io.netty.channel.ChannelInboundHandler;

public interface CETPEventDecoderFactory {

    ChannelInboundHandler build(IUserConfigurations userConfigurations);
}
