package torrefactor.core;

public class UnchokeMessage extends Message {
    final static byte id = 1;

    public byte id () {
        return UnchokeMessage.id;
    }
}
