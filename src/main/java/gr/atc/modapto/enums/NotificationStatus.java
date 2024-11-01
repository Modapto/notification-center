package gr.atc.modapto.enums;

/*
 * Enum for Notification Status
 */
public enum NotificationStatus {
    NOT_VIEWED("NOT_VIEWED"),
    VIEWED("VIEWED");

    private final String status;

    NotificationStatus(final String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}