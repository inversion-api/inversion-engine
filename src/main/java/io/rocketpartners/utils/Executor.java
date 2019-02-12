/*
 * Copyright (c) 2016-2019 Rocket Partners, LLC
 * http://rocketpartners.io
 * 
 * Copyright 2008-2016 Wells Burke
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
package io.rocketpartners.utils;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Executor
{
   int                        poolMin  = 1;
   int                        poolMax  = 3;

   long                       queueMax = Integer.MAX_VALUE;

   LinkedList<RunnableFuture> queue    = new LinkedList();
   Vector<Thread>             threads  = new Vector();

   boolean                    daemon   = true;

   String                     poolName = "Executor";

   boolean                    shutdown = false;
   long                       delay    = 1000;

   static Timer               timer    = null;

   public Executor(int poolMin, int poolMax, long queueMax)
   {
      this(poolMin, poolMax, queueMax, true, "Executor");
   }

   public Executor(int poolMin, int poolMax, long queueMax, boolean daemon, String poolName)
   {
      this.poolMin = Math.max(this.poolMin, poolMin);
      this.poolMax = poolMax;
      this.queueMax = queueMax;
      this.daemon = daemon;
      this.poolName = poolName;
   }

   public synchronized Future submit(final Runnable task)
   {
      return submit(new RunnableFuture()
         {
            boolean started  = false;
            boolean canceled = false;
            boolean done     = false;

            @Override
            public void run()
            {
               try
               {
                  if (canceled || done)
                     return;

                  started = true;
                  task.run();
               }
               finally
               {
                  synchronized (this)
                  {
                     done = true;
                     notifyAll();
                  }
               }
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning)
            {
               canceled = true;
               return !started;
            }

            @Override
            public boolean isCancelled()
            {
               return canceled;
            }

            @Override
            public boolean isDone()
            {
               return false;
            }

            @Override
            public Object get() throws InterruptedException, ExecutionException
            {
               synchronized (this)
               {
                  while (!done)
                  {
                     wait();
                  }
               }
               return null;
            }

            @Override
            public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
            {
               synchronized (this)
               {
                  while (!done)
                  {
                     wait(unit.toMillis(timeout));
                  }
               }
               return null;
            }

         });
   }

   public synchronized RunnableFuture submit(RunnableFuture task)
   {
      put(task);
      checkStartThread();
      return task;
   }

   public synchronized RunnableFuture submit(final RunnableFuture task, long delay)
   {
      getTimer().schedule(new TimerTask()
         {
            @Override
            public void run()
            {
               Thread t = new Thread(new Runnable()
                  {
                     @Override
                     public void run()
                     {
                        submit(task);
                     }
                  });
               t.setDaemon(daemon);
               t.start();
            }
         }, delay);

      return task;
   }

   static synchronized Timer getTimer()
   {
      if (timer == null)
         timer = new Timer("Executor timer");

      return timer;
   }

   synchronized boolean checkStartThread()
   {
      if (queue.size() > 0 && threads.size() < poolMax)
      {
         Thread t = new Thread(new Runnable()
            {
               public void run()
               {
                  processQueue();
               }
            }, poolName + " worker");
         t.setDaemon(daemon);
         threads.add(t);
         t.start();
         return true;
      }
      return false;
   }

   synchronized boolean checkEndThread()
   {
      if (queue.size() == 0 && threads.size() > poolMin)
      {
         threads.remove(Thread.currentThread());
         return true;
      }
      return false;
   }

   int queued()
   {
      synchronized (queue)
      {
         return queue.size();
      }
   }

   void put(RunnableFuture task)
   {
      synchronized (queue)
      {
         while (queue.size() >= queueMax)
         {
            try
            {
               queue.wait();
            }
            catch (Exception ex)
            {

            }
         }
         queue.add(task);
         queue.notifyAll();
      }
   }

   RunnableFuture take()
   {
      RunnableFuture t = null;
      synchronized (queue)
      {
         while (queue.size() == 0)
         {
            try
            {
               queue.wait();
            }
            catch (InterruptedException ex)
            {

            }
         }

         t = queue.removeFirst();
         queue.notifyAll();
      }
      return t;
   }

   void processQueue()
   {
      try
      {
         while (true && !shutdown && !checkEndThread())
         {
            do
            {
               RunnableFuture task = take();
               task.run();
            }
            while (queue.size() > 0);

            //            if (!shutdown)
            //            {
            //               Thread.sleep(delay);
            //            }
            //
            //            if (queue.size() == 0)
            //               break;
         }
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }
   }

}
