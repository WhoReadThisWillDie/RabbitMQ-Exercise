package model;

public class Room {
    private final int number;
    private boolean isBooked = false;

    public Room(int number) {
        this.number = number;
    }

    public synchronized void book() throws IllegalStateException {
        if (isBooked) {
            throw new IllegalStateException("This room is already booked");
        }

        isBooked = true;
    }

    public boolean isBooked() {
        return isBooked;
    }

    @Override
    public String toString() {
        return "Room number %d: %s".formatted(number, isBooked ? "booked" : "not booked");
    }
}
