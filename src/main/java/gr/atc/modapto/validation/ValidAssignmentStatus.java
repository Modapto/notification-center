package gr.atc.modapto.validation;

import gr.atc.modapto.validation.validators.AssignmentStatusValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AssignmentStatusValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAssignmentStatus {
    String message() default "Invalid assignment status. Only 'Open', 'Re_Open', 'In_Progress' and 'Done' are allowed.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
