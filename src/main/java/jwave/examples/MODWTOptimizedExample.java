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

import jwave.transforms.MODWTTransform;
import jwave.transforms.OptimizedFastFourierTransform;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.haar.Haar1;

/**
 * Demonstrates how to use MODWT with optimized implementations.
 * 
 * This example shows how to inject the OptimizedFastFourierTransform
 * for better performance while maintaining backward compatibility.
 * 
 * @author Stephen Romano
 */
public class MODWTOptimizedExample {
    
    public static void main(String[] args) {
        System.out.println("=== MODWT Optimized Example ===\n");
        
        // Example 1: Standard MODWT (default behavior)
        standardMODWTExample();
        
        // Example 2: MODWT with optimized FFT
        optimizedMODWTExample();
        
        // Example 3: Performance comparison
        performanceComparisonExample();
    }
    
    /**
     * Example 1: Standard MODWT with default FFT implementation
     */
    private static void standardMODWTExample() {
        System.out.println("Example 1: Standard MODWT (Default)");
        System.out.println("-----------------------------------");
        
        // Create test signal
        int signalLength = 128;
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.cos(4 * Math.PI * i / 32);
        }
        
        // Create standard MODWT (uses FastFourierTransform by default)
        Daubechies4 wavelet = new Daubechies4();
        MODWTTransform modwt = new MODWTTransform(wavelet);
        
        // Set to use FFT-based convolution
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        
        // Perform decomposition
        int levels = 3;
        double[][] coeffs = modwt.forwardMODWT(signal, levels);
        
        System.out.println("Standard MODWT decomposition completed");
        System.out.println("Using: FastFourierTransform (default)");
        System.out.printf("  Level 1 energy: %.3f\n", calculateEnergy(coeffs[0]));
        System.out.printf("  Level 2 energy: %.3f\n", calculateEnergy(coeffs[1]));
        System.out.printf("  Level 3 energy: %.3f\n\n", calculateEnergy(coeffs[2]));
    }
    
    /**
     * Example 2: MODWT with optimized FFT implementation
     */
    private static void optimizedMODWTExample() {
        System.out.println("Example 2: MODWT with Optimized FFT");
        System.out.println("------------------------------------");
        
        // Create test signal
        int signalLength = 128;
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.cos(4 * Math.PI * i / 32);
        }
        
        // Create MODWT with optimized FFT using dependency injection
        Daubechies4 wavelet = new Daubechies4();
        OptimizedFastFourierTransform optimizedFFT = new OptimizedFastFourierTransform();
        MODWTTransform modwt = new MODWTTransform(wavelet, optimizedFFT);
        
        // Set to use FFT-based convolution
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        
        // Perform decomposition
        int levels = 3;
        double[][] coeffs = modwt.forwardMODWT(signal, levels);
        
        System.out.println("Optimized MODWT decomposition completed");
        System.out.println("Using: OptimizedFastFourierTransform (injected)");
        System.out.printf("  Level 1 energy: %.3f\n", calculateEnergy(coeffs[0]));
        System.out.printf("  Level 2 energy: %.3f\n", calculateEnergy(coeffs[1]));
        System.out.printf("  Level 3 energy: %.3f\n\n", calculateEnergy(coeffs[2]));
    }
    
    /**
     * Example 3: Performance comparison
     */
    private static void performanceComparisonExample() {
        System.out.println("Example 3: Performance Comparison");
        System.out.println("---------------------------------");
        
        // Test parameters
        int signalLength = 4096;
        int levels = 6;
        int warmupRuns = 10;
        int benchmarkRuns = 100;
        
        // Create test signal
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64) + 
                       0.5 * Math.cos(4 * Math.PI * i / 64) +
                       0.1 * Math.sin(8 * Math.PI * i / 64);
        }
        
        // Test with Haar wavelet (short filter)
        Haar1 haar = new Haar1();
        performBenchmark("Haar wavelet", signal, haar, levels, warmupRuns, benchmarkRuns);
        
        // Test with Daubechies-4 (longer filter)
        Daubechies4 db4 = new Daubechies4();
        performBenchmark("Daubechies-4", signal, db4, levels, warmupRuns, benchmarkRuns);
    }
    
    private static void performBenchmark(String waveletName, double[] signal, 
                                       jwave.transforms.wavelets.Wavelet wavelet,
                                       int levels, int warmupRuns, int benchmarkRuns) {
        System.out.println("\n" + waveletName + " performance:");
        
        // Standard MODWT
        MODWTTransform standardModwt = new MODWTTransform(wavelet);
        standardModwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        
        // Warmup
        for (int i = 0; i < warmupRuns; i++) {
            standardModwt.forwardMODWT(signal, levels);
        }
        
        // Benchmark standard
        long standardTime = System.nanoTime();
        for (int i = 0; i < benchmarkRuns; i++) {
            standardModwt.forwardMODWT(signal, levels);
        }
        standardTime = System.nanoTime() - standardTime;
        
        // Optimized MODWT
        MODWTTransform optimizedModwt = new MODWTTransform(wavelet, 
                                                          new OptimizedFastFourierTransform());
        optimizedModwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        
        // Warmup
        for (int i = 0; i < warmupRuns; i++) {
            optimizedModwt.forwardMODWT(signal, levels);
        }
        
        // Benchmark optimized
        long optimizedTime = System.nanoTime();
        for (int i = 0; i < benchmarkRuns; i++) {
            optimizedModwt.forwardMODWT(signal, levels);
        }
        optimizedTime = System.nanoTime() - optimizedTime;
        
        // Calculate results
        double standardMs = standardTime / 1_000_000.0 / benchmarkRuns;
        double optimizedMs = optimizedTime / 1_000_000.0 / benchmarkRuns;
        double speedup = standardMs / optimizedMs;
        
        System.out.printf("  Standard FFT: %.2f ms\n", standardMs);
        System.out.printf("  Optimized FFT: %.2f ms\n", optimizedMs);
        System.out.printf("  Speedup: %.2fx\n", speedup);
    }
    
    private static double calculateEnergy(double[] signal) {
        double energy = 0;
        for (double value : signal) {
            energy += value * value;
        }
        return Math.sqrt(energy);
    }
}