package io.inversion.utils;

import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class TempStreamTest {
    @Test
    public void smallDataUsesMemoryBufferOnly() throws IOException {

        String dataOut = "hello world";

        StreamBuffer out = new StreamBuffer();
        out.write(dataOut.getBytes());

        InputStream in   = out.getInputStream();
        assertTrue(in instanceof ByteArrayInputStream);

        String      dataIn = Utils.read(in);
        assertEquals(dataIn, dataOut);
    }

    @Test
    public void bigDataUsesTempFile() throws IOException {

        StreamBuffer out = new StreamBuffer();

        StringBuilder dataOut = new StringBuilder();

        int written = 0;
        while(written < out.getBufferSize() + 10){
            byte[] bytes = "x".getBytes();
            out.write(bytes);
            dataOut.append("x");
            written += bytes.length;
        }

        InputStream in   = out.getInputStream();
        assertTrue(in instanceof FileInputStream);

        File tempFile = out.getTempFile();
        assertTrue(tempFile.exists());

        String      dataIn = Utils.read(in);
        in.close();
        assertEquals(dataIn, dataOut.toString());
    }
}
