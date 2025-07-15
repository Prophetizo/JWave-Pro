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
import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.daubechies.Daubechies8;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for OptimizedWavelet operations.
 * 
 * @author Stephen Romano
 */
public class OptimizedWaveletTest extends Base {
    
    private static final double DELTA = 1e-10;
    
    @Test
    public void testForwardTransformCorrectness() {
        // Test with different wavelets
        Wavelet[] wavelets = {
            new Haar1(),
            new Daubechies4(),
            new Daubechies8()
        };
        
        for (Wavelet wavelet : wavelets) {
            System.out.println("Testing forward transform with " + wavelet.getName());
            
            double[] scalingDeCom = wavelet.getScalingDeComposition();
            double[] waveletDeCom = wavelet.getWaveletDeComposition();
            int motherWavelength = wavelet.getMotherWavelength();
            
            // Test with different signal sizes
            int[] sizes = { 16, 32, 64, 128 };
            
            for (int size : sizes) {
                if (size < wavelet.getTransformWavelength()) {
                    continue;
                }
                
                // Create test signal
                double[] signal = new double[size];
                for (int i = 0; i < size; i++) {
                    signal[i] = Math.sin(2 * Math.PI * i / 16) + 0.5 * Math.cos(4 * Math.PI * i / 16);
                }
                
                // Compute transforms
                double[] standardResult = wavelet.forward(signal, size);
                double[] optimizedResult = OptimizedWavelet.forwardOptimized(signal, size,
                        scalingDeCom, waveletDeCom, motherWavelength);
                
                // Compare results
                assertArrayEquals("Forward transform mismatch for " + wavelet.getName() + " size " + size,
                        standardResult, optimizedResult, DELTA);
            }
        }
    }
    
    @Test
    public void testReverseTransformCorrectness() {
        // Test with different wavelets
        Wavelet[] wavelets = {
            new Haar1(),
            new Daubechies4(),
            new Daubechies8()
        };
        
        for (Wavelet wavelet : wavelets) {
            System.out.println("Testing reverse transform with " + wavelet.getName());
            
            double[] scalingDeCom = wavelet.getScalingDeComposition();
            double[] waveletDeCom = wavelet.getWaveletDeComposition();
            double[] scalingReCon = wavelet.getScalingReConstruction();
            double[] waveletReCon = wavelet.getWaveletReConstruction();
            int motherWavelength = wavelet.getMotherWavelength();
            
            // Test with different signal sizes
            int[] sizes = { 16, 32, 64, 128 };
            
            for (int size : sizes) {
                if (size < wavelet.getTransformWavelength()) {
                    continue;
                }
                
                // Create test signal
                double[] signal = new double[size];
                for (int i = 0; i < size; i++) {
                    signal[i] = Math.sin(2 * Math.PI * i / 16) + 0.5 * Math.cos(4 * Math.PI * i / 16);
                }
                
                // Forward transform
                double[] transformed = wavelet.forward(signal, size);
                
                // Compute reverse transforms
                double[] standardResult = wavelet.reverse(transformed, size);
                double[] optimizedResult = OptimizedWavelet.reverseOptimized(transformed, size,
                        scalingReCon, waveletReCon, motherWavelength);
                
                // Compare results
                assertArrayEquals("Reverse transform mismatch for " + wavelet.getName() + " size " + size,
                        standardResult, optimizedResult, DELTA);
            }
        }
    }
    
