package ec.gob.conagopare.sona.application.errors;


import io.github.luidmidev.springframework.web.problemdetails.DefaultProblemDetailsExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends DefaultProblemDetailsExceptionHandler {

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<Object> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException ex, WebRequest request) {
        var defaultDetail = "SQL Integrity Constraint Violation Exception.";
        log.warn("Se recibió una excepción de SQLIntegrityConstraintViolationException, se recomienda revisar la integridad de los datos, la excepción es: {}", ex.getMessage());
        return createDefaultResponseEntity(ex, new HttpHeaders(), INTERNAL_SERVER_ERROR, defaultDetail, null, null, request);
    }

}