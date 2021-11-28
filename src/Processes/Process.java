package Processes;

import java.util.LinkedList;
import java.util.Queue;

public class Process {

    private final Queue<Section> sections;

    Process(Template template) {
        sections = new LinkedList<>();
        for (Template.Section tSection : template.getSections()) {
            Section pSection = new Section(tSection.isCritical());
            for (Template.OperationSet tOpSet : tSection.getOperationSets()) {
                OperationSet pOpSet = new OperationSet(tOpSet.getOperation(), tOpSet.generateCycleCount());
                pSection.getOperationSets().add(pOpSet);
            }
            sections.add(pSection);
        }
    }

    // Used for creating a child process
    // Instructions start immediately after FORK instruction used to create child
    Process(Process parent, Section currentSection) {
        sections = new LinkedList<>();
        sections.add(currentSection.deepCopy());
        for (Section section : parent.sections) {
            sections.add(section.deepCopy());
        }
    }

    Section nextSection() {
        return sections.poll();
    }

    static class Section {
        private Queue<OperationSet> operationSets;
        private final boolean critical;

        Section(boolean critical) {
            this.critical = critical;
        }

        Queue<OperationSet> getOperationSets() {
            if (operationSets == null) {
                operationSets = new LinkedList<>();
            }
            return operationSets;
        }

        boolean isCritical() {
            return critical;
        }

        Section deepCopy() {
            Section copy = new Section(critical);
            Queue<OperationSet> copyOpSets = copy.getOperationSets();
            for (OperationSet opSet : operationSets) {
                copyOpSets.add(new OperationSet(opSet.getOperation(), opSet.getCycles()));
            }
            return copy;
        }
    }

    static class OperationSet {
        private final Operation operation;
        private int cycles;

        OperationSet(Operation operation, int cycles) {
            this.operation = operation;
            this.cycles = cycles;
        }

        Operation getOperation() {
            return operation;
        }

        void progressOneCycle() {
            cycles--;
        }

        int getCycles() {
            return cycles;
        }
    }
}
