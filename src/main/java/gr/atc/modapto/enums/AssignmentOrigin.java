package gr.atc.modapto.enums;

/*
 * Enum for Assignment Origin
 */
public enum AssignmentOrigin {
    System("System"),
    Source("Source"),
    Target("Target");

    private final String origin;

    AssignmentOrigin(final String origin) {
        this.origin = origin;
    }

    @Override
    public String toString() {
        return origin;
    }
}
