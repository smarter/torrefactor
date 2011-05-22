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
