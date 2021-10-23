package Processes;

import Control.OperatingSystem;

public class PCB {

    private final int pid;
    // private final int parentId;
    // private final long startTime;
    // private final Template template;
    private final Process process;
    // private int priority;
    private State state;
    private Process.Section currentSection;
    private Process.OperationSet currentOpSet;
    private boolean criticalSecured;

    public PCB(Template template, int pid, int parentId) {
        state = State.NEW;
        this.pid = pid;
        // this.parentId = parentId;
        // this.template = template;
        process = new Process(template);
        // startTime = System.currentTimeMillis();
        criticalSecured = false;
    }

    public void activate() {
        if (state == State.NEW) {
            statusChange(null);
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

    public void wakeup() {
        criticalSecured = true;
        requestResource();
    }

    private void requestResource() {
        if (currentSection.isCritical() && !criticalSecured) {
            state = State.WAIT;
            OperatingSystem.getInstance().requestCriticalSection(pid);
        } else if (currentOpSet.getOperation() == Operation.CALCULATE) {
            state = State.READY;
            OperatingSystem.getInstance().requestCPU(pid);
        } else if (currentOpSet.getOperation() == Operation.IO) {
            state = State.WAIT;
            OperatingSystem.getInstance().requestIO(pid);
        }
    }

    public void progressOneCycle() {
        currentOpSet.progressOneCycle();
        Operation lastOperation = currentOpSet.getOperation();

        // if (lastOperation == Operation.FORK) {
        //     OperatingSystem.getInstance().createChildProcess(template, pid);
        // }

        // Current set of operations completed
        if (currentOpSet.getCycles() == 0) {
            currentOpSet = currentSection.getOperationSets().poll();
            statusChange(lastOperation);
        }
    }

    // Invoked every time we switch to a new set of operations
    private void statusChange(Operation lastOperation) {
        // Process just created or section just completed
        if (currentOpSet == null) {
            currentSection = process.nextSection();
            // Process completed
            if (currentSection == null) {
                // Request that OS terminate the current process
                OperatingSystem.getInstance().exit(pid);
            } else {
                currentOpSet = currentSection.getOperationSets().poll();
                // Any section switch means entering or leaving the critical section
                if (!currentSection.isCritical()) {
                    if (criticalSecured) {
                        // Release resource protected by semaphore
                        criticalSecured = false;
                        OperatingSystem.getInstance().releaseCriticalSection();
                    }
                    // If current instruction has changed, request new resource
                    if (lastOperation != currentOpSet.getOperation()) {
                        requestResource();
                    }
                } else {
                    // Request new resource
                    requestResource();
                }
            }
        }
        // Operation set just completed
        else {
            if (lastOperation != currentOpSet.getOperation()) {
                if (lastOperation == Operation.IO) {
                    OperatingSystem.getInstance().completeIO(pid);
                }
                // If current instruction has changed, request new resource
                requestResource();
            }
        }
    }

}
