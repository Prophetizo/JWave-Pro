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

import jwave.operations.ComplexOperations;
import jwave.operations.ComplexOperationsFactory;
import jwave.operations.StandardComplexOperations;
import jwave.operations.SIMDComplexOperations;
import jwave.transforms.CWTResult;
import jwave.transforms.ContinuousWaveletTransform;
import jwave.transforms.FastFourierTransform;
import jwave.transforms.wavelets.continuous.MorletWavelet;

/**
 * Example demonstrating the use of ComplexOperations interface with CWT.
 * 
 * This example shows how to:
 * - Use dependency injection for complex operations
 * - Compare performance between standard and SIMD implementations
 * - Configure CWT with different optimization strategies
 * 
 * @author Stephen Romano
 */
public class ComplexOperationsCWTExample {
    
    public static void main(String[] args) {
        // Create test signal - chirp signal
        int signalLength = 4096;
        double samplingRate = 1000.0; // Hz
        double[] signal = createChirpSignal(signalLength, samplingRate, 10, 100);
        
        // Define scales for CWT
        double[] scales = ContinuousWaveletTransform.generateLogScales(1, 50, 30);
        
        // Create Morlet wavelet
        MorletWavelet wavelet = new MorletWavelet();
        
        System.out.println("=== Complex Operations CWT Example ===");
        System.out.println("Signal length: " + signalLength);
        System.out.println("Number of scales: " + scales.length);
        System.out.println();
        
        // Test 1: Standard ComplexOperations
        System.out.println("1. Using Standard ComplexOperations:");
        ComplexOperations standardOps = new StandardComplexOperations();
        ContinuousWaveletTransform cwtStandard = new ContinuousWaveletTransform(
            wavelet, 
            ContinuousWaveletTransform.PaddingType.SYMMETRIC,
            new FastFourierTransform(),
            standardOps
        );
        
        long startTime = System.currentTimeMillis();
        CWTResult resultStandard = cwtStandard.transformFFT(signal, scales, samplingRate);
        long standardTime = System.currentTimeMillis() - startTime;
        System.out.println("   Implementation: " + standardOps.getImplementationName());
        System.out.println("   Time: " + standardTime + " ms");
        System.out.println("   Optimized: " + standardOps.isOptimized());
        System.out.println();
        
        // Test 2: SIMD ComplexOperations
        System.out.println("2. Using SIMD ComplexOperations:");
        ComplexOperations simdOps = new SIMDComplexOperations();
        ContinuousWaveletTransform cwtSIMD = new ContinuousWaveletTransform(
            wavelet,
            ContinuousWaveletTransform.PaddingType.SYMMETRIC,
            new FastFourierTransform(),
            simdOps
        );
        
        startTime = System.currentTimeMillis();
        CWTResult resultSIMD = cwtSIMD.transformFFT(signal, scales, samplingRate);
        long simdTime = System.currentTimeMillis() - startTime;
        System.out.println("   Implementation: " + simdOps.getImplementationName());
        System.out.println("   Time: " + simdTime + " ms");
        System.out.println("   Optimized: " + simdOps.isOptimized());
        System.out.println();
        
        // Test 3: Using Factory default
        System.out.println("3. Using Factory Default:");
        ComplexOperations defaultOps = ComplexOperationsFactory.getDefault();
        ContinuousWaveletTransform cwtDefault = new ContinuousWaveletTransform(
            wavelet,
            ContinuousWaveletTransform.PaddingType.SYMMETRIC,
            new FastFourierTransform(),
            defaultOps
        );
        
        startTime = System.currentTimeMillis();
        CWTResult resultDefault = cwtDefault.transformFFT(signal, scales, samplingRate);
        long defaultTime = System.currentTimeMillis() - startTime;
        System.out.println("   Implementation: " + defaultOps.getImplementationName());
        System.out.println("   Time: " + defaultTime + " ms");
        System.out.println("   Optimized: " + defaultOps.isOptimized());
        System.out.println();
        
        // Performance comparison
        System.out.println("=== Performance Comparison ===");
        if (simdTime < standardTime) {
            double speedup = (double) standardTime / simdTime;
            System.out.printf("SIMD is %.2fx faster than Standard\n", speedup);
        } else {
            double speedup = (double) simdTime / standardTime;
            System.out.printf("Standard is %.2fx faster than SIMD\n", speedup);
        }
        
        // Verify results are similar
        System.out.println("\n=== Result Verification ===");
        double maxDiff = compareResults(resultStandard, resultSIMD);
        System.out.printf("Maximum difference between Standard and SIMD: %.2e\n", maxDiff);
        if (maxDiff < 1e-10) {
            System.out.println("Results are identical within numerical precision");
        }
        
        // Demonstrate factory configuration
        System.out.println("\n=== Factory Configuration ===");
        System.out.println("Current default: " + ComplexOperationsFactory.getDefault().getImplementationName());
        
        // Change default to standard
        ComplexOperationsFactory.setDefault(ComplexOperationsFactory.getStandard());
        System.out.println("After setDefault(Standard): " + ComplexOperationsFactory.getDefault().getImplementationName());
        
        // Reset to original
        ComplexOperationsFactory.resetDefault();
        System.out.println("After reset: " + ComplexOperationsFactory.getDefault().getImplementationName());
    }
    
    /**
     * Create a chirp signal (frequency sweep).
     */
    private static double[] createChirpSignal(int length, double samplingRate, 
                                              double startFreq, double endFreq) {
        double[] signal = new double[length];
        double dt = 1.0 / samplingRate;
        double chirpRate = (endFreq - startFreq) / (length * dt);
        
        for (int i = 0; i < length; i++) {
            double t = i * dt;
            double instantFreq = startFreq + chirpRate * t;
            signal[i] = Math.sin(2 * Math.PI * instantFreq * t);
        }
        
        return signal;
    }
    
    /**
     * Compare two CWT results and return maximum difference.
     */
    private static double compareResults(CWTResult result1, CWTResult result2) {
        double maxDiff = 0.0;
        
        for (int i = 0; i < result1.getCoefficients().length; i++) {
            for (int j = 0; j < result1.getCoefficients()[i].length; j++) {
                double diff = Math.abs(result1.getCoefficients()[i][j].getMag() - 
                                     result2.getCoefficients()[i][j].getMag());
                maxDiff = Math.max(maxDiff, diff);
            }
        }
        
        return maxDiff;
    }
}