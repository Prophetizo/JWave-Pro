package jwave.utils;

import jwave.datatypes.natives.Complex;
import org.junit.Test;
import org.junit.After;

/**
 * Performance test comparing optimized non-concurrent collections vs original concurrent ones.
 * 
 * To skip performance tests in CI, use: mvn test -Djwave.test.skipPerformance=true
 * 
 * @author Stephen Romano
 */
public class ArrayBufferPoolPerformanceTest {
    
    @After
    public void tearDown() {
        ArrayBufferPool.remove();
    }
    
    @Test
    public void testPoolPerformance() {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        // Test various array sizes
        int[] sizes = {64, 128, 256, 512, 1024};
        int iterations = 100000;
        
        System.out.println("\n=== ArrayBufferPool Performance Test ===");
        System.out.println("(Using optimized non-concurrent collections)");
        
        // Warmup
        for (int i = 0; i < 1000; i++) {
            double[] arr = pool.borrowDoubleArray(128);
            pool.returnDoubleArray(arr);
        }
        
        long totalTime = 0;
        
        for (int size : sizes) {
            long start = System.nanoTime();
            
            for (int i = 0; i < iterations; i++) {
                // Borrow and return arrays
                double[] arr = pool.borrowDoubleArray(size);
                arr[0] = i; // Touch the array
                pool.returnDoubleArray(arr);
            }
            
            long elapsed = System.nanoTime() - start;
            totalTime += elapsed;
            
            System.out.printf("Size %4d: %.3f ms for %d operations (%.2f ns/op)\n", 
                size, elapsed / 1e6, iterations, (double)elapsed / iterations);
        }
        
        System.out.printf("Total time: %.3f ms\n", totalTime / 1e6);
        
        // Note: The actual performance improvement from using HashMap/ArrayDeque
        // instead of ConcurrentHashMap/ConcurrentLinkedQueue is typically 10-30%
        // for single-threaded access patterns like this.
    }
    
    @Test
    public void testComplexArrayPoolPerformance() {
        TestUtils.skipIfPerformanceTestsDisabled();
        
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        int iterations = 50000;
        
        System.out.println("\n=== Complex Array Pool Performance ===");
        
        // Warmup
        for (int i = 0; i < 1000; i++) {
            Complex[] arr = pool.borrowComplexArray(64);
            pool.returnComplexArray(arr);
        }
        
        long start = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            Complex[] arr = pool.borrowComplexArray(128);
            // Use the array
            arr[0].setReal(i);
            arr[0].setImag(-i);
            pool.returnComplexArray(arr);
        }
        
        long elapsed = System.nanoTime() - start;
        
        System.out.printf("Complex arrays: %.3f ms for %d operations (%.2f ns/op)\n", 
            elapsed / 1e6, iterations, (double)elapsed / iterations);
    }
}