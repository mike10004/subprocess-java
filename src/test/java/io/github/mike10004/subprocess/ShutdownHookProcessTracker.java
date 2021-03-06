package io.github.mike10004.subprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Process tracker that adds a shutdown hook to terminate processes that
 * are still alive when the JVM exits. This is backed by the {@code ProcessDestroyer}
 * from Apache Ant, which I vaguely trust, mostly because it's been around and
 * heavily used for so long.
 */
@VisibleForTesting
public class ShutdownHookProcessTracker implements ProcessTracker {

    private static final Logger log = LoggerFactory.getLogger(ShutdownHookProcessTracker.class);

    private final AntProcessDestroyer destroyer = new AntProcessDestroyer();

    @Override
    public synchronized void add(Process process) {
        destroyer.add(process);
    }

    @Override
    public synchronized boolean remove(Process process) {
        boolean removed = destroyer.remove(process);
        if (!removed) {
            log.debug("not removed (probably not still in processes set): {}", process);
        }
        return removed;
    }

    public synchronized int activeCount() {
        return destroyer.processes.size();
    }

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    @VisibleForTesting
    List<Process> destroyAll(long timeout, TimeUnit unit) {
        return destroyer.destroyAll(timeout, unit);
    }

    /*
     *  Licensed to the Apache Software Foundation (ASF) under one or more
     *  contributor license agreements.  See the NOTICE file distributed with
     *  this work for additional information regarding copyright ownership.
     *  The ASF licenses this file to You under the Apache License, Version 2.0
     *  (the "License"); you may not use this file except in compliance with
     *  the License.  You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     *  Unless required by applicable law or agreed to in writing, software
     *  distributed under the License is distributed on an "AS IS" BASIS,
     *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     *  See the License for the specific language governing permissions and
     *  limitations under the License.
     *
     */
    /**
     * Destroys all registered <code>Process</code>es when the VM exits.
     *
     * @since Ant 1.5
     */
    @SuppressWarnings({"SynchronizeOnNonFinalField", "WhileLoopReplaceableByForEach", "unchecked", "unused"})
    private static class AntProcessDestroyer implements Runnable {
        private static final int THREAD_DIE_TIMEOUT = 20000;
        private HashSet<Process> processes = new HashSet<>();
        // methods to register and unregister shutdown hooks
        private Method addShutdownHookMethod;
        private Method removeShutdownHookMethod;
        private ProcessDestroyerImpl destroyProcessThread = null;

        // whether or not this ProcessDestroyer has been registered as a
        // shutdown hook
        private boolean added = false;
        // whether or not this ProcessDestroyer is currently running as
        // shutdown hook
        private boolean running = false;

        private class ProcessDestroyerImpl extends Thread {
            private boolean shouldDestroy = true;

            public ProcessDestroyerImpl() {
                super("ProcessDestroyer Shutdown Hook");
            }

            @Override
            public void run() {
                if (shouldDestroy) {
                    AntProcessDestroyer.this.run();
                }
            }

            public void setShouldDestroy(boolean shouldDestroy) {
                this.shouldDestroy = shouldDestroy;
            }
        }

        /**
         * Constructs a <code>ProcessDestroyer</code> and obtains
         * <code>Runtime.addShutdownHook()</code> and
         * <code>Runtime.removeShutdownHook()</code> through reflection. The
         * ProcessDestroyer manages a list of processes to be destroyed when the
         * VM exits. If a process is added when the list is empty,
         * this <code>ProcessDestroyer</code> is registered as a shutdown hook. If
         * removing a process results in an empty list, the
         * <code>ProcessDestroyer</code> is removed as a shutdown hook.
         */
        AntProcessDestroyer() {
            try {
                // check to see if the shutdown hook methods exists
                // (support pre-JDK 1.3 and Non-Sun VMs)
                Class[] paramTypes = {Thread.class};
                addShutdownHookMethod =
                        Runtime.class.getMethod("addShutdownHook", paramTypes);

                removeShutdownHookMethod =
                        Runtime.class.getMethod("removeShutdownHook", paramTypes);
                // wait to add shutdown hook as needed
            } catch (NoSuchMethodException e) {
                // it just won't be added as a shutdown hook... :(
            } catch (Exception e) {
                e.printStackTrace(); //NOSONAR
            }
        }

