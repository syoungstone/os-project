package Control;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Manages a selection of unnamed system resources using the Banker's Algorithm to simulate deadlock avoidance
public class ResourceManager {

    public static final int NUM_RESOURCE_TYPES = 8;
    public static final int NUM_RESOURCES_PER_TYPE = 16;

    private static ResourceManager instance;

    private ResourceVector availableVector;
    private final Map<Integer, ResourceVector> maxDemandVectors;
    private final Map<Integer, ResourceVector> allocationVectors;
    private final Map<Integer, ResourceVector> needVectors;

    private ResourceManager() {
        availableVector = new ResourceVector(NUM_RESOURCES_PER_TYPE);
        maxDemandVectors = new HashMap<>();
        allocationVectors = new HashMap<>();
        needVectors = new HashMap<>();
    }

    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    public synchronized void addProcess(int pid, int[] maxDemand) {
        maxDemandVectors.put(pid, new ResourceVector(maxDemand));
        allocationVectors.put(pid, new ResourceVector());
        needVectors.put(pid, new ResourceVector(maxDemand));
    }

    public synchronized void removeProcess(int pid) {
        maxDemandVectors.remove(pid);
        needVectors.remove(pid);
        ResourceVector allocated = allocationVectors.remove(pid);
        if (allocated != null) {
            availableVector = availableVector.add(allocated);
        }
    }

    public synchronized void releaseResources(int pid, int[] releasing) {
        ResourceVector releaseVector = new ResourceVector(releasing);
        availableVector = availableVector.add(releaseVector);
        allocationVectors.put(pid, allocationVectors.get(pid).subtract(releaseVector));
        needVectors.put(pid, needVectors.get(pid).add(releaseVector));
    }

    public synchronized boolean requestResources(int pid, int[] request) {
        ResourceVector requestVector = new ResourceVector(request);
        if (requestVector.lessThanOrEqualTo(needVectors.get(pid))) {
            if (requestVector.lessThanOrEqualTo(availableVector)) {

                // Save current state in case we need to return to it
                ResourceVector availableOld = availableVector;
                ResourceVector allocationOld = allocationVectors.get(pid);
                ResourceVector needOld = needVectors.get(pid);

                // Update state as if request was fulfilled
                availableVector = availableOld.subtract(requestVector);
                allocationVectors.put(pid, allocationOld.add(requestVector));
                needVectors.put(pid, needOld.subtract(requestVector));

                if (safeState()) {
                    return true;
                } else {
                    // Return to state before request
                    availableVector = availableOld;
                    allocationVectors.put(pid, allocationOld);
                    needVectors.put(pid, needOld);
                }

            }
        }
        return false;
    }

    private boolean safeState() {
        ResourceVector work = availableVector;
        Set<Integer> finish = maxDemandVectors.keySet();

        Integer candidatePid = findCandidate(work, finish);
        while(candidatePid != null) {
            work = work.add(allocationVectors.get(candidatePid));
            candidatePid = findCandidate(work, finish);
        }

        // Return true if all values have been removed from finish, false otherwise
        return finish.size() == 0;
    }

    private Integer findCandidate(ResourceVector work, Set<Integer> finish) {
        for (int pid : finish) {
            if (needVectors.get(pid).lessThanOrEqualTo(work)) {
                finish.remove(pid);
                return pid;
            }
        }
        return null;
    }

    private static class ResourceVector {

        private final int[] values;

        ResourceVector() {
            values = new int[NUM_RESOURCE_TYPES];
        }

        ResourceVector(int value) {
            values = new int[NUM_RESOURCE_TYPES];
            for (int i = 0 ; i < NUM_RESOURCE_TYPES ; i++) {
                values[i] = value;
            }
        }

        ResourceVector(int[] values) {
            this.values = values.clone();
        }

        boolean lessThanOrEqualTo(ResourceVector vector) {
            boolean result = true;
            for (int i = 0 ; i < NUM_RESOURCE_TYPES ; i++) {
                result = result && values[i] <= vector.values[i];
            }
            return result;
        }

        ResourceVector add(ResourceVector vector) {
            int[] newValues = values.clone();
            for (int i = 0 ; i < NUM_RESOURCE_TYPES ; i++) {
                newValues[i] += vector.values[i];
            }
            return new ResourceVector(newValues);
        }

        ResourceVector subtract(ResourceVector vector) {
            int[] newValues = values.clone();
            for (int i = 0 ; i < NUM_RESOURCE_TYPES ; i++) {
                newValues[i] -= vector.values[i];
            }
            return new ResourceVector(newValues);
        }

    }

}
