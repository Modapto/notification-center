package gr.atc.modapto.validation.validators;

import gr.atc.modapto.enums.AssignmentType;
import gr.atc.modapto.validation.ValidAssignmentType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.EnumUtils;

public class AssignmentTypeValidator implements ConstraintValidator<ValidAssignmentType, String> {

    @Override
    public boolean isValid(String status, ConstraintValidatorContext context) {
        if (status == null)
            return true;

        return EnumUtils.isValidEnumIgnoreCase(AssignmentType.class, status);
    }
}
