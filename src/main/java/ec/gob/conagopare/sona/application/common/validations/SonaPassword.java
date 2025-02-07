package ec.gob.conagopare.sona.application.common.validations;

import io.github.luidmidev.jakarta.validations.Password;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Password(value = SonaPasswordRules.class)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SonaPassword {

    String message() default "Su contraseña es insegura, recuerde que una contraseña segura lleva al menos 12 caracteres, una letra mayúscula, una letra minúscula, un número y un caracter especial";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
