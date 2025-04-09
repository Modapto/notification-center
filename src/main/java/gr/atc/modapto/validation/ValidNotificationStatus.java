package gr.atc.modapto.validation;

import gr.atc.modapto.validation.validators.NotificationStatusValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotificationStatusValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNotificationStatus {
    String message() default "Invalid assignment origin. Only 'Read' or 'Unread' are allowed.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
