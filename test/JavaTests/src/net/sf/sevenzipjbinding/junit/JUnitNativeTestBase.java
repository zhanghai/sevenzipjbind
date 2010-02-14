package net.sf.sevenzipjbinding.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.SevenZipNativeInitializationException;
import net.sf.sevenzipjbinding.junit.tools.SevenZipDebug;

import org.junit.After;
import org.junit.BeforeClass;

/**
 * This is the base class for all JUnit test classes, that needs native library to be loaded. This class provides:<br>
 * - Initialization of the native library
 * 
 * @author Boris Brodski
 * @version 4.65-1
 * 
 */
public class JUnitNativeTestBase {
    protected interface RunnableThrowsException {
        public void run() throws Exception;
    }

    private static final int SINGLE_TEST_THREAD_COUNT = 2;//15;
    protected static final int SINGLE_TEST_REPEAT_COUNT = 2;//60;
    private static final int SINGLE_TEST_TIMEOUT = 100000;

    private static int deadCPPObjectCount = 0;
    private static boolean initializeNativeLibrary = true;
    private static boolean nonDebugLibraryWasReported = false;
    private static boolean nonUseMyAssertLibraryWasReported = false;

    /**
     * Initialize native SevenZipJBinding library for all JUnit tests
     * 
     * @throws SevenZipNativeInitializationException
     *             in case initialization of SevenZipJBinding fails
     */
    @BeforeClass
    public static void initializeSevenZipJBinding() throws SevenZipNativeInitializationException {
        if (initializeNativeLibrary) {
            SevenZip.initSevenZipFromPlatformJAR();
            initializeNativeLibrary = false;
        }
    }

    @After
    public void afterTest() {
        try {
            int objectCount = SevenZipDebug.getCPPObjectCount();
            int newDeadObjectCount = objectCount - deadCPPObjectCount;
            if (newDeadObjectCount != 0) {
                SevenZipDebug.printCPPObjects();
                deadCPPObjectCount = objectCount;
            }
            assertEquals("Not all CPP Objects was freed", 0, newDeadObjectCount);
        } catch (UnsatisfiedLinkError e) {
            if (!nonDebugLibraryWasReported) {
                System.out.println("WARNING! SevenZip native libraray was build"
                        + " without object tracing debug function.");
                nonDebugLibraryWasReported = true;
            }
        }
        try {
            int threadCount = SevenZipDebug.getAttachedThreadCount();
            assertEquals("Not all attached thread was detached from VM", 0, threadCount);
        } catch (UnsatisfiedLinkError e) {
            if (!nonUseMyAssertLibraryWasReported) {
                System.out.println("WARNING! SevenZip native libraray was build without support for MY_ASSERTs.");
                nonUseMyAssertLibraryWasReported = true;
            }
        }
    }

    protected void runMultithreaded(final RunnableThrowsException runnable,
            final Class<? extends Exception> exceptionToBeExpected) throws Exception {
        runMultithreaded(runnable, exceptionToBeExpected, SINGLE_TEST_THREAD_COUNT, SINGLE_TEST_TIMEOUT
                * SINGLE_TEST_REPEAT_COUNT);
    }

    protected void runMultithreaded(final RunnableThrowsException runnable,
            final Class<? extends Exception> exceptionToBeExpected, int threadCount, int threadTimeout)
            throws Exception {
        final int[] threadsFinished = new int[] { threadCount };
        final Throwable[] firstThrowable = new Throwable[] { null };
        final Throwable[] firstExpectedThrowable = new Throwable[] { null };
        for (int i = 0; i < threadCount; i++) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        runnable.run();
                        if (exceptionToBeExpected != null) {
                            throw new Exception("Expected exception wasn't thrown: "
                                    + exceptionToBeExpected.getCanonicalName());
                        }
                    } catch (Throwable e) {
                        synchronized (firstThrowable) {
                            boolean wasExceptionExpected = exceptionToBeExpected != null
                                    && exceptionToBeExpected.isAssignableFrom(e.getClass());
                            if (firstThrowable[0] == null //
                                    && !wasExceptionExpected) {
                                firstThrowable[0] = e;
                            }
                            if (wasExceptionExpected) {
                                firstExpectedThrowable[0] = e;
                            }
                        }
                    } finally {
                        synchronized (JUnitNativeTestBase.this) {
                            try {
                                threadsFinished[0]--;
                                JUnitNativeTestBase.this.notify();
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                            }
                        }
                    }
                }
            }).start();
        }
        long start = System.currentTimeMillis();
        synchronized (this) {
            while (true) {
                try {
                    if (threadsFinished[0] == 0) {
                        break;
                    }
                    wait(threadCount * threadTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (System.currentTimeMillis() - start > threadTimeout) {
                    fail("Time out");
                }
            }
        }
        if (firstThrowable[0] != null) {
            if (firstThrowable[0] instanceof SevenZipException) {
                throw (SevenZipException) firstThrowable[0];
            }
            throw new RuntimeException("Exception in underlying thread", firstThrowable[0]);
        }
        if (firstExpectedThrowable[0] != null) {
            if (firstExpectedThrowable[0] instanceof Exception) {
                throw (Exception) firstExpectedThrowable[0];
            }
            throw (Error) firstExpectedThrowable[0];
        }
    }
}
