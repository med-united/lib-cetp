package de.health.service.cetp.domain.eventservice.cardTerminal;

import de.health.service.cetp.domain.CetpStatus;
import lombok.Data;

import java.util.List;

@Data
public class CardTerminalsResponse {

    private List<CardTerminal> cardTerminals;

    private CetpStatus status;
}
