/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
 *
 * Copyright (c) 2008-2024 Christian (graetz23@gmail.com)
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
import org.junit.Ignore;
import org.junit.Test;

/**
 * Performance tests for OptimizedWavelet vs standard Wavelet operations.
 * 
 * @author Stephen Romano
 */
public class OptimizedWaveletPerformanceTest extends Base {
    
    private static final int WARMUP_ITERATIONS = 100;
    private static final int TEST_ITERATIONS = 1000;
    private static final int[] SIZES = { 256, 512, 1024, 2048, 4096 };
    
    @Test
    @Ignore("Performance test - run manually")
    public void testWaveletTransformPerformance() {
        System.out.println("Wavelet Transform Performance Test");
        System.out.println("==================================");
        
        // Test with different wavelets
        Wavelet[] wavelets = {
            new Haar1(),
            new Daubechies4(),
            new Daubechies8()
        };
        
        for (Wavelet wavelet : wavelets) {
            System.out.println("\nTesting with " + wavelet.getName());
            System.out.println("-".repeat(40));
            
            double[] scalingDeCom = wavelet.getScalingDeComposition();
            double[] waveletDeCom = wavelet.getWaveletDeComposition();
            double[] scalingReCon = wavelet.getScalingReConstruction();
            double[] waveletReCon = wavelet.getWaveletReConstruction();
            int motherWavelength = wavelet.getMotherWavelength();
            
            for (int size : SIZES) {
                if (size < wavelet.getTransformWavelength()) {
                    continue; // Skip if signal too small
                }
                
                // Prepare test data
                double[] signal = new double[size];
                for (int i = 0; i < size; i++) {
                    signal[i] = Math.sin(2 * Math.PI * i / 64) + 0.5 * Math.cos(4 * Math.PI * i / 64);
                }
                
                // Warmup
                for (int iter = 0; iter < WARMUP_ITERATIONS; iter++) {
                    // Standard
                    wavelet.forward(signal, size);
                    // Optimized
                    OptimizedWavelet.forwardOptimized(signal, size, scalingDeCom, waveletDeCom, motherWavelength);
                }
                
                // Benchmark forward transform
                long standardTime = 0;
                for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                    long start = System.nanoTime();
                    wavelet.forward(signal, size);
                    standardTime += System.nanoTime() - start;
                }
                
                long optimizedTime = 0;
                for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                    long start = System.nanoTime();
                    OptimizedWavelet.forwardOptimized(signal, size, scalingDeCom, waveletDeCom, motherWavelength);
                    optimizedTime += System.nanoTime() - start;
                }
                
                double standardAvg = standardTime / (double) TEST_ITERATIONS / 1000.0; // microseconds
                double optimizedAvg = optimizedTime / (double) TEST_ITERATIONS / 1000.0;
                double speedup = standardAvg / optimizedAvg;
                
                System.out.printf("Size %d Forward: Standard %.2f μs, Optimized %.2f μs, Speedup: %.2fx%n",
                        size, standardAvg, optimizedAvg, speedup);
                
                // Benchmark reverse transform
                double[] transformed = wavelet.forward(signal, size);
                
                // Warmup
                for (int iter = 0; iter < WARMUP_ITERATIONS; iter++) {
                    // Standard
                    wavelet.reverse(transformed, size);
                    // Optimized
                    OptimizedWavelet.reverseOptimized(transformed, size, scalingReCon, waveletReCon, motherWavelength);
                }
                
                standardTime = 0;
                for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                    long start = System.nanoTime();
                    wavelet.reverse(transformed, size);
                    standardTime += System.nanoTime() - start;
                }
                
                optimizedTime = 0;
                for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                    long start = System.nanoTime();
                    OptimizedWavelet.reverseOptimized(transformed, size, scalingReCon, waveletReCon, motherWavelength);
                    optimizedTime += System.nanoTime() - start;
                }
                
                standardAvg = standardTime / (double) TEST_ITERATIONS / 1000.0;
                optimizedAvg = optimizedTime / (double) TEST_ITERATIONS / 1000.0;
                speedup = standardAvg / optimizedAvg;
                
                System.out.printf("Size %d Reverse: Standard %.2f μs, Optimized %.2f μs, Speedup: %.2fx%n",
                        size, standardAvg, optimizedAvg, speedup);
            }
        }
        System.out.println();
    }
    
    @Test
    @Ignore("Performance test - run manually")
    public void testCircularConvolutionPerformance() {
        System.out.println("Circular Convolution Performance Test");
        System.out.println("=====================================");
        
        // Test with different filter lengths
        int[] filterLengths = { 4, 8, 16, 32 };
        
        for (int filterLength : filterLengths) {
            System.out.println("\nFilter length: " + filterLength);
            System.out.println("-".repeat(40));
            
            // Create filter
            double[] filter = new double[filterLength];
            for (int i = 0; i < filterLength; i++) {
                filter[i] = 1.0 / filterLength; // Simple averaging filter
            }
            
            for (int size : SIZES) {
                // Prepare test data
                double[] signal = new double[size];
                for (int i = 0; i < size; i++) {
                    signal[i] = Math.random();
                }
                
                // Warmup
                for (int iter = 0; iter < WARMUP_ITERATIONS; iter++) {
                    // Standard (inline implementation)
                    double[] result1 = new double[size];
                    for (int n = 0; n < size; n++) {
                        double sum = 0.0;
                        for (int k = 0; k < filterLength; k++) {
                            int idx = (n - k + size) % size;
                            sum += signal[idx] * filter[k];
                        }
                        result1[n] = sum;
                    }
                    
                    // Optimized
                    OptimizedWavelet.circularConvolve(signal, filter, size, filterLength, 1);
                }
                
                // Benchmark standard
                long standardTime = 0;
                for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                    long start = System.nanoTime();
                    double[] result = new double[size];
                    for (int n = 0; n < size; n++) {
                        double sum = 0.0;
                        for (int k = 0; k < filterLength; k++) {
                            int idx = (n - k + size) % size;
                            sum += signal[idx] * filter[k];
                        }
                        result[n] = sum;
                    }
                    standardTime += System.nanoTime() - start;
                }
                
                // Benchmark optimized
                long optimizedTime = 0;
                for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                    long start = System.nanoTime();
                    OptimizedWavelet.circularConvolve(signal, filter, size, filterLength, 1);
                    optimizedTime += System.nanoTime() - start;
                }
                
                double standardAvg = standardTime / (double) TEST_ITERATIONS / 1000.0;
                double optimizedAvg = optimizedTime / (double) TEST_ITERATIONS / 1000.0;
                double speedup = standardAvg / optimizedAvg;
                
                System.out.printf("Size %d: Standard %.2f μs, Optimized %.2f μs, Speedup: %.2fx%n",
                        size, standardAvg, optimizedAvg, speedup);
            }
        }
        System.out.println();
    }
    
    @Test
    @Ignore("Performance test - run manually")
    public void testCompletePerformanceSuite() {
        testWaveletTransformPerformance();
        testCircularConvolutionPerformance();
    }
}