        /**
         * Registers this <code>ProcessDestroyer</code> as a shutdown hook,
         * uses reflection to ensure pre-JDK 1.3 compatibility.
         */
        private void addShutdownHook() {
            if (addShutdownHookMethod != null && !running) {
                destroyProcessThread = new AntProcessDestroyer.ProcessDestroyerImpl();
                Object[] args = {destroyProcessThread};
                try {
                    addShutdownHookMethod.invoke(Runtime.getRuntime(), args);
                    added = true;
                } catch (IllegalAccessException e) {
                    e.printStackTrace(); //NOSONAR
                } catch (InvocationTargetException e) {
                    Throwable t = e.getTargetException();
                    if (t != null && t.getClass() == IllegalStateException.class) {
                        // shutdown already is in progress
                        running = true;
                    } else {
                        e.printStackTrace(); //NOSONAR
                    }
                }
            }
        }

        /**
         * Removes this <code>ProcessDestroyer</code> as a shutdown hook,
         * uses reflection to ensure pre-JDK 1.3 compatibility
         */
        private void removeShutdownHook() {
            if (removeShutdownHookMethod != null && added && !running) {
                Object[] args = {destroyProcessThread};
                try {
                    Boolean removed =
                            (Boolean) removeShutdownHookMethod.invoke(
                                    Runtime.getRuntime(),
                                    args);
                    if (!removed.booleanValue()) {
                        System.err.println("Could not remove shutdown hook");
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace(); //NOSONAR
                } catch (InvocationTargetException e) {
                    Throwable t = e.getTargetException();
                    if (t != null && t.getClass() == IllegalStateException.class) {
                        // shutdown already is in progress
                        running = true;
                    } else {
                        e.printStackTrace(); //NOSONAR
                    }
                }
                // start the hook thread, a unstarted thread may not be
                // eligible for garbage collection
                // Cf.: http://developer.java.sun.com/developer/bugParade/bugs/4533087.html
                destroyProcessThread.setShouldDestroy(false);
                if (!destroyProcessThread.getThreadGroup().isDestroyed()) {
                    // start() would throw IllegalThreadStateException from
                    // ThreadGroup.add if it were destroyed
                    destroyProcessThread.start();
                }
                // this should return quickly, since it basically is a NO-OP.
                try {
                    destroyProcessThread.join(THREAD_DIE_TIMEOUT);
                } catch (InterruptedException ie) {
                    // the thread didn't die in time
                    // it should not kill any processes unexpectedly
                }
                destroyProcessThread = null;
                added = false;
            }
        }

        /**
         * Returns whether or not the ProcessDestroyer is registered as
         * as shutdown hook
         * @return true if this is currently added as shutdown hook
         */
        public boolean isAddedAsShutdownHook() {
            return added;
        }

        /**
         * Returns <code>true</code> if the specified <code>Process</code> was
         * successfully added to the list of processes to destroy upon VM exit.
         *
         * @param   process the process to add
         * @return  <code>true</code> if the specified <code>Process</code> was
         *          successfully added
         */
        public boolean add(Process process) {
            synchronized (processes) {
                // if this list is empty, register the shutdown hook
                if (processes.size() == 0) {
                    addShutdownHook();
                }
                return processes.add(process);
            }
        }

        /**
         * Returns <code>true</code> if the specified <code>Process</code> was
         * successfully removed from the list of processes to destroy upon VM exit.
         *
         * @param   process the process to remove
         * @return  <code>true</code> if the specified <code>Process</code> was
         *          successfully removed
         */
        public boolean remove(Process process) {
            synchronized (processes) {
                boolean processRemoved = processes.remove(process);
                if (processRemoved && processes.size() == 0) {
                    removeShutdownHook();
                }
                return processRemoved;
            }
        }

        /**
         * Invoked by the VM when it is exiting.
         */
        public void run() {
            synchronized (processes) {
                running = true;
                Iterator e = processes.iterator();
                while (e.hasNext()) {
                    ((Process) e.next()).destroy();
                }
            }
        }

        /**
         * Invokes {@link ProcessTracker#destroyAll(Iterable, long, TimeUnit)} and
         * stops tracking the destroyed processes.
         * @return undestroyed processes
         */
        public List<Process> destroyAll(long timeoutPerProcess, TimeUnit unit) {
            synchronized (processes) {
                List<Process> undestroyed = ProcessTracker.destroyAll(processes, timeoutPerProcess, unit);
                processes.retainAll(undestroyed); // I think this is the first time I've used this method
                return undestroyed;
            }
        }
    }
}
