package model;

public record Reservation(int building, int roomNumber, String reservationCode) {
    @Override
    public String toString() {
        return "Building %d, room %d, reservation code: %s".formatted(building, roomNumber, reservationCode);
    }
}
