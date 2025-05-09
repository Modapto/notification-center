package gr.atc.modapto.validation;

import gr.atc.modapto.validation.validators.NotificationTypeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotificationTypeValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNotificationType {
    String message() default "Invalid notification type. Only 'Event' or 'Assignment' are allowed.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
