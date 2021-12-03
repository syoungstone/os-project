package Processor;

import Processes.PCB;

// Used by the Core class to measure scheduler performance
public class StatisticalUnit {

    private long startTime = 0;
    private long endTime = 0;

    private int completedProcesses = 0;

    private long totalTurnaroundTime = 0;
    private long totalWaitingTime = 0;

    private int utilizedCycles = 0;
    private int totalCycles = 0;

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void stop() {
        endTime = System.currentTimeMillis();
    }

    public synchronized void incrementUtilizedCycles() {
        utilizedCycles++;
    }

    public synchronized void incrementTotalCycles() {
        totalCycles++;
    }

    public synchronized void registerTermination(PCB p) {
        completedProcesses++;
        totalTurnaroundTime += p.getTurnaroundTime();
        totalWaitingTime += p.getWaitingTime();
    }

    public double getUtilization() {
        // Prevent division by 0
        if (totalCycles == 0) {
            return 0;
        }
        return (double) utilizedCycles / totalCycles;
    }

    public double getThroughput() {
        double secondsElapsed = (double) (endTime - startTime) / 1000;
        // Prevent division by 0
        if (secondsElapsed == 0) {
            return 0;
        }
        return (double) completedProcesses / secondsElapsed;
    }

    public double getAvgTurnaroundTime() {
        // Prevent division by 0
        if (completedProcesses == 0) {
            return 0;
        }
        return (double) totalTurnaroundTime / completedProcesses;
    }

    public double getAvgWaitingTime() {
        // Prevent division by 0
        if (completedProcesses == 0) {
            return 0;
        }
        return (double) totalWaitingTime / completedProcesses;
    }
}
