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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for SIMD buffer growth using integer arithmetic instead of floating-point.
 * 
 * @author Stephen Romano
 */
public class SIMDBufferGrowthTest {

    @Test
    public void testIntegerArithmeticGrowth() {
        System.out.println("=== SIMD Buffer Growth Test (Integer Arithmetic) ===");
        
        SIMDComplexOperations ops = new SIMDComplexOperations();
        
        // Test various buffer sizes to ensure integer arithmetic works correctly
        int[] testSizes = {10, 100, 1000, 5000, 10000, 32000};
        
        for (int size : testSizes) {
            // Clear any existing buffers
            SIMDComplexOperations.clearThreadBuffers();
            
            // Create test arrays
            Complex[] array1 = createTestArray(size);
            Complex[] array2 = createTestArray(size);
            Complex[] result = new Complex[size];
            
            // Perform operation to trigger buffer allocation
            ops.add(array1, array2, result, size);
            
            // Get buffer stats to verify the buffer was allocated correctly
            String stats = SIMDComplexOperations.getBufferStats();
            System.out.printf("Size %d: %s%n", size, stats);
            
            // Verify that buffers were allocated
            assertTrue("Should have buffers after operation", 
                SIMDComplexOperations.hasThreadLocalBuffers());
            
            // Verify that memory usage is reasonable
            long memoryUsage = SIMDComplexOperations.estimateThreadLocalMemoryUsage();
            assertTrue("Memory usage should be > 0", memoryUsage > 0);
            
            // Verify that the operation produces correct results
            for (int i = 0; i < size; i++) {
                double expectedReal = array1[i].getReal() + array2[i].getReal();
                double expectedImag = array1[i].getImag() + array2[i].getImag();
                
                assertEquals("Real part should be correct", expectedReal, result[i].getReal(), 1e-10);
                assertEquals("Imaginary part should be correct", expectedImag, result[i].getImag(), 1e-10);
            }
        }
        
        System.out.println("Integer arithmetic buffer growth test completed successfully!");
    }

    @Test
    public void testGrowthFactorCalculation() {
        System.out.println("=== Growth Factor Calculation Test ===");
        
        // Test the mathematical equivalence of our integer arithmetic
        // currentSize + (currentSize / 2) should equal currentSize * 1.5 for integer division
        
        int[] testSizes = {2, 4, 8, 16, 32, 64, 100, 1000, 10000, 32768};
        
        for (int currentSize : testSizes) {
            // Our integer arithmetic: currentSize + (currentSize / 2)
            int integerGrowth = currentSize + (currentSize / 2);
            
            // Expected floating-point result (for comparison)
            double floatingPointGrowth = currentSize * 1.5;
            
            // The integer arithmetic should be very close to floating point for reasonable sizes
            double difference = Math.abs(integerGrowth - floatingPointGrowth);
            
            System.out.printf("Size: %5d, Integer: %5d, Float: %7.1f, Diff: %.1f%n", 
                currentSize, integerGrowth, floatingPointGrowth, difference);
            
            // For most practical sizes, the difference should be at most 0.5 due to integer division
            assertTrue("Difference should be minimal for size " + currentSize, difference <= 0.5);
            
            // For even numbers, the result should be exact
            if (currentSize % 2 == 0) {
                assertEquals("Should be exact for even sizes", floatingPointGrowth, integerGrowth, 0.0);
            }
        }
        
        System.out.println("Growth factor calculation test completed successfully!");
    }

    @Test
    public void testBufferResizingSequence() {
        System.out.println("=== Buffer Resizing Sequence Test ===");
        
        SIMDComplexOperations ops = new SIMDComplexOperations();
        
        // Clear any existing buffers
        SIMDComplexOperations.clearThreadBuffers();
        
        // Test a sequence of increasing buffer sizes to verify growth behavior
        int[] sizesToTest = {100, 200, 500, 1000, 2000, 5000};
        
        long previousMemoryUsage = 0;
        
        for (int size : sizesToTest) {
            Complex[] array1 = createTestArray(size);
            Complex[] array2 = createTestArray(size);
            Complex[] result = new Complex[size];
            
            // Perform operation
            ops.multiply(array1, array2, result, size);
            
            // Check memory usage - should generally increase or stay the same
            long currentMemoryUsage = SIMDComplexOperations.estimateThreadLocalMemoryUsage();
            
            System.out.printf("Size %d: Memory usage %d bytes%n", size, currentMemoryUsage);
            
            // Memory usage should not decrease (buffers should only grow or stay same size)
            assertTrue("Memory usage should not decrease from " + previousMemoryUsage + " to " + currentMemoryUsage,
                currentMemoryUsage >= previousMemoryUsage);
            
            previousMemoryUsage = currentMemoryUsage;
        }
        
        System.out.println("Buffer resizing sequence test completed successfully!");
    }

    private Complex[] createTestArray(int size) {
        Complex[] array = new Complex[size];
        for (int i = 0; i < size; i++) {
            array[i] = new Complex(Math.sin(i * 0.1), Math.cos(i * 0.1));
        }
        return array;
    }
}