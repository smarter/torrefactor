/* 
 *  This file is part of the Torrefactor project
 *  Copyright 2011 Guillaume Martres <smarter@ubuntu.com>
 *  Copyright 2011 Florian Vessaz <florian.vessaz@gmail.com> 
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *      1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *      2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

package test.core;

import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import torrefactor.core.DataManager;
import torrefactor.core.DataBlock;
import torrefactor.util.Pair;


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
        List<Pair<File, Long>> files = new ArrayList<Pair<File, Long>>();
        files.add(new Pair<File, Long>(
                    new File("data/test/DataManager/test0123"),
                    new Long(77)));
        int pieceSize = 16;
        byte[] blockData;
        byte[] blockDataExpected = {
            0x30 & 0xFF, 0x30 & 0xFF, 0x30 & 0xFF, 0x30 & 0xFF,
            0x31 & 0xFF, 0x31 & 0xFF, 0x31 & 0xFF, 0x31 & 0xFF,
            0x32 & 0xFF, 0x32 & 0xFF, 0x32 & 0xFF, 0x32 & 0xFF,
            0x33 & 0xFF, 0x33 & 0xFF, 0x33 & 0xFF, 0x33 & 0xFF};

        DataManager dataManager = new DataManager (files, pieceSize);


        blockData = dataManager.getBlock(0, 0, pieceSize);
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
        List<Pair<File, Long>> files = new ArrayList<Pair<File, Long>>();
        files.add(new Pair<File, Long>(
                    new File("data/test/DataManager/test0123"),
                    new Long(77)));
        int pieceSize = 4;
        byte[] blockData;
        byte[] blockDataExpected = {
            0x33 & 0xFF, 0x33 & 0xFF, 0x0A & 0xFF, 0x0A & 0xFF };

        DataManager dataManager = new DataManager (files, pieceSize);


        blockData = dataManager.getBlock(3, 2, pieceSize);
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
        List<Pair<File, Long>> files = new ArrayList<Pair<File, Long>>();
        files.add(new Pair<File, Long>(
                    new File("data/test/DataManager/test0123"),
                    new Long(77)));
        int pieceSize = 4;
        byte[] data;
        byte[] dataExpected;

        DataManager dataManager = new DataManager (files, pieceSize);

        data = dataManager.getPiece(0);

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
        dataManager.putBlock(0, 0, dataMod);
        printByteArray(dataMod);
        System.out.print("Read:  ");
        data = dataManager.getBlock(0, 0, pieceSize);
        printByteArray(data);
        assertTrue(Arrays.equals(data, dataMod));

        System.out.print("Write: ");
        dataManager.putBlock(0, 0, dataExpected);
        printByteArray(dataExpected);
        System.out.print("Read:  ");
        data = dataManager.getBlock(0, 0, pieceSize);
        printByteArray(data);
        assertTrue(Arrays.equals(data, dataExpected));
    }

    @Test public void testMultipleFiles()
    throws Exception {
        System.out.print('\n');
        int size = 77;
        List<Pair<File, Long>> files = new ArrayList<Pair<File, Long>>();
        files.add(new Pair<File, Long>(
                    new File("data/test/DataManager/test1"),
                    new Long(15)));
        files.add(new Pair<File, Long>(
                    new File("data/test/DataManager/test2"),
                    new Long(7)));
        int pieceSize = 4;
        byte[] blockData;
        byte[] blockDataExpected = {
            0x0A & 0xFF, 0x37 & 0xFF, 0x37 & 0xFF, 0x38 & 0xFF };

        DataManager dataManager = new DataManager (files, pieceSize);


        blockData = dataManager.getBlock(3, 2, pieceSize);
        System.out.print("Read: ");
        printByteArray(blockData);
        System.out.print("Expe: ");
        printByteArray(blockDataExpected);
        assertTrue(Arrays.equals(blockData, blockDataExpected));

    }
}
