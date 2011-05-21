package torrefactor.core;

import torrefactor.util.ByteArrays;

/**
 * Represents a port message.
 *  id      1 byte
 *  port    2 byte
 */
public class PortMessage extends Message {
    final static byte id = 9;
    final int port;

    /**
     * Creates a new PortMessage from the given port.
     *
     * @param port    the port to put in the PortMessage.
     */
    public PortMessage (int port) {
        this.port = port;
    }

    /**
     * Creates a new PortMessage from the given byte array representation.
     *
     * @param msg    the byte array representation
     */
    public PortMessage (byte[] msg) {
        this.port = ByteArrays.toShortInt(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte id () {
        return PortMessage.id;
    }

    // Java does not override static method thus we cannot use @inheritDoc
    /**
     * {@link torrefactor.core.Message#isValid(byte[])}
     */
    public static boolean isValid (byte[] msg) {
        return msg.length == 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toByteArray () {
        byte[] t = super.toByteArray();
        byte[] p = ByteArrays.fromShortInt(port);

        return ByteArrays.concat(new byte[][] {t, p});
    }

}
