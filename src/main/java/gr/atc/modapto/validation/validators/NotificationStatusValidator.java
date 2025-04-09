package gr.atc.modapto.validation.validators;

import gr.atc.modapto.validation.ValidNotificationStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.EnumUtils;

import gr.atc.modapto.enums.NotificationStatus;

public class NotificationStatusValidator implements ConstraintValidator<ValidNotificationStatus, String> {

    @Override
    public boolean isValid(String status, ConstraintValidatorContext context) {
        if (status == null)
            return false;

        return EnumUtils.isValidEnumIgnoreCase(NotificationStatus.class, status);
    }
}
