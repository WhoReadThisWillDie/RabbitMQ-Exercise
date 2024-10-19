package service;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.*;
import constant.ExchangeType;
import constant.QueueType;
import constant.RequestType;
import constant.RoutingKey;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;

public class RentalAgent {

    private Connection connection;
    private Channel channel;

    private String callbackQueue;
    private String uuid;

    private CompletableFuture<String> futureResponse;
    private int callbackTimeout = 1000;

    private void run() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(ExchangeType.CLIENT_AGENT.getName(), BuiltinExchangeType.DIRECT);
        channel.queueDeclare(QueueType.CLIENT_AGENT.getName(), false, false, false, null);
        channel.queueBind(QueueType.CLIENT_AGENT.getName(), ExchangeType.CLIENT_AGENT.getName(), RoutingKey.CLIENT_AGENT.getKey());

        channel.exchangeDeclare(ExchangeType.AGENT_BUILDING.getName(), BuiltinExchangeType.DIRECT);
        channel.exchangeDeclare(ExchangeType.AGENT_BUILDINGS.getName(), BuiltinExchangeType.FANOUT);

        callbackQueue = channel.queueDeclare().getQueue();
        uuid = UUID.randomUUID().toString();
        channel.queueBind(callbackQueue, ExchangeType.AGENT_BUILDING.getName(), uuid);

        listenForClientMessages();
        listenForBuildingMessages();
    }

    private void listenForClientMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            String clientId = delivery.getProperties().getCorrelationId();

            System.out.println("Received message from client with ID: " + clientId + " - " + message);

            if (message.equals(RequestType.GET_ALL_BUILDINGS.getName())) {
                System.out.println("Forwarding request to all buildings");

                BasicProperties callbackProperties = new BasicProperties.Builder()
                        .messageId(delivery.getProperties().getCorrelationId())
                        .correlationId(uuid)
                        .build();

                channel.basicPublish(ExchangeType.AGENT_BUILDINGS.getName(), "", callbackProperties, message.getBytes());
            }
            else if (message.equals(RequestType.BOOK_ROOM.getName())) {
                int buildingId = Integer.parseInt(delivery.getProperties().getHeaders().get("buildingId").toString());

                BasicProperties callbackProperties = new BasicProperties.Builder()
                        .headers(delivery.getProperties().getHeaders())
                        .messageId(delivery.getProperties().getCorrelationId())
                        .correlationId(uuid)
                        .build();

                System.out.println("Forwarding request to building " + buildingId);

                futureResponse = new CompletableFuture<>();

                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                scheduler.schedule(() -> {
                    if (!futureResponse.isDone()) {
                        String responseMessage = "The building did not respond in time (%d second)\n".formatted(callbackTimeout / 1000) +
                                "This building does not exist or currently is offline";

                        try {
                            channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), clientId, null, responseMessage.getBytes());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }, callbackTimeout, TimeUnit.MILLISECONDS);

                channel.basicPublish(ExchangeType.AGENT_BUILDING.getName(), String.valueOf(buildingId), true, callbackProperties, message.getBytes());
            }
        };

        channel.basicConsume(QueueType.CLIENT_AGENT.getName(), true, deliverCallback, consumerTag -> {
        });
    }

    private void listenForBuildingMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String responseMessage = new String(delivery.getBody());
            String clientId = delivery.getProperties().getMessageId();

            if (futureResponse != null) {
                futureResponse.complete(responseMessage);
            }

            System.out.println("Received message from building: " + responseMessage);
            System.out.println("Forwarding response to client with ID: " + clientId);

            channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), clientId, null, responseMessage.getBytes());
        };

        channel.basicConsume(callbackQueue, true, deliverCallback, consumerTag -> {
        });
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        new RentalAgent().run();
    }
}
