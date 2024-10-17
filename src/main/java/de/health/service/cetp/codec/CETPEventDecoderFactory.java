package de.health.service.cetp.codec;

import de.health.service.cetp.domain.eventservice.event.mapper.CetpEventMapper;
import de.servicehealth.config.api.IUserConfigurations;
import io.netty.channel.ChannelInboundHandler;

public interface CETPEventDecoderFactory {

    ChannelInboundHandler build(IUserConfigurations configurations, CetpEventMapper eventMapper);
}
