package net.skinsrestorer.shared.exception;

public class EnumNotFoundException extends ReflectiveOperationException {
    public EnumNotFoundException(String message) {
        super(message);
    }
}
