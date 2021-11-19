package Processes;

import Control.OperatingSystem;

import java.util.Random;

public class PCB {

    private static final int FORK_RANDOM_BOUND = 4;

    private final int pid;
    private final int parentId;
    // private final long startTime;
    private final Template template;
    private final Process process;
    // private int priority;
    private State state;
    private Process.Section currentSection;
    private Process.OperationSet currentOpSet;
    private Operation lastCompletedOperation;
    private boolean criticalSecured;

    public PCB(Template template, int pid, int parentId) {
        state = State.NEW;
        this.pid = pid;
        this.parentId = parentId;
        this.template = template;
        process = new Process(template);
        // startTime = System.currentTimeMillis();
        criticalSecured = false;
    }

    public void activate() {
        if (state == State.NEW) {
            newOpSet();
        }
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getPid() {
        return pid;
    }

    public void progressOneCycle() {
        currentOpSet.progressOneCycle();

        // FORK operation results in a 1/FORK_RANDOM_BOUND chance of a child process being created
        if (currentOpSet.getOperation() == Operation.FORK) {
            Random random = new Random();
            if (random.nextInt(FORK_RANDOM_BOUND) == 0) {
                OperatingSystem.getInstance().createChildProcess(template, pid);
            }
        }

        // Current set of operations completed
        if (currentOpSet.getCycles() == 0) {
            lastCompletedOperation = currentOpSet.getOperation();
            currentOpSet = currentSection.getOperationSets().poll();
            newOpSet();
        }
    }

    // Invoked every time we switch to a new set of operations
    private void newOpSet() {
        // Process just created or section just completed
        if (currentOpSet == null) {
            currentSection = process.nextSection();
            // Process completed
            if (currentSection == null) {
                terminateProcess();
                // Entering new section
            } else {
                currentOpSet = currentSection.getOperationSets().poll();
                // Any section switch means entering or leaving the critical section
                if (currentSection.isCritical()) {
                    releaseIO();
                    requestCriticalSection();
                } else {
                    releaseCriticalSection();
                    conditionalRequestResource();
                }
            }
        }
        // Operation set just completed
        else {
            conditionalRequestResource();
        }
    }

    private void terminateProcess() {
        state = State.EXIT;
        releaseIO();
        releaseCriticalSection();
        OperatingSystem.getInstance().exit(pid);
    }

    private void releaseIO() {
        if (lastCompletedOperation == Operation.IO) {
            OperatingSystem.getInstance().releaseIO(pid);
        }
    }

    private void requestCriticalSection() {
        state = State.WAIT;
        OperatingSystem.getInstance().requestCriticalSection(pid);
    }

    private void releaseCriticalSection() {
        if (criticalSecured) {
            criticalSecured = false;
            OperatingSystem.getInstance().releaseCriticalSection();
        }
    }

    public void wakeup() {
        criticalSecured = true;
        requestResource();
    }

    private void conditionalRequestResource() {
        if (lastCompletedOperation != currentOpSet.getOperation()) {
            releaseIO();
            requestResource();
        }
    }

    private void requestResource() {
        if (currentOpSet.getOperation() == Operation.CALCULATE) {
            state = State.READY;
            OperatingSystem.getInstance().requestCPU(pid);
        } else if (currentOpSet.getOperation() == Operation.IO) {
            state = State.WAIT;
            OperatingSystem.getInstance().requestIO(pid);
        }
    }


}
