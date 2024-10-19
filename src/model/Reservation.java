package model;

public record Reservation(int building, int roomNumber, String reservationCode) {
    @Override
    public String toString() {
        return "Building %d, Room %d, Reservation code: %s".formatted(building, roomNumber, reservationCode);
    }
}
