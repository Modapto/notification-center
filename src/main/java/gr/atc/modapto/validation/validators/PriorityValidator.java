package gr.atc.modapto.validation.validators;

import gr.atc.modapto.enums.MessagePriority;
import gr.atc.modapto.validation.ValidPriority;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.EnumUtils;

public class PriorityValidator implements ConstraintValidator<ValidPriority, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null)
            return true;

       return EnumUtils.isValidEnumIgnoreCase(MessagePriority.class, value);
    }
}
