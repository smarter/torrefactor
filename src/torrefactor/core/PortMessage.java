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

    public PortMessage (int port) {
        this.port = port;
    }

    public PortMessage (byte[] msg) {
        this.port = ByteArrays.toShortInt(msg);
    }

    public byte id () {
        return PortMessage.id;
    }

    public static boolean isValid (byte[] msg) {
        return msg.length == 2;
    }

    public byte[] toByteArray () {
        byte[] t = super.toByteArray();
        byte[] p = ByteArrays.fromShortInt(port);

        return ByteArrays.concat(new byte[][] {t, p});
    }

}
