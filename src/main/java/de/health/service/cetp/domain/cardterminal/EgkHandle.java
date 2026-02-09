package de.health.service.cetp.domain.cardterminal;

import java.math.BigInteger;

public record EgkHandle(String cardHandle, String ctId, BigInteger slotId) {
}
