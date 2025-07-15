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

import jwave.datatypes.natives.Complex;
import jwave.datatypes.natives.OptimizedComplex;
import java.util.Arrays;

/**
 * Demonstrates how to use the SIMD-optimized complex number operations.
 * 
 * The OptimizedComplex class provides static methods for bulk complex number
 * operations with 2-5x speedup through SIMD-friendly algorithms.
 * 
 * @author Stephen Romano
 */
public class OptimizedComplexExample {
    
    public static void main(String[] args) {
        System.out.println("=== JWave Optimized Complex Operations Example ===\n");
        
        // Example 1: Bulk multiplication
        bulkMultiplicationExample();
        
        // Example 2: Complex array conversions
        arrayConversionExample();
        
        // Example 3: Signal processing with complex numbers
        signalProcessingExample();
        
        // Example 4: FFT butterfly operations
        butterflyOperationsExample();
        
        // Example 5: Performance comparison
        performanceComparisonExample();
    }
    
    /**
     * Example 1: Bulk complex multiplication
     */
    private static void bulkMultiplicationExample() {
        System.out.println("Example 1: Bulk Complex Multiplication");
        System.out.println("--------------------------------------");
        
        int length = 1024;
        
        // Create two complex signals using separate arrays
        double[] signal1Real = new double[length];
        double[] signal1Imag = new double[length];
        double[] signal2Real = new double[length];
        double[] signal2Imag = new double[length];
        
        // Initialize with test data (complex exponentials)
        for (int i = 0; i < length; i++) {
            double phase1 = 2 * Math.PI * i / 64;
            double phase2 = 2 * Math.PI * i / 32;
            
            signal1Real[i] = Math.cos(phase1);
            signal1Imag[i] = Math.sin(phase1);
            signal2Real[i] = Math.cos(phase2);
            signal2Imag[i] = Math.sin(phase2);
        }
        
        // Allocate output arrays
        double[] productReal = new double[length];
        double[] productImag = new double[length];
        
        // Perform optimized bulk multiplication
        OptimizedComplex.multiplyBulk(signal1Real, signal1Imag,
                                     signal2Real, signal2Imag,
                                     productReal, productImag, length);
        
        // Verify result (complex multiplication should produce sum of phases)
        System.out.println("Multiplication verification:");
        System.out.printf("  Expected phase at index 100: %.3f radians\n", 
                         3 * 2 * Math.PI * 100 / 64);
        System.out.printf("  Actual phase at index 100: %.3f radians\n",
                         Math.atan2(productImag[100], productReal[100]));
        
        // Calculate total energy
        double energy = 0.0;
        for (int i = 0; i < length; i++) {
            energy += productReal[i] * productReal[i] + productImag[i] * productImag[i];
        }
        System.out.printf("  Total energy: %.3f\n\n", energy);
    }
    
    /**
     * Example 2: Array conversions between Complex objects and separate arrays
     */
    private static void arrayConversionExample() {
        System.out.println("Example 2: Complex Array Conversions");
        System.out.println("------------------------------------");
        
        // Create Complex array
        int length = 256;
        Complex[] complexArray = new Complex[length];
        
        // Initialize with a chirp signal
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double phase = 2 * Math.PI * (5 * t + 10 * t * t);
            complexArray[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }
        
        // Convert to separate arrays using optimized method
        double[] realPart = new double[length];
        double[] imagPart = new double[length];
        
        OptimizedComplex.toSeparateArrays(complexArray, realPart, imagPart);
        
        System.out.println("Converted Complex[] to separate arrays:");
        System.out.printf("  First element: %.3f + %.3fi\n", realPart[0], imagPart[0]);
        System.out.printf("  Middle element: %.3f + %.3fi\n", 
                         realPart[length/2], imagPart[length/2]);
        
        // Modify the separate arrays (e.g., apply a filter)
        for (int i = 0; i < length; i++) {
            double magnitude = Math.sqrt(realPart[i] * realPart[i] + 
                                       imagPart[i] * imagPart[i]);
            if (magnitude > 0) {
                // Normalize to unit magnitude
                realPart[i] /= magnitude;
                imagPart[i] /= magnitude;
            }
        }
        
        // Convert back to Complex array
        Complex[] reconstructed = new Complex[length];
        OptimizedComplex.fromSeparateArrays(realPart, imagPart, reconstructed);
        
        System.out.println("\nAfter normalization:");
        System.out.printf("  First element magnitude: %.6f\n", reconstructed[0].getMag());
        System.out.printf("  Middle element magnitude: %.6f\n\n", 
                         reconstructed[length/2].getMag());
    }
    
