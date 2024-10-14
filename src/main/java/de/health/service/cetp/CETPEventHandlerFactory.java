package de.health.service.cetp;

import de.health.service.cetp.konnektorconfig.KonnektorConfig;
import io.netty.channel.ChannelInboundHandler;

public interface CETPEventHandlerFactory {

    ChannelInboundHandler build(KonnektorConfig konnektorConfig);
}
