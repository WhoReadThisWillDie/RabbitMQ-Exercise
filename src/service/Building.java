package service;

import com.rabbitmq.client.*;
import constant.ExchangeType;
import constant.QueueType;
import constant.RequestType;
import model.Reservation;
import model.Room;
import util.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class Building {
    private Connection connection;
    private Channel channel;

    private final int id;
    private final List<Room> rooms = List.of(new Room(1), new Room(2), new Room(3));

    private Building(int id) {
        this.id = id;
    }

    private void run() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(ExchangeType.AGENT_BUILDING.getName(), BuiltinExchangeType.DIRECT);
        channel.queueDeclare(QueueType.AGENT_BUILDING.getName() + id, false, false, false, null);
        channel.queueBind(QueueType.AGENT_BUILDING.getName() + id, ExchangeType.AGENT_BUILDING.getName(), String.valueOf(id));

        channel.exchangeDeclare(ExchangeType.AGENT_BUILDINGS.getName(), BuiltinExchangeType.FANOUT);
        channel.queueDeclare(QueueType.AGENT_BUILDINGS.getName() + id, false, false, false, null);
        channel.queueBind(QueueType.AGENT_BUILDINGS.getName() + id, ExchangeType.AGENT_BUILDINGS.getName(), "");

        listenForMessages();
    }

    private void listenForMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            String agentId = delivery.getProperties().getCorrelationId();

            System.out.println("Received message from Agent: " + message);

            if (message.equals(RequestType.GET_ALL_BUILDINGS.getName())) {
                System.out.println("Sending room info back to agent");

                channel.basicPublish(ExchangeType.AGENT_BUILDING.getName(),
                        agentId, delivery.getProperties(), this.toString().getBytes());
            } else if (message.equals(RequestType.BOOK_ROOM.getName())) {
                System.out.println("Processing booking request");
                int roomNumber = Integer.parseInt(delivery.getProperties().getHeaders().get("roomNumber").toString());

                boolean roomExists = false;

                for (Room room : rooms) {
                    if (room.getNumber() == roomNumber) {
                        roomExists = true;

                        if (!room.isBooked()) {
                            System.out.println("Room booked successfully");

                            room.book();
                            Reservation reservation = new Reservation(id, roomNumber, UUID.randomUUID().toString().substring(0, 4));

                            channel.basicPublish(
                                    ExchangeType.AGENT_BUILDING.getName(),
                                    agentId, delivery.getProperties(),
                                    Utils.serializeMessage(reservation).getBytes()
                            );
                        } else {
                            System.out.println("Room is already booked");

                            channel.basicPublish(ExchangeType.AGENT_BUILDING.getName(),
                                    agentId, delivery.getProperties(), "This room is already booked".getBytes());
                        }
                    }
                }

                if (!roomExists) {
                    channel.basicPublish(ExchangeType.AGENT_BUILDING.getName(),
                            agentId, delivery.getProperties(), "Wrong room number".getBytes());
                }
            }
        };

        channel.basicConsume(QueueType.AGENT_BUILDING.getName() + id, true, deliverCallback, consumerTag -> {
        });

        channel.basicConsume(QueueType.AGENT_BUILDINGS.getName() + id, true, deliverCallback, consumerTag -> {
        });
    }

    private void bookRoom(Room bookedRoom) throws IllegalStateException {
        for (Room room : rooms) {
            if (bookedRoom.equals(room)) {
                room.book();
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("\nBuilding %d:".formatted(id));

        for (Room room : rooms) {
            str.append("\n\t%s".formatted(room));
        }

        return str.toString();
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        System.out.print("Please specify an ID for this building: ");
        new Building(new Scanner(System.in).nextInt()).run();
    }
}
