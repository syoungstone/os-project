package Communication;

import Memory.Word;

import java.util.LinkedList;
import java.util.Queue;

public class OrdinaryPipe {

    private final int parent;
    private final int child;

    private final Queue<Word> buffer;

    OrdinaryPipe(int parent, int child) {
        this.parent = parent;
        this.child = child;
        this.buffer = new LinkedList<>();
    }

    public synchronized void write(int writer, Word contents) {
        if (writer == parent) {
            buffer.add(contents);
        }
    }

    public synchronized Word read(int reader) {
        if (reader == child) {
            return buffer.poll();
        }
        else {
            return null;
        }
    }

}
