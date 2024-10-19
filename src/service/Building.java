package service;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.*;
import constant.ExchangeType;
import constant.QueueType;
import constant.RequestType;
import model.Reservation;
import model.Room;
import util.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Building {
    private Connection connection;
    private Channel channel;

    private BasicProperties props;

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

        props = new BasicProperties.Builder().headers(Map.of("buildingId", String.valueOf(id))).build();

        sendInfoToAgent();
        listenForMessages();
    }

    private void sendInfoToAgent() throws IOException {
        channel.basicPublish(ExchangeType.BUILDING_AGENTS.getName(), "", props, this.toString().getBytes());
    }

    private void listenForMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());

            System.out.println("Received message from Agent: " + message);

            if (message.equals(RequestType.BOOK_ROOM.getName())) {
                bookRoom(delivery.getProperties());
            } else if (message.equals(RequestType.CONFIRM_RESERVATION.toString())) {
                confirmBooking(delivery.getProperties());
            } else if (message.equals(RequestType.CANCEL_RESERVATION.toString())) {
                cancelBooking(delivery.getProperties());
            }
        };

        channel.basicConsume(QueueType.AGENT_BUILDING.getName() + id, true, deliverCallback, consumerTag -> {
        });
    }

    private void bookRoom(BasicProperties properties) throws IOException {
        Room room = findRoom(Integer.parseInt(properties.getHeaders().get("roomNumber").toString()));
        String response;

        if (room == null) response = "Wrong room number";
        else if (room.isPending()) response = "This room reservation is awaiting confirmation";
        else if (room.isBooked()) response = "This room is already booked";
        else {
            room.reserve();
            response = Utils.serializeMessage(new Reservation(id, room.getNumber(), room.getReservationCode()));
            sendInfoToAgent();
        }

        sendResponse(properties, response);
    }

    private void confirmBooking(BasicProperties properties) throws IOException {
        Room room = findRoom(properties.getHeaders().get("reservationCode").toString());
        String response;

        if (room == null) response = "Wrong reservation code";
        else if (room.isBooked()) response = "This room is already booked";
        else {
            room.book();
            response = "Reservation confirmed successfully";
            sendInfoToAgent();
        }

        sendResponse(properties, response);
    }

    private void cancelBooking(BasicProperties properties) throws IOException {
        Room room = findRoom(properties.getHeaders().get("reservationCode").toString());
        String response;

        if (room == null) response = "Wrong reservation code";
        else {
            room.cancel();
            response = "Reservation cancelled successfully";
            sendInfoToAgent();
        }

        sendResponse(properties, response);
    }

    private void sendResponse(BasicProperties properties, String response) throws IOException {
        channel.basicPublish(ExchangeType.AGENT_BUILDING.getName(), properties.getCorrelationId(), properties, response.getBytes());
    }

    private Room findRoom(String reservationCode) {
        for (Room room : rooms) {
            if (room.getReservationCode().equals(reservationCode)) return room;
        }

        return null;
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
