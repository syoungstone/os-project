package Processor;

import Processes.PCB;

public abstract class ShortTermScheduler {
    public abstract void add(PCB p);
    public abstract PCB remove();
    public abstract int getReadyCount();
    public abstract boolean scheduleNew(int counter);
    public abstract String name();
}
