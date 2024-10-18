package service;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.*;
import constant.ExchangeType;
import constant.QueueType;
import constant.RoutingKey;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class RentalAgent {

    private Connection connection;
    private Channel channel;

    private String callbackQueue;
    private String uuid;

    public void run() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(ExchangeType.AGENT_BUILDING.getName(), BuiltinExchangeType.DIRECT);

        channel.exchangeDeclare(ExchangeType.AGENT_BUILDINGS.getName(), BuiltinExchangeType.FANOUT);

        callbackQueue = channel.queueDeclare().getQueue();
        uuid = UUID.randomUUID().toString();
        channel.queueBind(callbackQueue, ExchangeType.AGENT_BUILDING.getName(), uuid);
        channel.queueBind(callbackQueue, ExchangeType.AGENT_BUILDINGS.getName(), "");

        listenForClientMessages();
        listenForBuildingMessages();
    }

    private void listenForClientMessages() throws IOException, TimeoutException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            String clientId = delivery.getProperties().getCorrelationId();

            System.out.println("Received message: " + message + " from client with ID: " + clientId);

            switch (message) {
                case "1" -> {
                    System.out.println("Sent message to get all buildings");
                    BasicProperties callbackProperties = new BasicProperties.Builder().messageId(clientId).correlationId(uuid).build();
                    channel.basicPublish(ExchangeType.AGENT_BUILDINGS.getName(),
                            "", callbackProperties, message.getBytes());
                }
                case "Building1:1" -> {
                    System.out.println("NOT IMPLEMENTED");
//                    BasicProperties callbackProperties = new BasicProperties.Builder()
//                            .headers(Map.of()).correlationId(uuid).build();
//                    channel.basicPublish(ExchangeType.AGENT_BUILDING.getName(),
//                            RoutingKey.AGENT_BUILDING.getKey(), callbackProperties, message.getBytes());
                }
            }
        };

        channel.basicConsume(QueueType.CLIENT_AGENT.getName(), true, deliverCallback, consumerTag -> {});
    }

    private void listenForBuildingMessages() throws IOException, TimeoutException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String finalMessage = new String(delivery.getBody());
            String clientId = delivery.getProperties().getMessageId();

            System.out.println("Received from building: " + finalMessage);
            channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), clientId, null, finalMessage.getBytes());
        };

        channel.basicConsume(callbackQueue, true, deliverCallback, consumerTag -> {});
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        new RentalAgent().run();
    }
}
