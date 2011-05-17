package test.util;

import java.io.*;
import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import torrefactor.util.*;

public class BEncodeTest {
    public static void main (String[] args) {
        org.junit.runner.JUnitCore.main("test.util.BEncodeTest");
    }

    @Test public void DecodeEncodeDecode()
    throws IOException, InvalidBDecodeException {
        InputStream origStream = new FileInputStream("data/Ocarina_of_Time__Complete_Collection_-_Zelda_Reorchestrated.5241636.TPB.torrent");
        Map<String, BValue> decoded1 = BDecode.decodeDict(origStream);
        //System.out.println("XX");
        byte[] encoded = BEncode.encode(new BValue(decoded1));
        InputStream encStream = new ByteArrayInputStream(encoded);
        Map<String, BValue> decoded2 = BDecode.decodeDict(encStream);
        //System.out.println(decoded1.toString());
        //System.out.println(decoded2.toString());
        /*for (Map.Entry<String, BValue> dec1 : decoded1.entrySet()) {
            BValue dec2Val = decoded2.get(dec1.getKey());
            if (!dec2Val.equals(dec1.getValue())) {
                System.out.println("XX");
                System.out.println(dec2Val.toString());
                System.out.println(dec1.getValue().toString());
            }
        }*/
        assertTrue(decoded1.equals(decoded2));
    }
}
