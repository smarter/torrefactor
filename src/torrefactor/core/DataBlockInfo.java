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

/**
 * An immutable object used internally by DataManager to handle
 * blocks of datas from bittorent pieces.
 */
public class DataBlockInfo {
    private int pieceIndex;
    private int offset;
    private int length;

    /**
     * Constructs an immutable object to store info about a block.
     * @param _pieceIndex Index of the piece this block is in
     * @param _offset     Offset of this block within the piece
     * @param _length     Length of this block in bytes
     */
    public DataBlockInfo(int _pieceIndex, int _offset, int _length) {
        this.pieceIndex = _pieceIndex;
        this.offset = _offset;
        this.length = _length;
    }

    /**
     * Returns the index of the piece this block is in.
     */
    public int pieceIndex() {
        return this.pieceIndex;
    }

    /**
     * Returns the offset of this block within the piece.
     */
    public int offset() {
        return this.offset;
    }

    /**
     * Returns the length of this block in bytes.
     */
    public int length() {
        return this.length;
    }

    public String toString() {
        return "DataBlockInfo: index=" + pieceIndex + " offset=" + offset
               + " length=" + length;
    }
}
