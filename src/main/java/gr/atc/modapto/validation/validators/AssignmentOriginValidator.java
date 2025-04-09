package gr.atc.modapto.validation.validators;

import gr.atc.modapto.enums.AssignmentOrigin;
import gr.atc.modapto.validation.ValidAssignmentOrigin;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.EnumUtils;

public class AssignmentOriginValidator implements ConstraintValidator<ValidAssignmentOrigin, String> {

    @Override
    public boolean isValid(String origin, ConstraintValidatorContext context) {
        if (origin == null)
            return false;

        return EnumUtils.isValidEnumIgnoreCase(AssignmentOrigin.class, origin);
    }
}
