package de.health.service.cetp.domain.eventservice.event.mapper;

import de.health.service.cetp.domain.eventservice.event.CetpEvent;

public interface CetpEventMapper<S> {

    CetpEvent toDomain(S soap);
}
