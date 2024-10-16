package constant;

public enum ExchangeType {
    USER_AGENT("UserAgentExchange"),
    AGENT_BOOKING("AgentBookingExchange");


    private final String name;

    ExchangeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
