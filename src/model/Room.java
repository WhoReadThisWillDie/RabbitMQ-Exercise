package model;

import java.util.UUID;

public class Room {
    private final int number;
    private boolean isBooked = false;
    private String reservationCode;

    public Room(int number) {
        this.number = number;
    }

    public void book() {
        this.isBooked = true;
        this.reservationCode = UUID.randomUUID().toString().substring(0, 4);
    }

    public int getNumber() {
        return number;
    }

    public String getReservationCode() {
        if (!isBooked) {
            return "This room is not booked yet";
        }

        return reservationCode;
    }

    public boolean isBooked() {
        return isBooked;
    }

    @Override
    public String toString() {
        return "Room %d".formatted(number);
    }
}
