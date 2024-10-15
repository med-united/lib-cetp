package de.health.service.cetp.domain.eventservice.card;

import lombok.Getter;

import java.util.Arrays;

@SuppressWarnings("unused")
@Getter
public enum CardType {

    EGK("EGK"),
    HBA_Q_SIG("HBA-qSig"),
    HBA("HBA"),
    SMC_B("SMC-B"),
    HSM_B("HSM-B"),
    SMC_KT("SMC-KT"),
    KVK("KVK"),
    ZOD_2_0("ZOD_2.0"),
    UNKNOWN("UNKNOWN"),
    HB_AX("HBAx"),
    SM_B("SM-B");

    private final String value;

    CardType(String value) {
        this.value = value;
    }

    public static CardType from(String value) {
        return Arrays.stream(CardType.values())
            .filter(t -> t.getValue().equals(value))
            .findFirst()
            .orElse(null);
    }
}
