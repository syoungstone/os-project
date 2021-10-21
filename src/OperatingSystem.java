import java.util.*;

public class OperatingSystem {

    private static final int KERNEL_ID = 0;
    private static final int INITIAL_WAITING_QUEUE_CAPACITY = 10;

    private static OperatingSystem instance;

    private int nextPid;
    private final List<Template> templates;
    private final Map<Integer,PCB> processes;
    private final PriorityQueue<PCB> waitingQueue;
    private final Scheduler shortTermScheduler;
    private final Semaphore semaphore;

    private OperatingSystem() {
        nextPid = 1;
        templates = new ArrayList<>();
        processes = new HashMap<>();
        waitingQueue = new PriorityQueue<>(INITIAL_WAITING_QUEUE_CAPACITY, new Comparator<PCB>() {
            @Override
            public int compare(PCB o1, PCB o2) {
                return o1.getProcess().getRemainingCycles() - o2.getProcess().getRemainingCycles();
            }
        });
        shortTermScheduler = new RRScheduler();
        semaphore = new Semaphore();
    }

    public void boot() {
        System.out.println("Booting up...");
        System.out.println("Loading templates...");
        try {
            templates.addAll(Template.getTemplates());
            runOS();
        } catch (MalformedTemplateException e) {
            System.out.println(e.getMessage());
            System.out.println("Exiting...");
        }
    }

    private void runOS() {
        System.out.println("Operating system is running!");
        System.out.println("Available process templates:");
        int i = 1;
        for (Template t : templates) {
            System.out.println(i + ") " + t.name());
            i++;
        }
    }

    public static OperatingSystem getInstance() {
        if (instance == null) {
            instance = new OperatingSystem();
        }
        return instance;
    }

    public void createProcess(Template template) {
        createChildProcess(template, KERNEL_ID);
    }

    public void createChildProcess(Template template, int parentId) {
        int newPid = nextPid++;
        PCB newProcess = new PCB(template, newPid, parentId);
        processes.put(newPid, newProcess);
    }

    public PCB pidLookup(int pid) {
        return processes.get(pid);
    }

    public void requestCPU(int pid) {
        PCB process = pidLookup(pid);
        shortTermScheduler.addProcess(process);
        process.setState(State.READY);
    }

    public void requestCriticalSection(int pid) {
        PCB process = pidLookup(pid);
        semaphore.wait(process);
        process.setState(State.WAIT);
    }

    public void requestIO(int pid) {
        PCB process = pidLookup(pid);
        waitingQueue.add(process);
        process.setState(State.WAIT);
    }

    public void releaseCriticalSection() {
        semaphore.signal();
    }

}
