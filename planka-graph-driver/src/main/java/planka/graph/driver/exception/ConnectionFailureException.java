package planka.graph.driver.exception;

public class ConnectionFailureException extends RuntimeException {

    public ConnectionFailureException(String message) {
        super(message);
    }

    public ConnectionFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
