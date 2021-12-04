package Processes;

import Communication.*;
import Control.OperatingSystem;
import Control.ResourceManager;
import Memory.Page;
import Memory.Word;
import Processor.Processor;

import java.util.*;

public class PCB implements Comparable<PCB> {

    private static final int FORK_RANDOM_BOUND = 4;
    private static final int RESOURCE_REQUEST_RANDOM_BOUND = 8;

    private final Processor.CoreId coreId;
    private final IPCStandard ipcStandard;

    private long startTime = 0;
    private long currentWaitStartTime = 0;
    private long waitingTime = 0;

    private final int pid;
    private final int parent;
    private final Set<Integer> children;

    // Only used if ipcStandard is IPCStandard.ORDINARY_PIPE
    private final OrdinaryPipe pipeFromParent;
    private final Map<Integer,OrdinaryPipe> pipesToChildren;

    private final int memoryRequiredBytes;
    private final List<Page> pageTable;
    private final Register register;
    private Integer lastPageAccessed;

    private final int[] maxResources;
    private final int[] currentResources;

    private final Template template;
    private final Process process;
    private final Priority priority;

    private State state;
    private Process.Section currentSection;
    private Process.OperationSet currentOpSet;
    private Operation lastCompletedOperation;
    private boolean criticalSecured;

    // For a process created at startup
    public PCB(Template template, int pid, IPCStandard ipcStandard) {
        this(template, new Process(template), pid, OperatingSystem.KERNEL_ID, ipcStandard);
    }

    // For a process created by a FORK
    public PCB(Template template, Process process, int pid, int parent, IPCStandard ipcStandard) {
        this.state = State.NEW;

        this.ipcStandard = ipcStandard;
        pipeFromParent = ipcStandard == IPCStandard.ORDINARY_PIPE
                ? PipeManager.getInstance().createPipe(parent, pid)
                : null;
        pipesToChildren = ipcStandard == IPCStandard.ORDINARY_PIPE
                ? new HashMap<>()
                : null;

        // Equal chance of being assigned either processor core
        // Process will always be sent to same core when requesting CPU
        Random random = new Random();
        int coreId = random.nextInt(2);
        if (coreId == 0) {
            this.coreId = Processor.CoreId.CORE1;
        } else {
            this.coreId = Processor.CoreId.CORE2;
        }

        this.pid = pid;
        this.parent = parent;
        this.children = new HashSet<>();

        int memoryRequiredMB = template.memoryRequirements();
        this.memoryRequiredBytes = 1024 * 1024 * memoryRequiredMB;
        List<Page> pages = OperatingSystem.getInstance().requestMemory(memoryRequiredMB);
        this.pageTable = new ArrayList<>(pages);
        this.register = new Register();
        this.lastPageAccessed = null;

        maxResources = new int[ResourceManager.NUM_RESOURCE_TYPES];
        currentResources = new int[ResourceManager.NUM_RESOURCE_TYPES];
        // Randomly deciding the maximum resources requested by this process
        for (int i = 0 ; i < ResourceManager.NUM_RESOURCE_TYPES ; i++) {
            maxResources[i] = random.nextInt(ResourceManager.NUM_RESOURCES_PER_TYPE / 4);
        }
        ResourceManager.getInstance().addProcess(pid, maxResources);

        this.template = template;
        this.process = process;

        // Equal chance of being assigned each priority
        int priority = random.nextInt(3);
        if (priority == 0) {
            this.priority = Priority.LOW;
        } else if (priority == 1) {
            this.priority = Priority.MEDIUM;
        } else {
            this.priority = Priority.HIGH;
        }

        this.criticalSecured = false;

        newOpSet();
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
        if (state == State.READY) {
            this.currentWaitStartTime = startTime;
        }
    }

    public long getTurnaroundTime() {
        return System.currentTimeMillis() - startTime;
    }

    public long getWaitingTime() {
        return waitingTime;
    }

    public Processor.CoreId getCoreId() {
        return coreId;
    }

    public Priority getPriority() {
        return priority;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
        if (state == State.READY) {
            currentWaitStartTime = System.currentTimeMillis();
        } else if (state == State.RUN) {
            waitingTime += (System.currentTimeMillis() - currentWaitStartTime);
        }
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
            if (ipcStandard == IPCStandard.MESSAGE_PASSING) {
                sendMessage();
                receiveMessage();
            } else {
                writeToPipes();
                readFromPipe();
            }
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
            int childPid = OperatingSystem.getInstance().createChildProcess(template, pid, childProcess, ipcStandard);
            children.add(childPid);
            if (ipcStandard == IPCStandard.ORDINARY_PIPE) {
                pipesToChildren.put(childPid, PipeManager.getInstance().retrievePipe(childPid));
            }
        }
    }

    // Simulates memory access required to perform a calculation
    private void memoryAccess() {
        int logicalAddress = generateLogicalAddress();
        Word contents = readFromMemory(logicalAddress);
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

    private Word readFromMemory(int logicalAddress) {
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

    // Send current value of Register to all child processes
    private void sendMessage() {
        for (int child : children) {
            MessagePasser.getInstance().send(child, new Message(pid, register.getContents()));
        }
    }

    // Retrieve up to one Message waiting for the process in the MessagePasser
    private void receiveMessage() {
        Message message = MessagePasser.getInstance().receive(pid);
        if (message != null && message.getSender() == parent) {
            // Set register to the contents of the message
            // logicalAddress of -1 indicates source of register contents was not one of this process's pages
            register.set(-1, message.getContents());
        }
    }

    // Send current value of Register to all child processes
    private void writeToPipes() {
        for (int child : children) {
            pipesToChildren.get(child).write(pid, register.getContents());
        }
    }

    // Retrieve up to one piece of information waiting for the process in the Pipe
    private void readFromPipe() {
        if (parent != OperatingSystem.KERNEL_ID) {
            Word word = pipeFromParent.read(pid);
            if (word != null) {
                // Set register to the word retrieved from the Pipe
                // logicalAddress of -1 indicates source of register contents was not one of this process's pages
                register.set(-1, word);
            }
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
        ResourceManager.getInstance().removeProcess(pid);
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
            // Possibility of needing to acquire more resources before proceeding
            Random random = new Random();
            int[] resourceRequest = new int[ResourceManager.NUM_RESOURCE_TYPES];
            if (random.nextInt(RESOURCE_REQUEST_RANDOM_BOUND) == 0) {
                for (int i = 0 ; i < ResourceManager.NUM_RESOURCE_TYPES ; i++) {
                    resourceRequest[i] = random.nextInt(maxResources[i] - currentResources[i] + 1);
                }
                OperatingSystem.getInstance().requestResources(pid, resourceRequest);
            } else {
                necessaryResourcesAcquired(resourceRequest);
            }
        } else if (currentOpSet.getOperation() == Operation.IO) {
            state = State.WAIT;
            OperatingSystem.getInstance().requestIO(pid);
        }
    }

    // Called when done waiting on resources and ready to wait on CPU
    public synchronized void necessaryResourcesAcquired(int[] resourceRequest) {
        for (int i = 0 ; i < ResourceManager.NUM_RESOURCE_TYPES ; i++) {
            currentResources[i] += resourceRequest[i];
        }
        state = State.READY;
        currentWaitStartTime = System.currentTimeMillis();
        OperatingSystem.getInstance().requestCPU(this);
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
