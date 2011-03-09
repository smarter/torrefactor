package test.core;

import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.*;

import java.util.Arrays;

import torrefactor.core.DataManager;
import torrefactor.core.DataBlock;


public class DataManagerTest {

    public static void main (String[] args) {
        org.junit.runner.JUnitCore.main("test.core.DataManagerTest");
    }

    public static void printByteArray(byte[] array) {
        for (int i=0; i<array.length; i++) {
            System.out.print(Integer.toString(array[i] & 0xFF, 16) + " ");
        }
        System.out.print("\n");
    }



    @Test public void test0123r1()
    throws Exception {
        System.out.print('\n');
        int size = 77;
        String[] filePaths = {"data/test/DataManager/test0123"};
        long[] fileSizes = {77};
        int pieceSize = 16;
        byte[] blockData;
        byte[] blockDataExpected = {
            0x30 & 0xFF, 0x30 & 0xFF, 0x30 & 0xFF, 0x30 & 0xFF,
            0x31 & 0xFF, 0x31 & 0xFF, 0x31 & 0xFF, 0x31 & 0xFF,
            0x32 & 0xFF, 0x32 & 0xFF, 0x32 & 0xFF, 0x32 & 0xFF,
            0x33 & 0xFF, 0x33 & 0xFF, 0x33 & 0xFF, 0x33 & 0xFF};

        DataManager dataManager = new DataManager (
                                             filePaths, fileSizes, pieceSize);


        DataBlock block = dataManager.getBlock(0, 0, pieceSize);
        blockData = block.get(0, pieceSize);
        System.out.print("Read: ");
        printByteArray(blockData);
        System.out.print("Expe: ");
        printByteArray(blockDataExpected);
        assertTrue(Arrays.equals(blockData, blockDataExpected));

    }

    @Test public void test0123r2()
    throws Exception {
        System.out.print('\n');
        int size = 77;
        String[] filePaths = {"data/test/DataManager/test0123"};
        long[] fileSizes = {77};
        int pieceSize = 4;
        byte[] blockData;
        byte[] blockDataExpected = {
            0x33 & 0xFF, 0x33 & 0xFF, 0x0A & 0xFF, 0x0A & 0xFF };

        DataManager dataManager = new DataManager (
                                             filePaths, fileSizes, pieceSize);


        DataBlock block = dataManager.getBlock(3, 2, pieceSize);
        blockData = block.get(0, pieceSize);
        System.out.print("Read: ");
        printByteArray(blockData);
        System.out.print("Expe: ");
        printByteArray(blockDataExpected);
        assertTrue(Arrays.equals(blockData, blockDataExpected));

    }

    @Test public void test0123w1()
    throws Exception {
        System.out.print('\n');
        int size = 77;
        String[] filePaths = {"data/test/DataManager/test0123"};
        long[] fileSizes = {77};
        int pieceSize = 4;
        byte[] data;
        byte[] dataExpected;

        DataManager dataManager = new DataManager (
                                             filePaths, fileSizes, pieceSize);

        DataBlock block = dataManager.getPiece(0);
        data = block.get(0, pieceSize);

        dataExpected = new byte[] {
            0x30 & 0xFF, 0x30 & 0xFF, 0x30 & 0xFF, 0x30 & 0xFF };
        System.out.print("Read: ");
        printByteArray(data);
        System.out.print("Expe: ");
        printByteArray(dataExpected);
        assertTrue(Arrays.equals(data, dataExpected));

        System.out.print("Write: ");
        byte[] dataMod = new byte[] {
            0x10 & 0xFF, 0x10 & 0xFF, 0x10 & 0xFF, 0x10 & 0xFF };
        block.put(0, dataMod);
        printByteArray(dataMod);
        System.out.print("Read:  ");
        data = block.get(0, pieceSize);
        printByteArray(data);
        assertTrue(Arrays.equals(data, dataMod));

        System.out.print("Write: ");
        block.put(0, dataExpected);
        printByteArray(dataExpected);
        System.out.print("Read:  ");
        data = block.get(0, pieceSize);
        printByteArray(data);
        assertTrue(Arrays.equals(data, dataExpected));
    }

    @Test public void testMultipleFiles()
    throws Exception {
        System.out.print('\n');
        int size = 77;
        String[] filePaths = {
            "data/test/DataManager/test1",
            "data/test/DataManager/test2" };
        long[] fileSizes = {15, 7};
        int pieceSize = 4;
        byte[] blockData;
        byte[] blockDataExpected = {
            0x0A & 0xFF, 0x37 & 0xFF, 0x37 & 0xFF, 0x38 & 0xFF };

        DataManager dataManager = new DataManager (
                                             filePaths, fileSizes, pieceSize);


        DataBlock block = dataManager.getBlock(3, 2, pieceSize);
        blockData = block.get(0, pieceSize);
        System.out.print("Read: ");
        printByteArray(blockData);
        System.out.print("Expe: ");
        printByteArray(blockDataExpected);
        assertTrue(Arrays.equals(blockData, blockDataExpected));

    }
}
