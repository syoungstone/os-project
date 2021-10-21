public class PCB {

    private int pid;
    private int parentId;
    private int priority;
    private final long startTime;
    private final Process process;
    private State state;

    PCB(Template template, int pid, int parentId) {
        state = State.NEW;
        this.pid = pid;
        this.parentId = parentId;
        process = new Process(template, pid);
        startTime = System.currentTimeMillis();
    }

    public Process getProcess() {
        return process;
    }

    public void setState(State state) {
        this.state = state;
    }

}
