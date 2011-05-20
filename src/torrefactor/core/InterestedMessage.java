package torrefactor.core;

public class InterestedMessage extends Message {
    final static byte id = 2;

    public byte id () {
        return InterestedMessage.id;
    }
}
