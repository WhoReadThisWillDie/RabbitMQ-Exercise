package model;

import java.util.UUID;

public class Room {
    private final int number;
    private boolean isBooked = false;
    private boolean isPending = false;
    private String reservationCode;

    public Room(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public String getReservationCode() {
        if (reservationCode == null) {
            return "Room was not booked or reserved yet";
        }
        return reservationCode;
    }

    public boolean isBooked() {
        return isBooked;
    }

    public boolean isPending() {
        return isPending;
    }

    public void book() {
        isPending = false;
        isBooked = true;
    }

    public void reserve() {
        isPending = true;
        reservationCode = UUID.randomUUID().toString().substring(0, 4);
    }

    public void cancel() {
        reservationCode = null;
        isPending = false;
        isBooked = false;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Room %d: ".formatted(number));

        if (isPending) str.append("pending");
        else if (isBooked) str.append("booked");
        else str.append("not booked");

        return str.toString();
    }
}
