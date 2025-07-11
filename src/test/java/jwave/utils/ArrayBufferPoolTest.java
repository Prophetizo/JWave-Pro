package jwave.utils;

import jwave.datatypes.natives.Complex;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

/**
 * Unit tests for ArrayBufferPool.
 * 
 * @author Stephen Romano
 */
public class ArrayBufferPoolTest {
    
    @After
    public void tearDown() {
        // Clean up thread-local pool after each test
        ArrayBufferPool.remove();
    }
    
    @Test
    public void testDoubleArrayBorrowAndReturn() {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        // Borrow an array
        double[] array1 = pool.borrowDoubleArray(100);
        assertNotNull(array1);
        assertTrue(array1.length >= 100);
        
        // Fill with test data
        for (int i = 0; i < 100; i++) {
            array1[i] = i;
        }
        
        // Return the array
        pool.returnDoubleArray(array1);
        
        // Borrow again - should get the same array cleared
        double[] array2 = pool.borrowDoubleArray(100);
        assertSame(array1, array2);
        
        // Verify it was cleared
        for (int i = 0; i < 100; i++) {
            assertEquals(0.0, array2[i], 0.0);
        }
    }
    
    @Test
    public void testComplexArrayInitialization() {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        // Test normal sized array
        Complex[] array1 = pool.borrowComplexArray(64);
        assertNotNull(array1);
        for (int i = 0; i < array1.length; i++) {
            assertNotNull("Complex element should be initialized", array1[i]);
            assertEquals(0.0, array1[i].getReal(), 0.0);
            assertEquals(0.0, array1[i].getImag(), 0.0);
        }
        pool.returnComplexArray(array1);
        
        // Test oversized array (larger than MAX_POOLED_SIZE)
        Complex[] array2 = pool.borrowComplexArray(200000);
        assertNotNull(array2);
        assertEquals(200000, array2.length);
        for (int i = 0; i < Math.min(100, array2.length); i++) {
            assertNotNull("Complex element should be initialized for oversized array", array2[i]);
            assertEquals(0.0, array2[i].getReal(), 0.0);
            assertEquals(0.0, array2[i].getImag(), 0.0);
        }
        // This array won't be pooled due to size
        pool.returnComplexArray(array2);
    }
    
    @Test
    public void testPowerOfTwoRounding() {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        // Request size 100, should get rounded up to 128
        double[] array = pool.borrowDoubleArray(100);
        assertEquals(128, array.length);
        pool.returnDoubleArray(array);
        
        // Request size 64, should get exactly 64
        array = pool.borrowDoubleArray(64);
        assertEquals(64, array.length);
        pool.returnDoubleArray(array);
    }
    
    @Test
    public void test2DArrayPooling() {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        double[][] array2D = pool.borrow2DDoubleArray(10, 20);
        assertNotNull(array2D);
        assertEquals(10, array2D.length);
        for (int i = 0; i < 10; i++) {
            assertNotNull(array2D[i]);
            assertTrue(array2D[i].length >= 20);
        }
        
        pool.return2DDoubleArray(array2D);
    }
    
    @Test
    public void testMaxPooledArraysPerBucket() {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        // Borrow and return many arrays of the same size
        double[][] arrays = new double[10][];
        for (int i = 0; i < 10; i++) {
            arrays[i] = pool.borrowDoubleArray(64);
        }
        
        // Return all arrays
        for (int i = 0; i < 10; i++) {
            pool.returnDoubleArray(arrays[i]);
        }
        
        // Only MAX_ARRAYS_PER_BUCKET should be pooled
        // The rest should be garbage collected
        // This test mainly ensures no exceptions are thrown
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeSizeDouble() {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        pool.borrowDoubleArray(-1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeSizeComplex() {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        pool.borrowComplexArray(-1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void test2DArrayNegativeRows() {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        pool.borrow2DDoubleArray(-1, 10);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void test2DArrayNegativeCols() {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        pool.borrow2DDoubleArray(10, -1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void test2DArrayZeroRows() {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        pool.borrow2DDoubleArray(0, 10);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void test2DArrayZeroCols() {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        pool.borrow2DDoubleArray(10, 0);
    }
    
    @Test
    public void testThreadLocalIsolation() throws InterruptedException {
        final boolean[] thread1Success = {false};
        final boolean[] thread2Success = {false};
        
        Thread thread1 = new Thread(() -> {
            ArrayBufferPool pool = ArrayBufferPool.getInstance();
            double[] array = pool.borrowDoubleArray(100);
            array[0] = 42.0;
            pool.returnDoubleArray(array);
            
            // Borrow again and check it's cleared
            double[] array2 = pool.borrowDoubleArray(100);
            thread1Success[0] = (array == array2 && array2[0] == 0.0);
            ArrayBufferPool.remove();
        });
        
        Thread thread2 = new Thread(() -> {
            ArrayBufferPool pool = ArrayBufferPool.getInstance();
            double[] array = pool.borrowDoubleArray(100);
            array[0] = 99.0;
            pool.returnDoubleArray(array);
            
            // Borrow again and check it's cleared
            double[] array2 = pool.borrowDoubleArray(100);
            thread2Success[0] = (array == array2 && array2[0] == 0.0);
            ArrayBufferPool.remove();
        });
        
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        
        assertTrue("Thread 1 should have its own pool", thread1Success[0]);
        assertTrue("Thread 2 should have its own pool", thread2Success[0]);
    }
}