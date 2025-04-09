package gr.atc.modapto.enums;

/*
 * Enum for Message Priority
 */
public enum MessagePriority {
    Low("Low"),
    Mid("Mid"),
    High("High");

    private final String priority;

    MessagePriority(final String priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return priority;
    }

}
