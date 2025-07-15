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
package jwave.transforms.wavelets;

import jwave.Base;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Random;

/**
 * Unit tests for OptimizedWavelet adjoint convolution operations.
 * 
 * @author Stephen Romano
 */
public class OptimizedWaveletAdjointTest extends Base {
    
    private static final double DELTA = 1e-10;
    private static final long RANDOM_SEED = 42L; // Fixed seed for reproducible tests
    private static final Random random = new Random(RANDOM_SEED);
    
    @Test
    public void testAdjointConvolutionCorrectness() {
        // Test with different signal and filter sizes
        int[] signalSizes = {8, 16, 32, 64, 128};
        int[] filterSizes = {2, 4, 8, 16};
        
        for (int signalSize : signalSizes) {
            for (int filterSize : filterSizes) {
                // Create test signal and filter
                double[] signal = new double[signalSize];
                double[] filter = new double[filterSize];
                
                // Initialize with some test values
                for (int i = 0; i < signalSize; i++) {
                    signal[i] = Math.sin(2 * Math.PI * i / signalSize) + 
                               0.5 * Math.cos(4 * Math.PI * i / signalSize);
                }
                for (int i = 0; i < filterSize; i++) {
                    filter[i] = 1.0 / filterSize; // Simple averaging filter
                }
                
                // Compute reference adjoint convolution
                double[] referenceResult = computeReferenceAdjoint(signal, filter);
                
                // Compute optimized adjoint convolution
                double[] optimizedResult = OptimizedWavelet.circularConvolveAdjoint(
                    signal, filter, signalSize, filterSize, 1);
                
                // Compare results
                assertArrayEquals("Adjoint convolution mismatch for signal size " + 
                                signalSize + " and filter size " + filterSize,
                                referenceResult, optimizedResult, DELTA);
            }
        }
    }
    
    @Test
    public void testAdjointConvolutionProperties() {
        // Test that adjoint convolution has the correct mathematical properties
        int signalSize = 32;
        int filterSize = 8;
        
        double[] signal = new double[signalSize];
        double[] filter = new double[filterSize];
        
        // Initialize with random values using seeded Random for reproducibility
        for (int i = 0; i < signalSize; i++) {
            signal[i] = random.nextDouble();
        }
        for (int i = 0; i < filterSize; i++) {
            filter[i] = random.nextDouble();
        }
        
        // Test 1: Verify that <Hx, y> = <x, H*y> (adjoint property)
        // where H is convolution and H* is adjoint convolution
        double[] y = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            y[i] = random.nextDouble();
        }
        
        // Compute Hx (forward convolution)
        double[] Hx = OptimizedWavelet.circularConvolve(signal, filter, signalSize, filterSize, 1);
        
        // Compute H*y (adjoint convolution)
        double[] Hstary = OptimizedWavelet.circularConvolveAdjoint(y, filter, signalSize, filterSize, 1);
        
        // Compute inner products
        double innerProduct1 = 0.0;
        double innerProduct2 = 0.0;
        for (int i = 0; i < signalSize; i++) {
            innerProduct1 += Hx[i] * y[i];
            innerProduct2 += signal[i] * Hstary[i];
        }
        
        assertEquals("Adjoint property not satisfied", innerProduct1, innerProduct2, DELTA);
    }
    
    @Test
    public void testAdjointConvolutionWithStride() {
        // Test adjoint convolution with different strides
        int signalSize = 64;
        int filterSize = 4;
        int[] strides = {1, 2, 4};
        
        double[] signal = new double[signalSize];
        double[] filter = new double[filterSize];
        
        // Initialize
        for (int i = 0; i < signalSize; i++) {
            signal[i] = i + 1; // Simple linear sequence
        }
        for (int i = 0; i < filterSize; i++) {
            filter[i] = 0.25; // Averaging filter
        }
        
        for (int stride : strides) {
            // Compute with optimized method
            double[] result = OptimizedWavelet.circularConvolveAdjoint(
                signal, filter, signalSize, filterSize, stride);
            
            // Verify result is not null and has correct size
            assertNotNull("Result should not be null for stride " + stride, result);
            assertEquals("Result size mismatch for stride " + stride, signalSize, result.length);
            
            // Check that result contains valid values
            for (int i = 0; i < signalSize; i++) {
                assertFalse("Result should not contain NaN at index " + i + " for stride " + stride,
                           Double.isNaN(result[i]));
                assertFalse("Result should not contain infinity at index " + i + " for stride " + stride,
                           Double.isInfinite(result[i]));
            }
        }
    }
    
    /**
     * Reference implementation of adjoint circular convolution.
     */
    private double[] computeReferenceAdjoint(double[] signal, double[] filter) {
        int N = signal.length;
        int M = filter.length;
        double[] output = new double[N];
        
        for (int n = 0; n < N; n++) {
            double sum = 0.0;
            for (int m = 0; m < M; m++) {
                int signalIndex = (n + m) % N;
                sum += signal[signalIndex] * filter[m];
            }
            output[n] = sum;
        }
        
        return output;
    }
}