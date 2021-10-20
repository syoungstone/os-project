import java.util.HashMap;
import java.util.Map;

// This enum is used to represent the five different commands which may be found in a template file
public enum Operation {
    CALCULATE("CALCULATE"),
    IO("IO"),
    FORK("FORK");

    public final String string;
    private static final Map<String, Operation> BY_STRING = new HashMap<>();

    static {
        for (Operation o : values()) {
            BY_STRING.put(o.string, o);
        }
    }

    private Operation(String string) {
        this.string = string;
    }

    public static Operation read(String string) {
        return BY_STRING.get(string);
    }

}
