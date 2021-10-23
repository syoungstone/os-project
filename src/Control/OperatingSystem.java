package Control;

import Processes.MalformedTemplateException;
import Processes.PCB;
import Processes.Template;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class OperatingSystem {

    static final int KERNEL_ID = 0;
    private static final int CYCLE_DELAY_MS = 2;
    private static final int CYCLES_PER_STATUS_PRINTOUT = 200;

    private static OperatingSystem instance;

    private final Processor CPU;

    private int nextPid;
    private long startTime = 0;
    private final List<Template> templates;
    private final Map<Integer, PCB> processes;
    private final Set<Integer> waiting;
    private final Set<Integer> doneWaiting;
    private final Semaphore semaphore;

    private OperatingSystem() {
        CPU = new Processor();
        nextPid = KERNEL_ID + 1;
        templates = new ArrayList<>();
        processes = new HashMap<>();
        waiting = new HashSet<>();
        doneWaiting = new HashSet<>();
        semaphore = new Semaphore();
    }

    public void boot() {
        System.out.println("Booting up...");
        System.out.println("Loading templates...");
        try {
            templates.addAll(Template.getTemplates());
            selectTemplates();
            runOS();
        } catch (MalformedTemplateException e) {
            System.out.println(e.getMessage());
            System.out.println("Exiting...");
        }
    }

    private void selectTemplates() {
        System.out.println("Available process templates:");
        int templateNumber = 1;
        for (Template t : templates) {
            System.out.println(templateNumber + ") " + t.name());
            templateNumber++;
        }
        Scanner sc = new Scanner(System.in);
        for (Template t : templates) {
            boolean validInput = false;
            while(!validInput) {
                System.out.println("Select a number of processes to create using template: " + t.name());
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
        System.out.println("All processes successfully created. Launching OS...");
        runOS();
    }

    private void runOS() {
        startTime = System.currentTimeMillis();
        int cyclesTilNextPrintout = CYCLES_PER_STATUS_PRINTOUT;
        while (processes.size() > 0) {
            CPU.advance();
            for (int pid : waiting) {
                pidLookup(pid).progressOneCycle();
            }
            /* Use of HashSet doneWaiting here avoids ConcurrentModificationException by
             * preventing the modification of HashSet waiting while iterating over it */
            waiting.removeAll(doneWaiting);
            doneWaiting.clear();
            if (--cyclesTilNextPrintout <= 0) {
                printStatus();
            }
            sleep();
        }
    }

    private void printStatus() {
        System.out.println("-------------------------STATUS REPORT-------------------------");
        long msElapsed = System.currentTimeMillis() - startTime;
        int minutesElapsed = (int) (msElapsed / (1000 * 60));
        int secondsElapsed = (int) ((msElapsed % (1000 * 60)) / (1000));
        int millisecondsElapsed = (int) (msElapsed % 1000);
        System.out.print("Time since startup: " + minutesElapsed + " min, " + secondsElapsed + " sec, ");
        System.out.println(millisecondsElapsed + " ms");
        System.out.println("PID of process in CPU: " + CPU.getCurrentPid());
        System.out.println("Total processes running: " + processes.size());
        System.out.println("Total processes in ready queue: " + CPU.getReadyCount());
        System.out.println("Total processes executing I/O cycles: " + waiting.size());
        System.out.println("Total processes waiting on critical section: " + semaphore.getWaitingCount());
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
        createChildProcess(template, KERNEL_ID);
    }

    public void createChildProcess(Template template, int parentId) {
        int pid = nextPid++;
        PCB p = new PCB(template, pid, parentId);
        processes.put(pid, p);
        p.activate();
    }

    public void requestCPU(int pid) {
        CPU.request(pid);
    }

    public void requestCriticalSection(int pid) {
        semaphore.wait(pid);
    }

    public void requestIO(int pid) {
        waiting.add(pid);
    }

    public void completeIO(int pid) {
        doneWaiting.add(pid);
    }

    public void releaseCriticalSection() {
        semaphore.signal();
    }

    public void wakeup(int pid) {
        PCB p = pidLookup(pid);
        if (p != null) {
            p.wakeup();
        }
    }

    public void exit(int pid) {
        processes.remove(pid);
    }

    PCB pidLookup(int pid) {
        return processes.get(pid);
    }

}
