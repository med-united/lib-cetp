package de.health.service.cetp;

import lombok.Getter;

import java.security.cert.X509Certificate;

@Getter
public class CertificateInfo {

    // A_24883-02 - clientAttest als ECDSA-Signatur
    public static final String URN_BSI_TR_03111_ECDSA = "urn:bsi:tr:03111:ecdsa";

    // A_24884-01 - clientAttest signieren als PKCS#1-Signatur
    public static final String URN_IETF_RFC_3447 = "urn:ietf:rfc:3447";
    

    private final X509Certificate certificate;
    private final String signatureType;

    private CertificateInfo(X509Certificate certificate, String signatureType) {
        this.certificate = certificate;
        this.signatureType = signatureType;
    }

    public static CertificateInfo create03111ECDSAInfo(X509Certificate certificate) {
        return new CertificateInfo(certificate, URN_BSI_TR_03111_ECDSA);
    }

    public static CertificateInfo createRfc3447Info(X509Certificate certificate) {
        return new CertificateInfo(certificate, URN_IETF_RFC_3447);
    }
}