    /**
     * Example 3: Signal processing with optimized complex operations
     */
    private static void signalProcessingExample() {
        System.out.println("Example 3: Signal Processing");
        System.out.println("----------------------------");
        
        // Create a complex signal with multiple components
        int length = 512;
        double[] signalReal = new double[length];
        double[] signalImag = new double[length];
        
        // Add three complex sinusoids
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            
            // Component 1: 5 Hz with phase shift
            signalReal[i] += Math.cos(2 * Math.PI * 5 * t + Math.PI/4);
            signalImag[i] += Math.sin(2 * Math.PI * 5 * t + Math.PI/4);
            
            // Component 2: 12 Hz
            signalReal[i] += 0.5 * Math.cos(2 * Math.PI * 12 * t);
            signalImag[i] += 0.5 * Math.sin(2 * Math.PI * 12 * t);
            
            // Component 3: 20 Hz with different phase
            signalReal[i] += 0.3 * Math.cos(2 * Math.PI * 20 * t - Math.PI/3);
            signalImag[i] += 0.3 * Math.sin(2 * Math.PI * 20 * t - Math.PI/3);
        }
        
        // Apply complex mixing (frequency shift)
        double mixFreq = 8.0; // Shift by 8 Hz
        double[] mixerReal = new double[length];
        double[] mixerImag = new double[length];
        
        for (int i = 0; i < length; i++) {
            double phase = 2 * Math.PI * mixFreq * i / length;
            mixerReal[i] = Math.cos(phase);
            mixerImag[i] = Math.sin(phase);
        }
        
        // Perform mixing using optimized multiplication
        double[] mixedReal = new double[length];
        double[] mixedImag = new double[length];
        
        OptimizedComplex.multiplyBulk(signalReal, signalImag,
                                     mixerReal, mixerImag,
                                     mixedReal, mixedImag, length);
        
