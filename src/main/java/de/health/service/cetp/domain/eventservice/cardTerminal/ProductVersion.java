package de.health.service.cetp.domain.eventservice.cardTerminal;

import lombok.Data;

@Data
public class ProductVersion {

    private ProductVersionLocal local;

    private String central;
}
