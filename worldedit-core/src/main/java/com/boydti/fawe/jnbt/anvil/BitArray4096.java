package com.boydti.fawe.jnbt.anvil;

public final class BitArray4096 {

    private final int bitsPerEntry;
    private final int maxSeqLocIndex;
    private final int maxEntryValue;
    private final long[] data;
    private final int longLen;

    public BitArray4096(long[] buffer, int bitsPerEntry) {
        this.bitsPerEntry = bitsPerEntry;
        this.maxSeqLocIndex = 64 - bitsPerEntry;
        maxEntryValue = (1 << bitsPerEntry) - 1;
        this.longLen = (this.bitsPerEntry * 4096) >> 6;
        if (buffer.length < longLen) {
            System.out.println("Invalid buffer " + buffer.length + " | " + longLen);
            this.data = new long[longLen];
        } else {
            this.data = buffer;
        }
    }

    public BitArray4096(int bitsPerEntry) {
        this.bitsPerEntry = bitsPerEntry;
        this.maxSeqLocIndex = 64 - bitsPerEntry;
        maxEntryValue = (1 << bitsPerEntry) - 1;
        this.longLen = (this.bitsPerEntry * 4096) >> 6;
        this.data = new long[longLen];
    }

    public final void setAt(int index, int value) {
        if (longLen == 0) return;
        int bitIndexStart = index * bitsPerEntry;
        int longIndexStart = bitIndexStart >> 6;
        int localBitIndexStart = bitIndexStart & 63;
        this.data[longIndexStart] = this.data[longIndexStart] & ~((long) maxEntryValue << localBitIndexStart) | ((long) value) << localBitIndexStart;

        if(localBitIndexStart > maxSeqLocIndex) {
            int longIndexEnd = longIndexStart + 1;
            int localShiftStart = 64 - localBitIndexStart;
            int localShiftEnd = bitsPerEntry - localShiftStart;
            this.data[longIndexEnd] = this.data[longIndexEnd] >>> localShiftEnd << localShiftEnd | (((long) value) >> localShiftStart);
        }
    }

    public final int getAt(int index) {
        if (longLen == 0) return 0;
        int bitIndexStart = index * bitsPerEntry;

        int longIndexStart = bitIndexStart >> 6;

        int localBitIndexStart = bitIndexStart & 63;
        if(localBitIndexStart <= maxSeqLocIndex) {
            return (int)(this.data[longIndexStart] >>> localBitIndexStart & maxEntryValue);
        } else {
            int localShift = 64 - localBitIndexStart;
            return (int) ((this.data[longIndexStart] >>> localBitIndexStart | this.data[longIndexStart + 1] << localShift) & maxEntryValue);
        }
    }

    public int getLength() {
        return longLen;
    }

    public final void fromRawSlow(char[] arr) {
        for (int i = 0; i < arr.length; i++) {
            setAt(i, arr[i]);
        }
    }

    public final void fromRaw(int[] arr) {
        final long[] data = this.data;
        final int bitsPerEntry = this.bitsPerEntry;
        final int maxSeqLocIndex = this.maxSeqLocIndex;

        int localStart = 0;
        int lastVal;
        int arrI = 0;
        long l = 0;
        long nextVal;
        for (int i = 0; i < longLen; i++) {
            for (; localStart <= maxSeqLocIndex; localStart += bitsPerEntry) {
                lastVal = arr[arrI++];
                l |= ((long) lastVal << localStart);
            }
            if (localStart < 64) {
                if (i != longLen - 1) {
                    lastVal = arr[arrI++];
                    int shift = 64 - localStart;

                    nextVal = lastVal >> shift;

                    l |= ((lastVal - (nextVal << shift)) << localStart);

                    data[i] = l;
                    data[i + 1] = l = nextVal;

                    localStart -= maxSeqLocIndex;
                }
            } else {
                localStart = 0;
                data[i] = l;
                l = 0;
            }
        }
    }

    public BitArray4096 growSlow(int bitsPerEntry) {
        BitArray4096 newBitArray = new BitArray4096(bitsPerEntry);
        for (int i = 0; i < 4096; i++) {
            newBitArray.setAt(i, getAt(i));
        }
        return newBitArray;
    }

    public final char[] toRawSlow() {
        char[] arr = new char[4096];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (char) getAt(i);
        }
        return arr;
    }

    public final int[] toRaw() {
        return toRaw(new int[4096]);
    }

    public final int[] toRaw(int[] buffer) {
        final long[] data = this.data;
        final int dataLength = longLen;
        final int bitsPerEntry = this.bitsPerEntry;
        final int maxEntryValue = this.maxEntryValue;
        final int maxSeqLocIndex = this.maxSeqLocIndex;

        int localStart = 0;
        char lastVal;
        int arrI = 0;
        long l;
        for (int i = 0; i < dataLength; i++) {
            l = data[i];
            for (; localStart <= maxSeqLocIndex; localStart += bitsPerEntry) {
                lastVal = (char) (l >>> localStart & maxEntryValue);
                buffer[arrI++] = lastVal;
            }
            if (localStart < 64) {
                if (i != dataLength - 1) {
                    lastVal = (char) (l >>> localStart);
                    localStart -= maxSeqLocIndex;
                    l = data[i + 1];
                    int localShift = bitsPerEntry - localStart;
                    lastVal |= l << localShift;
                    lastVal &= maxEntryValue;
                    buffer[arrI++] = lastVal;
                }
            } else {
                localStart = 0;
            }
        }
        return buffer;
    }
}