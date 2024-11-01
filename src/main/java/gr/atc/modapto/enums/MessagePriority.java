package gr.atc.modapto.enums;

/*
 * Enum for Message Priority
 */
public enum MessagePriority {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH");

    private final String priority;

    MessagePriority(final String priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return priority;
    }

}
