package Processes;

import Control.OperatingSystem;
import Memory.Page;
import Memory.Word;

import java.util.*;

public class PCB implements Comparable<PCB> {

    private static final int FORK_RANDOM_BOUND = 4;

    private final int pid;
    private final int parent;
    private final Set<Integer> children;

    private final int memoryRequiredMB;
    private final int memoryRequiredBytes;
    private final List<Page> pageTable;
    private final Register register;
    private Integer lastPageAccessed;

    // private final long startTime;
    private final Template template;
    private final Process process;
    // private int priority;

    private State state;
    private Process.Section currentSection;
    private Process.OperationSet currentOpSet;
    private Operation lastCompletedOperation;
    private boolean criticalSecured;

    // For a process created at startup
    public PCB(Template template, int pid) {
        this(template, new Process(template), pid, OperatingSystem.KERNEL_ID);
    }

    // For a process created by a FORK
    public PCB(Template template, Process process, int pid, int parent) {
        this.state = State.NEW;

        this.pid = pid;
        this.parent = parent;
        this.children = new HashSet<>();

        this.memoryRequiredMB = template.memoryRequirements();
        this.memoryRequiredBytes = 1024 * 1024 * memoryRequiredMB;
        List<Page> pages = OperatingSystem.getInstance().requestMemory(memoryRequiredMB);
        this.pageTable = new ArrayList<>(pages);
        this.register = new Register();
        this.lastPageAccessed = null;

        this.template = template;
        this.process = process;
        // startTime = System.currentTimeMillis();
        this.criticalSecured = false;

        newOpSet();
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

    public synchronized void progressOneCycle() {
        currentOpSet.progressOneCycle();

        if (currentOpSet.getOperation() == Operation.FORK) {
            fork();
        } else if (currentOpSet.getOperation() == Operation.CALCULATE) {
            memoryAccess();
        }

        // Current set of operations completed
        if (currentOpSet.getCycles() == 0) {
            lastCompletedOperation = currentOpSet.getOperation();
            currentOpSet = currentSection.getOperationSets().poll();
            newOpSet();
        }
    }

    // FORK operation results in a 1/FORK_RANDOM_BOUND chance of a child process being created
    private void fork() {
        Random random = new Random();
        if (random.nextInt(FORK_RANDOM_BOUND) == 0) {
            Process childProcess = new Process(process, currentSection);
            int childPid = OperatingSystem.getInstance().createChildProcess(template, pid, childProcess);
            children.add(childPid);
        }
    }

    // Simulates memory access required to perform a calculation
    private void memoryAccess() {
        int logicalAddress = generateLogicalAddress();
        Word contents = read(logicalAddress);
        register.set(logicalAddress, contents);
    }

    // Generates a logical address to be read based on a set of rules
    private int generateLogicalAddress() {
        Random random = new Random();
        // Check if a previous memory read has been stored
        if (register.isSet() && lastPageAccessed != null) {
            // 50% chance of reading from same logical address as last read
            // 25% chance of reading from same page as last read
            if (Math.random() < 0.5) {
                return register.getLogicalAddress();
            } else if (Math.random() < 0.5) {
                // Check whether using the last page
                if (lastPageAccessed < pageTable.size() - 1) {
                    return random.nextInt(Page.getSizeBytes())
                            + pageTable.get(lastPageAccessed).getStartAddress();
                } else {
                    // If using the last page, make sure address is valid
                    int bytesUsedInFinalPage;
                    if (memoryRequiredBytes % Page.getSizeBytes() == 0) {
                        bytesUsedInFinalPage = Page.getSizeBytes();
                    } else {
                        bytesUsedInFinalPage = memoryRequiredBytes % Page.getSizeBytes();
                    }
                    return random.nextInt(bytesUsedInFinalPage - Word.WORD_SIZE_IN_BYTES + 1)
                            + pageTable.get(lastPageAccessed).getStartAddress();
                }
            }
        }
        return random.nextInt(memoryRequiredBytes - Word.WORD_SIZE_IN_BYTES + 1);
    }

    private Word read(int logicalAddress) {
        // Check if contents of logical address already stored in register
        if (register.isSet() && register.getLogicalAddress() == logicalAddress) {
            return register.getContents();
        } else {
            int pageNumber = logicalAddress / Page.getSizeBytes();
            int offset = logicalAddress % Page.getSizeBytes();
            Word contents;

            // Check if desired word straddles two pages
            if (offset > Page.getSizeBytes() - Word.WORD_SIZE_IN_BYTES) {
                Page page1 = pageTable.get(pageNumber);
                Page page2 = pageTable.get(pageNumber + 1);
                contents = OperatingSystem.getInstance().readAcrossPageBreak(page1, offset, page2);
            } else {
                Page page = pageTable.get(pageNumber);
                contents = OperatingSystem.getInstance().read(page, offset);
            }
            lastPageAccessed = pageNumber;
            return contents;
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

    public synchronized void terminateProcess() {
        state = State.EXIT;
        OperatingSystem.getInstance().removeFromSemaphore(pid, template.index());
        releaseCriticalSection();
        OperatingSystem.getInstance().releaseIO(pid);
        OperatingSystem.getInstance().releaseMemory(pageTable);
        OperatingSystem.getInstance().exit(pid, children);
    }

    private void releaseIO() {
        if (lastCompletedOperation == Operation.IO) {
            OperatingSystem.getInstance().releaseIO(pid);
        }
    }

    private void requestCriticalSection() {
        state = State.WAIT;
        OperatingSystem.getInstance().requestCriticalSection(pid, template.index());
    }

    private void releaseCriticalSection() {
        if (criticalSecured) {
            criticalSecured = false;
            OperatingSystem.getInstance().releaseCriticalSection(template.index());
        }
    }

    public synchronized void wakeup() {
        criticalSecured = true;
        requestResource();
    }

    private void conditionalRequestResource() {
        if (lastCompletedOperation == null
                || (lastCompletedOperation == Operation.IO
                        && currentOpSet.getOperation() != Operation.IO)
                || (lastCompletedOperation != Operation.IO
                        && currentOpSet.getOperation() == Operation.IO)) {
            releaseIO();
            requestResource();
        }
    }

    private void requestResource() {
        if (currentOpSet.getOperation() == Operation.CALCULATE
                || currentOpSet.getOperation() == Operation.FORK) {
            state = State.READY;
            OperatingSystem.getInstance().requestCPU(this);
        } else if (currentOpSet.getOperation() == Operation.IO) {
            state = State.WAIT;
            OperatingSystem.getInstance().requestIO(pid);
        }
    }

    // Compares PCBs by length of CALCULATE bursts for the purpose of the SJFScheduler
    @Override
    public int compareTo(PCB o) {
        return this.getRemainingCalculateCycles().compareTo(o.getRemainingCalculateCycles());
    }

    private synchronized Integer getRemainingCalculateCycles() {
        if (currentOpSet != null && Operation.CALCULATE.equals(currentOpSet.getOperation())) {
            return currentOpSet.getCycles();
        }
        return 0;
    }

    private static class Register {
        private int logicalAddress;
        private Word contents;
        private boolean set;

        Register() {
            set = false;
        }

        public void set(int logicalAddress, Word contents) {
            this.logicalAddress = logicalAddress;
            this.contents = contents;
            this.set = true;
        }

        public boolean isSet() {
            return set;
        }

        public int getLogicalAddress() {
            return logicalAddress;
        }

        public Word getContents() {
            return contents;
        }
    }
}
