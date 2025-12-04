package gr.atc.modapto.validation;

import gr.atc.modapto.validation.validators.AssignmentOriginValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AssignmentOriginValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAssignmentOrigin {
    String message() default "Invalid assignment origin. Only 'System', 'Source' or 'Target' are allowed.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
