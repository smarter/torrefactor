package torrefactor.util;

public class InvalidBDecodeException extends Exception {

    // FIXME: define a dummy serial so java's babysiter doesn't complain about
    // missing serial although we DON'T use serialization.
    public static final long serialVersionUID = 12345678L;

    public InvalidBDecodeException(String message) {
        super(message);
    }
}
