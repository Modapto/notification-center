package gr.atc.modapto.validation.validators;

import gr.atc.modapto.enums.AssignmentStatus;
import gr.atc.modapto.validation.ValidAssignmentStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.EnumUtils;

public class AssignmentStatusValidator implements ConstraintValidator<ValidAssignmentStatus, String> {

    @Override
    public boolean isValid(String status, ConstraintValidatorContext context) {
        if (status == null)
            return true;

        return EnumUtils.isValidEnumIgnoreCase(AssignmentStatus.class, status);
    }
}
