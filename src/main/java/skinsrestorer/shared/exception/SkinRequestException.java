package skinsrestorer.shared.exception;

/**
 * Created by McLive on 21.07.2019.
 */
public class SkinRequestException extends Exception {

    private String reason;

    public SkinRequestException(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public String getMessage() {
        return reason;
    }

}