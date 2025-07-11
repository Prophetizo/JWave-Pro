package jwave.utils;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import jwave.datatypes.natives.Complex;

/**
 * Thread-local buffer pool for array allocations to reduce GC pressure.
 * 
 * <p>This pool manages reusable arrays of various types and sizes, particularly
 * optimized for wavelet transform operations where many temporary arrays are
 * created and discarded.</p>
 * 
 * <p>The pool uses size buckets based on powers of 2 to minimize fragmentation
 * and improve cache locality. Arrays are cleared before being returned to the
 * pool to prevent data leakage.</p>
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class ArrayBufferPool {
    
    /**
     * Thread-local instance to avoid contention between threads.
     */
    private static final ThreadLocal<ArrayBufferPool> THREAD_LOCAL_POOL = 
        ThreadLocal.withInitial(ArrayBufferPool::new);
    
    /**
     * Maximum array size to pool (128K elements).
     * Larger arrays are allocated directly to avoid excessive memory retention.
     */
    private static final int MAX_POOLED_SIZE = 131072;
    
    /**
     * Maximum number of arrays to keep per size bucket.
     */
    private static final int MAX_ARRAYS_PER_BUCKET = 4;
    
    // Separate pools for different array types
    // Since this is thread-local, we can use non-concurrent collections for better performance
    private final Map<Integer, Queue<double[]>> doubleArrayPool = new HashMap<>();
    private final Map<Integer, Queue<Complex[]>> complexArrayPool = new HashMap<>();
    
    /**
     * Private constructor - use getInstance() to get thread-local instance.
     */
    private ArrayBufferPool() {}
    
    /**
     * Gets the thread-local pool instance.
     * 
     * @return The buffer pool for the current thread
     */
    public static ArrayBufferPool getInstance() {
        return THREAD_LOCAL_POOL.get();
    }
    
    /**
     * Borrows a double array of at least the specified size.
     * 
     * @param minSize The minimum required size
     * @return A double array of at least minSize capacity
     */
    public double[] borrowDoubleArray(int minSize) {
        if (minSize <= 0) {
            throw new IllegalArgumentException("Array size must be positive, got: " + minSize);
        }
        
        // Don't pool very large arrays
        if (minSize > MAX_POOLED_SIZE) {
            return new double[minSize];
        }
        
        int bucketSize = nextPowerOfTwo(minSize);
        Queue<double[]> bucket = doubleArrayPool.computeIfAbsent(bucketSize, 
            k -> new ArrayDeque<>());
        
        double[] array = bucket.poll();
        if (array == null) {
            array = new double[bucketSize];
        }
        
        return array;
    }
    
    /**
     * Returns a double array to the pool for reuse.
     * The array is cleared before being pooled.
     * 
     * @param array The array to return to the pool
     */
    public void returnDoubleArray(double[] array) {
        if (array == null || array.length > MAX_POOLED_SIZE) {
            return; // Don't pool null or very large arrays
        }
        
        // Clear the array for security and to help GC
        Arrays.fill(array, 0.0);
        
        Queue<double[]> bucket = doubleArrayPool.get(array.length);
        if (bucket != null && bucket.size() < MAX_ARRAYS_PER_BUCKET) {
            bucket.offer(array);
        }
    }
    
    /**
     * Borrows a Complex array of at least the specified size.
     * 
     * @param minSize The minimum required size
     * @return A Complex array of at least minSize capacity
     */
    public Complex[] borrowComplexArray(int minSize) {
        if (minSize <= 0) {
            throw new IllegalArgumentException("Array size must be positive, got: " + minSize);
        }
        
        // Don't pool very large arrays
        if (minSize > MAX_POOLED_SIZE) {
            Complex[] array = new Complex[minSize];
            initializeComplexArray(array);
            return array;
        }
        
        int bucketSize = nextPowerOfTwo(minSize);
        Queue<Complex[]> bucket = complexArrayPool.computeIfAbsent(bucketSize, 
            k -> new ArrayDeque<>());
        
        Complex[] array = bucket.poll();
        if (array == null) {
            array = new Complex[bucketSize];
            // Pre-initialize Complex objects to avoid allocation during use
            initializeComplexArray(array);
        }
        
        return array;
    }
    
    /**
     * Returns a Complex array to the pool for reuse.
     * The array elements are reset to zero before being pooled.
     * 
     * @param array The array to return to the pool
     */
    public void returnComplexArray(Complex[] array) {
        if (array == null || array.length > MAX_POOLED_SIZE) {
            return; // Don't pool null or very large arrays
        }
        
        // Reset all Complex values to zero
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null) {
                array[i].setReal(0);
                array[i].setImag(0);
            }
        }
        
        Queue<Complex[]> bucket = complexArrayPool.get(array.length);
        if (bucket != null && bucket.size() < MAX_ARRAYS_PER_BUCKET) {
            bucket.offer(array);
        }
    }
    
    /**
     * Borrows a 2D double array with the specified dimensions.
     * 
     * @param rows The number of rows (must be positive)
     * @param cols The number of columns (must be positive)
     * @return A 2D double array
     * @throws IllegalArgumentException if rows or cols is not positive
     */
    public double[][] borrow2DDoubleArray(int rows, int cols) {
        if (rows <= 0) {
            throw new IllegalArgumentException("Number of rows must be positive, got: " + rows);
        }
        if (cols <= 0) {
            throw new IllegalArgumentException("Number of columns must be positive, got: " + cols);
        }
        
        double[][] array = new double[rows][];
        for (int i = 0; i < rows; i++) {
            array[i] = borrowDoubleArray(cols);
        }
        return array;
    }
    
    /**
     * Returns a 2D double array to the pool.
     * 
     * @param array The 2D array to return
     */
    public void return2DDoubleArray(double[][] array) {
        if (array != null) {
            for (double[] row : array) {
                returnDoubleArray(row);
            }
        }
    }
    
    /**
     * Pre-initializes Complex array elements to avoid allocation during use.
     * This is more efficient than creating new Complex objects each time.
     * 
     * @param array The Complex array to initialize
     */
    private static void initializeComplexArray(Complex[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                array[i] = new Complex(0, 0);
            }
        }
    }
    
    /**
     * Clears all pooled arrays for this thread.
     * Call this when the thread is done with wavelet operations
     * to free memory.
     */
    public void clear() {
        doubleArrayPool.clear();
        complexArrayPool.clear();
    }
    
    /**
     * Removes the thread-local pool instance.
     * Call this when the thread is terminating.
     */
    public static void remove() {
        THREAD_LOCAL_POOL.get().clear();
        THREAD_LOCAL_POOL.remove();
    }
    
    /**
     * Rounds up to the next power of two.
     * 
     * @param n The number to round up
     * @return The next power of two greater than or equal to n
     */
    private static int nextPowerOfTwo(int n) {
        if (n <= 0) return 1;
        if ((n & (n - 1)) == 0) return n; // Already a power of 2
        
        // Find the next power of 2
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        n++;
        
        return n;
    }
}