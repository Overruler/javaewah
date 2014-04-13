package com.googlecode.javaewah32;

import com.googlecode.javaewah.IntIterator;

import static com.googlecode.javaewah32.EWAHCompressedBitmap32.wordinbits;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, gssiyankai
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * This class is equivalent to IntIteratorImpl, except that it allows
 * use to iterate over "clear" bits (bits set to 0).
 *
 * @author gssiyankai
 */
final class ClearIntIterator32 implements IntIterator {

    private final EWAHIterator32 ewahIter;
    private final int sizeInBits;
    private final int[] ewahBuffer;
    private int position;
    private int runningLength;
    private int word;
    private int wordPosition;
    private int wordLength;
    private int literalPosition;
    private boolean hasNext;

    ClearIntIterator32(EWAHIterator32 ewahIter, int sizeInBits) {
        this.ewahIter = ewahIter;
        this.sizeInBits = sizeInBits;
        this.ewahBuffer = ewahIter.buffer();
        this.hasNext = this.moveToNext();
    }

    public boolean moveToNext() {
        while (!runningHasNext() && !literalHasNext()) {
            if (!this.ewahIter.hasNext()) {
                return false;
            }
            setRunningLengthWord(this.ewahIter.next());
        }
        return true;
    }

    @Override
    public boolean hasNext() {
        return this.hasNext;
    }

    @Override
    public int next() {
        final int answer;
        if (runningHasNext()) {
            answer = this.position++;
        } else {
            final int t = this.word & -this.word;
            answer = this.literalPosition + Integer.bitCount(t - 1);
            this.word ^= t;
        }
        this.hasNext = this.moveToNext();
        return answer;
    }

    private void setRunningLengthWord(RunningLengthWord32 rlw) {
        this.runningLength = wordinbits * rlw.getRunningLength() + this.position;
        if (rlw.getRunningBit()) {
            this.position = this.runningLength;
        }

        this.wordPosition = this.ewahIter.literalWords();
        this.wordLength = this.wordPosition
                + rlw.getNumberOfLiteralWords();
    }

    private boolean runningHasNext() {
        return this.position < this.runningLength;
    }

    private boolean literalHasNext() {
        while (this.word == 0 && this.wordPosition < this.wordLength) {
            this.word = ~this.ewahBuffer[this.wordPosition++];
            if (this.wordPosition == this.wordLength && !this.ewahIter.hasNext()) {
                final int usedBitsInLast = this.sizeInBits % wordinbits;
                if (usedBitsInLast > 0) {
                    this.word &= ((~0) >>> (wordinbits - usedBitsInLast));
                }
            }
            this.literalPosition = this.position;
            this.position += wordinbits;
        }
        return this.word != 0;
    }
}
