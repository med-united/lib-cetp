package de.health.service.cetp.domain.eventservice.cardTerminal;

import lombok.Data;

@Data
public class ProductIdentification {

    private String productVendorID;

    private String productCode;

    private ProductVersion productVersion;
}
