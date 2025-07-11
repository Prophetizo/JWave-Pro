package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Test thread safety of MODWTTransform with concurrent access to filter cache.
 */
public class MODWTThreadSafetyTest {
    
    private static final int THREAD_COUNT = 10;
    private static final int ITERATIONS_PER_THREAD = 100;
    
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        System.out.println("=== MODWT Thread Safety Test ===\n");
        
        // Shared MODWT instance
        MODWTTransform modwt = new MODWTTransform(new Daubechies4());
        
        // Test signal
        double[] signal = TestSignalGenerator.generateSimpleSignal(256);
        int levels = 4;
        
        // Reference result (single-threaded)
        double[][] referenceResult = modwt.forwardMODWT(signal, levels);
        
        // Concurrent test
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                        // Randomly clear cache to stress test initialization
                        if (i % 10 == 0) {
                            modwt.clearFilterCache();
                        }
                        
                        // Perform transform
                        double[][] result = modwt.forwardMODWT(signal, levels);
                        
                        // Verify result matches reference
                        boolean matches = true;
                        for (int level = 0; level < result.length && matches; level++) {
                            for (int j = 0; j < result[level].length && matches; j++) {
                                if (Math.abs(result[level][j] - referenceResult[level][j]) > 1e-10) {
                                    matches = false;
                                }
                            }
                        }
                        
                        if (matches) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                            System.err.println("Thread " + threadId + " iteration " + i + 
                                             " produced different results!");
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        assertTrue("Threads should complete within 30 seconds",
                  completeLatch.await(30, TimeUnit.SECONDS));
        
        executor.shutdown();
        
        // Verify results
        int totalOperations = THREAD_COUNT * ITERATIONS_PER_THREAD;
        System.out.println("Total operations: " + totalOperations);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Errors: " + errorCount.get());
        
        assertEquals("All operations should succeed", totalOperations, successCount.get());
        assertEquals("No errors should occur", 0, errorCount.get());
    }
    
    @Test
    public void testConcurrentDifferentWavelets() throws InterruptedException {
        System.out.println("\n=== MODWT Concurrent Different Wavelets Test ===\n");
        
        // Multiple MODWT instances with different wavelets
        MODWTTransform modwtHaar = new MODWTTransform(new Haar1());
        MODWTTransform modwtDb4 = new MODWTTransform(new Daubechies4());
        
        double[] signal = TestSignalGenerator.generateSimpleSignal(128);
        
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Two threads per wavelet
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        modwtHaar.forwardMODWT(signal, 3);
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
            
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        modwtDb4.forwardMODWT(signal, 3);
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue("Operations should complete", latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        assertEquals("All operations should complete", 200, successCount.get());
        System.out.println("Successfully completed " + successCount.get() + " concurrent operations");
    }
    
}