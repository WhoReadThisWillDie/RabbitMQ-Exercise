package constant;

public enum QueueType {
    CLIENT_AGENT("ClientAgentQueue"),
    AGENT_BUILDING("AgentBuildingQueue"),
    AGENT_BUILDINGS("AgentBuildingsQueue");

    private final String name;

    QueueType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
