package Control;

import Processes.PCB;
import Processes.State;

class Processor {

    private static final int TIME_QUANTUM = 10;

    private final Scheduler shortTermScheduler;

    private PCB p;
    private int counter;

    Processor() {
        counter = 0;
        shortTermScheduler = new RRScheduler();
    }

    public void request(int pid) {
        if (pid > OperatingSystem.KERNEL_ID) {
            shortTermScheduler.add(pid);
            if (p == null) {
                scheduleNew();
            }
        }
    }

    public void advance() {
        if (p == null || p.getState() != State.RUN){
            scheduleNew();
        }
        if (p != null) {
            p.progressOneCycle();
            counter++;
            if (counter >= TIME_QUANTUM) {
                scheduleNew();
                counter = 0;
            }
        } else {
            counter = 0;
        }
    }

    public int getCurrent() {
        if (p == null) {
            return OperatingSystem.KERNEL_ID;
        } else {
            return p.getPid();
        }
    }

    public int getReadyCount() {
        return shortTermScheduler.getReadyCount();
    }

    private void scheduleNew() {
        if (p != null && p.getState() == State.RUN) {
            p.setState(State.READY);
            shortTermScheduler.add(p.getPid());
        }
        p = OperatingSystem.getInstance().pidLookup(shortTermScheduler.remove());
        if (p != null) {
            p.setState(State.RUN);
        }
    }

}