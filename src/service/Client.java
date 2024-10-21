package service;

import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;
import constant.ExchangeType;
import constant.RequestType;
import constant.RoutingKey;
import model.Reservation;
import util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

public class Client {
    private Connection connection;
    private Channel channel;

    private String callbackQueue;
    private String uuid;
    private BasicProperties callbackProperties;

    private Scanner in;
    private CountDownLatch latch;

    private final ArrayList<Reservation> reservations = new ArrayList<>();

    public void connect() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(ExchangeType.CLIENT_AGENT.getName(), BuiltinExchangeType.DIRECT);

        callbackQueue = channel.queueDeclare().getQueue();
        uuid = UUID.randomUUID().toString();
        channel.queueBind(callbackQueue, ExchangeType.CLIENT_AGENT.getName(), uuid);

        listenForMessages();
    }

    public void getAllBuildings() throws IOException {
        System.out.println("Sent get request to agent");

        callbackProperties = new BasicProperties.Builder().correlationId(uuid).build();

        channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), RoutingKey.CLIENT_AGENT.getKey(),
                callbackProperties, RequestType.GET_ALL_BUILDINGS.getName().getBytes());
    }

    public void bookRoom(int buildingId, int roomNumber) throws IOException {
        System.out.println("Sent book request to agent");

        callbackProperties = new BasicProperties.Builder()
                .headers(Map.of("buildingId", buildingId, "roomNumber", roomNumber))
                .correlationId(uuid)
                .build();

        channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), RoutingKey.CLIENT_AGENT.getKey(),
                callbackProperties, RequestType.BOOK_ROOM.getName().getBytes());
    }

    public void listAllReservations() {
        reservations.forEach(System.out::println);
    }

    public void confirmReservation(int buildingId, String reservationCode) throws IOException {
        System.out.println("Sent confirmation request to agent");
        callbackProperties = new BasicProperties.Builder()
                .headers(Map.of("buildingId", buildingId, "reservationCode", reservationCode))
                .correlationId(uuid)
                .build();

        channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), RoutingKey.CLIENT_AGENT.getKey(),
                callbackProperties, RequestType.CONFIRM_RESERVATION.getName().getBytes());
    }

    public void cancelReservation(int buildingId, String reservationCode) throws IOException {
        System.out.println("Sent cancellation request to agent");
        callbackProperties = new BasicProperties.Builder()
                .headers(Map.of("buildingId", buildingId, "reservationCode", reservationCode))
                .correlationId(uuid)
                .build();

        reservations.removeIf(reservation -> reservation.reservationCode().equals(reservationCode));

        channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), RoutingKey.CLIENT_AGENT.getKey(),
                callbackProperties, RequestType.CANCEL_RESERVATION.getName().getBytes());
    }

    private void listenForMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            if (new String(delivery.getBody()).contains("{")) {
                Reservation reservation = Utils.deserializeMessage(new String(delivery.getBody()));
                reservations.add(reservation);
                System.out.println("\nMessage received: " + reservation);
            }
            else {
                System.out.println("\nMessage received: " + new String(delivery.getBody()));
            }

            if (latch != null) {
                latch.countDown();
            }
        };

        channel.basicConsume(callbackQueue, true, deliverCallback, consumerTag -> {});
    }

    public void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    public void run() throws IOException, TimeoutException, InterruptedException {
        connect();

        in = new Scanner(System.in);
        String userInput;
        do {
            printMenu();
            userInput = in.nextLine();
            processInput(userInput);
        } while (!userInput.equals("q"));

        close();
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
        latch = new CountDownLatch(1);
        try {
            switch (userInput) {
                case "1" -> {
                    getAllBuildings();
                    latch.await();
                }
                case "2" -> {
                    System.out.print("ID of the building: ");
                    int buildingId = Integer.parseInt(in.nextLine());

                    System.out.print("Number of the room: ");
                    int roomNumber = Integer.parseInt(in.nextLine());

                    bookRoom(buildingId, roomNumber);
                    latch.await();
                }
                case "3" -> listAllReservations();
                case "4" -> {
                    System.out.print("ID of the building where you reserved the room: ");
                    int buildingId = Integer.parseInt(in.nextLine());

                    System.out.print("Your reservation code: ");
                    String reservationCode = in.nextLine();

                    confirmReservation(buildingId, reservationCode);
                    latch.await();
                }
                case "5" -> {
                    System.out.print("ID of the building where you reserved the room: ");
                    int buildingId = Integer.parseInt(in.nextLine());

                    System.out.print("Your reservation code: ");
                    String reservationCode = in.nextLine();

                    cancelReservation(buildingId, reservationCode);
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

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        Client client = new Client();
        client.run();
    }
}
