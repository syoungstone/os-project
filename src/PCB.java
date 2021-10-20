public class PCB {

    private int pid;
    private int priority;
    private final long startTime;
    private Process process;
    private Process parent;

    PCB() {
        startTime = System.currentTimeMillis();
    }

}