    @Test
    public void testPerfectReconstruction() {
        // Test that forward followed by reverse gives back the original signal
        Wavelet wavelet = new Daubechies4();
        
        double[] scalingDeCom = wavelet.getScalingDeComposition();
        double[] waveletDeCom = wavelet.getWaveletDeComposition();
        double[] scalingReCon = wavelet.getScalingReConstruction();
        double[] waveletReCon = wavelet.getWaveletReConstruction();
        int motherWavelength = wavelet.getMotherWavelength();
        
        // Test signal
        int size = 64;
        double[] original = new double[size];
        for (int i = 0; i < size; i++) {
            original[i] = Math.sin(2 * Math.PI * i / 16) + 0.5 * Math.cos(4 * Math.PI * i / 16);
        }
        
        // Forward then reverse with optimized methods
        double[] transformed = OptimizedWavelet.forwardOptimized(original, size,
                scalingDeCom, waveletDeCom, motherWavelength);
        double[] reconstructed = OptimizedWavelet.reverseOptimized(transformed, size,
                scalingReCon, waveletReCon, motherWavelength);
        
        // Check perfect reconstruction
        assertArrayEquals("Perfect reconstruction failed", original, reconstructed, DELTA);
    }
    
    @Test
    public void testCircularConvolution() {
        // Test circular convolution correctness
        int signalLength = 16;
        int filterLength = 4;
        
        double[] signal = new double[signalLength];
        double[] filter = new double[filterLength];
        
        // Initialize with simple values
        for (int i = 0; i < signalLength; i++) {
            signal[i] = i + 1;
        }
        for (int i = 0; i < filterLength; i++) {
            filter[i] = 0.25; // Simple averaging filter
        }
        
        // Compute convolution manually
        double[] expected = new double[signalLength];
        for (int n = 0; n < signalLength; n++) {
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = (n - k + signalLength) % signalLength;
                sum += signal[idx] * filter[k];
            }
            expected[n] = sum;
        }
        
        // Compute with optimized method
        double[] result = OptimizedWavelet.circularConvolve(signal, filter,
                signalLength, filterLength, 1);
        
        // Compare
        assertArrayEquals("Circular convolution mismatch", expected, result, DELTA);
    }
    
    @Test
    public void testMultiLevelDecomposition() {
        // Test multi-level decomposition
        Wavelet wavelet = new Haar1();
        
        double[] scalingDeCom = wavelet.getScalingDeComposition();
        double[] waveletDeCom = wavelet.getWaveletDeComposition();
        int motherWavelength = wavelet.getMotherWavelength();
        
        // Create test signal
        int size = 32;
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 8);
        }
        
        // Perform multi-level decomposition
        int levels = 3;
        double[][] decomposition = OptimizedWavelet.multiLevelDecomposition(signal, levels,
                scalingDeCom, waveletDeCom, motherWavelength);
        
        // Verify structure
        assertEquals("Wrong number of decomposition levels", levels + 1, decomposition.length);
        
        // Check sizes
        assertEquals("Level 0 size incorrect", 16, decomposition[0].length);
        assertEquals("Level 1 size incorrect", 8, decomposition[1].length);
        assertEquals("Level 2 size incorrect", 4, decomposition[2].length);
        assertEquals("Final scaling coefficients size incorrect", 4, decomposition[3].length);
    }
    
    @Test
    public void testEdgeCases() {
        Wavelet wavelet = new Haar1();
        double[] scalingDeCom = wavelet.getScalingDeComposition();
        double[] waveletDeCom = wavelet.getWaveletDeComposition();
        int motherWavelength = wavelet.getMotherWavelength();
        
        // Test with minimum size
        int minSize = wavelet.getTransformWavelength();
        double[] smallSignal = new double[minSize];
        for (int i = 0; i < minSize; i++) {
            smallSignal[i] = i;
        }
        
        double[] result = OptimizedWavelet.forwardOptimized(smallSignal, minSize,
                scalingDeCom, waveletDeCom, motherWavelength);
        
        assertNotNull("Result should not be null", result);
        assertEquals("Result size mismatch", minSize, result.length);
        
        // Test circular convolution with stride > 1
        double[] signal = { 1, 2, 3, 4, 5, 6, 7, 8 };
        double[] filter = { 0.5, 0.5 };
        
        double[] convResult = OptimizedWavelet.circularConvolve(signal, filter, 8, 2, 2);
        assertNotNull("Convolution result should not be null", convResult);
        assertEquals("Convolution result size mismatch", 8, convResult.length);
    }
}