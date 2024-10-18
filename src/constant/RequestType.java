package constant;

public enum RequestType {
    GET_ALL_BUILDINGS("GET_ALL_BUILDINGS"),
    BOOK_ROOM("BOOK_ROOM");

    private String name;

    RequestType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
