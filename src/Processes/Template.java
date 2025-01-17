package Processes;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

public class Template {

    private static final String TEMPLATES_DIRECTORY_PATH = "templates";
    private static final String MEMORY_REQUIRED_STRING = "MEMORY_REQUIRED_MB:";
    private static final String CRITICAL_SECTION_START = "CRITICAL";
    private static final String CRITICAL_SECTION_END = "/CRITICAL";

    private static List<Template> templates;

    private final int index;
    private final String name;
    private final int memoryRequiredMB;
    private final List<Section> sections;

    private Template(int index, String name, int memoryRequiredMB) {
        this.index = index;
        this.name = name;
        this.memoryRequiredMB = memoryRequiredMB;
        this.sections = new ArrayList<>();
    }

    private static void loadTemplates() throws MalformedTemplateException {
        int index = 0;
        templates = new ArrayList<>();
        final File directory = Paths.get(TEMPLATES_DIRECTORY_PATH).toFile();
        File[] files = directory.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (!file.isDirectory() && file.getName().endsWith(".txt")) {
                    Template template = loadTemplate(index, file);
                    if (template != null) {
                        templates.add(template);
                        index++;
                    }
                }
            }
        }
    }

    private static Template loadTemplate(int index, File file) throws MalformedTemplateException {
        String name = file.getName().split("\\.")[0];
        Template template;
        List<Section> sections;
        try {
            Scanner sc = new Scanner(file);
            boolean critical = false;
            List<OperationSet> operationSets = new ArrayList<>();
            if (sc.hasNextLine()) {
                String[] contents = sc.nextLine().trim().split("\\s+");
                if (contents.length == 2 && MEMORY_REQUIRED_STRING.equals(contents[0])) {
                    try {
                        int memoryRequiredMB = Integer.parseInt(contents[1]);
                        template = new Template(index, name, memoryRequiredMB);
                        sections = template.getSections();
                    } catch (Exception e) {
                        throw new MalformedTemplateException("Memory requirement not an integer");
                    }
                } else {
                    throw new MalformedTemplateException("Missing memory requirements");
                }
            } else {
                throw new MalformedTemplateException("Empty template");
            }
            while (sc.hasNextLine()) {
                String[] contents = sc.nextLine().trim().split("\\s+");
                if (contents.length == 1) {
                    boolean isStartCritical = CRITICAL_SECTION_START.equals(contents[0]);
                    boolean isEndCritical = CRITICAL_SECTION_END.equals(contents[0]);
                    boolean isFork = Operation.FORK == Operation.read(contents[0]);
                    if (isStartCritical || isEndCritical) {
                        if (critical && isStartCritical) {
                            throw new MalformedTemplateException("New CS formed before ending current CS");
                        }
                        if (!critical && isEndCritical) {
                            throw new MalformedTemplateException("Ending CS where none exists");
                        }
                        if (operationSets.size() > 0) {
                            Section newSection = new Section(critical);
                            newSection.getOperationSets().addAll(operationSets);
                            sections.add(newSection);
                            operationSets.clear();
                        }
                        critical = !critical;
                    } else if (isFork) {
                        operationSets.add(new OperationSet(Operation.FORK));
                    } else {
                        throw new MalformedTemplateException("Unrecognized command");
                    }
                } else if (contents.length == 3) {
                    Operation operation = Operation.read(contents[0]);
                    if (operation == null) {
                        throw new MalformedTemplateException("Unrecognized operation");
                    } else if (operation == Operation.FORK) {
                        throw new MalformedTemplateException("FORK command should stand alone");
                    }
                    try {
                        int min = Integer.parseInt(contents[1]);
                        int max = Integer.parseInt(contents[2]);
                        if (min >= max) {
                            throw new MalformedTemplateException("Min cycles greater than or equal to max cycles");
                        }
                        operationSets.add(new OperationSet(operation, min, max));
                    } catch (NumberFormatException e) {
                        throw new MalformedTemplateException("Min or max cycles value not an integer");
                    }
                } else if (contents.length != 0) {
                    throw new MalformedTemplateException("Wrong number of arguments");
                }
            }
            if (critical) {
                throw new MalformedTemplateException("Final critical section never terminated");
            } else if (operationSets.size() > 0) {
                Section newSection = new Section(critical);
                newSection.getOperationSets().addAll(operationSets);
                sections.add(newSection);
            }
            sc.close();
            if (sections.size() == 0 ||
                    sections.get(0) == null ||
                    sections.get(0).getOperationSets() == null ||
                    sections.get(0).getOperationSets().size() == 0) {
                throw new MalformedTemplateException("No operations were processed from template");
            }
            return template;
        } catch (Exception e) {
            if (e instanceof MalformedTemplateException) {
                throw (MalformedTemplateException) e;
            } else {
                return null;
            }
        }
    }

    public static List<Template> getTemplates() throws MalformedTemplateException {
        if (templates == null) {
            loadTemplates();
        }
        return templates;
    }

    public synchronized int memoryRequirements() {
        return memoryRequiredMB;
    }

    public int index() {
        return index;
    }

    public synchronized String name() {
        return name;
    }

    public synchronized List<Section> getSections() {
        return sections;
    }

    /* When parsing a template, any number of lines which form a critical or non-critical section
     * of code become a Section. Each Section contains a boolean specifying whether the section
     * is critical, as well as a list of OperationSets, each of which is derived from a single
     * line in the template. Each operation set contains an Operation, as well as the integers
     * minCycles and maxCycles to use when producing a Process.  */

    static class Section {
        private List<OperationSet> operationSets;
        private final boolean critical;

        Section(boolean critical) {
            this.critical = critical;
        }

        synchronized List<OperationSet> getOperationSets() {
            if (operationSets == null) {
                operationSets = new ArrayList<>();
            }
            return operationSets;
        }

        synchronized boolean isCritical() {
            return critical;
        }
    }

    static class OperationSet {
        private final Operation operation;
        private final int minCycles;
        private final int maxCycles;

        OperationSet(Operation operation) {
            this.operation = operation;
            this.minCycles = 1;
            this.maxCycles = 1;
        }

        OperationSet(Operation operation, int minCycles, int maxCycles) {
            this.operation = operation;
            this.minCycles = minCycles;
            this.maxCycles = maxCycles;
        }

        synchronized Operation getOperation() {
            return operation;
        }

        synchronized int generateCycleCount() {
            if (operation == Operation.FORK) {
                return 1;
            }
            Random random = new Random();
            return random.nextInt(maxCycles + 1 - minCycles) + minCycles;
        }
    }
}
