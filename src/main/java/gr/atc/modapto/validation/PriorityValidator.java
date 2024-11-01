package gr.atc.modapto.validation;

import gr.atc.modapto.enums.MessagePriority;
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