        // Calculate instantaneous frequency at a few points
        System.out.println("Frequency shifting results:");
        for (int i : new int[]{100, 200, 300}) {
            if (i < length - 1) {
                // Approximate instantaneous frequency
                double phase1 = Math.atan2(mixedImag[i], mixedReal[i]);
                double phase2 = Math.atan2(mixedImag[i+1], mixedReal[i+1]);
                double phaseDiff = phase2 - phase1;
                
                // Unwrap phase
                if (phaseDiff > Math.PI) phaseDiff -= 2 * Math.PI;
                if (phaseDiff < -Math.PI) phaseDiff += 2 * Math.PI;
                
                double instFreq = phaseDiff * length / (2 * Math.PI);
                System.out.printf("  Instantaneous frequency at sample %d: %.1f Hz\n", 
                                i, instFreq);
            }
        }
        System.out.println();
    }
    
    /**
     * Example 4: FFT butterfly operations
     */
    private static void butterflyOperationsExample() {
        System.out.println("Example 4: FFT Butterfly Operations");
        System.out.println("-----------------------------------");
        
        // Demonstrate the building blocks of FFT
        int length = 8; // Small size for demonstration
        double[] real = new double[length];
        double[] imag = new double[length];
        
        // Initialize with simple test pattern
        for (int i = 0; i < length; i++) {
            real[i] = i;
            imag[i] = 0;
        }
        
        System.out.println("Initial values:");
        printComplexArray(real, imag, 4);
        
        // Perform one stage of FFT butterflies
        int halfSize = length / 2;
        
        // Twiddle factors for first stage
        double[] twiddleReal = new double[halfSize];
        double[] twiddleImag = new double[halfSize];
        
        for (int k = 0; k < halfSize; k++) {
            double angle = -2 * Math.PI * k / length;
            twiddleReal[k] = Math.cos(angle);
            twiddleImag[k] = Math.sin(angle);
        }
        
        // Apply butterfly operations
        double[] tempReal = new double[halfSize];
        double[] tempImag = new double[halfSize];
        
        // Lower half × twiddle factors
        System.arraycopy(real, halfSize, tempReal, 0, halfSize);
        System.arraycopy(imag, halfSize, tempImag, 0, halfSize);
        
        OptimizedComplex.multiplyBulk(tempReal, tempImag,
                                     twiddleReal, twiddleImag,
                                     tempReal, tempImag, halfSize);
        
        // Butterfly combinations
        for (int k = 0; k < halfSize; k++) {
            double upperReal = real[k];
            double upperImag = imag[k];
            
            // Upper + twiddle * lower
            real[k] = upperReal + tempReal[k];
            imag[k] = upperImag + tempImag[k];
            
            // Upper - twiddle * lower
            real[k + halfSize] = upperReal - tempReal[k];
            imag[k + halfSize] = upperImag - tempImag[k];
        }
        
        System.out.println("\nAfter first butterfly stage:");
        printComplexArray(real, imag, 4);
        System.out.println();
    }
    
    /**
     * Example 5: Performance comparison
     */
    private static void performanceComparisonExample() {
        System.out.println("Example 5: Performance Comparison");
        System.out.println("---------------------------------");
        
        int[] sizes = {1024, 4096, 16384};
        int warmupRuns = 100;
        int benchmarkRuns = 1000;
        
        for (int size : sizes) {
            // Create test data
            Complex[] complexArray1 = new Complex[size];
            Complex[] complexArray2 = new Complex[size];
            double[] real1 = new double[size];
            double[] imag1 = new double[size];
            double[] real2 = new double[size];
            double[] imag2 = new double[size];
            double[] resultReal = new double[size];
            double[] resultImag = new double[size];
            
            // Initialize with random data
            for (int i = 0; i < size; i++) {
                double r1 = Math.random();
                double i1 = Math.random();
                double r2 = Math.random();
                double i2 = Math.random();
                
                complexArray1[i] = new Complex(r1, i1);
                complexArray2[i] = new Complex(r2, i2);
                real1[i] = r1;
                imag1[i] = i1;
                real2[i] = r2;
                imag2[i] = i2;
            }
            
            // Warmup
            for (int i = 0; i < warmupRuns; i++) {
                // Standard approach
                Complex[] result1 = new Complex[size];
                for (int j = 0; j < size; j++) {
                    result1[j] = complexArray1[j].mul(complexArray2[j]);
                }
                
                // Optimized approach
                OptimizedComplex.multiplyBulk(real1, imag1, real2, imag2,
                                            resultReal, resultImag, size);
            }
            
            // Benchmark standard approach
            long standardTime = System.nanoTime();
            for (int i = 0; i < benchmarkRuns; i++) {
                Complex[] result = new Complex[size];
                for (int j = 0; j < size; j++) {
                    result[j] = complexArray1[j].mul(complexArray2[j]);
                }
            }
            standardTime = System.nanoTime() - standardTime;
            
            // Benchmark optimized approach
            long optimizedTime = System.nanoTime();
            for (int i = 0; i < benchmarkRuns; i++) {
                OptimizedComplex.multiplyBulk(real1, imag1, real2, imag2,
                                            resultReal, resultImag, size);
            }
            optimizedTime = System.nanoTime() - optimizedTime;
            
            // Calculate speedup
            double speedup = (double) standardTime / optimizedTime;
            double standardUs = standardTime / 1000.0 / benchmarkRuns;
            double optimizedUs = optimizedTime / 1000.0 / benchmarkRuns;
            
            System.out.printf("Size %5d: Standard: %8.2f μs, Optimized: %8.2f μs, Speedup: %.2fx\n",
                            size, standardUs, optimizedUs, speedup);
        }
    }
    
    /**
     * Helper method to print complex arrays nicely
     */
    private static void printComplexArray(double[] real, double[] imag, int limit) {
        int count = Math.min(limit, real.length);
        for (int i = 0; i < count; i++) {
            System.out.printf("  [%d]: %.3f %+.3fi\n", i, real[i], imag[i]);
        }
        if (count < real.length) {
            System.out.println("  ...");
        }
    }
}