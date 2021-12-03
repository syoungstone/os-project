package Communication;

import Memory.Word;

public class Message {

    private final int sender;
    private final Word contents;

    public Message(int sender, Word contents) {
        this.sender = sender;
        this.contents = contents;
    }

    public int getSender() {
        return sender;
    }

    public Word getContents() {
        return contents;
    }

}
