package Control;

import Processes.PCB;

public abstract class Scheduler {
    public abstract void add(PCB p);
    public abstract PCB remove();
    public abstract int getReadyCount();
    public abstract boolean scheduleNew(int counter);
}
