import service.Client;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.spi.AbstractResourceBundleProvider;

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
            System.out.println("1. See a list of buildings");
            System.out.println("2. Book a room");
            System.out.println("Type 'q' to exit");

            userInput = in.nextLine();

            CountDownLatch latch = new CountDownLatch(1);
            switch (userInput) {
                case "1" -> {
                    getAllBuildings(latch);
                    latch.await();
                }
                case "2" -> {
                    bookRoom(latch);
                    latch.await();
                }
                case "q" -> {}
                default -> System.out.println("Wrong input, please try again");
            }

            System.out.println();
        } while (!userInput.equals("q"));

        client.close();
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
}