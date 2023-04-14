/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.inversion.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream that limits the number of bytes which can be read.
 *
 * @author Charles Fry
 * @since 1.0
 */
public final class LimitInputStream extends FilterInputStream {

    long    left  = -1;
    boolean exact = false;

    public LimitInputStream(InputStream in, long limit, boolean exact) {
        super(in);
        this.left = limit;
    }

    @Override
    public int read() throws IOException {
        int result = in.read();
        if (result != -1) {
            --left;
        } else {
            if (exact && left != 0) {
                throw new IOException("The input stream is less than its expected size, you should have " + left + " bytes remaining");
            }
        }

        if (left < 0) {
            throw new IOException("The input stream has exceeded it maximum allowed size");
        }

        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = in.read(b, off, len);
        if (result != -1) {
            left -= result;
        } else {
            if (exact && left != 0) {
                throw new IOException("The input stream is less than its expected size, you should have " + left + " bytes remaining");
            }
        }

        if (left < 0)
            throw new IOException("The input stream has exceeded it maximum allowed size");

        return result;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("Mark not supported");
    }

    @Override
    public long skip(long n) throws IOException {
        if (left - n < 0) {
            throw new IOException("The input stream has exceeded it maximum allowed size");
        }
        long skipped = in.skip(n);
        left -= skipped;
        return skipped;
    }
}