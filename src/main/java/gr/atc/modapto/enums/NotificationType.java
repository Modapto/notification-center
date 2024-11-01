package gr.atc.modapto.enums;

/*
 * Enum for Notification Type
 */
public enum NotificationType {
    EVENT("Event"),
    ASSIGNMENT("Assignment");

    private final String type;

    NotificationType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
