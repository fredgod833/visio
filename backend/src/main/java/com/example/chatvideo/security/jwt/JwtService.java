package com.example.chatvideo.security.jwt;

import com.example.chatvideo.security.services.UserDetailsImpl;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class JwtService {

    private final String secret;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public JwtService(Environment env) {
        this.secret = env.getProperty("secret.key");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "RSA");
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes()));
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /**
     * Génère un token JWT pour l'utilisateur
     *
     * @param user UserDetailsImpl contenant les infos utilisateur
     * @return JWT token string
     */
    public String generateAccessToken(UserDetailsImpl user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.DAYS))
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .build();

        JwtEncoderParameters jwtEncoderParameters = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        );

        return jwtEncoder.encode(jwtEncoderParameters).getTokenValue();
    }

    /**
     * Valide un token JWT
     * Vérifie la signature, l'expiration et la structure
     *
     * @param token JWT token à valider
     * @return true si le token est valide, false sinon
     */
    public boolean validateAccessToken(String token) {
        try {
            Jwt jwt = this.jwtDecoder.decode(token);

            // Vérifier l'expiration
            Instant expiresAt = jwt.getExpiresAt();
            if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
                log.warn("Token expired at: {}", expiresAt);
                return false;
            }

            // Vérifier que le subject (username) existe
            if (jwt.getSubject() == null || jwt.getSubject().isEmpty()) {
                log.warn("Token has no subject (username)");
                return false;
            }

            return true;

        } catch (JwtException e) {
            log.error("Invalid Token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Décode et retourne le JWT sans validation supplémentaire
     * Utilisé après avoir validé le token avec validateAccessToken()
     *
     * @param token JWT token
     * @return Jwt object décodé
     * @throws JwtException si le token est invalide
     */
    public Jwt readJwt(final String token) {
        return this.jwtDecoder.decode(token);
    }

    /**
     * Extrait le username (subject) depuis le token
     *
     * @param token JWT token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        Jwt jwt = readJwt(token);
        return jwt.getSubject();
    }

    /**
     * Extrait l'ID utilisateur depuis le token
     *
     * @param token JWT token
     * @return userId
     */
    public Long getUserIdFromToken(String token) {
        Jwt jwt = readJwt(token);
        return jwt.getClaim("userId");
    }

    /**
     * Extrait l'email depuis le token
     *
     * @param token JWT token
     * @return email
     */
    public String getEmailFromToken(String token) {
        Jwt jwt = readJwt(token);
        return jwt.getClaim("email");
    }
}