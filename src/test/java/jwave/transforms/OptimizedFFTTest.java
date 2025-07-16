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

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import jwave.datatypes.natives.Complex;
import jwave.exceptions.JWaveException;
import jwave.PerformanceTestConfig;
import java.util.Random;

/**
 * Test suite for Optimized Fast Fourier Transform implementation.
 * 
 * Verifies correctness and compatibility with the standard FFT implementation.
 *
 * @author Stephen Romano
 */
public class OptimizedFFTTest {

    private static final double DELTA = 1e-10;
    private OptimizedFastFourierTransform optimizedFft;
    private FastFourierTransform standardFft;
    private Random random;
    
    @Before
    public void setUp() {
        optimizedFft = new OptimizedFastFourierTransform();
        standardFft = new FastFourierTransform();
        random = new Random(PerformanceTestConfig.RANDOM_SEED);
    }
    
    /**
     * Test that optimized FFT produces same results as standard FFT.
     */
    @Test
    public void testOptimizedVsStandardFFT() throws JWaveException {
        // Test various power-of-2 sizes
        int[] sizes = {64, 128, 256, 512, 1024, 2048, 4096};
        
        for (int n : sizes) {
            // Generate test signal
            double[] signal = new double[n];
            for (int i = 0; i < n; i++) {
                signal[i] = Math.sin(2 * Math.PI * i * 5 / n) +
                           0.5 * Math.cos(2 * Math.PI * i * 13 / n);
            }
            
            // Forward transform
            double[] optimizedResult = optimizedFft.forward(signal);
            double[] standardResult = standardFft.forward(signal);
            
            // Compare results
            assertArrayEquals("Forward FFT mismatch for size " + n,
                            standardResult, optimizedResult, DELTA);
            
            // Inverse transform
            double[] optimizedInverse = optimizedFft.reverse(optimizedResult);
            double[] standardInverse = standardFft.reverse(standardResult);
            
            // Compare inverse results
            assertArrayEquals("Inverse FFT mismatch for size " + n,
                            standardInverse, optimizedInverse, DELTA);
            
            // Verify reconstruction
            assertArrayEquals("Reconstruction mismatch for size " + n,
                            signal, optimizedInverse, DELTA);
        }
    }
    
    /**
     * Test optimized FFT with complex input.
     */
    @Test
    public void testOptimizedComplexFFT() {
        // Test sizes
        int[] sizes = {64, 128, 256, 512, 1024};
        
        for (int n : sizes) {
            // Generate complex test signal
            Complex[] signal = new Complex[n];
            for (int i = 0; i < n; i++) {
                signal[i] = new Complex(
                    Math.sin(2 * Math.PI * i * 3 / n),
                    Math.cos(2 * Math.PI * i * 7 / n)
                );
            }
            
            // Forward transform
            Complex[] optimizedResult = optimizedFft.forward(signal);
            Complex[] standardResult = standardFft.forward(signal);
            
            // Compare results
            for (int i = 0; i < n; i++) {
                assertEquals("Real part mismatch at " + i + " for size " + n,
                           standardResult[i].getReal(), optimizedResult[i].getReal(), DELTA);
                assertEquals("Imaginary part mismatch at " + i + " for size " + n,
                           standardResult[i].getImag(), optimizedResult[i].getImag(), DELTA);
            }
            
            // Test inverse
            Complex[] optimizedInverse = optimizedFft.reverse(optimizedResult);
            
            // Verify reconstruction
            for (int i = 0; i < n; i++) {
                assertEquals("Reconstruction real mismatch at " + i + " for size " + n,
                           signal[i].getReal(), optimizedInverse[i].getReal(), DELTA);
                assertEquals("Reconstruction imag mismatch at " + i + " for size " + n,
                           signal[i].getImag(), optimizedInverse[i].getImag(), DELTA);
            }
        }
    }
    
    /**
     * Test that optimization falls back correctly for small sizes.
     */
    @Test
    public void testSmallSizeFallback() throws JWaveException {
        // Small power-of-2 sizes
        int[] sizes = {2, 4, 8, 16, 32};
        
        for (int n : sizes) {
            // Generate test signal
            double[] signal = new double[n];
            for (int i = 0; i < n; i++) {
                signal[i] = random.nextDouble() - 0.5;
            }
            
            // Should fall back to standard implementation
            double[] optimizedResult = optimizedFft.forward(signal);
            double[] standardResult = standardFft.forward(signal);
            
            // Results should be identical
            assertArrayEquals("Fallback mismatch for size " + n,
                            standardResult, optimizedResult, DELTA);
        }
    }
    
    /**
     * Test edge cases.
     */
    @Test
    public void testOptimizedEdgeCases() {
        // Empty array
        Complex[] empty = new Complex[0];
        Complex[] result = optimizedFft.forward(empty);
        assertEquals(0, result.length);
        
        // Single element
        Complex[] single = { new Complex(1.0, 0.0) };
        result = optimizedFft.forward(single);
        assertEquals(1, result.length);
        assertEquals(1.0, result[0].getReal(), DELTA);
        assertEquals(0.0, result[0].getImag(), DELTA);
    }
    
    /**
     * Test correctness of optimized real FFT.
     */
    @Test 
    public void testOptimizedRealFFT() throws JWaveException {
        // Test real FFT optimization for large sizes
        int[] sizes = {256, 512, 1024};
        
        for (int n : sizes) {
            // Generate real signal
            double[] signal = new double[n];
            for (int i = 0; i < n; i++) {
                signal[i] = Math.cos(2 * Math.PI * i * 7 / n) + 
                           Math.sin(2 * Math.PI * i * 13 / n);
            }
            
            // Compute FFT using standard implementation
            double[] standardResult = standardFft.forward(signal);
            
            // Compute FFT using optimized implementation
            double[] optimizedResult = optimizedFft.forward(signal);
            
            // Compare results
            assertArrayEquals("FFT mismatch for size " + n,
                            standardResult, optimizedResult, DELTA);
            
            // Verify Hermitian symmetry (characteristic of real signal FFT)
            // X[k] = conj(X[N-k]) for k = 1 to N/2-1
            for (int k = 1; k < n / 2; k++) {
                double realK = standardResult[2 * k];
                double imagK = standardResult[2 * k + 1];
                double realNK = standardResult[2 * (n - k)];
                double imagNK = standardResult[2 * (n - k) + 1];
                
                assertEquals("Hermitian symmetry real part at " + k,
                           realK, realNK, DELTA);
                assertEquals("Hermitian symmetry imag part at " + k,
                           imagK, -imagNK, DELTA);
            }
            
            // Verify DC and Nyquist components are real
            assertEquals("DC component should be real", 0.0, standardResult[1], DELTA);
            assertEquals("Nyquist component should be real", 0.0, standardResult[2 * (n / 2) + 1], DELTA);
        }
    }
}