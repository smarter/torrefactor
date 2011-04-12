package torrefactor.util;

import torrefactor.util.BDecode;
import torrefactor.util.BValue;

import java.io.*;
import java.util.*;

public class BEncode {
    private static void encode(int val, ByteArrayOutputStream out)
    throws IOException {
        out.write('i');
        out.write(Integer.toString(val).getBytes());
        out.write('e');
    }

    private static void encode(byte[] string, ByteArrayOutputStream out)
    throws IOException {
        out.write(Integer.toString(string.length).getBytes());
        out.write(':');
        out.write(string);
    }

    private static void encode(List<BValue> list, ByteArrayOutputStream out)
    throws IOException {
        out.write('l');
        for (BValue elem : list) {
            encode(elem, out);
        }
        out.write('e');
    }

    private static void encode(Map<String, BValue> map, ByteArrayOutputStream out)
    throws IOException {
        out.write('d');
        for (Map.Entry<String, BValue> entry : map.entrySet()) {
            encode(entry.getKey().getBytes(), out);
            encode(entry.getValue(), out);
        }
        out.write('e');
    }

    public static void encode(BValue elem, ByteArrayOutputStream out)
    throws IOException {
        Object object = elem.toObject();
        if (object instanceof Integer) {
            encode(elem.toInt(), out);
        } else if (object instanceof byte[]) {
            encode(elem.toByteArray(), out);
        } else if (object instanceof List) {
            encode(elem.toList(), out);
        } else if (object instanceof Map) {
            encode(elem.toMap(), out);
        }
    }

    public static byte[] encode(BValue elem) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encode(elem, out);
        return out.toByteArray();
    }
}
