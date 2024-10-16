package constant;

public enum RoutingKey {
    USER_AGENT("UserAgentKey"),
    AGENT_BUILDING("AgentBuildingKey");

    private final String key;

    RoutingKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
