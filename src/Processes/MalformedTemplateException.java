package Processes;

// Indicates that a template file is not properly formatted
public class MalformedTemplateException extends Exception {
    MalformedTemplateException(String str) { super("MalformedTemplateException: " + str); }
}
