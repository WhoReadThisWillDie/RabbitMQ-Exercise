package service;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import constant.ExchangeType;
import constant.QueueType;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RentalAgent {

    private Connection connection;
    private Channel channel;

    public void run() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(ExchangeType.AGENT_BOOKING.getName(), BuiltinExchangeType.FANOUT);
        channel.queueDeclare(QueueType.USER_AGENT.getName(), false, false, false, null);
        channel.queueDeclare(QueueType.AGENT_BOOKING.getName(), false, false, false, null);
        channel.queueBind(QueueType.USER_AGENT.getName(), "", "", null);
        channel.queueBind(QueueType.AGENT_BOOKING.getName(), ExchangeType.AGENT_BOOKING.getName(), "", null);

        listenMessages();
    }

    public void listenMessages() throws IOException, TimeoutException {
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        new RentalAgent().run();
    }
}
