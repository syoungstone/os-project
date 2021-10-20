import java.util.ArrayList;
import java.util.List;

public class Process {

    private Template template;
    private List<Section> sections;

    public Process(Template template) {
        this.template = template;
        this.sections = new ArrayList<>();
        for (Template.Section tSection : template.getSections()) {
            Section pSection = new Section(tSection.isCritical());
            for (Template.OperationSet tOpSet : tSection.getOperationSets()) {
                OperationSet pOpSet = new OperationSet(tOpSet.getOperation(), tOpSet.generateCycleCount());
                pSection.getOperationSets().add(pOpSet);
            }
            sections.add(pSection);
        }
    }
    public void block() {

    }

    public void wakeup() {

    }


    private static class Section {
        private List<OperationSet> operationSets;
        private final boolean critical;

        Section(boolean critical) {
            this.critical = critical;
        }

        List<OperationSet> getOperationSets() {
            if (operationSets == null) {
                operationSets = new ArrayList<>();
            }
            return operationSets;
        }

        boolean isCritical() {
            return critical;
        }
    }

    private static class OperationSet {
        private Operation operation;
        private int cycles;

        OperationSet(Operation operation, int cycles) {
            this.operation = operation;
            this.cycles = cycles;
        }
    }
}
