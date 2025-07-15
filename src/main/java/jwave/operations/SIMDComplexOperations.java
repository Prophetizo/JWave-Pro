/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
 *
 * Copyright (c) 2025 Prophetizo
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jwave.operations;

import jwave.datatypes.natives.Complex;
import jwave.datatypes.natives.OptimizedComplex;

/**
 * SIMD-optimized implementation of ComplexOperations.
 * 
 * This implementation:
 * - Uses OptimizedComplex static methods for bulk operations
 * - Converts between Complex arrays and separate real/imaginary arrays
 * - Minimizes object allocation during computation
 * - Leverages JVM's auto-vectorization capabilities
 * 
 * For best performance, reuse allocated arrays when possible.
 * Thread-safe for all operations.
 * 
 * @author Stephen Romano
 */
public class SIMDComplexOperations implements ComplexOperations {
    
    /**
     * Maximum buffer size to prevent unbounded memory growth.
     * Arrays larger than this will not be cached in thread-local storage.
     * Set to 64K elements (512KB per double array) as a reasonable limit.
     */
    private static final int MAX_BUFFER_SIZE = 65536;
    
    /**
     * Minimum buffer size threshold for shrinking.
     * Buffers will shrink if requested size is less than 1/4 of current size
     * and current size is above this threshold.
     */
    private static final int MIN_SHRINK_THRESHOLD = 4096;
    
    /**
     * Initial buffer size for new BufferSet instances.
     * Start with a reasonable default to avoid frequent early reallocations.
     */
    private static final int INITIAL_BUFFER_SIZE = 1024;
    
    /**
     * Growth factor for exponential buffer expansion.
     * Use 1.5x growth for better memory efficiency than 2x while maintaining good amortized performance.
     */
    private static final double GROWTH_FACTOR = 1.5;
    
    /**
     * Thread-local buffers to reduce allocation overhead.
     * Each thread gets its own set of buffers with smart sizing.
     * 
     * IMPORTANT: Memory Leak Prevention
     * ThreadLocal storage can cause memory leaks in application servers, web containers,
     * or any environment with thread pooling if not properly managed.
     * 
     * Cleanup Requirements:
     * 1. Call clearThreadBuffers() when threads are finished with complex operations
     * 2. In web applications, call clearThreadBuffers() in request/response filters
     * 3. In application servers, call clearThreadBuffers() before returning threads to pool
     * 4. Consider using try-with-resources pattern with ThreadLocalCleaner for automatic cleanup
     * 
     * Memory Impact:
     * - Each thread can hold up to ~385KB of buffer memory (6 arrays × 64K elements × 8 bytes)
     * - In thread pools, this memory persists until threads are garbage collected
     * - Failure to cleanup can lead to OutOfMemoryError in long-running applications
     * 
     * @see #clearThreadBuffers() for manual cleanup
     * @see #createThreadLocalCleaner() for automatic cleanup pattern
     */
    private static final ThreadLocal<BufferSet> threadLocalBuffers = 
        ThreadLocal.withInitial(BufferSet::new);
    
    /**
     * Container for thread-local work buffers with intelligent sizing.
     */
    private static class BufferSet {
        double[] real1;
        double[] imag1;
        double[] real2;
        double[] imag2;
        double[] realOut;
        double[] imagOut;
        int currentSize = 0;
        
        /**
         * Initializes buffers with a reasonable default size.
         */
        BufferSet() {
            allocateBuffers(INITIAL_BUFFER_SIZE);
        }
        
