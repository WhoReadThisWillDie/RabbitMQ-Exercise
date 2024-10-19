package service;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.*;
import constant.ExchangeType;
import constant.QueueType;
import constant.RequestType;
import constant.RoutingKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;

public class RentalAgent {

    private Connection connection;
    private Channel channel;

    private String callbackQueue;
    private String buildingsQueue;
    private String uuid;

    private CompletableFuture<String> futureResponse;
    private int callbackTimeout = 3000;

    private ArrayList<String> buildingsInfo = new ArrayList<>();

    private void run() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(ExchangeType.CLIENT_AGENT.getName(), BuiltinExchangeType.DIRECT);
        channel.queueDeclare(QueueType.CLIENT_AGENT.getName(), false, false, false, null);
        channel.queueBind(QueueType.CLIENT_AGENT.getName(), ExchangeType.CLIENT_AGENT.getName(), RoutingKey.CLIENT_AGENT.getKey());

        channel.exchangeDeclare(ExchangeType.AGENT_BUILDING.getName(), BuiltinExchangeType.DIRECT);
        callbackQueue = channel.queueDeclare().getQueue();
        uuid = UUID.randomUUID().toString();
        channel.queueBind(callbackQueue, ExchangeType.AGENT_BUILDING.getName(), uuid);

        channel.exchangeDeclare(ExchangeType.BUILDING_AGENTS.getName(), BuiltinExchangeType.FANOUT);
        buildingsQueue = channel.queueDeclare().getQueue();
        channel.queueBind(buildingsQueue, ExchangeType.BUILDING_AGENTS.getName(), "");

        listenForBuildingsInfo();
        listenForClientMessages();
        listenForBookingResponses();
    }

    private void listenForClientMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            String clientId = delivery.getProperties().getCorrelationId();

            System.out.println("Received message from client with ID: " + clientId + " - " + message);

            if (message.equals(RequestType.GET_ALL_BUILDINGS.getName())) {
                channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), clientId, null,
                        buildingsInfo.toString().replace(",", "").getBytes());
            }
            else {
                sendRequestToBuilding(delivery.getProperties(), message);
                setCallbackTimeout(clientId);
            }
        };

        channel.basicConsume(QueueType.CLIENT_AGENT.getName(), true, deliverCallback, consumerTag -> {
        });
    }

    private void sendRequestToBuilding(BasicProperties properties, String message) throws IOException {
        String buildingId = (properties.getHeaders().get("buildingId").toString());

        BasicProperties callbackProperties = new BasicProperties.Builder()
                .headers(properties.getHeaders())
                .messageId(properties.getCorrelationId())
                .headers(properties.getHeaders())
                .correlationId(uuid)
                .build();

        System.out.println("Forwarding request to building " + buildingId);

        channel.basicPublish(ExchangeType.AGENT_BUILDING.getName(), buildingId, true, callbackProperties, message.getBytes());
    }

    private void setCallbackTimeout(String clientId) {
        futureResponse = new CompletableFuture<>();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            if (!futureResponse.isDone()) {
                String responseMessage = "The building did not respond in time (%d second)\n".formatted(callbackTimeout / 1000) +
                        "This building does not exist or is currently offline";

                try {
                    channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), clientId, null, responseMessage.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }, callbackTimeout, TimeUnit.MILLISECONDS);
    }

    private void listenForBookingResponses() throws IOException {
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

    private void listenForBuildingsInfo() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            System.out.println("Received building info");
            String message = new String(delivery.getBody());
            String buildingId = delivery.getProperties().getHeaders().get("buildingId").toString();

            for (String buildingInfo : buildingsInfo) {
                if (buildingInfo.contains("Building " + buildingId)) {
                    buildingsInfo.remove(buildingInfo);
                    break;
                }
            }
            buildingsInfo.add(message);
        };

        channel.basicConsume(buildingsQueue, true, deliverCallback, consumerTag -> {
        });
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        new RentalAgent().run();
    }
}
