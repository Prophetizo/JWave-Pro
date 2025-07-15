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

import jwave.transforms.ContinuousWaveletTransform;
import jwave.transforms.CWTResult;
import jwave.transforms.FastFourierTransform;
import jwave.transforms.OptimizedFastFourierTransform;
import jwave.transforms.wavelets.continuous.MorletWavelet;
import jwave.transforms.wavelets.continuous.MexicanHatWavelet;
import jwave.datatypes.natives.Complex;

/**
 * Demonstrates how to use CWT with dependency-injected FFT implementations.
 * 
 * This example shows how to inject the OptimizedFastFourierTransform
 * for better performance while maintaining backward compatibility.
 * 
 * @author Stephen Romano
 */
public class CWTOptimizedExample {
    
    public static void main(String[] args) {
        System.out.println("=== CWT with Optimized FFT Example ===\n");
        
        // Example 1: Standard CWT (default behavior)
        standardCWTExample();
        
        // Example 2: CWT with optimized FFT
        optimizedCWTExample();
        
        // Example 3: Performance comparison
        performanceComparisonExample();
    }
    
    /**
     * Example 1: Standard CWT with default FFT implementation
     */
    private static void standardCWTExample() {
        System.out.println("Example 1: Standard CWT (Default)");
        System.out.println("---------------------------------");
        
        // Create test signal
        int signalLength = 512;
        double samplingRate = 1000.0;
        double[] signal = createChirpSignal(signalLength, samplingRate, 10, 200);
        
        // Create standard CWT (uses FastFourierTransform by default)
        MorletWavelet morlet = new MorletWavelet();
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(morlet);
        
        // Define scales
        double[] scales = generateLogScales(2, 100, 32);
        
        // Perform CWT
        CWTResult result = cwt.transformFFT(signal, scales, samplingRate);
        
        System.out.println("Standard CWT completed");
        System.out.println("Using: FastFourierTransform (default)");
        analyzeResult(result, scales, samplingRate);
        System.out.println();
    }
    
    /**
     * Example 2: CWT with optimized FFT implementation
     */
    private static void optimizedCWTExample() {
        System.out.println("Example 2: CWT with Optimized FFT");
        System.out.println("----------------------------------");
        
        // Create test signal
        int signalLength = 512;
        double samplingRate = 1000.0;
        double[] signal = createChirpSignal(signalLength, samplingRate, 10, 200);
        
        // Create CWT with optimized FFT using dependency injection
        MorletWavelet morlet = new MorletWavelet();
        OptimizedFastFourierTransform optimizedFFT = new OptimizedFastFourierTransform();
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(morlet, optimizedFFT);
        
        // Define scales
        double[] scales = generateLogScales(2, 100, 32);
        
        // Perform CWT
        CWTResult result = cwt.transformFFT(signal, scales, samplingRate);
        
        System.out.println("Optimized CWT completed");
        System.out.println("Using: OptimizedFastFourierTransform (injected)");
        analyzeResult(result, scales, samplingRate);
        System.out.println();
    }
    
    /**
     * Example 3: Performance comparison
     */
    private static void performanceComparisonExample() {
        System.out.println("Example 3: Performance Comparison");
        System.out.println("---------------------------------");
        
        // Test parameters
        int[] signalLengths = {512, 1024, 2048};
        int numScales = 64;
        double samplingRate = 1000.0;
        int warmupRuns = 5;
        int benchmarkRuns = 20;
        
        // Test with different wavelets
        testWaveletPerformance("Morlet", new MorletWavelet(), 
                             signalLengths, numScales, samplingRate, 
                             warmupRuns, benchmarkRuns);
        
        testWaveletPerformance("Mexican Hat", new MexicanHatWavelet(), 
                             signalLengths, numScales, samplingRate, 
                             warmupRuns, benchmarkRuns);
    }
    
