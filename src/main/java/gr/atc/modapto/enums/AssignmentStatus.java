package gr.atc.modapto.enums;

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

    @Override
    public String toString() {
        return status;
    }
}