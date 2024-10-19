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
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.AMQP.BasicProperties;

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

        channel.exchangeDeclare(ExchangeType.BUILDING_AGENTS.getName(), BuiltinExchangeType.FANOUT);

        channel.basicPublish(ExchangeType.BUILDING_AGENTS.getName(), "", null, this.toString().getBytes());

        listenForMessages();
    }

    private void listenForMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            String agentId = delivery.getProperties().getCorrelationId();

            System.out.println("Received message from Agent: " + message);

            if (message.equals(RequestType.BOOK_ROOM.getName())) {
                bookRoom(delivery.getProperties());
            }
        };

        channel.basicConsume(QueueType.AGENT_BUILDING.getName() + id, true, deliverCallback, consumerTag -> {
        });
    }

    private void bookRoom(BasicProperties properties) throws IOException {
        Room room = findRoom(Integer.parseInt(properties.getHeaders().get("roomNumber").toString()));
        String agentId = properties.getCorrelationId();
        String response;

        if (room == null) response = "Wrong room number";
        else if (room.isBooked()) response = "This room is already booked or awaiting confirmation from another user";
        else {
            room.book();
            response = Utils.serializeMessage(new Reservation(id, room.getNumber(), room.getReservationCode()));
        }

        channel.basicPublish(ExchangeType.AGENT_BUILDING.getName(), agentId, properties, response.getBytes());
    }

    private Room findRoom(int roomNumber) {
        for (Room room : rooms) {
            if (room.getNumber() == roomNumber) {
                return room;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("\nBuilding %d:\n".formatted(id));

        for (Room room : rooms) {
            str.append("\t%s\n".formatted(room));
        }

        return str.toString();
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        System.out.print("Please specify an ID for this building: ");
        new Building(new Scanner(System.in).nextInt()).run();
    }
}
