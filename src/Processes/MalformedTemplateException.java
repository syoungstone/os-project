package Processes;

public class MalformedTemplateException extends Exception {
    MalformedTemplateException(String str) { super("MalformedTemplateException: " + str); }
}
