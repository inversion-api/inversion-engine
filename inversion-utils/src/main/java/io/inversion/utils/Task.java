package io.inversion.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Task {

    List     steps  = new ArrayList();
    String   method = null;
    Object[] args   = null;
    int      next   = 0;

    public static Task buildTask(List tasks, String method, Object... args) {
        Task tl = new Task();
        tl.steps.addAll(tasks);
        tl.method = method;
        tl.args = args;
        return tl;
    }


    public void go() {
        while (next()) {
            //-- intentionally empty
        }
    }

    public boolean next() {
        if (next < steps.size()) {
            Object step = steps.get(next);
            next += 1;
            if (step != null) {
                Method method = Utils.getMethod(step.getClass(), this.method);
                if (method != null) {
                    try {
                        if (method.getParameterCount() != (args != null ? args.length : 0)) {
                            if (args == null)
                                args = new Object[0];

                            List argsList = Utils.asList(args);
                            argsList.add(0, this);
                            args = argsList.toArray();
                        }

                        method.invoke(step, args);
                    } catch (Exception ex) {
                        throw Utils.ex(ex);
                    }
                }
            }
        }
        return next < steps.size();
    }

    public int getNext() {
        return next;
    }

    /**
     * Use this method to intentionally skip invoking the
     * supplied method on the next Object in the chain.
     *
     * @return
     */
    public Task skipNext() {
        this.next += 1;
        return this;
    }
}
