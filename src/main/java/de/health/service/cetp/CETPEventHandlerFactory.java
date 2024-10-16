package de.health.service.cetp;

import de.servicehealth.epa4all.config.KonnektorConfig;
import io.netty.channel.ChannelInboundHandler;

public interface CETPEventHandlerFactory {

    ChannelInboundHandler build(KonnektorConfig konnektorConfig);
}
