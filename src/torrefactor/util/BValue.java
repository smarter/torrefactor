package torrefactor.util;

import java.util.*;

/**
 * This class wraps a bencoded object. It is used to provide type-safety
 * when decoding Bencoded data. 
 * toString(), toInt(), toLong(), toByteArray(), toList(), toMap() do NOT check
 * the type of the wrapped value and shouldn't be used without knowing the type
 * of the underlying value (which can be obtained using toObject().getClass()).
 *
 * In Haskell, this whole class could be replaced by:
 * data BValue = BInteger Integer | BByteString ByteString | BString String | BDict (Map String BValue) | BList [BValue]
 */
public class BValue {
    private final Object value;

    public BValue(int number) {
        // This is not an error we store int as long
        this.value = Long.valueOf(number);
    }

    public BValue(long number) {
        this.value = Long.valueOf(number);
    }

    public BValue(byte[] byteArray) {
        this.value = byteArray;
    }

    public BValue(String string) {
        this.value = string.getBytes();
    }

    public BValue(List<BValue> list) {
        this.value = list;
    }

    public BValue(Map<String, BValue> map) {
        this.value = map;
    }

    public boolean equals(Object obj) {
        if (! (obj instanceof BValue)) return false;
        BValue other = (BValue) obj;
        if (this.value instanceof byte[]) {
            return Arrays.equals((byte[]) this.value, (byte[]) other.value);
        } else {
            return this.value.equals(other.value);
        }
    }

    public int hashCode() {
        if (this.value instanceof byte[]) {
            return Arrays.hashCode((byte[]) this.value);
        } else {
            return this.value.hashCode();
        }
    }

    public Object toObject() {
        return this.value;
    }

    public String toString() {
        if (this.value instanceof byte[]) {
            return new String( (byte[]) this.value);
        } else if (this.value instanceof Integer) {
            return ( (Integer) this.value).toString();
        } else if (this.value instanceof Long) {
            return ( (Long) this.value).toString();
        } else if (this.value instanceof List) {
            return ( (List) this.value).toString();
        } else {
            return this.value.toString();
        }
    }

    public int toInt() {
        return (int) ((Long) this.value).longValue();
    }

    public long toLong() {
        return ((Long) this.value).longValue();
    }

    public byte[] toByteArray() {
        return (byte[]) this.value;
    }

    @SuppressWarnings(value = "unchecked")
    public List<BValue> toList() {
        return (List<BValue>) this.value;
    }

    @SuppressWarnings(value = "unchecked")
    public HashMap<String, BValue> toMap() {
        return (HashMap<String, BValue>) this.value;
    }
}
