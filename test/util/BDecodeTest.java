package test.util;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.*;

import torrefactor.util.BDecode;
import torrefactor.util.BValue;
import torrefactor.util.InvalidBDecodeException;


public class BDecodeTest {

    public static void main (String[] args) {
        org.junit.runner.JUnitCore.main("test.util.BDecodeTest");
    }

    @Test public void decodeValidInt()
    throws java.io.IOException, InvalidBDecodeException {
        String string = "i12345e";
        InputStream stream = new ByteArrayInputStream(string.getBytes());
        int expected = 12345;

        long result = BDecode.decodeLong(stream);
        assertTrue((int) result == expected);
    }

    @Test public void decodeValidString()
    throws java.io.IOException, InvalidBDecodeException {
        String string = "6:Foobar";
        InputStream stream = new ByteArrayInputStream(string.getBytes());
        String expected = "Foobar";

        String result = new String(BDecode.decodeByteArray(stream));
        assertTrue(result.equals(expected));
    }

    @Test public void decodeValidList()
    throws java.io.IOException, InvalidBDecodeException {
        String string = "l6:coffeei42ee";
        InputStream stream = new ByteArrayInputStream(string.getBytes());
        ArrayList<BValue> result = BDecode.decodeList(stream);

        assertTrue((new String(result.get(0).toByteArray())).equals("coffee"));
        assertTrue(result.get(1).toInt() ==  42);
    }

    @Test public void decodeValidDictionary()
    throws java.io.IOException, InvalidBDecodeException {
        String string = "d3:Foo3:Bar11:coffee cupsi849ee";
        InputStream stream = new ByteArrayInputStream(string.getBytes());

        HashMap<String, BValue> result = BDecode.decodeDict(stream);

        assertTrue(new String(result.get("Foo").toByteArray()).equals("Bar"));
        assertTrue(result.get("coffee cups").toInt() == 849);
    }
}
