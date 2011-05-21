package torrefactor.core;

/**
 * Represents a not interested messag.
 */
public class NotInterestedMessage extends Message {
    final static byte id = 3;

    /**
     * {@inheritDoc}
     */
    @Override
    public byte id () {
        return NotInterestedMessage.id;
    }
}
