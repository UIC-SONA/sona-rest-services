package ec.gob.conagopare.sona.application.errors;


import io.github.luidmidev.springframework.data.crud.core.exceptions.NotFoundEntityException;
import io.github.luidmidev.springframework.web.problemdetails.DefaultProblemDetailsExceptionHandler;
import io.github.luidmidev.springframework.web.problemdetails.schemas.FieldMessage;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends DefaultProblemDetailsExceptionHandler {

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<Object> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException ex, WebRequest request) {
        var defaultDetail = "SQL Integrity Constraint Violation Exception.";
        log.warn("Se recibió una excepción de SQLIntegrityConstraintViolationException, se recomienda revisar la integridad de los datos, la excepción es: {}", ex.getMessage());
        return createDefaultResponseEntity(ex, new HttpHeaders(), INTERNAL_SERVER_ERROR, defaultDetail, null, null, request);
    }

    @ExceptionHandler(NotFoundEntityException.class)
    public ResponseEntity<Object> handleNotFoundEntityException(NotFoundEntityException ex, WebRequest request) {
        var defaultDetail = "No encontrado.";
        return createDefaultResponseEntity(ex, new HttpHeaders(), NOT_FOUND, defaultDetail, null, null, request);
    }

    @Override
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        var defaultDetail = "One or more fields are invalid.";
        var body = super.createProblemDetail(ex, BAD_REQUEST, defaultDetail, null, null, request);

        addValidationErrors(body, ex.getConstraintViolations(), violation -> {
            var path = violation.getPropertyPath();
            if (path instanceof PathImpl pathImpl) {
                return new FieldMessage(pathImpl.getLeafNode().getName(), violation.getMessage());
            }
            return new FieldMessage(path.toString(), violation.getMessage());
        });

        return createResponseEntity(ex, new HttpHeaders(), BAD_REQUEST, request, body);
    }

}