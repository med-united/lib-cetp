package de.health.service.cetp.domain.eventservice.cardTerminal;

import lombok.Data;

import java.util.Date;

@Data
public class ProductInformation {

    private Date informationDate;

    private ProductTypeInformation productTypeInformation;

    private ProductIdentification productIdentification;

    private ProductMiscellaneous productMiscellaneous;
}
