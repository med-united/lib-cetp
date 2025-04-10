package de.health.service.cetp.domain.eventservice.cardTerminal;

import lombok.Data;
import java.math.BigInteger;

@Data
public class CardTerminal {

    private ProductInformation productInformation;

    private String ctId;

    private WorkplaceIds workplaceIds;

    private String name;

    private String macAddress;

    private IPAddress ipAddress;

    private BigInteger slots;

    private boolean isphysical;

    private boolean connected;
}