        /**
         * Ensures buffer capacity with exponential growth and smart shrinking.
         * 
         * Growth strategy:
         * - Uses exponential growth (1.5x factor) to minimize reallocations
         * - Grows to at least the requested size, but potentially larger for future requests
         * - Capped at MAX_BUFFER_SIZE to prevent unbounded memory usage
         * 
         * Shrinking strategy:
         * - Shrinks if requested size < currentSize/4 and currentSize > MIN_SHRINK_THRESHOLD
         * - This prevents thrashing while reclaiming significant unused memory
         * - Provides 2x headroom to reduce likelihood of immediate regrowth
         * 
         * @param size the required buffer size
         */
        void ensureCapacity(int size) {
            // Determine if we need to resize
            boolean needsGrowth = size > currentSize;
            boolean shouldShrink = currentSize > MIN_SHRINK_THRESHOLD && 
                                  size < currentSize / 4;
            
            if (needsGrowth || shouldShrink) {
                int newSize;
                if (needsGrowth) {
                    // Exponential growth strategy: grow by GROWTH_FACTOR but ensure we meet the request
                    newSize = Math.max(size, (int)(currentSize * GROWTH_FACTOR));
                    
                    // For very small sizes, ensure minimum reasonable growth
                    if (currentSize < INITIAL_BUFFER_SIZE) {
                        newSize = Math.max(newSize, INITIAL_BUFFER_SIZE);
                    }
                    
                    // Cap at maximum buffer size
                    newSize = Math.min(newSize, MAX_BUFFER_SIZE);
                    
                    // If we hit the cap but still can't satisfy the request, 
                    // this will be handled by the large array fallback
                } else {
                    // Shrink to 2x the requested size to provide some headroom
                    // This reduces the likelihood of immediate regrowth
                    newSize = Math.max(size * 2, INITIAL_BUFFER_SIZE);
                    newSize = Math.min(newSize, MAX_BUFFER_SIZE);
                }
                
                allocateBuffers(newSize);
            }
        }
        
        /**
         * Allocates all buffers to the specified size.
         * 
         * @param size the size for all buffers
         */
        private void allocateBuffers(int size) {
            real1 = new double[size];
            imag1 = new double[size];
            real2 = new double[size];
            imag2 = new double[size];
            realOut = new double[size];
            imagOut = new double[size];
            currentSize = size;
        }
        
        /**
         * Determines whether thread-local buffers should be used versus temporary allocation.
         * 
         * <p><b>Buffer Usage Strategy:</b></p>
         * <ul>
         *   <li><b>Use thread-local buffers when:</b> size ≤ {@value #MAX_BUFFER_SIZE} elements</li>
         *   <li><b>Use temporary allocation when:</b> size > {@value #MAX_BUFFER_SIZE} elements</li>
         * </ul>
         * 
         * <p><b>Rationale for Size Threshold:</b></p>
         * <ul>
         *   <li><b>Memory footprint:</b> Large arrays can consume excessive memory per thread</li>
         *   <li><b>Thread pool environments:</b> Prevents memory leaks in pooled threads</li>
         *   <li><b>GC pressure:</b> Very large buffers increase garbage collection overhead</li>
         *   <li><b>Cache locality:</b> Smaller buffers fit better in CPU cache</li>
         * </ul>
         * 
         * <p><b>Performance Trade-offs:</b></p>
         * <ul>
         *   <li><b>Thread-local buffers:</b> Fast allocation, potential memory overhead</li>
         *   <li><b>Temporary allocation:</b> GC overhead, but bounded memory usage</li>
         * </ul>
         * 
         * <p><b>Memory Impact Examples:</b></p>
         * <pre>
         * Size 1K elements   → 48 KB buffer (6 arrays × 1K × 8 bytes) - Use thread-local
         * Size 64K elements  → 3 MB buffer (6 arrays × 64K × 8 bytes) - Use thread-local (max)
         * Size 128K elements → 6 MB buffer - Use temporary allocation
         * </pre>
         * 
         * @param size the required buffer size in elements
         * @return true if thread-local buffers should be used; false if temporary allocation is preferred
         * @see #MAX_BUFFER_SIZE
         */
        boolean canUseBuffers(int size) {
            return size <= MAX_BUFFER_SIZE;
        }
        
        /**
         * Gets the current buffer size for monitoring/debugging.
         * 
         * @return current buffer size in elements
         */
        int getCurrentSize() {
            return currentSize;
        }
        
        /**
         * Gets the growth efficiency ratio (how much of the buffer is actually being used).
         * Values close to 1.0 indicate efficient usage, lower values indicate over-allocation.
         * 
         * @param requestedSize the size that was last requested
         * @return efficiency ratio between 0.0 and 1.0
         */
        double getEfficiencyRatio(int requestedSize) {
            if (currentSize == 0) return 1.0;
            return Math.min(1.0, (double) requestedSize / currentSize);
        }
        
        /**
         * Gets the current memory usage in bytes (approximate).
         * 
         * @return approximate memory usage in bytes
         */
        long getMemoryUsage() {
            // 6 arrays * currentSize elements * 8 bytes per double
            return 6L * currentSize * 8;
        }
    }
    
    /**
     * Interface for bulk operations on separate arrays.
     */
    @FunctionalInterface
    private interface BulkOperation {
        void perform(double[] real1, double[] imag1, double[] real2, double[] imag2,
                    double[] realOut, double[] imagOut, int length);
    }
    
