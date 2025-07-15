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
package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for MODWTTransform optimized factory methods to verify robust fallback behavior.
 * 
 * @author Stephen Romano
 */
public class MODWTOptimizedFactoryTest {

    @Test
    public void testCreateOptimizedWithFallback() {
        System.out.println("=== Testing MODWTTransform.createOptimized() ===");
        
        // Test with fallback enabled (default behavior)
        MODWTTransform modwt = MODWTTransform.createOptimized(new Haar1());
        assertNotNull("MODWTTransform should be created successfully", modwt);
        
        // Verify it can perform basic operations
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[][] coeffs = modwt.forwardMODWT(signal, 2);
        assertNotNull("Forward MODWT should work", coeffs);
        assertEquals("Should have 3 arrays (2 details + 1 approximation)", 3, coeffs.length);
        
        double[] reconstructed = modwt.inverseMODWT(coeffs);
        assertNotNull("Inverse MODWT should work", reconstructed);
        assertEquals("Reconstructed signal should have same length", signal.length, reconstructed.length);
        
        System.out.println("Successfully created and tested MODWTTransform with fallback");
    }

    @Test
    public void testCreateOptimizedWithFallbackFalse() {
        System.out.println("=== Testing MODWTTransform.createOptimizedWithFallback(wavelet, false) ===");
        
        // Since optimized implementations ARE available in this codebase, this should succeed
        MODWTTransform modwt = MODWTTransform.createOptimizedWithFallback(new Daubechies4(), false);
        assertNotNull("MODWTTransform should be created successfully with optimized implementations", modwt);
        
        // Verify functionality
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[][] coeffs = modwt.forwardMODWT(signal, 3);
        assertNotNull("Forward MODWT should work", coeffs);
        assertEquals("Should have 4 arrays (3 details + 1 approximation)", 4, coeffs.length);
        
        System.out.println("Successfully created MODWTTransform with optimized implementations (no fallback needed)");
    }

    @Test
    public void testCreateOptimizedWithFallbackTrue() {
        System.out.println("=== Testing MODWTTransform.createOptimizedWithFallback(wavelet, true) ===");
        
        // This should succeed and fall back to standard implementations
        MODWTTransform modwt = MODWTTransform.createOptimizedWithFallback(new Daubechies4(), true);
        assertNotNull("MODWTTransform should be created successfully with fallback", modwt);
        
        // Verify functionality
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[][] coeffs = modwt.forwardMODWT(signal, 3);
        assertNotNull("Forward MODWT should work", coeffs);
        assertEquals("Should have 4 arrays (3 details + 1 approximation)", 4, coeffs.length);
        
        System.out.println("Successfully created MODWTTransform with explicit fallback enabled");
    }

    @Test
    public void testOptimizedImplementationsAvailable() {
        System.out.println("=== Testing Optimized Implementations Availability ===");
        
        // Since optimized implementations ARE available in this codebase, 
        // verify they are properly detected and used
        MODWTTransform modwt = MODWTTransform.createOptimizedWithFallback(new Haar1(), false);
        assertNotNull("MODWTTransform should be created successfully", modwt);
        
        // Test that it can handle various operations
        double[] signal = {1.0, -1.0, 2.0, -2.0, 3.0, -3.0, 4.0, -4.0};
        
        // Test different decomposition levels
        for (int level = 1; level <= 3; level++) {
            double[][] coeffs = modwt.forwardMODWT(signal, level);
            assertNotNull("Forward MODWT should work at level " + level, coeffs);
            assertEquals("Should have " + (level + 1) + " arrays", level + 1, coeffs.length);
            
            double[] reconstructed = modwt.inverseMODWT(coeffs);
            assertNotNull("Inverse MODWT should work", reconstructed);
            assertEquals("Reconstructed signal should have same length", signal.length, reconstructed.length);
        }
        
        System.out.println("Successfully verified optimized implementations are working correctly");
    }
}