package torrefactor.core;

/**
 * Represents an unchoke message.
 */
public class UnchokeMessage extends Message {
    final static byte id = 1;

    /**
     * {@inheritDoc}
     */
    @Override
    public byte id () {
        return UnchokeMessage.id;
    }
}
