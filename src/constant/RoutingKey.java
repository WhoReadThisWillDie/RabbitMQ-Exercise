package constant;

public enum RoutingKey {
    CLIENT_AGENT("ClientAgentKey"),
    AGENT_BUILDING("AgentBuildingKey"),
    AGENT_BUILDINGS("AgentBuildingsKey");

    private final String key;

    RoutingKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