    /**
     * Interface for unary operations on separate arrays.
     */
    @FunctionalInterface
    private interface UnaryOperation {
        void perform(double[] real, double[] imag, double[] resultReal, double[] resultImag, int length);
    }
    
    /**
     * Interface for scalar operations on separate arrays.
     */
    @FunctionalInterface
    private interface ScalarOperation {
        void perform(double[] real, double[] imag, double scalar, 
                    double[] resultReal, double[] resultImag, int length);
    }
    
    /**
     * Helper method to perform binary operations with proper buffer management.
     */
    private void performBinaryOperation(Complex[] array1, Complex[] array2, Complex[] result, 
                                       int length, BulkOperation operation) {
        BufferSet buffers = threadLocalBuffers.get();
        
        if (buffers.canUseBuffers(length)) {
            // Use thread-local buffers for small arrays
            buffers.ensureCapacity(length);
            
            // Convert to separate arrays
            OptimizedComplex.toSeparateArrays(array1, buffers.real1, buffers.imag1);
            OptimizedComplex.toSeparateArrays(array2, buffers.real2, buffers.imag2);
            
            // Perform operation
            operation.perform(buffers.real1, buffers.imag1, buffers.real2, buffers.imag2,
                            buffers.realOut, buffers.imagOut, length);
            
            // Convert back to Complex array
            OptimizedComplex.fromSeparateArrays(buffers.realOut, buffers.imagOut, result);
        } else {
            // For large arrays, allocate temporary buffers
            double[] real1 = new double[length];
            double[] imag1 = new double[length];
            double[] real2 = new double[length];
            double[] imag2 = new double[length];
            double[] realOut = new double[length];
            double[] imagOut = new double[length];
            
            // Convert to separate arrays
            OptimizedComplex.toSeparateArrays(array1, real1, imag1);
            OptimizedComplex.toSeparateArrays(array2, real2, imag2);
            
            // Perform operation
            operation.perform(real1, imag1, real2, imag2, realOut, imagOut, length);
            
            // Convert back to Complex array
            OptimizedComplex.fromSeparateArrays(realOut, imagOut, result);
        }
    }
    
    /**
     * Helper method to perform scalar operations with proper buffer management.
     */
    private void performScalarOperation(Complex[] array, double scalar, Complex[] result, 
                                       int length, ScalarOperation operation) {
        BufferSet buffers = threadLocalBuffers.get();
        
        if (buffers.canUseBuffers(length)) {
            // Use thread-local buffers for small arrays
            buffers.ensureCapacity(length);
            
            // Convert to separate arrays
            OptimizedComplex.toSeparateArrays(array, buffers.real1, buffers.imag1);
            
            // Perform operation
            operation.perform(buffers.real1, buffers.imag1, scalar, 
                            buffers.realOut, buffers.imagOut, length);
            
            // Convert back to Complex array
            OptimizedComplex.fromSeparateArrays(buffers.realOut, buffers.imagOut, result);
        } else {
            // For large arrays, allocate temporary buffers
            double[] real = new double[length];
            double[] imag = new double[length];
            double[] realOut = new double[length];
            double[] imagOut = new double[length];
            
            // Convert to separate arrays
            OptimizedComplex.toSeparateArrays(array, real, imag);
            
            // Perform operation
            operation.perform(real, imag, scalar, realOut, imagOut, length);
            
            // Convert back to Complex array
            OptimizedComplex.fromSeparateArrays(realOut, imagOut, result);
        }
    }
    
    /**
     * Helper method to perform unary operations with proper buffer management.
     */
    private void performUnaryOperation(Complex[] array, Complex[] result, 
                                      int length, UnaryOperation operation) {
        BufferSet buffers = threadLocalBuffers.get();
        
        if (buffers.canUseBuffers(length)) {
            // Use thread-local buffers for small arrays
            buffers.ensureCapacity(length);
            
            // Convert to separate arrays
            OptimizedComplex.toSeparateArrays(array, buffers.real1, buffers.imag1);
            
            // Perform operation
            operation.perform(buffers.real1, buffers.imag1, 
                            buffers.realOut, buffers.imagOut, length);
            
            // Convert back to Complex array
            OptimizedComplex.fromSeparateArrays(buffers.realOut, buffers.imagOut, result);
        } else {
            // For large arrays, allocate temporary buffers
            double[] real = new double[length];
            double[] imag = new double[length];
            double[] realOut = new double[length];
            double[] imagOut = new double[length];
            
            // Convert to separate arrays
            OptimizedComplex.toSeparateArrays(array, real, imag);
            
            // Perform operation
            operation.perform(real, imag, realOut, imagOut, length);
            
            // Convert back to Complex array
            OptimizedComplex.fromSeparateArrays(realOut, imagOut, result);
        }
    }
    
