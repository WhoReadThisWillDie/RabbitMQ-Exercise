package util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Reservation;

public class Utils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String serializeMessage(Reservation reservation) throws JsonProcessingException {
        return mapper.writeValueAsString(reservation.toString());
    }

    public static Reservation deserializeMessage(String message) throws JsonProcessingException {
        return mapper.readValue(message, Reservation.class);
    }
}
