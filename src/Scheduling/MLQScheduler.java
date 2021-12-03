package Scheduling;

import Processes.PCB;
import Processes.Priority;

import java.util.LinkedList;
import java.util.Queue;

// Multi-Level Queue scheduler with circular Round Robin queues for each level
// A lower priority process may only be scheduled if there are no higher priority processes waiting
public class MLQScheduler extends ShortTermScheduler {

    private static final int TIME_QUANTUM = 10;

    private final Queue<PCB> highPriorityQueue;
    private final Queue<PCB> mediumPriorityQueue;
    private final Queue<PCB> lowPriorityQueue;

    MLQScheduler() {
        highPriorityQueue = new LinkedList<>();
        mediumPriorityQueue = new LinkedList<>();
        lowPriorityQueue = new LinkedList<>();
    }

    public synchronized void add(PCB p) {
        if (p.getPriority() == Priority.HIGH) {
            highPriorityQueue.add(p);
        } else if (p.getPriority() == Priority.MEDIUM) {
            mediumPriorityQueue.add(p);
        } else {
            lowPriorityQueue.add(p);
        }
    }

    public synchronized PCB remove() {
        if (!highPriorityQueue.isEmpty()) {
            return highPriorityQueue.poll();
        } else if (!mediumPriorityQueue.isEmpty()) {
            return mediumPriorityQueue.poll();
        } else if (!lowPriorityQueue.isEmpty()) {
            return lowPriorityQueue.poll();
        } else {
            return null;
        }
    }

    public synchronized int getReadyCount() {
        return highPriorityQueue.size() + mediumPriorityQueue.size() + lowPriorityQueue.size();
    }

    // Time to schedule a new process if counter has reached the time quantum
    public boolean scheduleNew(int counter) {
        return counter >= TIME_QUANTUM;
    }

    public String name() {
        return "Multi-Level Queue";
    }

}
