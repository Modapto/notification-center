package gr.atc.modapto.enums;

/*
 * Enum for Assignment Status
 */
public enum AssignmentStatus {
    Open("Open"),
    Re_Open("Re-Open"),
    In_Progress("In Progress"),
    Done("Done");

    private final String status;

    AssignmentStatus(final String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}