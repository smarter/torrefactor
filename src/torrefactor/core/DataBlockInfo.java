package torrefactor.core;

public class DataBlockInfo {
    final int pieceIndex;
    final int offset;
    final int length;

    public DataBlockInfo(int _pieceIndex, int _offset, int _length) {
        this.pieceIndex = _pieceIndex;
        this.offset = _offset;
        this.length = _length;
    }

    public int pieceIndex() {
        return this.pieceIndex;
    }

    public int offset() {
        return this.offset;
    }

    public int length() {
        return this.length;
    }
}
