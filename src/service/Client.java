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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;


public class Client {
    private Connection connection;
    private Channel channel;

    private String callbackQueue;
    private String uuid;
    private BasicProperties callbackProperties;

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

    public void getAllBuildings(CountDownLatch latch) throws IOException {
        this.latch = latch;
        System.out.println("Sent get request to agent");

        callbackProperties = new BasicProperties.Builder().correlationId(uuid).build();

        channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), RoutingKey.CLIENT_AGENT.getKey(),
                callbackProperties, RequestType.GET_ALL_BUILDINGS.getName().getBytes());
    }

    public void bookRoom(int buildingId, int roomNumber, CountDownLatch latch) throws IOException {
        this.latch = latch;
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
            if (new String(delivery.getBody()).contains("{")) {
                Reservation reservation = Utils.deserializeMessage(new String(delivery.getBody()));
                reservations.add(reservation);
                System.out.println("Message received: " + reservation);
            }
            else {
                System.out.println("Message received: " + new String(delivery.getBody()));
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
}
