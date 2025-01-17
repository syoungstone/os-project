package Control;

import Communication.IPCStandard;
import GUI.TaskManager;
import Memory.MainMemory;
import Memory.Page;
import Memory.Word;
import Processes.*;
import Processes.Process;
import Processor.Processor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OperatingSystem {

    public static final int KERNEL_ID = 0;
    private static final int CYCLE_DELAY_MS = 2;
    private static final int CYCLES_PER_STATUS_PRINTOUT = 200;

    private static OperatingSystem instance;

    private final Processor processor;
    private final IoModule ioModule;
    private final MainMemory mainMemory;

    private long maxCycles;
    private long elapsedCycles;
    private long startTime = 0;
    private long elapsedTime = 0;
    private final Map<Integer, PCB> processes;
    private final List<PCB> terminated;
    private final Set<Integer> waitingOnIo;
    private final Map<Integer, int[]> waitingOnResources;
    // One critical section semaphore per template
    private final List<Semaphore> semaphores;
    private final PidGenerator pidGenerator;

    private TaskManager taskManager;

    private boolean halt = false;

    private OperatingSystem() {
        processor = new Processor();
        ioModule = new IoModule();
        mainMemory = MainMemory.getInstance();

        elapsedCycles = 0;
        // ConcurrentHashMap class & Collections.synchronized methods for thread safety
        processes = new ConcurrentHashMap<>();
        terminated = Collections.synchronizedList(new ArrayList<>());
        waitingOnIo = Collections.synchronizedSet(new HashSet<>());
        waitingOnResources = new ConcurrentHashMap<>();
        // Semaphore & PidGenerator instance methods are synchronized for thread safety
        semaphores = Collections.synchronizedList(new ArrayList<>());
        pidGenerator = new PidGenerator();
    }

    public void boot(TaskManager taskManager) {

        this.taskManager = taskManager;

        try {
            List<Template> templates = Template.getTemplates();
            // Create one critical section semaphore for each template
            for (int i = 0 ; i < templates.size() ; i++) {
                semaphores.add(new Semaphore());
            }
            taskManager.requestNumProcesses(templates);
        } catch (MalformedTemplateException e) {
            System.out.println(e.getMessage());
            System.out.println("Exiting...");
        }
    }

    public void createProcesses(List<Template> templates, List<Integer> processesPerTemplate) {
        for (int i = 0 ; i < templates.size() ; i++) {
            int numProcesses = processesPerTemplate.get(i);
            for (int j = 0; j < numProcesses; j++) {
                createProcess(templates.get(i));
            }
        }
        taskManager.requestNumCycles();
    }

    public void setMaxCycles(long maxCycles) {
        this.maxCycles = maxCycles;
    }

    public void runOS() {
        ioModule.start();
        processor.start();
        startTime = System.currentTimeMillis();
        for (PCB p : processes.values()) {
            p.setStartTime(startTime);
        }
        while (processes.size() > 0 && elapsedCycles < maxCycles && !halt) {
            for (Map.Entry<Integer,int[]> entry : waitingOnResources.entrySet()) {
                int pid = entry.getKey();
                int[] resourceRequest = entry.getValue();
                PCB p = processes.get(pid);
                if (p != null && ResourceManager.getInstance().requestResources(pid, resourceRequest)) {
                    p.necessaryResourcesAcquired(resourceRequest);
                }
            }
            ioModule.advance();
            processor.advance();
            // Busy wait until all threads have finished
            while (!(processor.cycleFinished() && ioModule.cycleFinished()));
            // Check for an I/O interrupt
            if (ioModule.interruptGenerated()) {
                ioModule.handleInterrupt();
            }
            if (elapsedCycles % CYCLES_PER_STATUS_PRINTOUT == 0) {
                sendStatus();
            }
            elapsedCycles++;
            sleep();
        }
        sendStatus();
        ioModule.stop();
        processor.stop();
        long haltTime = System.currentTimeMillis();
        elapsedTime += haltTime - startTime;
        for (PCB p : processes.values()) {
            p.setHaltTime(haltTime);
        }
        if (processes.size() == 0) {
            taskManager.setCompleted();
        } else {
            taskManager.setHalted();
        }
    }

    private void sendStatus() {
        // Get all running processes sorted by PID
        List<PCB> executingProcesses =
                processes.values().stream()
                        .filter(pcb -> pcb.getState() == State.RUN)
                        .sorted(Comparator.comparingInt(PCB::getPid))
                        .collect(Collectors.toList());

        long elapsedMs = elapsedTime + System.currentTimeMillis() - startTime;
        int waitingOnCritical = semaphores.stream().mapToInt(Semaphore::getWaitingCount).sum();

        taskManager.updateRunningScene(
                executingProcesses,
                elapsedMs,
                elapsedCycles,
                processes.size(),
                processor.getReadyCount(),
                waitingOnIo.size(),
                waitingOnResources.size(),
                waitingOnCritical,
                terminated.size(),
                processor.getStatistics()
        );
    }

    public void halt() {
        halt = true;
    }

    public void resume() {
        maxCycles = Long.MAX_VALUE;
        halt = false;
        runOS();
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(CYCLE_DELAY_MS);
        } catch (InterruptedException ignored) {}
    }

    public static OperatingSystem getInstance() {
        if (instance == null) {
            instance = new OperatingSystem();
        }
        return instance;
    }

    public void createProcess(Template template) {
        int pid = pidGenerator.getNextPid();
        IPCStandard ipcStandard;
        // Equal chance of being assigned either IPCStandard
        Random random = new Random();
        int standard = random.nextInt(2);
        if (standard == 0) {
            ipcStandard = IPCStandard.MESSAGE_PASSING;
        } else {
            ipcStandard = IPCStandard.ORDINARY_PIPE;
        }
        processes.put(pid, new PCB(template, pid, ipcStandard));
    }

    public int createChildProcess(Template template, int parent, Process childProcess, IPCStandard ipcStandard) {
        int pid = pidGenerator.getNextPid();
        PCB p = new PCB(template, childProcess, pid, parent, ipcStandard);
        processes.put(pid, p);
        p.setStartTime(System.currentTimeMillis());
        return pid;
    }

    public void requestCPU(PCB p) {
        processor.request(p);
    }

    public List<Page> requestMemory(int requestSizeMB) {
        return mainMemory.requestMemory(requestSizeMB);
    }

    public void releaseMemory(List<Page> pages) {
        mainMemory.releaseMemory(pages);
    }

    public void requestIO(int pid) {
        waitingOnIo.add(pid);
    }

    public void releaseIO(int pid) {
        waitingOnIo.remove(pid);
    }

    public void requestCriticalSection(int pid, int index) {
        semaphores.get(index).wait(pid);
    }

    public void releaseCriticalSection(int index) {
        semaphores.get(index).signal();
    }

    public void requestResources(int pid, int[] resourceRequest) {
        if (ResourceManager.getInstance().requestResources(pid, resourceRequest)) {
            PCB p = processes.get(pid);
            if (p != null) {
                p.necessaryResourcesAcquired(resourceRequest);
            }
        } else {
            waitingOnResources.put(pid, resourceRequest);
        }
    }

    public void releaseResources(int pid, int[] releasing) {
        ResourceManager.getInstance().releaseResources(pid, releasing);
    }

    public void removeFromSemaphore(int pid, int index) {
        semaphores.get(index).removeFromQueue(pid);
    }

    public Word readAcrossPageBreak(Page page1, int offset, Page page2) {
        return mainMemory.readAcrossPageBreak(page1, offset, page2);
    }

    public Word read(Page page, int offset) {
        return mainMemory.read(page, offset);
    }

    public void exit(int pid, Set<Integer> children) {
        // Cascading termination
        for (int child : children) {
            PCB p = processes.get(child);
            if (p != null) {
                p.terminateProcess();
            }
        }
        PCB p = processes.remove(pid);
        waitingOnIo.remove(pid);
        waitingOnResources.remove(pid);
        if (p != null) {
            terminated.add(p);
            // For the purpose of recording core statistics
            processor.registerTermination(p);
        }
    }

    PCB pidLookup(int pid) {
        return processes.get(pid);
    }

    // Wrapper class to prevent concurrent access of nextPid
    private static class PidGenerator {
        private int nextPid;

        PidGenerator() {
            nextPid = KERNEL_ID + 1;
        }

        synchronized int getNextPid() {
            return nextPid++;
        }
    }

    private class IoModule {

        private static final int INTERRUPT_RANDOM_BOUND = 16;

        private Thread ioThread;
        private final Object threadCoordinator;
        private final Set<Integer> waitingThisCycle;
        private boolean interrupt;

        IoModule() {
            instantiateThread();
            threadCoordinator = new Object();
            waitingThisCycle = new HashSet<>();
            interrupt = false;
        }

        void start() {
            ioThread.start();
        }

        void advance() {
            waitingThisCycle.addAll(waitingOnIo);
            // Notify waiting ioThread to start a new cycle
            synchronized (threadCoordinator) {
                threadCoordinator.notifyAll();
            }
            generateRandomInterrupt();
        }

        void stop() {
            ioThread.interrupt();
            instantiateThread();
        }

        boolean cycleFinished() {
            return ioThread.getState() == Thread.State.WAITING;
        }

        boolean interruptGenerated() {
            return interrupt;
        }

        // Simulates the use of CPU cycle time to handle an I/O interrupt
        void handleInterrupt() {
            interrupt = false;
            sleep();
        }

        // Provides a 1 in 16 chance of generating an I/O interrupt
        private void generateRandomInterrupt() {
            Random random = new Random();
            if (random.nextInt(INTERRUPT_RANDOM_BOUND) == 0) {
                interrupt = true;
            }
        }

        private void instantiateThread() {
            ioThread = new Thread(() -> {
                boolean run = true;
                while (run) {
                    try {
                        // Wait for signal to begin cycle
                        synchronized (threadCoordinator) {
                            threadCoordinator.wait();
                        }
                        // Progress all processes receiving I/O
                        for (int pid : waitingThisCycle) {
                            PCB p = pidLookup(pid);
                            if (p != null) {
                                p.progressOneCycle();
                            }
                        }
                        waitingThisCycle.clear();
                    } catch (InterruptedException e) {
                        run = false;
                    }
                }
            });
        }

    }

}
