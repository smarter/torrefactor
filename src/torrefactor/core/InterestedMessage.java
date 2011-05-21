package torrefactor.core;

/**
 * Represents an interested message.
 */
public class InterestedMessage extends Message {
    final static byte id = 2;

    /**
     * {@inheritDoc}
     */
    @Override
    public byte id () {
        return InterestedMessage.id;
    }
}
