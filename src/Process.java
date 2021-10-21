import java.util.LinkedList;
import java.util.Queue;

public class Process {

    private final int pid;
    private final Template template;
    private final Queue<Section> sections;
    private Section currentSection;
    private OperationSet currentOpSet;
    private boolean criticalSecured;

    public Process(Template template, int pid) {
        this.pid = pid;
        this.template = template;
        sections = new LinkedList<>();
        for (Template.Section tSection : template.getSections()) {
            Section pSection = new Section(tSection.isCritical());
            for (Template.OperationSet tOpSet : tSection.getOperationSets()) {
                OperationSet pOpSet = new OperationSet(tOpSet.getOperation(), tOpSet.generateCycleCount());
                pSection.getOperationSets().add(pOpSet);
            }
            sections.add(pSection);
        }
        currentSection = sections.poll();
        currentOpSet = currentSection.getOperationSets().poll();
        criticalSecured = false;
    }

    // Returns true if process is complete, false otherwise
    public boolean progressOneCycle() {
        currentOpSet.progressOneCycle();
        Operation completedOperation = currentOpSet.getOperation();
        if (completedOperation == Operation.FORK) {
            OperatingSystem.getInstance().createChildProcess(template, pid);
        }
        // Current set of operations completed
        if (currentOpSet.getCycles() == 0) {
            currentOpSet = currentSection.getOperationSets().poll();
            // Current section completed
            if (currentOpSet == null) {
                currentSection = sections.poll();
                // Process completed
                if (currentSection == null) {
                    return true;
                }
                currentOpSet = currentSection.getOperationSets().poll();
                // Any section switch means entering or leaving the critical section
                if (!currentSection.isCritical()) {
                    // Release resource protected by semaphore
                    criticalSecured = false;
                    OperatingSystem.getInstance().releaseCriticalSection();
                    // If current instruction has changed, request new resource
                    if (completedOperation != currentOpSet.getOperation()) {
                        statusChange();
                    }
                } else {
                    // Request new resource
                    statusChange();
                }
            } else {
                if (completedOperation != currentOpSet.getOperation()) {
                    // If current instruction has changed, request new resource
                    statusChange();
                }
            }
        }
        return false;
    }

    public void wakeup() {
        criticalSecured = true;
        statusChange();
    }

    public int getRemainingCycles() {
        return currentOpSet.getCycles();
    }

    private void statusChange() {
        if (currentSection.isCritical() && !criticalSecured) {
            OperatingSystem.getInstance().requestCriticalSection(pid);
        } else if (currentOpSet.getOperation() == Operation.CALCULATE) {
            OperatingSystem.getInstance().requestCPU(pid);
        } else if (currentOpSet.getOperation() == Operation.IO) {
            OperatingSystem.getInstance().requestIO(pid);
        }
    }

    private static class Section {
        private Queue<OperationSet> operationSets;
        private final boolean critical;

        Section(boolean critical) {
            this.critical = critical;
        }

        Queue<OperationSet> getOperationSets() {
            if (operationSets == null) {
                operationSets = new LinkedList<>();
            }
            return operationSets;
        }

        boolean isCritical() {
            return critical;
        }
    }

    private static class OperationSet {
        private final Operation operation;
        private int cycles;

        OperationSet(Operation operation, int cycles) {
            this.operation = operation;
            this.cycles = cycles;
        }

        Operation getOperation() {
            return operation;
        }

        void progressOneCycle() {
            cycles--;
        }

        int getCycles() {
            return cycles;
        }
    }
}