    private static void testWaveletPerformance(String waveletName, 
                                             jwave.transforms.wavelets.continuous.ContinuousWavelet wavelet,
                                             int[] signalLengths, int numScales, 
                                             double samplingRate, int warmupRuns, int benchmarkRuns) {
        System.out.println("\n" + waveletName + " wavelet performance:");
        System.out.printf("%-12s | %-15s | %-15s | %-8s\n", 
                         "Signal Size", "Standard (ms)", "Optimized (ms)", "Speedup");
        System.out.println("-".repeat(60));
        
        for (int signalLength : signalLengths) {
            // Create test signal
            double[] signal = createChirpSignal(signalLength, samplingRate, 10, 200);
            double[] scales = generateLogScales(2, 100, numScales);
            
            // Standard CWT
            ContinuousWaveletTransform standardCWT = new ContinuousWaveletTransform(wavelet);
            
            // Warmup
            for (int i = 0; i < warmupRuns; i++) {
                standardCWT.transformFFT(signal, scales, samplingRate);
            }
            
            // Benchmark standard
            long standardTime = System.nanoTime();
            for (int i = 0; i < benchmarkRuns; i++) {
                standardCWT.transformFFT(signal, scales, samplingRate);
            }
            standardTime = System.nanoTime() - standardTime;
            
            // Optimized CWT
            ContinuousWaveletTransform optimizedCWT = new ContinuousWaveletTransform(
                wavelet, new OptimizedFastFourierTransform());
            
            // Warmup
            for (int i = 0; i < warmupRuns; i++) {
                optimizedCWT.transformFFT(signal, scales, samplingRate);
            }
            
            // Benchmark optimized
            long optimizedTime = System.nanoTime();
            for (int i = 0; i < benchmarkRuns; i++) {
                optimizedCWT.transformFFT(signal, scales, samplingRate);
            }
            optimizedTime = System.nanoTime() - optimizedTime;
            
            // Calculate results
            double standardMs = standardTime / 1_000_000.0 / benchmarkRuns;
            double optimizedMs = optimizedTime / 1_000_000.0 / benchmarkRuns;
            double speedup = standardMs / optimizedMs;
            
            System.out.printf("%-12d | %15.2f | %15.2f | %8.2fx\n",
                            signalLength, standardMs, optimizedMs, speedup);
        }
    }
    
    // Helper methods
    
    private static double[] createChirpSignal(int length, double samplingRate, 
                                            double f0, double f1) {
        double[] signal = new double[length];
        double T = length / samplingRate;
        
        for (int i = 0; i < length; i++) {
            double t = i / samplingRate;
            double phase = 2 * Math.PI * (f0 * t + (f1 - f0) * t * t / (2 * T));
            signal[i] = Math.cos(phase);
        }
        
        return signal;
    }
    
    private static double[] generateLogScales(double minScale, double maxScale, int numScales) {
        double[] scales = new double[numScales];
        double logMin = Math.log(minScale);
        double logMax = Math.log(maxScale);
        
        for (int i = 0; i < numScales; i++) {
            double logScale = logMin + (logMax - logMin) * i / (numScales - 1);
            scales[i] = Math.exp(logScale);
        }
        
        return scales;
    }
    
    private static void analyzeResult(CWTResult result, double[] scales, double samplingRate) {
        Complex[][] coeffs = result.getCoefficients();
        int numScales = coeffs.length;
        int signalLength = coeffs[0].length;
        
        // Find maximum coefficient at middle time point
        int midPoint = signalLength / 2;
        double maxMag = 0;
        int maxScaleIdx = 0;
        
        for (int i = 0; i < numScales; i++) {
            double mag = coeffs[i][midPoint].getMag();
            if (mag > maxMag) {
                maxMag = mag;
                maxScaleIdx = i;
            }
        }
        
        System.out.printf("  Peak scale at t=%.3f s: %.1f (magnitude: %.2f)\n",
                        midPoint / samplingRate, scales[maxScaleIdx], maxMag);
    }
}