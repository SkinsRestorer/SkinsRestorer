package net.skinsrestorer.shared.exception;

/**
 * Created by McLive on 21.07.2019.
 */
public class SkinRequestException extends Exception {
    private final String reason;

    public SkinRequestException(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String getMessage() {
        return reason;
    }
}