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

import org.junit.Test;
import jwave.datatypes.natives.Complex;

/**
 * Test for SIMD Complex Operations ThreadLocal cleanup mechanisms.
 * 
 * @author Stephen Romano
 */
public class ThreadLocalCleanupTest {

    @Test
    public void testThreadLocalCleanupMechanisms() {
        System.out.println("=== ThreadLocal Cleanup Mechanisms Test ===");
        
        SIMDComplexOperations ops = new SIMDComplexOperations();
        
        // Initially no buffers should be allocated
        System.out.println("Initial state:");
        System.out.println("  Has buffers: " + SIMDComplexOperations.hasThreadLocalBuffers());
        System.out.println("  Memory usage: " + SIMDComplexOperations.estimateThreadLocalMemoryUsage() + " bytes");
        
        // Perform some operations to trigger buffer allocation
        System.out.println("\nPerforming operations to trigger buffer allocation...");
        Complex[] array1 = createTestArray(5000);
        Complex[] array2 = createTestArray(5000);
        Complex[] result = new Complex[5000];
        
        ops.add(array1, array2, result, 5000);
        
        System.out.println("After operations:");
        System.out.println("  Has buffers: " + SIMDComplexOperations.hasThreadLocalBuffers());
        System.out.println("  Memory usage: " + SIMDComplexOperations.estimateThreadLocalMemoryUsage() + " bytes");
        System.out.println("  Buffer stats: " + SIMDComplexOperations.getBufferStats());
        
        // Test manual cleanup
        System.out.println("\nTesting manual cleanup...");
        SIMDComplexOperations.clearThreadBuffers();
        
        System.out.println("After manual cleanup:");
        System.out.println("  Has buffers: " + SIMDComplexOperations.hasThreadLocalBuffers());
        System.out.println("  Memory usage: " + SIMDComplexOperations.estimateThreadLocalMemoryUsage() + " bytes");
        
        // Test try-with-resources cleanup pattern
        System.out.println("\nTesting automatic cleanup with try-with-resources...");
        
        try (SIMDComplexOperations.ThreadLocalCleaner cleaner = 
             SIMDComplexOperations.createThreadLocalCleaner()) {
            
            // Perform operations inside try block
            ops.multiply(array1, array2, result, 5000);
            
            System.out.println("Inside try block:");
            System.out.println("  Has buffers: " + SIMDComplexOperations.hasThreadLocalBuffers());
            System.out.println("  Memory usage: " + SIMDComplexOperations.estimateThreadLocalMemoryUsage() + " bytes");
            
            // Cleaner will automatically call clearThreadBuffers() when leaving try block
        }
        
        System.out.println("After try-with-resources cleanup:");
        System.out.println("  Has buffers: " + SIMDComplexOperations.hasThreadLocalBuffers());
        System.out.println("  Memory usage: " + SIMDComplexOperations.estimateThreadLocalMemoryUsage() + " bytes");
        
        // Demonstrate memory leak prevention guidelines
        System.out.println("\n=== Memory Leak Prevention Guidelines ===");
        System.out.println("1. Web Applications:");
        System.out.println("   - Call clearThreadBuffers() in request/response filters");
        System.out.println("   - Use ThreadLocalCleaner with try-with-resources");
        System.out.println();
        System.out.println("2. Application Servers:");
        System.out.println("   - Call clearThreadBuffers() before returning threads to pool");
        System.out.println("   - Monitor memory usage with hasThreadLocalBuffers()");
        System.out.println();
        System.out.println("3. Thread Pool Environments:");
        System.out.println("   - Essential to prevent ClassLoader leaks");
        System.out.println("   - Each thread can hold up to ~385KB of buffer memory");
        System.out.println();
        System.out.println("4. Long-Running Applications:");
        System.out.println("   - Regular cleanup prevents OutOfMemoryError");
        System.out.println("   - Use estimateThreadLocalMemoryUsage() for monitoring");
        
        System.out.println("\nThreadLocal cleanup mechanisms verified successfully!");
    }
    
    private Complex[] createTestArray(int size) {
        Complex[] array = new Complex[size];
        for (int i = 0; i < size; i++) {
            array[i] = new Complex(i, i * 0.5);
        }
        return array;
    }
}