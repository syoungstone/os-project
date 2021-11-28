package Control;

import Memory.MainMemory;
import Memory.Page;
import Memory.VirtualMemory;
import Memory.Word;
import Processes.MalformedTemplateException;
import Processes.PCB;
import Processes.Process;
import Processes.Template;
import Processor.Processor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class OperatingSystem {

    public static final int KERNEL_ID = 0;
    private static final int CYCLE_DELAY_MS = 2;
    private static final int CYCLES_PER_STATUS_PRINTOUT = 200;

    private static OperatingSystem instance;

    private final Processor processor;
    private final IoModule ioModule;
    private final MainMemory mainMemory;
    private final VirtualMemory virtualMemory;

    private long maxCycles;
    private long elapsedCycles;
    private long startTime = 0;
    private final Map<Integer, PCB> processes;
    private final List<PCB> terminated;
    private final Set<Integer> waiting;
    private final Semaphore semaphore;
    private final PidGenerator pidGenerator;

    private OperatingSystem() {
        processor = new Processor();
        ioModule = new IoModule();
        mainMemory = MainMemory.getInstance();
        virtualMemory = VirtualMemory.getInstance();

        elapsedCycles = 0;
        // ConcurrentHashMap class & Collections.synchronized methods for thread safety
        processes = new ConcurrentHashMap<>();
        terminated = Collections.synchronizedList(new ArrayList<>());
        waiting = Collections.synchronizedSet(new HashSet<>());
        // Semaphore & PidGenerator instance methods are synchronized for thread safety
        semaphore = new Semaphore();
        pidGenerator = new PidGenerator();
    }

    public void boot() {
        System.out.println("Booting up...");
        System.out.println("Loading templates...");
        try {
            selectTemplates(Template.getTemplates());
            runOS();
        } catch (MalformedTemplateException e) {
            System.out.println(e.getMessage());
            System.out.println("Exiting...");
        }
    }

    private void selectTemplates(List<Template> templates) {
        System.out.println("\nAvailable process templates:");
        int templateNumber = 1;
        for (Template t : templates) {
            System.out.println(templateNumber + ") " + t.name());
            templateNumber++;
        }
        Scanner sc = new Scanner(System.in);
        for (Template t : templates) {
            boolean validInput = false;
            while(!validInput) {
                System.out.println("\nSelect a number of processes to create using template: " + t.name());
                try {
                    int numProcesses = Integer.parseInt(sc.nextLine());
                    validInput = true;
                    System.out.println("Creating " + numProcesses + " processes from template: " + t.name());
                    for (int i = 0; i < numProcesses; i++) {
                        createProcess(t);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please try again.");
                }
            }
        }
        System.out.println("\nAll processes successfully created.");
        boolean validInput = false;
        while(!validInput) {
            System.out.println("\nPlease enter a number of cycles to execute before halting OS,");
            System.out.println("or enter 'none' to continue until last process terminates:");
            try {
                String input = sc.nextLine().trim().toLowerCase();
                if (input.equals("none")) {
                    maxCycles = Long.MAX_VALUE;
                } else {
                    maxCycles = Integer.parseInt(input);
                }
                validInput = true;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please try again.");
            }
        }
        System.out.println("Thank you. Launching OS...");
    }

    private void runOS() {
        ioModule.start();
        processor.start();
        startTime = System.currentTimeMillis();
        while (processes.size() > 0 && elapsedCycles < maxCycles) {
            ioModule.advance();
            processor.advance();
            // Busy wait until all threads have finished
            while (!(processor.cycleFinished() && ioModule.cycleFinished()));
            if (elapsedCycles % CYCLES_PER_STATUS_PRINTOUT == 0) {
                printStatus();
            }
            elapsedCycles++;
            sleep();
        }
        ioModule.stop();
        processor.stop();
        if (processes.size() == 0) {
            System.out.println("\nAll processes terminated. Goodbye!");
        } else {
            System.out.println("\nTotal cycles elapsed has reached the maximum amount of " + maxCycles + ". Halting.");
        }
    }

    private void printStatus() {
        System.out.println("\n-------------------------STATUS REPORT-------------------------");
        long msElapsed = System.currentTimeMillis() - startTime;
        int minutesElapsed = (int) (msElapsed / (1000 * 60));
        int secondsElapsed = (int) ((msElapsed % (1000 * 60)) / (1000));
        int millisecondsElapsed = (int) (msElapsed % 1000);
        System.out.print("Time since startup: " + minutesElapsed + " min, " + secondsElapsed + " sec, ");
        System.out.println(millisecondsElapsed + " ms");
        System.out.println("Cycles elapsed: " + elapsedCycles);
        System.out.println("PIDs of processes in CPU: " + processor.getCurrentPids());
        System.out.println("Total processes running: " + processes.size());
        System.out.println("Total processes in ready queue: " + processor.getReadyCount());
        System.out.println("Total processes executing I/O cycles: " + waiting.size());
        System.out.println("Total processes waiting on critical section: " + semaphore.getWaitingCount());
        System.out.println("Total processes terminated: " + terminated.size());
        System.out.println("---------------------------------------------------------------");
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
        PCB p = new PCB(template, pid);
        processes.put(pid, p);
        p.activate();
    }

    public int createChildProcess(Template template, int parent, Process childProcess) {
        int pid = pidGenerator.getNextPid();
        PCB p = new PCB(template, childProcess, pid, parent);
        processes.put(pid, p);
        p.activate();
        return pid;
    }

    public void requestCPU(int pid) {
        processor.request(processes.get(pid));
    }

    public List<Page> requestMemory(int requestSizeMB) {
        return mainMemory.requestMemory(requestSizeMB);
    }

    public void releaseMemory(List<Page> pages) {
        mainMemory.releaseMemory(pages);
    }

    public void requestIO(int pid) {
        waiting.add(pid);
    }

    public void releaseIO(int pid) {
        waiting.remove(pid);
    }

    public void requestCriticalSection(int pid) {
        semaphore.wait(pid);
    }

    public void releaseCriticalSection() {
        semaphore.signal();
    }

    public void removeFromSemaphore(int pid) {
        semaphore.removeFromQueue(pid);
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
        terminated.add(processes.remove(pid));
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

        private Thread ioThread;
        private final Object threadCoordinator;
        private final Set<Integer> waitingThisCycle;

        IoModule() {
            instantiateThread();
            threadCoordinator = new Object();
            waitingThisCycle = new HashSet<>();
        }

        void start() {
            ioThread.start();
        }

        void advance() {
            waitingThisCycle.addAll(waiting);
            // Notify waiting ioThread to start a new cycle
            synchronized (threadCoordinator) {
                threadCoordinator.notifyAll();
            }
        }

        void stop() {
            ioThread.interrupt();
            instantiateThread();
        }

        boolean cycleFinished() {
            return ioThread.getState() == Thread.State.WAITING;
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
                            pidLookup(pid).progressOneCycle();
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
