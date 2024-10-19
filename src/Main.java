import service.Client;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class Main {
    Client client;
    Scanner in;

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        new Main().run();
    }

    private void run() throws IOException, TimeoutException, InterruptedException {
        in = new Scanner(System.in);
        client = new Client();
        client.connect();

        String userInput;
        do {
            printMenu();

            userInput = in.nextLine();
            processInput(userInput);
        } while (!userInput.equals("q"));

        client.close();
    }

    private void printMenu() {
        System.out.println("1. See a list of buildings");
        System.out.println("2. Book a room");
        System.out.println("3. List my reservations");
        System.out.println("4. Confirm my reservation");
        System.out.println("5. Cancel my reservation");
        System.out.println("Type 'q' to exit");
    }

    private void processInput(String userInput) throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            switch (userInput) {
                case "1" -> {
                    getAllBuildings(latch);
                    latch.await();
                }
                case "2" -> {
                    bookRoom(latch);
                    latch.await();
                }
                case "3" -> getAllReservations();
                case "4" -> {
                    confirmReservation(latch);
                    latch.await();
                }
                case "5" -> {
                    cancelReservation(latch);
                    latch.await();
                }
                case "q" -> {}
                default -> System.out.println("Wrong input, please try again");
            }
        } catch (NumberFormatException e) {
            System.out.println("Wrong input format");
        }

        System.out.println();
    }

    private void getAllBuildings(CountDownLatch latch) throws IOException {
        client.getAllBuildings(latch);
    }

    private void bookRoom(CountDownLatch latch) throws IOException {
        System.out.print("ID of the building: ");
        int buildingId = Integer.parseInt(in.nextLine());

        System.out.print("Number of the room: ");
        int roomNumber = Integer.parseInt(in.nextLine());

        client.bookRoom(buildingId, roomNumber, latch);
    }

    private void getAllReservations() {
        client.listAllReservations();
    }

    private void confirmReservation(CountDownLatch latch) throws IOException {
        System.out.print("ID of the building where you reserved the room: ");
        int buildingId = Integer.parseInt(in.nextLine());

        System.out.print("Your reservation code: ");
        String reservationCode = in.nextLine();

        client.confirmReservation(buildingId, reservationCode, latch);
    }

    private void cancelReservation(CountDownLatch latch) throws IOException {
        System.out.print("ID of the building where you reserved the room: ");
        int buildingId = Integer.parseInt(in.nextLine());

        System.out.print("Your reservation code: ");
        String reservationCode = in.nextLine();

        client.cancelReservation(buildingId, reservationCode, latch);
    }
}