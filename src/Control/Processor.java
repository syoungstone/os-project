package Control;

import Processes.PCB;
import Processes.State;

class Processor {

    private final Scheduler shortTermScheduler;

    private PCB p;
    private int counter;

    Processor() {
        shortTermScheduler = new RRScheduler();
        counter = 0;
    }

    public void request(PCB p) {
        if (p != null) {
            shortTermScheduler.add(p);
            if (this.p == null) {
                scheduleNew();
            }
        }
    }

    public void advance() {
        if ((p == null && getReadyCount() > 0) ||
                (p != null && p.getState() != State.RUN)){
            scheduleNew();
        }
        if (p != null) {
            p.progressOneCycle();
            counter++;
            if (shortTermScheduler.scheduleNew(counter)) {
                scheduleNew();
            }
        }
    }

    public int getCurrentPid() {
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
        counter = 0;
        if (p != null && p.getState() == State.RUN) {
            p.setState(State.READY);
            shortTermScheduler.add(p);
        }
        p = shortTermScheduler.remove();
        if (p != null) {
            p.setState(State.RUN);
        }
    }

}