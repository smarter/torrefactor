package torrefactor.util;

public class InvalidBencodeException extends Exception {

    // FIXME: define a dummy serial so java's babysiter doesn't complain about
    // missing serial although we DON'T use serialization.
    public static final long serialVersionUID = 12345678L;

    public InvalidBencodeException(String message) {
        super(message);
    }
}
