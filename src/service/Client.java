package service;

import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;
import constant.ExchangeType;
import constant.RequestType;
import constant.RoutingKey;
import model.Reservation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;


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

    private void listenForMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            System.out.println("Message received: " + message);
        };

        channel.basicConsume(callbackQueue, true, deliverCallback, consumerTag -> {});
    }


    public void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
}
