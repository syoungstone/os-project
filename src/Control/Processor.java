package Control;

import Processes.PCB;
import Processes.State;

class Processor {

    private static final int NUM_CORES = 4;

    private final Object threadCoordinator;
    private final Scheduler shortTermScheduler;
    private final Core[] cores;

    Processor(Scheduler scheduler) {
        threadCoordinator = new Object();
        shortTermScheduler = scheduler;
        cores = new Core[NUM_CORES];
        for (int i = 0 ; i < NUM_CORES ; i++) {
            cores[i] = new Core();
        }
    }

    public void start() {
        for (Core core : cores) {
            core.start();
        }
    }

    public void advance() {
        // Notify waiting CPU threads to start a new cycle
        synchronized (threadCoordinator) {
            threadCoordinator.notifyAll();
        }
    }

    public void stop() {
        for (Core core : cores) {
            core.stop();
        }
    }

    public boolean cycleFinished() {
        boolean finished = true;
        for (Core core : cores) {
            finished = finished && core.isWaiting();
        }
        return finished;
    }

    public void request(PCB p) {
        shortTermScheduler.add(p);
    }

    public int getReadyCount() {
        return shortTermScheduler.getReadyCount();
    }

    public String getCurrentPids() {
        StringBuilder pids = new StringBuilder();
        for (int i = 0 ; i < cores.length ; i++) {
            pids.append(cores[i].getCurrentPid());
            if (i < cores.length - 1) {
                pids.append(", ");
            }
        }
        return pids.toString();
    }

    private class Core {

        private PCB p;
        private int counter;
        private Thread thread;

        Core() {
            counter = 0;
            instantiateThread();
        }

        void start() {
            thread.start();
        }

        void stop() {
            thread.interrupt();
            instantiateThread();
        }

        boolean isWaiting() {
            return thread.getState() == Thread.State.WAITING;
        }

        int getCurrentPid() {
            if (p == null) {
                return OperatingSystem.KERNEL_ID;
            } else {
                return p.getPid();
            }
        }

        private void instantiateThread() {
            thread = new Thread(() -> {
                boolean run = true;
                while (run) {
                    try {
                        synchronized (threadCoordinator) {
                            threadCoordinator.wait();
                        }
                        advance();
                    } catch (InterruptedException e) {
                        run = false;
                    }
                }
            });
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

        private void advance() {
            if ((p == null && getReadyCount() > 0) ||
                    (p != null && p.getState() != State.RUN)){
                scheduleNew();
            }
            if (p != null) {
                p.progressOneCycle();
                counter++;
                if (shortTermScheduler.scheduleNew(counter) || p.getState() != State.RUN) {
                    scheduleNew();
                }
            }
        }

    }

}