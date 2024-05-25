package gov.cdc.izgateway.security.ocsp;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bouncycastle.asn1.ocsp.CertID;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.operator.DigestCalculator;

import java.math.BigInteger;
import java.util.Comparator;

public class CertificateId extends CertificateID implements Comparable<CertificateId> {
    private X509CertificateHolder issuerCertHolder;

    public CertificateId(DigestCalculator digestCalc, X509CertificateHolder issuerCertHolder, BigInteger certSerialNum) throws OCSPException {
        super(digestCalc, issuerCertHolder, certSerialNum);

        this.issuerCertHolder = issuerCertHolder;
    }

    public CertificateId(CertificateID certId) {
        this(certId.toASN1Primitive());
    }

    public CertificateId(CertID certId) {
        super(certId);
    }

    public boolean matches(CertificateId certId) throws OCSPException {
        return this.matches(certId.getIssuerCertificateHolder(), certId.getSerialNumber());
    }

    public boolean matches(X509CertificateHolder issuerCertHolder, BigInteger certSerialNum) throws OCSPException {
        return (this.matchesIssuer(issuerCertHolder, CryptoUtils.DIGEST_CALC_PROV) && this.getSerialNumber().equals(certSerialNum));
    }

    @Override
    public int compareTo(CertificateId obj) {
        return Comparator.comparing(Object::hashCode).compare(this, obj);
    }
    @Override
    public boolean equals(Object that) {
        return EqualsBuilder.reflectionEquals(this, that);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public X509CertificateHolder getIssuerCertificateHolder() {
        return this.issuerCertHolder;
    }
}
