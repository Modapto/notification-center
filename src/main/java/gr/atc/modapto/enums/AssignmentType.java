package gr.atc.modapto.enums;

/*
 * Enum for Assignment Type
 */
public enum AssignmentType {
    REQUESTED("Requested"),
    RECEIVED("Received");

    private final String type;

    AssignmentType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}