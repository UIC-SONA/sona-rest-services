package ec.gob.conagopare.sona.repositories;

import ec.gob.conagopare.sona.models.TokenForgetPassword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenForgetPasswordRepository extends JpaRepository<TokenForgetPassword, String> {

    Optional<TokenForgetPassword> findByToken(String token);

    default void deleteExpiredToken() {
        var tokens = findAll();
        var expired = tokens.stream().filter(TokenForgetPassword::isExpired).toList();
        deleteAll(expired);
    }
}

