package Communication;

import java.util.*;

public class MessagePasser {

    private static MessagePasser instance;

    private final Map<Integer, Queue<Message>> messages;

    private MessagePasser() {
        messages = new HashMap<>();
    }

    public static MessagePasser getInstance() {
        if (instance == null) {
            instance = new MessagePasser();
        }
        return instance;
    }

    public synchronized void send(int recipient, Message message) {
        Queue<Message> list = messages.get(recipient);
        if (list == null) {
            Queue<Message> newList = new LinkedList<>();
            newList.add(message);
            messages.put(recipient, newList);
        } else {
            list.add(message);
        }
    }

    public synchronized Message receive(int recipient) {
        Queue<Message> list = messages.get(recipient);
        if (list != null && list.size() > 0) {
            return list.poll();
        }
        return null;
    }

}
