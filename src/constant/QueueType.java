package constant;

public enum QueueType {
    USER_AGENT("UserAgentQueue"),
    AGENT_BOOKING("AgentBookingQueue");

    private final String name;

    QueueType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
