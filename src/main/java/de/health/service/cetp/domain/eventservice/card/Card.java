package de.health.service.cetp.domain.eventservice.card;

import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Data
public class Card {

    private String cardHandle;

    private CardType cardType;

    private CardVersion cardVersion;

    private String iccsn;

    private String ctId;

    private BigInteger slotId;

    private Date insertTime;

    private String cardHolderName;

    private String kvnr;

    private Date certificateExpirationDate;
}
