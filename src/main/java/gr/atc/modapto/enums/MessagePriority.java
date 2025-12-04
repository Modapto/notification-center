package gr.atc.modapto.enums;

/*
 * Enum for Message Priority
 */
public enum MessagePriority {
    LOW("Low"),
    MID("Mid"),
    HIGH("High");

    private final String priority;

    MessagePriority(final String priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return priority;
    }

}
