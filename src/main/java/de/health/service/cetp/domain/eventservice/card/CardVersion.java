package de.health.service.cetp.domain.eventservice.card;

import lombok.Data;

@Data
public class CardVersion {

    protected VersionInfo cosVersion;

    protected VersionInfo objectSystemVersion;

    protected VersionInfo cardPTPersVersion;

    protected VersionInfo dataStructureVersion;

    protected VersionInfo loggingVersion;

    protected VersionInfo atrVersion;

    protected VersionInfo gdoVersion;

    protected VersionInfo keyInfoVersion;
}
