package torrefactor.util;
import torrefactor.util.InvalidBencodeException;

import java.io.Reader;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Bencode {

    static public int decodeInt(Reader reader)
    throws java.io.IOException, InvalidBencodeException {
        int c, value;
        StringBuilder sb = new StringBuilder();

        while (true) {
            c = reader.read();
            if (c == (int) 'e') break;
            if (c == -1) throw new InvalidBencodeException(
                    "Reached end of string while parsing int.");
            sb.append((char) c);
        }

        try {
            value = Integer.parseInt(sb.toString());
        } catch (NumberFormatException exception) {
            throw new InvalidBencodeException('"' + sb.toString()
                                              + "\" is not a bencoded int.");
        }

        return value;
    }

    static public String decodeString(Reader reader)
    throws java.io.IOException, InvalidBencodeException {
        int len, c;
        String value;
        StringBuilder sb = new StringBuilder();

        while (true) {
            c = reader.read();
            if (c == (int) ':') break;
            if (c == -1) throw new InvalidBencodeException(
                    "Reached end of string while parsing string length.");
            sb.append((char) c);
        }
        
        try {
            len = Integer.parseInt(sb.toString());
        } catch (NumberFormatException exception) {
            throw new InvalidBencodeException('"' + sb.toString() 
                    + "\" is not a bencoded string: the length is not valid.");
        }

        sb = new StringBuilder();
        while (len != 0) {
            c = reader.read();
            if (c == -1) throw new InvalidBencodeException(
                    "Reached end of stream while parsing string.");
            sb.append((char) c);
            len--;
        }

        return sb.toString();
    }


    static public ArrayList decodeList(Reader reader)
    throws java.io.IOException, InvalidBencodeException {
        int c;
        ArrayList<Object> list = new ArrayList<Object>();
        PushbackReader pbreader = new PushbackReader(reader);

        while (true) {
            c = pbreader.read();
            if (c == -1) {
                throw new InvalidBencodeException(
                        "Reached end of stream while parsing list.");
            } else if (Character.isDigit(c)) {
                pbreader.unread(c);
                list.add(decodeString(pbreader));
            } else if (c == (int) 'i') {
                list.add(decodeInt(pbreader));
            } else if (c == (int) 'l') {
                list.add(decodeList(pbreader));
            } else if (c == (int) 'd') {
                list.add(decodeDictionary(pbreader));
            } else if (c == (int) 'e') {
                break;
            } else {
                throw new InvalidBencodeException("Didn't got list element"
                                        + "(digit, i, l, d) or end of list.");
            }
        }
        return list;
    }


    static public HashMap decodeDictionary(Reader reader) throws java.io.IOException, InvalidBencodeException {
        int c;
        String key;
        HashMap<String, Object> map = new HashMap<String, Object>();
        PushbackReader pbreader = new PushbackReader(reader);

        while (true) {
            c = pbreader.read();
            if (c == -1) {
                throw new InvalidBencodeException("Reached end of stream " 
                                        + "while parsing key in dictionary.");
            } else if (Character.isDigit(c)) {
                pbreader.unread(c);
                key = decodeString(pbreader);
            } else if (c == (int) 'e') {
                break;
            } else {
                throw new InvalidBencodeException("Didn't got end of " 
                        + "dictionary or bencoded string as dictionary key.");
            }

            c = pbreader.read();
            if (c == -1) {
                throw new InvalidBencodeException("Reached end of stream " 
                                        + "while parsing value in dictionary.");
            } else if (Character.isDigit(c)) {
                pbreader.unread(c);
                map.put(key, decodeString(pbreader));
            } else if (c == (int) 'i') {
                map.put(key, decodeInt(pbreader));
            } else if (c == (int) 'l') {
                map.put(key, decodeList(pbreader));
            } else if (c == (int) 'd') {
                map.put(key, decodeDictionary(pbreader));
            } else {
                throw new InvalidBencodeException(
                        "Didn't get a valid bencoded value.");
            }
        }

        return map;
    }
}
