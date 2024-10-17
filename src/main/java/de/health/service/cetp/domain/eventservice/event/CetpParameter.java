package de.health.service.cetp.domain.eventservice.event;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

@Data
public class CetpParameter {

    private String key;
    
    private String value;
}
