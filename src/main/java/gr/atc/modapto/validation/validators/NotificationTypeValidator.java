package gr.atc.modapto.validation.validators;

import gr.atc.modapto.enums.NotificationType;
import gr.atc.modapto.validation.ValidNotificationType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.EnumUtils;

public class NotificationTypeValidator implements ConstraintValidator<ValidNotificationType, String> {

    @Override
    public boolean isValid(String status, ConstraintValidatorContext context) {
        if (status == null)
            return false;

        return EnumUtils.isValidEnumIgnoreCase(NotificationType.class, status);
    }
}
