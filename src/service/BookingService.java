package service;

import com.rabbitmq.client.*;
import constant.ExchangeType;
import constant.QueueType;
import model.Building;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

public class BookingService {
    List<Building> buildings = new CopyOnWriteArrayList<>();

    private Connection connection;
    private Channel channel;

    public void run() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(ExchangeType.AGENT_BOOKING.getName(), BuiltinExchangeType.FANOUT);
        channel.queueDeclare(QueueType.AGENT_BOOKING.getName(), false, false, false, null);
        channel.queueBind(QueueType.AGENT_BOOKING.getName(), ExchangeType.AGENT_BOOKING.getName(), "", null);

        listenMessages();
    }

    public void listenMessages() throws IOException, TimeoutException {
//        channel.basicConsume()
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        new RentalAgent().run();
    }
}
