/*
 * Copyright (c) 2015-2022 Rocket Partners, LLC
 * https://github.com/inversion-api
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

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;

public class GzipCompressingInputStream extends SequenceInputStream
{
    public GzipCompressingInputStream(InputStream in) throws IOException
    {
        this(in,512);
    }
    public GzipCompressingInputStream(InputStream in, int bufferSize) throws IOException
    {
        super(new StatefullGzipStreamEnumerator(in,bufferSize));
    }

    static enum StreamState
    {
        HEADER,
        CONTENT,
        TRAILER
    }

    protected static class StatefullGzipStreamEnumerator implements Enumeration<InputStream>
    {

        protected final InputStream in;
        protected final int bufferSize;
        protected StreamState state;

        public StatefullGzipStreamEnumerator(InputStream in, int bufferSize)
        {
            this.in=in;
            this.bufferSize=bufferSize;
            state=StreamState.HEADER;
        }

        public boolean hasMoreElements()
        {
            return state!=null;
        }
        public InputStream nextElement()
        {
            switch (state)
            {
                case HEADER:
                    state=StreamState.CONTENT;
                    return createHeaderStream();
                case CONTENT:
                    state=StreamState.TRAILER;
                    return createContentStream();
                case TRAILER:
                    state=null;
                    return createTrailerStream();
            }
            return null;
        }

        static final int GZIP_MAGIC = 0x8b1f;
        static final byte[] GZIP_HEADER=new byte[] {
                (byte) GZIP_MAGIC,        // Magic number (short)
                (byte)(GZIP_MAGIC >> 8),  // Magic number (short)
                Deflater.DEFLATED,        // Compression method (CM)
                0,                        // Flags (FLG)
                0,                        // Modification time MTIME (int)
                0,                        // Modification time MTIME (int)
                0,                        // Modification time MTIME (int)
                0,                        // Modification time MTIME (int)
                0,                        // Extra flags (XFLG)
                0                         // Operating system (OS)
        };
        protected InputStream createHeaderStream()
        {
            return new ByteArrayInputStream(GZIP_HEADER);
        }
        protected InternalGzipCompressingInputStream contentStream;
        protected InputStream createContentStream()
        {
            contentStream=new InternalGzipCompressingInputStream(new CRC32InputStream(in), bufferSize);
            return contentStream;
        }
        protected InputStream createTrailerStream()
        {
            return new ByteArrayInputStream(contentStream.createTrailer());
        }
    }

    /**
     * Internal stream without header/trailer
     */
    protected static class CRC32InputStream extends FilterInputStream
    {
        protected CRC32 crc = new CRC32();
        protected long byteCount;
        public CRC32InputStream(InputStream in)
        {
            super(in);
        }

        @Override
        public int read() throws IOException
        {
            int val=super.read();
            if (val>=0)
            {
                crc.update(val);
                byteCount++;
            }
            return val;
        }
        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            len=super.read(b, off, len);
            if (len>=0)
            {
                crc.update(b,off,len);
                byteCount+=len;
            }
            return len;
        }
        public long getCrcValue()
        {
            return crc.getValue();
        }
        public long getByteCount()
        {
            return byteCount;
        }
    }

    /**
     * Internal stream without header/trailer
     */
    protected static class InternalGzipCompressingInputStream extends DeflaterInputStream
    {
        protected final CRC32InputStream crcIn;
        public InternalGzipCompressingInputStream(CRC32InputStream in, int bufferSize)
        {
            super(in, new Deflater(Deflater.DEFAULT_COMPRESSION, true),bufferSize);
            crcIn=in;
        }
        public void close() throws IOException
        {
            if (in != null)
            {
                try
                {
                    def.end();
                    in.close();
                }
                finally
                {
                    in = null;
                }
            }
        }

        protected final static int TRAILER_SIZE = 8;

        public byte[] createTrailer()
        {
            byte[] trailer= new byte[TRAILER_SIZE];
            writeTrailer(trailer, 0);
            return trailer;
        }

        /*
         * Writes GZIP member trailer to a byte array, starting at a given
         * offset.
         */
        private void writeTrailer(byte[] buf, int offset)
        {
            writeInt((int)crcIn.getCrcValue(), buf, offset); // CRC-32 of uncompr. data
            writeInt((int)crcIn.getByteCount(), buf, offset + 4); // Number of uncompr. bytes
        }

        /*
         * Writes integer in Intel byte order to a byte array, starting at a
         * given offset.
         */
        private void writeInt(int i, byte[] buf, int offset)
        {
            writeShort(i & 0xffff, buf, offset);
            writeShort((i >> 16) & 0xffff, buf, offset + 2);
        }

        /*
         * Writes short integer in Intel byte order to a byte array, starting
         * at a given offset
         */
        private void writeShort(int s, byte[] buf, int offset)
        {
            buf[offset] = (byte)(s & 0xff);
            buf[offset + 1] = (byte)((s >> 8) & 0xff);
        }
    }

}
