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
            if (p == null) {
                p = OperatingSystem.getInstance().pidLookup(pid);
            } else {
                shortTermScheduler.add(pid);
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

    private void scheduleNew() {
        if (p != null && p.getState() == State.RUN) {
            p.setState(State.READY);
        }
        p = OperatingSystem.getInstance().pidLookup(shortTermScheduler.remove());
        if (p != null) {
            p.setState(State.RUN);
        }
    }

}