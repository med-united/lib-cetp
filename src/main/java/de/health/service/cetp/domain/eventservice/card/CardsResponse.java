package de.health.service.cetp.domain.eventservice.card;

import de.health.service.cetp.domain.CetpStatus;
import lombok.Data;

import java.util.List;

@Data
public class CardsResponse {

    private List<Card> cards;

    private CetpStatus status;
}