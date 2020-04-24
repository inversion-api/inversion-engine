package io.inversion;

import java.io.InputStream;
import java.util.List;

public interface Uploader
{
   public List<Upload> getUploads();

   public static class Upload
   {
      String      fileName    = null;
      long        fileSize    = 0;
      String      requestPath = null;
      InputStream inputStream = null;

      public Upload(String fileName, long fileSize, String requestPath, InputStream inputStream)
      {
         super();
         this.fileName = fileName;
         this.fileSize = fileSize;
         this.requestPath = requestPath;
         this.inputStream = inputStream;
      }

      public String getFileName()
      {
         return fileName;
      }

      public void setFileName(String fileName)
      {
         this.fileName = fileName;
      }

      public long getFileSize()
      {
         return fileSize;
      }

      public void setFileSize(long fileSize)
      {
         this.fileSize = fileSize;
      }

      public String getRequestPath()
      {
         return requestPath;
      }

      public void setRequestPath(String requestPath)
      {
         this.requestPath = requestPath;
      }

      public InputStream getInputStream()
      {
         return inputStream;
      }

      public void setInputStream(InputStream inputStream)
      {
         this.inputStream = inputStream;
      }

   }
}
