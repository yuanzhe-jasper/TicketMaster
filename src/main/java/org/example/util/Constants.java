package org.example.util;

public final class Constants {

    private Constants() {}

    // SQS event types
    public static final String EVENT_TYPE_ORDER_CREATED = "order-created";
    public static final String EVENT_TYPE_ORDER_CANCELLED = "order-cancelled";

    // Notification types
    public static final String NOTIFICATION_TYPE_ORDER_CREATED = "ORDER_CREATED";
    public static final String NOTIFICATION_TYPE_ORDER_CANCELLED = "ORDER_CANCELLED";

    // Order statuses
    public static final String ORDER_STATUS_CONFIRMED = "CONFIRMED";
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    public static final String ORDER_STATUS_PENDING = "PENDING";

    // Ticket statuses
    public static final String TICKET_STATUS_AVAILABLE = "AVAILABLE";
    public static final String TICKET_STATUS_RESERVED = "RESERVED";
    public static final String TICKET_STATUS_SOLD = "SOLD";
}
