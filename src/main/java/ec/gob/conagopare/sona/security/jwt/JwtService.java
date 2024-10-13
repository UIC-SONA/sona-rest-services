package ec.gob.conagopare.sona.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import ec.gob.conagopare.sona.utils.EnvironmentChecker;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
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
public class JwtService {

    @Getter
    private final String issuer;
    private final long ttlMillis;
    private final Algorithm algorithm;
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();
    private final EnvironmentChecker environment;

    public JwtService(EnvironmentChecker environment, @Value("${security.jwt.issuer}") String issuer, @Value("${security.jwt.ttlMillis}") long expiration) throws NoSuchAlgorithmException {
        this.environment = environment;
        this.issuer = issuer;
        this.ttlMillis = expiration;
        this.algorithm = getAlgorithm();
    }

    byte[] hexToBytes(String hex) {
        var bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }


    private boolean isExpired(String token) {
        var decodedJWT = decode(token);
        return decodedJWT.getExpiresAt().before(new Date());
    }

    private @NotNull Algorithm getAlgorithm() throws NoSuchAlgorithmException {

        if (environment.isDevelopment()) {
            log.warn("Using hardcoded secret key for development purposes");
            var hexKey = "072E932153C5F10F3C1AF8B9107A334C109F3C336F1553326463BD6911894ED3994C864E6D2A1E49D0F48A71139001F307A65C418676D60802F7092907CAF809";
            return Algorithm.HMAC512(hexToBytes(hexKey));
        }

        var random = SecureRandom.getInstanceStrong();
        var secretKey = new byte[64];
        random.nextBytes(secretKey);
        return Algorithm.HMAC512(secretKey);

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


        if (ttlMillis >= 0) {
            var expMillis = nowMillis + ttlMillis;
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
