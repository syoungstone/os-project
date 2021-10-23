import jdk.nashorn.internal.runtime.ECMAException;

import java.util.*;

public class OperatingSystem {

    private static final int KERNEL_ID = 0;

    private static OperatingSystem instance;

    private final Processor CPU;

    private int nextPid;
    private final List<Template> templates;
    private final Map<Integer,PCB> processes;
    private final Set<Integer> waiting;
    private final Scheduler shortTermScheduler;
    private final Semaphore semaphore;

    private OperatingSystem() {
        CPU = new Processor();
        nextPid = KERNEL_ID + 1;
        templates = new ArrayList<>();
        processes = new HashMap<>();
        waiting = new HashSet<>();
        shortTermScheduler = new RRScheduler();
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
            System.out.println("Select a number of processes to create using template: " + t.name());
            try {
                int numProcesses = sc.nextInt();
                System.out.println("Creating " + numProcesses + " processes from template: " + t.name());
                for (int i = 0 ; i < numProcesses ; i++) {
                    createProcess(t);
                }
            } catch (Exception e) {
                System.out.println("Invalid input. Please try again.");
            }
        }
        System.out.println("All processes successfully created. Launching OS...");
        runOS();
    }

    private void runOS() {
        while (processes.size() > 0) {
            pidLookup(CPU.getPid()).progressOneCycle();
            CPU.advance();
            for (int pid : waiting) {
                pidLookup(pid).progressOneCycle();
            }
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
        int pid = nextPid++;
        PCB p = new PCB(template, pid, parentId);
        processes.put(pid, p);
    }

    public void requestCPU(int pid) {
        shortTermScheduler.add(pid);
        changeState(pid, State.READY);
    }

    public void requestCriticalSection(int pid) {
        semaphore.wait(pid);
        changeState(pid, State.WAIT);
    }

    public void requestIO(int pid) {
        waiting.add(pid);
        changeState(pid, State.WAIT);
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
        PCB p = processes.remove(pid);
        if (p != null) {
            p.setState(State.EXIT);
        }
        if (CPU.getPid() == pid) {
            scheduleNew();
        }
    }

    private PCB pidLookup(int pid) {
        return processes.get(pid);
    }

    private void changeState(int pid, State newState) {
        PCB p = pidLookup(pid);
        p.setState(newState);
    }

    private void scheduleNew() {
        PCB currentProcess = pidLookup(CPU.getPid());
        if (currentProcess != null && currentProcess.getState() == State.RUN) {
            currentProcess.setState(State.READY);
        }
        int newPid = shortTermScheduler.remove();
        PCB newProcess = pidLookup(newPid);
        newProcess.setState(State.RUN);
        CPU.setPid(newPid);
    }

    private class Processor {

        private static final int TIME_QUANTUM = 10;

        private int pid;
        private int counter;

        Processor() {
            counter = 0;
        }

        public void advance() {
            counter++;
            if (counter >= TIME_QUANTUM) {
                scheduleNew();
                counter = 0;
            }
        }

        public int getPid() {
            return pid;
        }

        public void setPid(int pid) {
            this.pid = pid;
        }

    }

}
