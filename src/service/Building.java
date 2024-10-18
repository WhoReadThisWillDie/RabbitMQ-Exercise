package service;

import com.rabbitmq.client.*;
import constant.ExchangeType;
import constant.QueueType;
import constant.RoutingKey;
import model.Room;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Building {
    private Connection connection;
    private Channel channel;

    private final String name;
    private final List<Room> rooms = List.of(new Room(1), new Room(2), new Room(3));

    public Building(String name) {
        this.name = name;
    }

    private void run() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(ExchangeType.AGENT_BUILDING.getName(), BuiltinExchangeType.DIRECT);
        channel.queueDeclare(QueueType.AGENT_BUILDING.getName() + name, false, false, false, null);
        channel.queueBind(QueueType.AGENT_BUILDING.getName() + name, ExchangeType.AGENT_BUILDING.getName(), name);

        channel.exchangeDeclare(ExchangeType.AGENT_BUILDINGS.getName(), BuiltinExchangeType.FANOUT);
        channel.queueDeclare(QueueType.AGENT_BUILDINGS.getName() + name, false, false, false, null);
        channel.queueBind(QueueType.AGENT_BUILDINGS.getName() + name, ExchangeType.AGENT_BUILDINGS.getName(), "");

        listenForMessages();
    }

    private void listenForMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());

            String agentId = delivery.getProperties().getCorrelationId();

            System.out.println("Received from Agent: " + message);

            switch (message) {
                case "1" -> channel.basicPublish(ExchangeType.AGENT_BUILDINGS.getName(),
                        agentId, delivery.getProperties(), this.toString().getBytes());
            }
        };

        channel.basicConsume(QueueType.AGENT_BUILDING.getName() + name, deliverCallback, consumerTag -> {});
        channel.basicConsume(QueueType.AGENT_BUILDINGS.getName() + name, deliverCallback, consumerTag -> {});
    }

    public void bookRoom(Room bookedRoom) throws IllegalStateException {
        for (Room room : rooms) {
            if (bookedRoom.equals(room)) {
                room.book();
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("\n%s:\n".formatted(name));

        for (Room room : rooms) {
            str.append("\t%s\n".formatted(room));
        }

        return str.toString();
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        System.out.print("Please choose an ID for this building: ");
        new Building(new Scanner(System.in).nextLine()).run();
    }
}
