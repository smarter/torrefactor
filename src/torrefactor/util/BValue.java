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
