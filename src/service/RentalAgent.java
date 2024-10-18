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

    private String callbackQueue1;
    private String callbackQueue2;
    private String uuid;

    public void run() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(ExchangeType.CLIENT_AGENT.getName(), BuiltinExchangeType.DIRECT);
        channel.queueDeclare(QueueType.CLIENT_AGENT.getName(), false, false, false, null);
        channel.queueBind(QueueType.CLIENT_AGENT.getName(), ExchangeType.CLIENT_AGENT.getName(), RoutingKey.CLIENT_AGENT.getKey());

        channel.exchangeDeclare(ExchangeType.AGENT_BUILDING.getName(), BuiltinExchangeType.DIRECT);
        channel.exchangeDeclare(ExchangeType.AGENT_BUILDINGS.getName(), BuiltinExchangeType.FANOUT);

        callbackQueue1 = channel.queueDeclare().getQueue();
        callbackQueue2 = channel.queueDeclare().getQueue();
        uuid = UUID.randomUUID().toString();
        channel.queueBind(callbackQueue1, ExchangeType.AGENT_BUILDING.getName(), uuid);
        channel.queueBind(callbackQueue2, ExchangeType.AGENT_BUILDINGS.getName(), "");

        listenForClientMessages();
        listenForBuildingMessages();
    }

    private void listenForClientMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            String clientId = delivery.getProperties().getCorrelationId();

            System.out.println("Received message from client with ID: " + clientId + " - " + message);

            switch (message) {
                case "GET_ALL_ROOMS" -> {
                    System.out.println("Forwarding request to all buildings");
                    BasicProperties callbackProperties = new BasicProperties.Builder()
                            .messageId(delivery.getProperties().getCorrelationId())
                            .correlationId(uuid)
                            .build();
                    channel.basicPublish(ExchangeType.AGENT_BUILDINGS.getName(), "", callbackProperties, message.getBytes());
                }
//                default -> {
//                    System.out.println("Forwarding room booking request to specific building");
//                    // Forward the request to the specific building
//                    String[] parts = message.split(":");
//                    if (parts.length == 2) {
//                        String buildingId = parts[0];
//                        BasicProperties callbackProperties = new BasicProperties.Builder()
//                                .correlationId(clientId)
//                                .build();
//                        channel.basicPublish(ExchangeType.AGENT_BUILDING.getName(), buildingId, callbackProperties, message.getBytes());
//                    }
//                }
            }
        };

        channel.basicConsume(QueueType.CLIENT_AGENT.getName(), true, deliverCallback, consumerTag -> {
        });
    }

    private void listenForBuildingMessages() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String responseMessage = new String(delivery.getBody());
            String clientId = delivery.getProperties().getMessageId();

            System.out.println("Received message from building: " + responseMessage);
            System.out.println("Forwarding response to client with ID: " + clientId);

            // Forward response back to the client
            channel.basicPublish(ExchangeType.CLIENT_AGENT.getName(), clientId, null, responseMessage.getBytes());
        };

        // Listen for responses from buildings
        channel.basicConsume(callbackQueue1, true, deliverCallback, consumerTag -> {
        });
    }


    public static void main(String[] args) throws IOException, TimeoutException {
        new RentalAgent().run();
    }
}
