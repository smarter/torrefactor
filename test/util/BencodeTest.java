package test.util;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.*;

import torrefactor.util.Bencode;
import torrefactor.util.InvalidBencodeException;


public class BencodeTest {

    public static void main (String[] args) {
        org.junit.runner.JUnitCore.main("test.util.BencodeTest");
    }

    @Test public void decodeValidInt()
    throws java.io.IOException, InvalidBencodeException {
        String string = "i12345e";
        InputStream stream = new ByteArrayInputStream(string.getBytes());
        int expected = 12345;

        int result = Bencode.decodeInt(stream);
        assertTrue(result == expected);
    }

    @Test public void decodeValidString()
    throws java.io.IOException, InvalidBencodeException {
        String string = "6:Foobar";
        InputStream stream = new ByteArrayInputStream(string.getBytes());
        String expected = "Foobar";

        String result = new String(Bencode.decodeByteArray(stream));
        assertTrue(result.equals(expected));
    }

    @Test public void decodeValidList()
    throws java.io.IOException, InvalidBencodeException {
        String string = "l6:coffeei42ee";
        InputStream stream = new ByteArrayInputStream(string.getBytes());
        ArrayList<Bencode> result = Bencode.decodeList(stream);

        assertTrue((new String(result.get(0).toByteArray())).equals("coffee"));
        assertTrue(result.get(1).toInt() ==  42);
    }

    @Test public void decodeValidDictionary()
    throws java.io.IOException, InvalidBencodeException {
        String string = "d3:Foo3:Bar11:coffee cupsi849ee";
        InputStream stream = new ByteArrayInputStream(string.getBytes());

        HashMap<String, Bencode> result = Bencode.decodeDict(stream);

        assertTrue(new String(result.get("Foo").toByteArray()).equals("Bar"));
        assertTrue(result.get("coffee cups").toInt() == 849);
    }
}
