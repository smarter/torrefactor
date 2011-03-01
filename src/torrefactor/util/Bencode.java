package torrefactor.util;
import torrefactor.util.InvalidBencodeException;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class Bencode {
    private int number = Integer.MAX_VALUE;
    private String string = null;
    private List<Bencode> list = null;
    private HashMap<String, Bencode> map = null;

    public Bencode(int _number) {
        this.number = _number;
    }

    public Bencode(String _string) {
        this.string = _string;
    }

    public Bencode(List<Bencode> _list) {
        this.list = _list;
    }

    public Bencode(HashMap<String, Bencode> _map) {
        this.map = _map;
    }

    public int toInt() {
        return number;
    }

    public String toString() {
        return string;
    }

    public List<Bencode> toList() {
        return list;
    }

    public HashMap<String, Bencode> toMap() {
        return map;
    }

    private static Bencode decode(InputStream stream)
    throws java.io.IOException, InvalidBencodeException {
        PushbackInputStream pbstream = new PushbackInputStream(stream);
        int c = pbstream.read();
        pbstream.unread(c);

        if (c == -1) throw new InvalidBencodeException("Unexpected end of stream");

        if (Character.isDigit((char) c)) {
            return new Bencode(decodeString(pbstream));
        }
        switch ((char) c) {
        case 'i':
            return new Bencode(decodeInt(pbstream));
        case 'l':
            return new Bencode(decodeList(pbstream));
        case 'd':
            return new Bencode(decodeDict(pbstream));
        default:
            throw new InvalidBencodeException(
                      "Not a Bencode string (should start with 'i', 'l', 'd' or a digit)");
        }
    }

    static public int decodeInt(InputStream stream)
    throws java.io.IOException, InvalidBencodeException {
        StringBuilder sb = new StringBuilder();

        int c = stream.read();
        if (c != (int) 'i') throw new InvalidBencodeException("Not an int");
        while ((c = stream.read()) != (int) 'e') {
            if (c == -1) throw new InvalidBencodeException(
                    "Reached end of stream while parsing int.");
            sb.append((char) c);
        }

        int value;
        try {
            value = Integer.parseInt(sb.toString());
        } catch (NumberFormatException exception) {
            throw new InvalidBencodeException('"' + sb.toString()
                                              + "\" is not a bencoded int.");
        }

        return value;
    }

    static public String decodeString(InputStream stream)
    throws java.io.IOException, InvalidBencodeException {
        StringBuilder sb = new StringBuilder();

        int c;
        while ((c = stream.read()) != (int) ':') {
            if (c == -1) throw new InvalidBencodeException(
                    "Reached end of stream while parsing string length.");
            sb.append((char) c);
        }

        int len;
        try {
            len = Integer.parseInt(sb.toString());
        } catch (NumberFormatException exception) {
            throw new InvalidBencodeException('"' + sb.toString()
                    + "\" is not a bencoded string: the length is not valid.");
        }

        sb = new StringBuilder();
        while (len != 0) {
            c = stream.read();
            if (c == -1) throw new InvalidBencodeException(
                    "Reached end of stream while parsing string.");
            sb.append((char) c);
            len--;
        }

        return sb.toString();
    }


    static public ArrayList<Bencode> decodeList(InputStream stream)
    throws java.io.IOException, InvalidBencodeException {
        ArrayList<Bencode> list = new ArrayList<Bencode>();
        PushbackInputStream pbstream = new PushbackInputStream(stream);

        int c = stream.read();
        if (c != (int) 'l') throw new InvalidBencodeException("Not a list");

        while ((c = stream.read()) != (int) 'e') {
            pbstream.unread(c);
            list.add(decode(pbstream));
        }
        return list;
    }

    static public HashMap<String, Bencode> decodeDict(InputStream stream)
    throws java.io.IOException, InvalidBencodeException {
        try {
            return decodeDict(stream, null, null);
        } catch (java.security.NoSuchAlgorithmException e) {
            //can't happen, DigestInputStream constructor is never called
            return null;
        }
    }

    static public HashMap<String, Bencode> decodeDict(InputStream stream, String hashTag, byte[] hashArray)
    throws java.io.IOException, java.security.NoSuchAlgorithmException, InvalidBencodeException {
        DigestInputStream dstream = null;

        PushbackInputStream pbstream;
        if (hashTag != null) {
            dstream = (DigestInputStream) stream;
            dstream.on(false);
            pbstream = new PushbackInputStream(dstream);
        } else {
            pbstream = new PushbackInputStream(stream);
        }

        HashMap<String, Bencode> map = new HashMap<String, Bencode>();

        int c = pbstream.read();
        if (c != (int) 'd') throw new InvalidBencodeException("Not a dictionary");

        boolean foundTag = false;
        while ((c = pbstream.read()) != (int) 'e') {
            if (!Character.isDigit((char) c)) {
                throw new InvalidBencodeException("Didn't got end of "
                        + "dictionary or bencoded string as dictionary key.");
            }
            pbstream.unread(c);
            String key;
            key = decodeString(pbstream);
            if (key.equals(hashTag)) {
                foundTag = true;
                dstream.on(true);
            }
            map.put(key, decode(pbstream));
            if (dstream != null && foundTag) {
                dstream.on(false);
                byte[] sha1Hash = dstream.getMessageDigest().digest();
                for (int i = 0; i < hashArray.length; i++) {
                    hashArray[i] = sha1Hash[i];
                }
                dstream = null;
            }
        }

        return map;
    }
}
