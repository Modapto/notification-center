package gr.atc.modapto.enums;

/*
 * Enum for Assignment Status
 */
public enum AssignmentStatus {
    OPEN("Open"),
    ACCEPTED("Accepted"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    private final String status;

    AssignmentStatus(final String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}