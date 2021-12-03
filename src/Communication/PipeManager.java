package Communication;

import java.util.HashMap;
import java.util.Map;

public class PipeManager {

    private static PipeManager instance;

    private final Map<Integer, OrdinaryPipe> pipesByReader;

    private PipeManager() {
        pipesByReader = new HashMap<>();
    }

    public static PipeManager getInstance() {
        if (instance == null) {
            instance = new PipeManager();
        }
        return instance;
    }

    public synchronized OrdinaryPipe createPipe(int parent, int child) {
        OrdinaryPipe pipe = new OrdinaryPipe(parent, child);
        pipesByReader.put(child, pipe);
        return pipe;
    }

    public synchronized OrdinaryPipe retrievePipe(int reader) {
        return pipesByReader.get(reader);
    }

}
