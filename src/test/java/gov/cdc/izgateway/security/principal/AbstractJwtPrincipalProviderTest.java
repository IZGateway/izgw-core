package gov.cdc.izgateway.security.principal;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AbstractJwtPrincipalProviderTest {
    private TestJwtPrincipalProvider jwtProvider;
    private final String sharedSecret = "f7b3f3bd00e12434bbb155007498f857e93803c2c01f686253979c408dd28a91f999922aad3a8fa5cbd8bbbbb1f8ef9fbcddcb99cb44d9c7028c6904f306c4e6";
    private final String jwtIssuer = "theIssuer";
    private final String jwtSubject = "sampleSubject";
    private final String jwtAudience = "theAudience";

    @BeforeEach
    void setUp() {
        jwtProvider = new TestJwtPrincipalProvider();
    }

    @Test
    void invalidSharedSecret() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer)
                .subject(jwtSubject)
                .audience(jwtAudience)
                .jwtID(java.util.UUID.randomUUID().toString())

                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))

                .build();

        String token = createToken(claims, "87eaf799da3d3d8fe5165d8b25482afa27f9133256a9d887bc24f47b30160db47636219d61047b53d9552723426befa0b242a4d61ddccb53340950ad831110ab");

        BadJwtException exception = assertThrows(BadJwtException.class, () -> getJwtFromToken(token));

        assertEquals("An error occurred while attempting to decode the Jwt: Signed JWT rejected: Invalid signature",
                exception.getMessage()
        );
    }

    @Test
    void allTimestampsValid() throws Exception {

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer)
                .subject(jwtSubject)
                .audience(jwtAudience)
                .jwtID(java.util.UUID.randomUUID().toString())

                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))

                .build();

        String token = createToken(claims, sharedSecret);
        Jwt jwt = getJwtFromToken(token);

        OAuth2TokenValidatorResult result = jwtProvider.createValidator().validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    void missingIat() throws Exception {

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer)
                .subject(jwtSubject)
                .audience(jwtAudience)
                .jwtID(java.util.UUID.randomUUID().toString())

                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))

                .build();

        String token = createToken(claims, sharedSecret);
        Jwt jwt = getJwtFromToken(token);

        OAuth2TokenValidatorResult result = jwtProvider.createValidator().validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    void missingNbf() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer)
                .subject(jwtSubject)
                .audience(jwtAudience)
                .jwtID(java.util.UUID.randomUUID().toString())

                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))

                .build();

        String token = createToken(claims, sharedSecret);
        Jwt jwt = getJwtFromToken(token);

        OAuth2TokenValidatorResult result = jwtProvider.createValidator().validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    void missingIatNbf() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer)
                .subject(jwtSubject)
                .audience(jwtAudience)
                .jwtID(java.util.UUID.randomUUID().toString())

                .expirationTime(Date.from(now.plusSeconds(60)))

                .build();

        String token = createToken(claims, sharedSecret);
        Jwt jwt = getJwtFromToken(token);

        OAuth2TokenValidatorResult result = jwtProvider.createValidator().validate(jwt);

        assertTrue(result.hasErrors());
    }

    @Test
    void missingAllTimestampsValid() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer)
                .subject(jwtSubject)
                .audience(jwtAudience)
                .jwtID(java.util.UUID.randomUUID().toString())

                .build();

        String token = createToken(claims, sharedSecret);
        Jwt jwt = getJwtFromToken(token);

        OAuth2TokenValidatorResult result = jwtProvider.createValidator().validate(jwt);

        assertTrue(result.hasErrors());
    }

    @Test
    void expiredExp() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer)
                .subject(jwtSubject)
                .audience(jwtAudience)
                .jwtID(java.util.UUID.randomUUID().toString())

                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(-60)))

                .build();

        String token = createToken(claims, sharedSecret);

        BadJwtException exception = assertThrows(BadJwtException.class, () -> getJwtFromToken(token));

        assertEquals("An error occurred while attempting to decode the Jwt: expiresAt must be after issuedAt",
                exception.getMessage()
        );
    }

    @Test
    void futureIat() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer)
                .subject(jwtSubject)
                .audience(jwtAudience)
                .jwtID(java.util.UUID.randomUUID().toString())

                .issueTime(Date.from(now.plusSeconds(30)))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))

                .build();

        String token = createToken(claims, sharedSecret);
        Jwt jwt = getJwtFromToken(token);

        OAuth2TokenValidatorResult result = jwtProvider.createValidator().validate(jwt);

        assertTrue(result.hasErrors());

    }

    @Test
    void futureNbf() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer)
                .subject(jwtSubject)
                .audience(jwtAudience)
                .jwtID(java.util.UUID.randomUUID().toString())

                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.plusSeconds(120)))
                .expirationTime(Date.from(now.plusSeconds(360)))

                .build();

        String token = createToken(claims, sharedSecret);

        assertThrows(JwtValidationException.class, () -> getJwtFromToken(token));
    }

    @Test
    void IatAfterExp() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer)
                .subject(jwtSubject)
                .audience(jwtAudience)
                .jwtID(java.util.UUID.randomUUID().toString())

                .issueTime(Date.from(now.plusSeconds(90)))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))

                .build();

        String token = createToken(claims, sharedSecret);
        BadJwtException exception = assertThrows(BadJwtException.class, () -> getJwtFromToken(token));

        assertEquals("An error occurred while attempting to decode the Jwt: expiresAt must be after issuedAt",
                exception.getMessage()
        );

    }

    @Test
    void NbfAfterExp() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer)
                .subject(jwtSubject)
                .audience(jwtAudience)
                .jwtID(java.util.UUID.randomUUID().toString())

                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.plusSeconds(360)))
                .expirationTime(Date.from(now.plusSeconds(120)))

                .build();

        String token = createToken(claims, sharedSecret);

        JwtValidationException exception = assertThrows(JwtValidationException.class, () -> getJwtFromToken(token));

        assertTrue(exception.getMessage().contains("Jwt used before"));
    }

    private Jwt getJwtFromToken(String token) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(sharedSecret.getBytes(), "HmacSHA256")
        ).build();

        return decoder.decode(token);
    }

    // Used to generate self-signed JWT using specified claims
    private String createToken(JWTClaimsSet claims, String sharedSecret) throws Exception {
        JWSSigner signer = new MACSigner(sharedSecret);
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims
        );
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private static class TestJwtPrincipalProvider extends AbstractJwtPrincipalProvider {
        TestJwtPrincipalProvider() {
            super(null, null, mock(JwtTokenExtractor.class));
        }

        @Override
        protected NimbusJwtDecoder createDecoder() {
            return mock(NimbusJwtDecoder.class);
        }

        @Override
        protected boolean validConfiguration() {
            return true;
        }
    }
}


