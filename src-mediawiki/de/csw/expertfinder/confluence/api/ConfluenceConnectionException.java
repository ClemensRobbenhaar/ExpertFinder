package de.csw.expertfinder.confluence.api;

// throw whenever something goes wrong
public class ConfluenceConnectionException extends RuntimeException {

    private static final long serialVersionUID = -398937888985835728L;

    public ConfluenceConnectionException() {
        // TODO Auto-generated constructor stub
    }

    public ConfluenceConnectionException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public ConfluenceConnectionException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public ConfluenceConnectionException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}
