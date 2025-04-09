package gr.atc.modapto.enums;

/*
 * Enum for Notification Type
 */
public enum NotificationType {
    Event("Event"),
    Assignment("Assignment");

    private final String type;

    NotificationType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