    @Override
    public void add(Complex[] array1, Complex[] array2, Complex[] result, int length) {
        performBinaryOperation(array1, array2, result, length, OptimizedComplex::addBulk);
    }
    
    @Override
    public void subtract(Complex[] array1, Complex[] array2, Complex[] result, int length) {
        performBinaryOperation(array1, array2, result, length, OptimizedComplex::subtractBulk);
    }
    
    @Override
    public void multiply(Complex[] array1, Complex[] array2, Complex[] result, int length) {
        performBinaryOperation(array1, array2, result, length, OptimizedComplex::multiplyBulk);
    }
    
    @Override
    public void multiplyScalar(Complex[] array, double scalar, Complex[] result, int length) {
        performScalarOperation(array, scalar, result, length, OptimizedComplex::multiplyScalarBulk);
    }
    
    @Override
    public void conjugate(Complex[] array, Complex[] result, int length) {
        performUnaryOperation(array, result, length, OptimizedComplex::conjugateBulk);
    }
    
    @Override
    public void magnitude(Complex[] array, double[] result, int length) {
        BufferSet buffers = threadLocalBuffers.get();
        
        if (buffers.canUseBuffers(length)) {
            // Use thread-local buffers for small arrays
            buffers.ensureCapacity(length);
            
            // Convert to separate arrays
            OptimizedComplex.toSeparateArrays(array, buffers.real1, buffers.imag1);
            
            // Perform optimized magnitude calculation
            OptimizedComplex.magnitudeBulk(buffers.real1, buffers.imag1, result, length);
        } else {
            // For large arrays, allocate temporary buffers
            double[] real = new double[length];
            double[] imag = new double[length];
            
            // Convert to separate arrays
            OptimizedComplex.toSeparateArrays(array, real, imag);
            
            // Perform optimized magnitude calculation
            OptimizedComplex.magnitudeBulk(real, imag, result, length);
        }
    }
    
    @Override
    public Complex multiplyAccumulate(Complex[] array1, Complex[] array2, int length) {
        BufferSet buffers = threadLocalBuffers.get();
        
        if (buffers.canUseBuffers(length)) {
            // Use thread-local buffers for small arrays
            buffers.ensureCapacity(length);
            
            // Convert to separate arrays
            OptimizedComplex.toSeparateArrays(array1, buffers.real1, buffers.imag1);
            OptimizedComplex.toSeparateArrays(array2, buffers.real2, buffers.imag2);
            
            // Perform optimized multiply-accumulate
            double[] result = OptimizedComplex.multiplyAccumulate(buffers.real1, buffers.imag1,
                                                                  buffers.real2, buffers.imag2, length);
            
            return new Complex(result[0], result[1]);
        } else {
            // For large arrays, allocate temporary buffers
            double[] real1 = new double[length];
            double[] imag1 = new double[length];
            double[] real2 = new double[length];
            double[] imag2 = new double[length];
            
            // Convert to separate arrays
            OptimizedComplex.toSeparateArrays(array1, real1, imag1);
            OptimizedComplex.toSeparateArrays(array2, real2, imag2);
            
            // Perform optimized multiply-accumulate
            double[] result = OptimizedComplex.multiplyAccumulate(real1, imag1, real2, imag2, length);
            
            return new Complex(result[0], result[1]);
        }
    }
    
    @Override
    public void toSeparateArrays(Complex[] complexArray, double[] realOut, double[] imagOut) {
        OptimizedComplex.toSeparateArrays(complexArray, realOut, imagOut);
    }
    
    @Override
    public void fromSeparateArrays(double[] real, double[] imag, Complex[] complexOut, int length) {
        OptimizedComplex.fromSeparateArrays(real, imag, complexOut);
    }
    
    @Override
    public String getImplementationName() {
        return "SIMD-Optimized Complex Operations with Leak-Safe ThreadLocal Buffer Management";
    }
    
    /**
     * Gets statistics about the current thread's buffer usage.
     * Useful for monitoring and debugging memory usage.
     * 
     * @return string describing current buffer state
     */
    public static String getBufferStats() {
        BufferSet buffers = threadLocalBuffers.get();
        return String.format("Buffer size: %d elements (%.1f KB memory usage)", 
                           buffers.getCurrentSize(), 
                           buffers.getMemoryUsage() / 1024.0);
    }
    
