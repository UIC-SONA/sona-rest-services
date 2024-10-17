package ec.gob.conagopare.sona.exceptions;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.ketoru.springframework.errors.DefaultExceptionHandler;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Log4j2
@ControllerAdvice
public class GlobalExceptionHandler extends DefaultExceptionHandler {

    @ExceptionHandler(JWTDecodeException.class)
    public ResponseEntity<Object> handleJWTDecodeException(JWTDecodeException ex) {
        var defaultDetail = "Invalid token.";
        return createDefaultResponseEntity(ex, new HttpHeaders(), UNAUTHORIZED, defaultDetail, null, null, null);
    }

    @ExceptionHandler(SignatureVerificationException.class)
    public ResponseEntity<Object> handleSignatureVerificationException(SignatureVerificationException ex, WebRequest request) {
        var defaultDetail = "User account is locked.";
        return createDefaultResponseEntity(ex, new HttpHeaders(), UNAUTHORIZED, defaultDetail, null, null, request);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Object> handleTokenExpiredException(TokenExpiredException ex) {
        var defaultDetail = "Token expired.";
        return createDefaultResponseEntity(ex, new HttpHeaders(), UNAUTHORIZED, defaultDetail, null, null, null);
    }


    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<Object> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException ex) {
        var defaultDetail = "SQL Integrity Constraint Violation Exception.";
        log.warn("Se recibió una excepción de SQLIntegrityConstraintViolationException, se recomienda revisar la integridad de los datos, la excepción es: {}", ex.getMessage());
        return createDefaultResponseEntity(ex, new HttpHeaders(), INTERNAL_SERVER_ERROR, defaultDetail, null, null, null);
    }
}