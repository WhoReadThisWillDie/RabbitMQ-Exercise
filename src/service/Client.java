package service;

import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;
import constant.ExchangeType;
import constant.QueueType;
import constant.RoutingKey;
import model.Reservation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;


public class Client {
    private Connection connection;
    private Channel channel;

    private String callbackQueue;
    private String uuid;
    private BasicProperties callbackProperties;

    private final ArrayList<Reservation> reservations = new ArrayList<>();

    public void connect() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(ExchangeType.CLIENT_AGENT.getName(), BuiltinExchangeType.DIRECT);

        callbackQueue = channel.queueDeclare().getQueue();
        uuid = UUID.randomUUID().toString();
        channel.queueBind(callbackQueue, ExchangeType.CLIENT_AGENT.getName(), uuid);

        callbackProperties = new BasicProperties.Builder().correlationId(uuid).build();

        listenForMessages();
    }

    public void getAllRooms() throws IOException {
        System.out.println("Sent get request to agent");
        channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), RoutingKey.CLIENT_AGENT.getKey(),
                callbackProperties, "GET_ALL_ROOMS".getBytes());
    }

    public void bookRoom(String building, int roomNumber) throws IOException {
        System.out.println("Sent book request to agent");
        callbackProperties = new BasicProperties.Builder()
                .headers(Map.of("BuildingId", building, "Room", roomNumber)).correlationId(uuid).build();
        channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), RoutingKey.CLIENT_AGENT.getKey(),
                callbackProperties, "%s:%d".formatted(building, roomNumber).getBytes());
    }

    private void listenForMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            System.out.println("Message received: " + message);
            // Process the response
        };

        channel.basicConsume(callbackQueue, true, deliverCallback, consumerTag -> {});
    }


    public void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
}
