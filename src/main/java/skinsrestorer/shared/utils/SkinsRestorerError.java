package skinsrestorer.shared.utils;

/**
 * Created by McLive on 20.01.2019.
 */
public class SkinsRestorerError {

    public enum ExceptionType {
        MINETOOLS_FETCH, MOJANG_FETCH, THIRD_FETCH, SKIN_SET, SKIN_SOTRAGE
    }

    public static class MojangRequestException extends Exception {

        private String message;
        private ExceptionType type;

        public MojangRequestException(String message, ExceptionType type) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public ExceptionType getType() {
            return type;
        }
    }

    public static class SkinSetException extends Exception {

        private String message;
        private ExceptionType type;

        public SkinSetException(String message, ExceptionType type) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public ExceptionType getType() {
            return type;
        }
    }
}
