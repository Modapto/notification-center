package gr.atc.modapto.enums;

import jakarta.validation.ValidationException;

/*
 * Enum for Assignment Status
 */
public enum AssignmentStatus {
    OPEN("Open"),
    RE_OPEN("Re-Open"),
    IN_PROGRESS("In Progress"),
    DONE("Done");

    private final String status;

    AssignmentStatus(final String status) {
        this.status = status;
    }

    public static AssignmentStatus fromString(final String input) {
        for (AssignmentStatus assignmentStatus : AssignmentStatus.values()) {
            if (assignmentStatus.status.equalsIgnoreCase(input)) {
                return assignmentStatus;
            }
        }
        throw new ValidationException("Unknown assignment status: " + input);
    }

    @Override
    public String toString() {
        return status;
    }
}