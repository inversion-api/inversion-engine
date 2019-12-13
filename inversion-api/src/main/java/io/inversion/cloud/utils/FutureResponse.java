package io.inversion.cloud.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.inversion.cloud.model.Response;

public abstract class FutureResponse implements RunnableFuture<Response>
{
   static Log            log          = LogFactory.getLog(HttpUtils.class);

   Response              response     = null;
   List<ResponseHandler> onSuccess    = new ArrayList();
   List<ResponseHandler> onFailure    = new ArrayList();
   List<ResponseHandler> onResponse   = new ArrayList();
   int                   retryCount   = 0;
   File                  retryFile;
   int                   totalRetries = 0;                                 // this number doesn't get reset and is the true measure of how many retries occured

   public FutureResponse onSuccess(ResponseHandler handler)
   {
      boolean done = false;
      synchronized (this)
      {
         done = isDone();
         if (!done)
         {
            onSuccess.add(handler);
         }
      }

      if (done && isSuccess())
      {
         try
         {
            handler.onResponse(response);
         }
         catch (Throwable ex)
         {
            log.error("Error handling onSuccess", ex);
         }
      }

      return this;
   }

   public FutureResponse onFailure(ResponseHandler handler)
   {
      boolean done = false;
      synchronized (this)
      {
         done = isDone();
         if (!done)
         {
            onFailure.add(handler);
         }
      }

      if (done && !isSuccess())
      {
         try
         {
            handler.onResponse(response);
         }
         catch (Throwable ex)
         {
            log.error("Error handling onFailure", ex);
         }
      }

      return this;
   }

   public FutureResponse onResponse(ResponseHandler handler)
   {
      boolean done = false;
      synchronized (this)
      {
         done = isDone();
         if (!done)
         {
            onResponse.add(handler);
         }
      }

      if (done)
      {
         try
         {
            handler.onResponse(response);
         }
         catch (Throwable ex)
         {
            log.error("Error handling onResponse", ex);
         }
      }

      return this;
   }

   public void setResponse(Response response)
   {
      synchronized (this)
      {
         this.response = response;

         if (isSuccess())
         {
            for (ResponseHandler h : onSuccess)
            {
               try
               {
                  h.onResponse(response);
               }
               catch (Throwable ex)
               {
                  log.error("Error handling success callbacks in setResponse", ex);
               }
            }
         }
         else
         {
            for (ResponseHandler h : onFailure)
            {
               try
               {
                  h.onResponse(response);
               }
               catch (Throwable ex)
               {
                  log.error("Error handling failure callbacks in setResponse", ex);
               }
            }
         }

         for (ResponseHandler h : onResponse)
         {
            try
            {
               h.onResponse(response);
            }
            catch (Throwable ex)
            {
               log.error("Error handling callbacks in setResponse", ex);
            }
         }

         notifyAll();
      }
   }

   @Override
   public boolean cancel(boolean arg0)
   {
      return false;
   }

   @Override
   public Response get()
   {
      if (SwingUtilities.isEventDispatchThread())
      {
         String msg = "Blocking on the Swing thread. Your code is blocking the UI by calling FutureResponse.get() on the Swing event dispatch thread.  You should consider moving your call into a background thread.";
         Exception ex = new Exception();
         ex.fillInStackTrace();
         log.warn(msg, ex);
      }

      while (response == null)
      {
         synchronized (this)
         {
            if (response == null)
            {
               try
               {
                  wait();
               }
               catch (Exception ex)
               {

               }
            }
         }
      }

      return response;
   }

   public boolean isSuccess()
   {
      if (response != null && response.getError() == null && response.getStatusCode() >= 200 && response.getStatusCode() < 300)
         return true;

      return false;
   }

   public Response get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
   {
      if (SwingUtilities.isEventDispatchThread())
      {
         String msg = "Blocking on the Swing thread. Your code is blocking the UI by calling FutureResponse.get() on the Swing event dispatch thread.  You should consider moving your call into a background thread.";
         Exception ex = new Exception();
         ex.fillInStackTrace();
         log.warn(msg, ex);
      }

      timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
      while (response == null)
      {
         synchronized (this)
         {
            if (response == null)
            {
               wait(timeout);
               if (response == null)
                  throw new TimeoutException(timeout + " millisecond timeout reached");
            }
         }
      }

      return response;
   }

   @Override
   public boolean isCancelled()
   {
      return false;
   }

   @Override
   public boolean isDone()
   {
      return response != null;
   }

   protected int getRetryCount()
   {
      return retryCount;
   }

   public void incrementRetryCount()
   {
      this.totalRetries++;
      this.retryCount++;
   }

   public void resetRetryCount()
   {
      this.retryCount = 0;
   }

   public int getTotalRetries()
   {
      return totalRetries;
   }

   public File getRetryFile()
   {
      return retryFile;
   }

   public void setRetryFile(File retryFile)
   {
      this.retryFile = retryFile;
   }

   public static interface ResponseHandler
   {
      public void onResponse(Response response) throws Exception;
   }
}
