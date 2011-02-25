package test.util;

import java.io.Reader;
import java.io.StringReader;
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
        Reader reader = new StringReader("12345e");
        int expected = 12345;

        int result = Bencode.decodeInt(reader);
        assertTrue(result == expected);
    }

    @Test public void decodeValidInt2()
    throws java.io.IOException, InvalidBencodeException {
        Reader reader = new StringReader("i987654321e");
        int expected = 987654321;

        int result = Bencode.decodeInt(reader);
        assertTrue(result == expected);
    }

    @Test public void decodeValidString()
    throws java.io.IOException, InvalidBencodeException {
        Reader reader = new StringReader("6:Foobar");
        String expected = "Foobar";

        String result = Bencode.decodeString(reader);
        assertTrue(result.equals(expected));
    }

    @Test public void decodeValidList()
    throws java.io.IOException, InvalidBencodeException {
        Reader reader = new StringReader("5:coffei42ee");
        ArrayList<Object> expected = new ArrayList<Object>();
        expected.add("coffe");
        expected.add(42);

        ArrayList result = Bencode.decodeList(reader);
        assertTrue(result.equals(expected));
    }

    @Test public void decodeValidList2()
    throws java.io.IOException, InvalidBencodeException {
        Reader reader = new StringReader("l4:Salt5:Sugare");
        ArrayList<Object> expected = new ArrayList<Object>();
        expected.add("Salt");
        expected.add("Sugar");

        ArrayList result = Bencode.decodeList(reader);
        assertTrue(result.equals(expected));
    }

    @Test public void decodeValidDictionary()
    throws java.io.IOException, InvalidBencodeException {
        Reader reader = new StringReader("3:Foo3:Bar10:coffe cupsi849ee");
        HashMap<String, Object> expected = new HashMap<String, Object>();
        expected.put("Foo", "Bar");
        expected.put("coffe cups", 849);

        HashMap result = Bencode.decodeDictionary(reader);
        assertTrue(result.equals(expected));
    }

    @Test public void decodeValidDictionary2()
    throws java.io.IOException, InvalidBencodeException {
        Reader reader = new StringReader("d3:Foo3:Bar10:coffe cupsi849ee");
        HashMap<String, Object> expected = new HashMap<String, Object>();
        expected.put("Foo", "Bar");
        expected.put("coffe cups", 849);

        HashMap result = Bencode.decodeDictionary(reader);
        assertTrue(result.equals(expected));
    }
}
