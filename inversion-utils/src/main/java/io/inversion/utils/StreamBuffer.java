package io.inversion.utils;



import java.io.*;

/**
 * Uses an in memory buffer to hold output until bufferSize data is written then will switch over
 * to writing everything to a temp file.  You get the data back by calling getInputStream() which
 * can be called multiple times, each time returning a new InputStream with all the data.  Calling
 * getInputStream() will close the OutputStream and prevent additional writing.
 */
public class StreamBuffer extends OutputStream {

    protected long bufferSize = 100 * 1024;
    protected String tempDir = null;

    ByteArrayOutputStream memOut = new ByteArrayOutputStream();
    BufferedOutputStream fileOut = null;
    int length = 0;
    File tempFile = null;
    boolean closed = false;
    byte[] bytes = null;
    int openFileStreams = 0;

    public StreamBuffer(){

    }

    public StreamBuffer(InputStream in){
        try{
            Utils.pipe(new BufferedInputStream(in), this);
        }
        catch(Exception ex){
            throw Utils.ex("Error buffering stream");
        }
    }

    OutputStream getOut(int toWrite) throws IOException{

        if(closed)
            throw new IOException("Stream is closed");

        length += toWrite;

        if(fileOut != null)
            return fileOut;

        if(length >= bufferSize){
            if(tempDir != null){
                File tempDirDir = new File(tempDir);
                tempDirDir.mkdirs();
                tempFile = File.createTempFile(getClass().getSimpleName() + "-" + System.currentTimeMillis() + "", ".tmp", tempDirDir);
            }else{
                tempFile = File.createTempFile(getClass().getSimpleName() + "-" + System.currentTimeMillis() + "", ".tmp");
            }
            tempFile.deleteOnExit();

            fileOut = new BufferedOutputStream(new FileOutputStream(tempFile));
            if(memOut != null){
                memOut.flush();
                memOut.close();
                byte[] bytes = memOut.toByteArray();
                memOut = null;
                Utils.pipe(new ByteArrayInputStream(bytes), fileOut, true, false);
            }
            return fileOut;
        }
        return memOut;
    }


    @Override
    public void write(byte[] b) throws IOException {
        getOut(b.length).write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        getOut(len).write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        getOut(1).write(b);
    }


    @Override
    public void flush() throws IOException {
        getOut(0).flush();
    }

    @Override
    public void close() throws IOException {
        getOut(0).close();
        closed = true;
    }

    /**
     * Returns the data that was written to this stream. If the stream is backed by a
     * temp file, the file will be deleted when the inputStream is closed or finalized.
     *
     * @return a stream containing the data written to this stream
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException{
        closed = true;

        if(memOut !=null){
            try{
                memOut.flush();
                memOut.close();
            }
            catch(Exception ex){
                //ignore
            }

            bytes = memOut.toByteArray();
            memOut = null;
        }

        if(bytes != null)
            return new ByteArrayInputStream(bytes);

        if(fileOut != null){
            try{
                fileOut.flush();
                fileOut.close();
            }
            catch(Exception ex){
                //ignore
            }
            fileOut = null;
        }

        if(tempFile != null) {

            FileInputStream in = new FileInputStream(tempFile) {
                //-- NOTE this reference should prevent the garbage collector from
                //-- finalizing the StreamBuffer until all streams have been finalized
                //-- and we know we can delete the underlying file
                StreamBuffer referenceHolder = StreamBuffer.this;
            };

            return in;

        }

        throw new IOException("InputStream has already been retrieved");
    }

    @Override
    public void finalize() throws Throwable {
        try {
            tempFile.delete();
        } catch (Exception ex) {
            //ignore
        } finally {
            tempFile = null;
        }
        super.finalize();
    }

    public int getLength() {
        return length;
    }

    public long getBufferSize() {
        return bufferSize;
    }

    public StreamBuffer withBufferSize(long bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public String getTempDir() {
        return tempDir;
    }

    public StreamBuffer withTempDir(String tempDir) {
        this.tempDir = tempDir;
        return this;
    }

    public File getTempFile() {
        return tempFile;
    }

    public String toString(){
        return "StreamBuffer[" + length + "]";
    }

}