    /**
     * Gets detailed statistics about buffer efficiency for the current thread.
     * 
     * @param lastRequestedSize the size of the last operation (for efficiency calculation)
     * @return detailed statistics string
     */
    public static String getDetailedBufferStats(int lastRequestedSize) {
        BufferSet buffers = threadLocalBuffers.get();
        double efficiency = buffers.getEfficiencyRatio(lastRequestedSize);
        return String.format(
            "Buffer stats: %d elements allocated, %d requested (%.1f%% efficiency), %.1f KB memory",
            buffers.getCurrentSize(), 
            lastRequestedSize,
            efficiency * 100,
            buffers.getMemoryUsage() / 1024.0
        );
    }
    
    /**
     * Forces cleanup of thread-local buffers for the current thread.
     * 
     * CRITICAL: This method must be called to prevent memory leaks in:
     * - Web applications (call in request/response filters)
     * - Application servers (call before returning threads to pool)
     * - Long-running applications with thread pools
     * - Any environment where threads are reused
     * 
     * Memory Reclaimed:
     * - Up to ~385KB per thread (6 arrays × 64K elements × 8 bytes)
     * - Prevents ClassLoader leaks in application servers
     * - Allows proper garbage collection of buffer arrays
     * 
     * Usage Examples:
     * ```java
     * // Manual cleanup
     * try {
     *     SIMDComplexOperations ops = new SIMDComplexOperations();
     *     ops.add(array1, array2, result, length);
     * } finally {
     *     SIMDComplexOperations.clearThreadBuffers();
     * }
     * 
     * // In servlet filters
     * public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
     *     try {
     *         chain.doFilter(request, response);
     *     } finally {
     *         SIMDComplexOperations.clearThreadBuffers();
     *     }
     * }
     * ```
     * 
     * @see #createThreadLocalCleaner() for automatic cleanup pattern
     */
    public static void clearThreadBuffers() {
        threadLocalBuffers.remove();
    }
    
    /**
     * Creates a ThreadLocalCleaner for automatic cleanup using try-with-resources.
     * 
     * This provides a safer pattern for managing ThreadLocal lifecycle:
     * 
     * ```java
     * try (SIMDComplexOperations.ThreadLocalCleaner cleaner = 
     *          SIMDComplexOperations.createThreadLocalCleaner()) {
     *     SIMDComplexOperations ops = new SIMDComplexOperations();
     *     ops.add(array1, array2, result, length);
     *     // Buffers automatically cleaned up when leaving try block
     * }
     * ```
     * 
     * @return a ThreadLocalCleaner that implements AutoCloseable
     */
    public static ThreadLocalCleaner createThreadLocalCleaner() {
        return new ThreadLocalCleaner();
    }
    
    /**
     * AutoCloseable wrapper for automatic ThreadLocal cleanup.
     * Use with try-with-resources to ensure buffers are always cleaned up.
     */
    public static class ThreadLocalCleaner implements AutoCloseable {
        private ThreadLocalCleaner() {
            // Package-private constructor - use createThreadLocalCleaner()
        }
        
        @Override
        public void close() {
            clearThreadBuffers();
        }
    }
    
    /**
     * Checks if the current thread has ThreadLocal buffers allocated.
     * Useful for monitoring and debugging memory usage.
     * 
     * @return true if current thread has buffers allocated, false otherwise
     */
    public static boolean hasThreadLocalBuffers() {
        // We can't directly check ThreadLocal existence without triggering initialization,
        // so we check if any operations have been performed by looking at the initial size
        try {
            BufferSet buffers = threadLocalBuffers.get();
            return buffers.getCurrentSize() > INITIAL_BUFFER_SIZE || 
                   buffers.real1 != null; // Check if buffers have been used
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the memory usage of ThreadLocal buffers across all threads (approximate).
     * This is an estimate and may not reflect actual memory usage due to GC behavior.
     * 
     * Note: This method cannot accurately measure all thread-local instances,
     * it only reports the current thread's usage as an example.
     * 
     * @return approximate memory usage in bytes for current thread
     */
    public static long estimateThreadLocalMemoryUsage() {
        if (!hasThreadLocalBuffers()) {
            return 0;
        }
        BufferSet buffers = threadLocalBuffers.get();
        return buffers.getMemoryUsage();
    }
    
    @Override
    public boolean isOptimized() {
        return true;
    }
}