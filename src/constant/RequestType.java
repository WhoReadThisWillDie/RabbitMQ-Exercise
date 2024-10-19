package constant;

public enum RequestType {
    GET_ALL_BUILDINGS("GET_ALL_BUILDINGS"),
    BOOK_ROOM("BOOK_ROOM"),
    CONFIRM_RESERVATION("CONFIRM_RESERVATION"),
    CANCEL_RESERVATION("CANCEL_RESERVATION");

    private String name;

    RequestType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
