import java.util.List;

public class OperatingSystem {

    private static OperatingSystem instance;

    private List<Template> templates;
    private Scheduler shortTermScheduler;
    private Semaphore semaphore;

    private OperatingSystem() {
        boot();
    }

    private void boot() {
        System.out.println("Booting up...");
        try {
            templates = Template.getTemplates();
            shortTermScheduler = new RRScheduler();
            semaphore = new Semaphore();
            runOS();
        } catch (MalformedTemplateException e) {
            System.out.println(e.getMessage());
            System.out.println("Exiting...");
        }
    }

    private void runOS() {
        System.out.println("Operating system is running!");
        System.out.println("Available process templates:");
        int i = 1;
        for (Template t : templates) {
            System.out.println(i + ") " + t.name());
            i++;
        }
    }

    public static OperatingSystem getInstance() {
        if (instance == null) {
            instance = new OperatingSystem();
        }
        return instance;
    }

}
