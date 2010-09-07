package dolda.jsvc.scgi;

public class InvalidRequestException extends java.io.IOException {
    public InvalidRequestException(String message) {
	super(message);
    }
}
