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

package torrefactor.core;

import torrefactor.util.*;
import java.io.*;
import java.security.*;
import java.util.*;

public class TorrentMaker {
    public static final int SHA1_LENGTH = 20; //In bytes
    private static final String CREATED_BY = "Torrefactor/0.1";

    /**
     * Write a Torrent file.
     * @param torrentFile  The file which will be written.
     * @param inputFile    The file to be shared in the torrent.
     * @param pieceLength  The length of a piece in the torrent.
     * @param announce     The URL(including port) to a tracker containing
     *                     peers for the torrent.
     * @param comment      A comment(optional, can be null)
     */
    public static void write(File torrentFile, File inputFile, int pieceLength,
        String announce, String comment)
        throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        Map<String, BValue> fileMap = makeFileMap(announce, comment);
        Map<String, BValue> infoMap = makeInfoMap(inputFile, pieceLength);
        fileMap.put("info", new BValue(infoMap));
        FileOutputStream fos = new FileOutputStream(torrentFile);
        fos.write(BEncode.encode(new BValue(fileMap)));
        fos.close();
    }

    /**
     * Create and return the bencoded dictionary of a Torrent file, excluding
     * the "info" entry.
     */
    private static Map<String, BValue>
        makeFileMap(String announce, String comment)
        throws FileNotFoundException, IOException {
        Map<String, BValue> fileMap = new HashMap<String, BValue>();
        fileMap.put("announce", new BValue(announce));
        if (comment != null) {
            fileMap.put("comment", new BValue(comment));
        }
        fileMap.put("creation date", new BValue(System.currentTimeMillis()/1000));
        fileMap.put("created by", new BValue(CREATED_BY));
        fileMap.put("encoding", new BValue("UTF-8"));
        return fileMap;
    }

    /**
     * Create and return the bencoded dictionary of the "info" entry of a
     * Torrent file.
     */
    private static Map<String, BValue> makeInfoMap(File file, int pieceLength)
        throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        int pieces = (int) ((file.length() - 1) / pieceLength) + 1;
        byte[] shaArray = new byte[pieces*SHA1_LENGTH];

        MessageDigest md = MessageDigest.getInstance("SHA1");
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        byte[] piece = new byte[pieceLength];
        System.out.println("Size: " + shaArray.length);
        for (int i = 0; i < pieces-1; i++) {
            dis.readFully(piece);
            System.arraycopy(md.digest(piece), 0, shaArray, SHA1_LENGTH*i, SHA1_LENGTH);
        }
        int lastSize = ((int)(file.length() % pieceLength));
        piece = new byte[lastSize];
        dis.readFully(piece);
        System.arraycopy(md.digest(piece), 0, shaArray, (pieces-1)*SHA1_LENGTH, SHA1_LENGTH);
        dis.close();
        Map<String, BValue> infoMap = new HashMap<String, BValue>();
        infoMap.put("name", new BValue(file.getName()));
        infoMap.put("length", new BValue(file.length()));
        infoMap.put("piece length", new BValue(pieceLength));
        infoMap.put("pieces", new BValue(shaArray));
        return infoMap;
    }
}
