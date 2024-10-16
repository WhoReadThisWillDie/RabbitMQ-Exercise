package model;

import java.util.List;

public class Building {
    private final String name;
    private final List<Room> rooms = List.of(new Room(1), new Room(2), new Room(3));

    public Building(String name) {
        this.name = name;
    }

    public synchronized void bookRoom(Room bookedRoom) throws IllegalStateException {
        for (Room room : rooms) {
            if (bookedRoom.equals(room)) {
                room.book();
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("%s:\n".formatted(name));

        for (Room room : rooms) {
            str.append("\t%s\n".formatted(room));
        }

        return str.toString();
    }
}
