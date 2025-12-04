package gr.atc.modapto.enums;

/*
 * Enum for Assignment Origin
 */
public enum AssignmentOrigin {
    SYSTEM("System"),
    SOURCE("Source"),
    TARGET("Target");

    private final String origin;

    AssignmentOrigin(final String origin) {
        this.origin = origin;
    }

    @Override
    public String toString() {
        return origin;
    }
}
