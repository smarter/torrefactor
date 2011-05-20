package torrefactor.core;

public class ChokeMessage extends Message {
    final static byte id = 0;

    public byte id () {
        return ChokeMessage.id;
    }
}
