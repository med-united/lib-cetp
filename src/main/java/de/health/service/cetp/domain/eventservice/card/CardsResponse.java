package de.health.service.cetp.domain.eventservice.card;

import de.health.service.cetp.domain.CetpStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
public class CardsResponse {

    private List<Card> cards;

    private CetpStatus status;
}