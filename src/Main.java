import service.Client;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) throws IOException, TimeoutException {
        new Main().run();
    }

    private void run() throws IOException, TimeoutException {
        Scanner in = new Scanner(System.in);

        Client client = new Client();
        client.connect();

        String userInput;

        do {
            System.out.println("1. See a list of buildings");
            System.out.println("2. Book a room");
            System.out.println("Type 'q' to exit");

            userInput = in.nextLine();

            switch (userInput) {
                case "1" -> client.getAllRooms();
                case "2" -> System.out.println("Not implemented");
                default -> System.out.println("Wrong input");
            }
        } while (!userInput.equals("q"));

        client.close();
    }
}