package torrefactor.core;

public class KeepAliveMessage extends Message {
    final static byte id = 1;

    public byte id () {
        return KeepAliveMessage.id;
    }

    public byte[] toByteArray () {
        return new byte[] {0, 0, 0, 0};
    }
}
