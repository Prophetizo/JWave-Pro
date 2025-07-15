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
package jwave.examples;

import jwave.transforms.wavelets.OptimizedWavelet;
import jwave.transforms.wavelets.Wavelet;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.daubechies.Daubechies8;
import jwave.transforms.wavelets.symlets.Symlet8;

/**
 * Demonstrates how to use the SIMD-optimized wavelet operations.
 * 
 * The OptimizedWavelet class provides static methods for high-performance
 * wavelet transforms with 2-8x speedup through SIMD-friendly algorithms.
 * 
 * @author Stephen Romano
 */
public class OptimizedWaveletExample {
    
    public static void main(String[] args) {
        System.out.println("=== JWave Optimized Wavelet Example ===\n");
        
        // Example 1: Direct optimized wavelet transform
        directTransformExample();
        
        // Example 2: Optimized circular convolution
        circularConvolutionExample();
        
        // Example 3: Multi-level decomposition
        multiLevelDecompositionExample();
        
        // Example 4: Signal denoising with optimized wavelets
        signalDenoisingExample();
        
        // Example 5: Performance comparison
        performanceComparisonExample();
    }
    
    /**
     * Example 1: Direct usage of optimized wavelet transform
     */
    private static void directTransformExample() {
        System.out.println("Example 1: Direct Optimized Wavelet Transform");
        System.out.println("---------------------------------------------");
        
        // Create test signal
        int signalLength = 64;
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16) + 
                       0.5 * Math.cos(4 * Math.PI * i / 16);
        }
        
        // Choose a wavelet
        Wavelet wavelet = new Daubechies4();
        
        // Get wavelet coefficients
        double[] scalingDeCom = wavelet.getScalingDeComposition();
        double[] waveletDeCom = wavelet.getWaveletDeComposition();
        double[] scalingReCon = wavelet.getScalingReConstruction();
        double[] waveletReCon = wavelet.getWaveletReConstruction();
        int motherWavelength = wavelet.getMotherWavelength();
        
        // Forward transform using optimized implementation
        double[] coefficients = OptimizedWavelet.forwardOptimized(
            signal, signalLength, scalingDeCom, waveletDeCom, motherWavelength);
        
        System.out.println("Forward transform completed");
        System.out.printf("  Approximation energy: %.3f\n", 
                         calculateEnergy(coefficients, 0, signalLength/2));
        System.out.printf("  Detail energy: %.3f\n", 
                         calculateEnergy(coefficients, signalLength/2, signalLength));
        
        // Inverse transform
        double[] reconstructed = OptimizedWavelet.reverseOptimized(
            coefficients, signalLength, scalingReCon, waveletReCon, motherWavelength);
        
        // Verify perfect reconstruction
        double error = 0.0;
        for (int i = 0; i < signalLength; i++) {
            error += Math.pow(signal[i] - reconstructed[i], 2);
        }
        System.out.printf("  Reconstruction error: %.2e\n\n", Math.sqrt(error / signalLength));
    }
    
    /**
     * Example 2: Optimized circular convolution (used in MODWT)
     */
    private static void circularConvolutionExample() {
        System.out.println("Example 2: Optimized Circular Convolution");
        System.out.println("-----------------------------------------");
        
        // This is particularly useful for MODWT implementation
        int signalLength = 128;
        double[] signal = new double[signalLength];
        
        // Generate a test signal
        for (int i = 0; i < signalLength; i++) {
            signal[i] = Math.exp(-Math.pow((i - signalLength/2.0) / 20.0, 2));
        }
        
        // Create a filter (e.g., Daubechies-8 scaling filter)
        Wavelet wavelet = new Daubechies8();
        double[] filter = wavelet.getScalingDeComposition();
        
        // Forward circular convolution (standard)
        double[] convolved = OptimizedWavelet.circularConvolve(
            signal, filter, signalLength, filter.length, 1);
        
        // Adjoint circular convolution (for inverse transforms)
        double[] adjointConvolved = OptimizedWavelet.circularConvolveAdjoint(
            signal, filter, signalLength, filter.length, 1);
        
        System.out.println("Circular convolution results:");
        System.out.printf("  Signal length: %d\n", signalLength);
        System.out.printf("  Filter length: %d\n", filter.length);
        System.out.printf("  Forward convolution sum: %.3f\n", sum(convolved));
        System.out.printf("  Adjoint convolution sum: %.3f\n\n", sum(adjointConvolved));
        
        // Demonstrate the adjoint property: <Hx, y> = <x, H*y>
        double[] y = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            y[i] = Math.random();
        }
        
        double[] Hx = OptimizedWavelet.circularConvolve(signal, filter, signalLength, filter.length, 1);
        double[] Hstary = OptimizedWavelet.circularConvolveAdjoint(y, filter, signalLength, filter.length, 1);
        
        double innerProduct1 = 0, innerProduct2 = 0;
        for (int i = 0; i < signalLength; i++) {
            innerProduct1 += Hx[i] * y[i];
            innerProduct2 += signal[i] * Hstary[i];
        }
        
        System.out.println("Adjoint property verification:");
        System.out.printf("  <Hx, y> = %.6f\n", innerProduct1);
        System.out.printf("  <x, H*y> = %.6f\n", innerProduct2);
        System.out.printf("  Difference: %.2e\n\n", Math.abs(innerProduct1 - innerProduct2));
    }
    
    /**
     * Example 3: Multi-level wavelet decomposition
     */
    private static void multiLevelDecompositionExample() {
        System.out.println("Example 3: Multi-Level Decomposition");
        System.out.println("------------------------------------");
        
        // Create a signal with features at different scales
        int signalLength = 256;
        double[] signal = new double[signalLength];
        
        // Add components at different frequencies
        for (int i = 0; i < signalLength; i++) {
            double t = (double) i / signalLength;
            signal[i] = Math.sin(2 * Math.PI * 2 * t)    // Low frequency
                      + 0.5 * Math.sin(2 * Math.PI * 8 * t)  // Medium frequency
                      + 0.3 * Math.sin(2 * Math.PI * 32 * t); // High frequency
        }
        
        // Use Symlet8 for better frequency localization
        Wavelet wavelet = new Symlet8();
        double[] scalingDeCom = wavelet.getScalingDeComposition();
        double[] waveletDeCom = wavelet.getWaveletDeComposition();
        int motherWavelength = wavelet.getMotherWavelength();
        
        // Perform 4-level decomposition
        int levels = 4;
        double[][] decomposition = OptimizedWavelet.multiLevelDecomposition(
            signal, levels, scalingDeCom, waveletDeCom, motherWavelength);
        
        System.out.println("Multi-level decomposition results:");
        for (int level = 0; level < levels; level++) {
            System.out.printf("  Level %d details: length=%d, energy=%.3f\n",
                            level + 1, decomposition[level].length,
                            calculateEnergy(decomposition[level], 0, decomposition[level].length));
        }
        System.out.printf("  Final approximation: length=%d, energy=%.3f\n\n",
                        decomposition[levels].length,
                        calculateEnergy(decomposition[levels], 0, decomposition[levels].length));
    }
    
    /**
     * Example 4: Signal denoising using optimized wavelets
     */
    private static void signalDenoisingExample() {
        System.out.println("Example 4: Signal Denoising");
        System.out.println("---------------------------");
        
        // Create a noisy signal
        int signalLength = 128;
        double[] cleanSignal = new double[signalLength];
        double[] noisySignal = new double[signalLength];
        
        // Generate clean signal and add noise
        double noiseLevel = 0.3;
        for (int i = 0; i < signalLength; i++) {
            cleanSignal[i] = Math.exp(-Math.pow((i - 64) / 20.0, 2));
            noisySignal[i] = cleanSignal[i] + noiseLevel * (Math.random() - 0.5);
        }
        
        // Use Daubechies4 for denoising
        Wavelet wavelet = new Daubechies4();
        double[] scalingDeCom = wavelet.getScalingDeComposition();
        double[] waveletDeCom = wavelet.getWaveletDeComposition();
        double[] scalingReCon = wavelet.getScalingReConstruction();
        double[] waveletReCon = wavelet.getWaveletReConstruction();
        int motherWavelength = wavelet.getMotherWavelength();
        
        // Forward transform
        double[] coefficients = OptimizedWavelet.forwardOptimized(
            noisySignal, signalLength, scalingDeCom, waveletDeCom, motherWavelength);
        
        // Apply soft thresholding to detail coefficients
        double threshold = noiseLevel * Math.sqrt(2 * Math.log(signalLength));
        int halfLength = signalLength / 2;
        for (int i = halfLength; i < signalLength; i++) {
            coefficients[i] = softThreshold(coefficients[i], threshold);
        }
        
        // Inverse transform
        double[] denoised = OptimizedWavelet.reverseOptimized(
            coefficients, signalLength, scalingReCon, waveletReCon, motherWavelength);
        
        // Calculate SNR improvement
        double noisyError = calculateMSE(cleanSignal, noisySignal);
        double denoisedError = calculateMSE(cleanSignal, denoised);
        double snrImprovement = 10 * Math.log10(noisyError / denoisedError);
        
        System.out.printf("Denoising results:\n");
        System.out.printf("  Original SNR: %.2f dB\n", 10 * Math.log10(1.0 / noisyError));
        System.out.printf("  Denoised SNR: %.2f dB\n", 10 * Math.log10(1.0 / denoisedError));
        System.out.printf("  SNR improvement: %.2f dB\n\n", snrImprovement);
    }
    
    /**
     * Example 5: Performance comparison
     */
    private static void performanceComparisonExample() {
        System.out.println("Example 5: Performance Comparison");
        System.out.println("---------------------------------");
        
        // Test parameters
        int[] signalSizes = {256, 512, 1024, 2048};
        Wavelet[] wavelets = {new Haar1(), new Daubechies4(), new Daubechies8()};
        int warmupRuns = 100;
        int benchmarkRuns = 1000;
        
        for (Wavelet wavelet : wavelets) {
            System.out.println("\n" + wavelet.getName() + " performance:");
            
            double[] scalingDeCom = wavelet.getScalingDeComposition();
            double[] waveletDeCom = wavelet.getWaveletDeComposition();
            int motherWavelength = wavelet.getMotherWavelength();
            
            for (int size : signalSizes) {
                if (size < wavelet.getTransformWavelength()) continue;
                
                // Generate test signal
                double[] signal = new double[size];
                for (int i = 0; i < size; i++) {
                    signal[i] = Math.random();
                }
                
                // Warmup
                for (int i = 0; i < warmupRuns; i++) {
                    wavelet.forward(signal, size);
                    OptimizedWavelet.forwardOptimized(signal, size, 
                        scalingDeCom, waveletDeCom, motherWavelength);
                }
                
                // Benchmark standard implementation
                long standardTime = System.nanoTime();
                for (int i = 0; i < benchmarkRuns; i++) {
                    wavelet.forward(signal, size);
                }
                standardTime = System.nanoTime() - standardTime;
                
                // Benchmark optimized implementation
                long optimizedTime = System.nanoTime();
                for (int i = 0; i < benchmarkRuns; i++) {
                    OptimizedWavelet.forwardOptimized(signal, size,
                        scalingDeCom, waveletDeCom, motherWavelength);
                }
                optimizedTime = System.nanoTime() - optimizedTime;
                
                // Calculate speedup
                double speedup = (double) standardTime / optimizedTime;
                double standardUs = standardTime / 1000.0 / benchmarkRuns;
                double optimizedUs = optimizedTime / 1000.0 / benchmarkRuns;
                
                System.out.printf("  Size %4d: Standard: %6.2f μs, Optimized: %6.2f μs, Speedup: %.2fx\n",
                                size, standardUs, optimizedUs, speedup);
            }
        }
    }
    
    // Helper methods
    
    private static double calculateEnergy(double[] signal, int start, int end) {
        double energy = 0;
        for (int i = start; i < end; i++) {
            energy += signal[i] * signal[i];
        }
        return energy;
    }
    
    private static double sum(double[] array) {
        double sum = 0;
        for (double value : array) {
            sum += value;
        }
        return sum;
    }
    
    private static double softThreshold(double x, double threshold) {
        if (Math.abs(x) <= threshold) {
            return 0;
        } else if (x > 0) {
            return x - threshold;
        } else {
            return x + threshold;
        }
    }
    
    private static double calculateMSE(double[] signal1, double[] signal2) {
        double mse = 0;
        for (int i = 0; i < signal1.length; i++) {
            mse += Math.pow(signal1[i] - signal2[i], 2);
        }
        return mse / signal1.length;
    }
}