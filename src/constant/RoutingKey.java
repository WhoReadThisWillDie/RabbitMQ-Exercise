package constant;

public enum RoutingKey {
    CLIENT_AGENT("ClientAgentKey");

    private final String key;

    RoutingKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
