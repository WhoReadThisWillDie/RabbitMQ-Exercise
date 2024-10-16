package service;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import constant.ExchangeType;
import constant.QueueType;
import constant.RoutingKey;
import model.Reservation;
import util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;


public class Client {
    private final String name;
    private Connection connection;
    private Channel channel;

    private final ArrayList<Reservation> reservations = new ArrayList<>();

    public Client(String name) {
        this.name = name;
    }

    public void connect() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(ExchangeType.USER_AGENT.getName(), BuiltinExchangeType.DIRECT);
        channel.queueDeclare(QueueType.USER_AGENT.getName(), false, false, false, null);
        channel.queueBind(QueueType.USER_AGENT.getName(), ExchangeType.USER_AGENT.getName(), RoutingKey.USER_AGENT.getKey());
    }

    public void getAllRooms() {

    }

    public void bookRoom(String building, int roomNumber) throws IOException {
        BasicProperties props = new BasicProperties.Builder()
                .correlationId(name)
                .replyTo(QueueType.USER_AGENT.getName())
                .build();

        channel.basicPublish(ExchangeType.USER_AGENT.getName(), RoutingKey.USER_AGENT.getKey(),
                props, Utils.serializeMessage(building, roomNumber).getBytes());
    }
}
