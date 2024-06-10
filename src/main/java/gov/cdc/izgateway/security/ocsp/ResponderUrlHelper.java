package gov.cdc.izgateway.security.ocsp;

import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;

public class ResponderUrlHelper {

	protected URL getOcspResponderUrl(X509Certificate cert)throws IOException {
		URL ocspResponderUrl;

		try {
			ocspResponderUrl = findOcspResponderUrl(cert);
		} catch (IOException e) {
			throw new IOException(String.format("Unable to get responder URL for cert with serial no: %s", cert.getSerialNumber()), e);
		}
		return ocspResponderUrl;
	}

	/* Find the OCSP Responder server url */
	protected static URL findOcspResponderUrl(X509Certificate cert) throws IOException {

		// find the authority info access from the extensions of the certificate
		byte[] authorityInfoAccessExtContent = cert.getExtensionValue(Extension.authorityInfoAccess.getId());

		if (authorityInfoAccessExtContent == null) {
			return null;
		}
		// find the OCSP URL from the access location based on the id matching the ocsp
		// identifier OID
		GeneralName ocspResponderUrlName = Stream
				.of(AuthorityInformationAccess.getInstance(ASN1Primitive.fromByteArray(
						((DEROctetString) ASN1Primitive.fromByteArray(authorityInfoAccessExtContent)).getOctets()))
						.getAccessDescriptions())
				.filter(accessDesc -> accessDesc.getAccessMethod().getId()
						.equals(OCSPObjectIdentifiers.id_pkix_ocsp.getId()))
				.map(AccessDescription::getAccessLocation)
				.filter(accessLoc -> (accessLoc.getTagNo() == GeneralName.uniformResourceIdentifier)).findFirst()
				.orElse(null);

		return ((ocspResponderUrlName != null)
				? new URL(
						DERIA5String.getInstance(((DERTaggedObject) ocspResponderUrlName.toASN1Primitive()).getObject())
								.getString())
				: null);
	}
}
