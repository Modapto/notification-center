package gr.atc.modapto.enums;

/*
 * Enum for Notification Status
 */
public enum NotificationStatus {
    Unread("Unread"),
    Read("Read");

    private final String status;

    NotificationStatus(final String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}