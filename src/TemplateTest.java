import java.util.ArrayList;
import java.util.List;

public class TemplateTest {
    public static void main(String[] args) {
        List<Template> templates = new ArrayList<>();
        try {
            templates = Template.getTemplates();
        } catch (MalformedTemplateException e) {
            System.out.println(e.getMessage());
        }
        System.out.println(templates.toString());
    }
}
