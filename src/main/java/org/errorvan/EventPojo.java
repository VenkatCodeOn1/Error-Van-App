package org.errorvan;

public class EventPojo {
    public String timestamp;
    public String id;
    public String type;
    public String traceId;

    public EventPojo(String timestamp, String id, String type, String traceId) {
        this.timestamp = timestamp;
        this.id = id;
        this.type = type;
        this.traceId = traceId;
    }
}
