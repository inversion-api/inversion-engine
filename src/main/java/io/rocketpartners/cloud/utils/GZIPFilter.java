/*
 * Copyright 2003 Jayson Falkner (jayson@jspinsider.com)
 * This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
 * http://www.jspbook.com. You may freely use the code both commercially
 * and non-commercially. If you like the code, please pick up a copy of
 * the book and help support the authors, development of more free code,
 * and the JSP/Servlet/J2EE community.
 */
package io.rocketpartners.cloud.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class GZIPFilter implements Filter
{

   @Override
   public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException
   {
      if (req instanceof HttpServletRequest)
      {
         HttpServletRequest request = (HttpServletRequest) req;
         HttpServletResponse response = (HttpServletResponse) res;
         String ae = request.getHeader("accept-encoding");
         if (ae != null && ae.indexOf("gzip") != -1)
         {
            GZIPResponseWrapper wrappedResponse = new GZIPResponseWrapper(response);
            chain.doFilter(req, wrappedResponse);
            wrappedResponse.finishResponse();
            return;
         }
         chain.doFilter(req, res);
      }
   }

   @Override
   public void init(FilterConfig filterConfig)
   {
      // noop
   }

   @Override
   public void destroy()
   {
      // noop
   }
}

class GZIPResponseStream extends ServletOutputStream
{
   protected ByteArrayOutputStream baos       = null;
   protected GZIPOutputStream      gzipstream = null;
   protected boolean               closed     = false;
   protected HttpServletResponse   response   = null;
   protected ServletOutputStream   output     = null;

   public GZIPResponseStream(HttpServletResponse response) throws IOException
   {
      super();
      closed = false;
      this.response = response;
      this.output = response.getOutputStream();
      baos = new ByteArrayOutputStream();
      gzipstream = new GZIPOutputStream(baos);
   }

   @Override
   public boolean isReady()
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void setWriteListener(WriteListener arg0)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void close() throws IOException
   {
      if (closed)
      {
         throw new IOException("This output stream has already been closed");
      }
      gzipstream.finish();

      byte[] bytes = baos.toByteArray();

      response.addHeader("Content-Length", Integer.toString(bytes.length));
      response.addHeader("Content-Encoding", "gzip");
      output.write(bytes);
      output.flush();
      output.close();
      closed = true;
   }

   @Override
   public void flush() throws IOException
   {
      if (closed)
      {
         throw new IOException("Cannot flush a closed output stream");
      }
      gzipstream.flush();
   }

   @Override
   public void write(int b) throws IOException
   {
      if (closed)
      {
         throw new IOException("Cannot write to a closed output stream");
      }
      gzipstream.write((byte) b);
   }

   @Override
   public void write(byte b[]) throws IOException
   {
      write(b, 0, b.length);
   }

   @Override
   public void write(byte b[], int off, int len) throws IOException
   {
      if (closed)
      {
         throw new IOException("Cannot write to a closed output stream");
      }
      gzipstream.write(b, off, len);
   }

   public boolean closed()
   {
      return (this.closed);
   }

   public void reset()
   {
      //noop
   }

   //   @Override
   //   public boolean isReady()
   //   {
   //      // TODO Auto-generated method stub
   //      return false;
   //   }
   //
   //   @Override
   //   public void setWriteListener(WriteListener arg0)
   //   {
   //      // TODO Auto-generated method stub
   //      
   //   }
}

/*
 * Copyright 2003 Jayson Falkner (jayson@jspinsider.com)
 * This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
 * http://www.jspbook.com. You may freely use the code both commercially
 * and non-commercially. If you like the code, please pick up a copy of
 * the book and help support the authors, development of more free code,
 * and the JSP/Servlet/J2EE community.
 */
class GZIPResponseWrapper extends HttpServletResponseWrapper
{
   protected HttpServletResponse origResponse = null;
   protected ServletOutputStream stream       = null;
   protected PrintWriter         writer       = null;

   public GZIPResponseWrapper(HttpServletResponse response)
   {
      super(response);
      origResponse = response;
   }

   public ServletOutputStream createOutputStream() throws IOException
   {
      return (new GZIPResponseStream(origResponse));
   }

   public void finishResponse()
   {
      try
      {
         if (writer != null)
         {
            writer.close();
         }
         else
         {
            if (stream != null)
            {
               stream.close();
            }
         }
      }
      catch (IOException e)
      {
      }
   }

   @Override
   public void flushBuffer() throws IOException
   {
      stream.flush();
   }

   @Override
   public ServletOutputStream getOutputStream() throws IOException
   {
      if (writer != null)
      {
         throw new IllegalStateException("getWriter() has already been called!");
      }

      if (stream == null)
         stream = createOutputStream();
      return (stream);
   }

   @Override
   public PrintWriter getWriter() throws IOException
   {
      if (writer != null)
      {
         return (writer);
      }

      if (stream != null)
      {
         throw new IllegalStateException("getOutputStream() has already been called!");
      }

      stream = createOutputStream();
      writer = new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));
      return (writer);
   }

   @Override
   public void setContentLength(int length)
   {
   }
}