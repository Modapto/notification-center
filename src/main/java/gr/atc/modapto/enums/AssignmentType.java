package gr.atc.modapto.enums;

/*
 * Enum for Assignment Type
 */
public enum AssignmentType {
    Requested("Requested"),
    Received("Received");

    private final String type;

    AssignmentType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}