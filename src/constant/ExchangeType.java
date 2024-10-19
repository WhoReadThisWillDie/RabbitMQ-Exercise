package constant;

public enum ExchangeType {
    CLIENT_AGENT("ClientAgentExchange"),
    AGENT_BUILDING("AgentBuildingExchange"),
    BUILDING_AGENTS("BuildingsAgentExchange");

    private final String name;

    ExchangeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
