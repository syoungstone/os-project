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
        criticalSecured = false;
        statusChange(null);
    }

    public void progressOneCycle() {
        currentOpSet.progressOneCycle();
        Operation lastOperation = currentOpSet.getOperation();
        if (lastOperation == Operation.FORK) {
            OperatingSystem.getInstance().createChildProcess(template, pid);
        }
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
            currentSection = sections.poll();
            // Process completed
            if (currentSection == null) {
                // Request that OS terminate the current process
                OperatingSystem.getInstance().exit(pid);
            } else {
                currentOpSet = currentSection.getOperationSets().poll();
                // Any section switch means entering or leaving the critical section
                if (!currentSection.isCritical()) {
                    // Release resource protected by semaphore
                    criticalSecured = false;
                    OperatingSystem.getInstance().releaseCriticalSection();
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
                // If current instruction has changed, request new resource
                requestResource();
            }
        }
    }

    public void wakeup() {
        criticalSecured = true;
        requestResource();
    }

    private void requestResource() {
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
