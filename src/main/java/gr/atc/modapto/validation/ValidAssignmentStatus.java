package gr.atc.modapto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AssignmentStatusValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAssignmentStatus {
    String message() default "Invalid assignment status. Only OPEN, ACCEPTED, IN_PROGRESS and COMPLETED are allowed.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
