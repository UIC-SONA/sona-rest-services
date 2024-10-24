package ec.gob.conagopare.sona.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import ec.gob.conagopare.sona.security.WebSecurityProperties;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Componente para la generación y validación de tokens JWT.
 */
@Log4j2
@Component
public class Jwt {

    @Getter
    private final String issuer;
    private final long expirationTimeMills;
    private final Algorithm algorithm;
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    public Jwt(WebSecurityProperties properties) throws NoSuchAlgorithmException {
        this.issuer = properties.getJwt().getIssuer();
        this.expirationTimeMills = properties.getJwt().getExpirationTime().toMillis();
        this.algorithm = getAlgorithm(properties.getJwt().getSecretKey());
    }

    private boolean isExpired(String token) {
        var decodedJWT = decode(token);
        return decodedJWT.getExpiresAt().before(new Date());
    }

    private Algorithm getAlgorithm(@Nullable String secretKey) throws NoSuchAlgorithmException {

        if (secretKey != null && !secretKey.isBlank()) {
            log.warn("Using provided secret key for JWT, this is not recommended for production environments");
            return Algorithm.HMAC256(secretKey);
        }

        log.info("Generating random secret key for JWT");
        var random = SecureRandom.getInstanceStrong();
        var randomSecretKey = new byte[64];
        random.nextBytes(randomSecretKey);
        return Algorithm.HMAC256(randomSecretKey);

    }

    /**
     * Crea un token JWT con el nombre de usuario y el correo electrónico proporcionados.
     *
     * @param id      Nombre de usuario para incluir en el token.
     * @param subject Correo electrónico para incluir en el token.
     * @return El token JWT generado.
     */
    public String create(String id, String subject) {

        log.debug("Creating JWT for user id {} and subject {}", id, subject);

        var nowMillis = System.currentTimeMillis();
        var now = new Date(nowMillis);

        var builder = JWT.create()
                .withJWTId(id) // ID del token
                .withIssuedAt(now) // Fecha de creación del token
                .withSubject(subject) // Asunto del token
                .withIssuer(issuer); // Emisor del


        if (expirationTimeMills >= 0) {
            var expMillis = nowMillis + expirationTimeMills;
            var exp = new Date(expMillis);
            builder.withExpiresAt(exp); // Fecha de expiración del token
        }
        return builder.sign(algorithm);
    }


    /**
     * Obtiene el nombre de usuario de un token JWT.
     *
     * @param jwt Token JWT del cual se obtiene el nombre de usuario.
     * @return El nombre de usuario del token.
     */
    public String getId(String jwt) {
        log.debug("getting id from jwt '{}'", jwt);
        return getClaim(jwt, DecodedJWT::getId);
    }

    /**
     * Obtiene el correo electrónico de un token JWT.
     *
     * @param jwt Token JWT del cual se obtiene el correo electrónico.
     * @return El correo electrónico del token.
     */
    public String getSubject(String jwt) {
        log.debug("getting subject from jwt '{}'", jwt);
        return getClaim(jwt, DecodedJWT::getSubject);
    }

    /**
     * Obtiene un reclamo específico del token JWT utilizando un resolvedor de reclamos dado.
     *
     * @param jwt            Token JWT del cual se obtendrá el reclamo.
     * @param claimsResolver Función que resuelve el reclamo deseado a partir de los Claims del token.
     * @param <T>            Tipo de dato del reclamo que se desea obtener.
     * @return El reclamo específico del token JWT.
     */
    private <T> T getClaim(String jwt, @NotNull Function<DecodedJWT, T> claimsResolver) {
        var claims = decodeAndVerify(jwt);
        return claimsResolver.apply(claims);
    }


    /**
     * Obtiene un reclamo específico del token JWT.
     *
     * @param jwt       Token JWT del cual se obtendrá el reclamo.
     * @param claimName Nombre del reclamo que se desea obtener.
     * @param clazz     Tipo de dato del reclamo que se desea obtener.
     * @param <T>       Tipo de dato del reclamo que se desea obtener.
     * @return El reclamo específico del token JWT.
     */
    private <T> T getClaim(String jwt, String claimName, Class<T> clazz) {
        var claims = decodeAndVerify(jwt);
        return claims.getClaim(claimName).as(clazz);
    }

    /**
     * Obtiene todos los reclamos (Claims) contenidos en el token JWT.
     *
     * @param jwt Token JWT del cual se obtendrán los reclamos.
     * @return Objeto Claims que representa todos los reclamos del token JWT.
     */
    private DecodedJWT decodeAndVerify(String jwt) {
        if (isRevoked(jwt)) throw new SecurityException("Session has been closed");
        return JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(jwt);
    }


    @Contract("_ -> new")
    public static @NotNull DecodedJWT decode(String jwt) {
        return JWT.decode(jwt);
    }


    /**
     * Verifica si un token JWT ha sido revocado.
     */
    private boolean isRevoked(@NotNull String jwt) {
        return revokedTokens.contains(jwt.replace("Bearer ", ""));
    }


    /**
     * Revoca un token JWT.
     *
     * @param jwt Token JWT a revocar.
     */
    public void revoke(String jwt) {
        revokedTokens.add(jwt);
    }

    /**
     * Elimina los tokens revocados que han expirado.
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    private void removeExpiredTokens() {
        log.info("Cleaning up revoked tokens list at {}", LocalDateTime.now());
        if (revokedTokens.isEmpty()) return;
        revokedTokens.removeIf(this::isExpired);
    }
}
