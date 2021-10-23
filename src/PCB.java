public class PCB {

    private final int pid;
    private final int parentId;
    private final long startTime;
    private final Process process;
    private int priority;
    private State state;

    PCB(Template template, int pid, int parentId) {
        state = State.NEW;
        this.pid = pid;
        this.parentId = parentId;
        process = new Process(template, pid);
        startTime = System.currentTimeMillis();
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void wakeup() {
        process.wakeup();
    }

    public void progressOneCycle() {
        process.progressOneCycle();
    }

}
