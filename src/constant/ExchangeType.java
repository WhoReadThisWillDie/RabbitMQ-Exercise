package constant;

public enum ExchangeType {
    CLIENT_AGENT("ClientAgentExchange"),
    AGENT_BUILDING("AgentBuildingExchange"),
    AGENT_BUILDINGS("AgentBuildingsExchange");

    private final String name;

    ExchangeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
