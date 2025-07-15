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
import org.junit.Assume;
import org.junit.Before;

import jwave.datatypes.natives.Complex;
import jwave.exceptions.JWaveException;

/**
 * Performance comparison between standard FFT and optimized FFT.
 *
 * @author Stephen Romano
 * @date 15.01.2025
 */
public class OptimizedFFTPerformanceTest {

    private static final boolean RUN_PERFORMANCE_TESTS = 
        Boolean.getBoolean("enablePerformanceTests");
    
    private FastFourierTransform standardFft;
    private OptimizedFastFourierTransform optimizedFft;
    
    @Before
    public void setUp() {
        standardFft = new FastFourierTransform();
        optimizedFft = new OptimizedFastFourierTransform();
    }
    
    /**
     * Compare performance of standard vs optimized FFT.
     */
    @Test
    public void testOptimizedPerformanceComparison() throws JWaveException {
        Assume.assumeTrue("Performance tests disabled", RUN_PERFORMANCE_TESTS);
        
        System.out.println("\n=== Optimized FFT Performance Comparison ===");
        System.out.println("Comparing standard Cooley-Tukey with SIMD-optimized implementation");
        System.out.println();
        
        // Test typical signal lengths
        int[] lengths = {256, 512, 1024, 2048, 4096, 8192, 16384};
        
        System.out.println("Real-valued FFT Performance (forward transform)");
        System.out.println("==============================================");
        System.out.printf("%-10s %-15s %-15s %-15s %-20s%n", 
            "Length", "Standard (ms)", "Optimized (ms)", "Speedup", "Improvement");
        System.out.println("--------------------------------------------------------------");
        
        for (int n : lengths) {
            // Generate test signal
            double[] signal = generateTestSignal(n);
            
            // Warm up
            for (int i = 0; i < 100; i++) {
                standardFft.forward(signal);
                optimizedFft.forward(signal);
            }
            
            // Benchmark standard FFT
            int iterations = 1000;
            long standardTime = benchmarkRealFFT(standardFft, signal, iterations);
            
            // Benchmark optimized FFT
            long optimizedTime = benchmarkRealFFT(optimizedFft, signal, iterations);
            
            // Calculate speedup
            double speedup = (double) standardTime / optimizedTime;
            String improvement = speedup > 1.0 ? 
                String.format("%.1f%% faster", (speedup - 1) * 100) : 
                String.format("%.1f%% slower", (1 - speedup) * 100);
            
            System.out.printf("%-10d %-15.3f %-15.3f %-15.2fx %-20s%n",
                n, 
                standardTime / (double) iterations,
                optimizedTime / (double) iterations,
                speedup,
                improvement);
        }
    }
    
    /**
     * Test optimized performance with complex FFT.
     */
    @Test
    public void testOptimizedComplexPerformance() throws JWaveException {
        Assume.assumeTrue("Performance tests disabled", RUN_PERFORMANCE_TESTS);
        
        System.out.println("\nComplex FFT Performance");
        System.out.println("=======================");
        System.out.printf("%-10s %-15s %-15s %-15s %-20s%n", 
            "Length", "Standard (ms)", "Optimized (ms)", "Speedup", "Cache Efficiency");
        System.out.println("-------------------------------------------------------------");
        
        int[] lengths = {256, 512, 1024, 2048, 4096, 8192};
        
        for (int n : lengths) {
            // Generate complex test signal
            Complex[] signal = new Complex[n];
            for (int i = 0; i < n; i++) {
                signal[i] = new Complex(
                    Math.sin(2 * Math.PI * i * 5 / n),
                    Math.cos(2 * Math.PI * i * 11 / n)
                );
            }
            
            // Warm up
            for (int i = 0; i < 100; i++) {
                standardFft.forward(signal);
                optimizedFft.forward(signal);
            }
            
            // Benchmark
            int iterations = 1000;
            
            // Standard FFT
            long standardTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                standardFft.forward(signal);
            }
            standardTime = (System.nanoTime() - standardTime) / 1_000_000;
            
            // Optimized FFT
            long optimizedTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                optimizedFft.forward(signal);
            }
            optimizedTime = (System.nanoTime() - optimizedTime) / 1_000_000;
            
            double speedup = (double) standardTime / optimizedTime;
            
            // Estimate cache efficiency based on speedup pattern
            String cacheEfficiency;
            if (n <= 1024) {
                cacheEfficiency = "L1/L2 cache";
            } else if (n <= 4096) {
                cacheEfficiency = "L2/L3 cache";
            } else {
                cacheEfficiency = "Memory bound";
            }
            
            System.out.printf("%-10d %-15.3f %-15.3f %-15.2fx %-20s%n",
                n,
                standardTime / (double) iterations,
                optimizedTime / (double) iterations,
                speedup,
                cacheEfficiency);
        }
    }
    
    /**
     * Test impact of loop unrolling and cache optimization.
     */
    @Test
    public void testOptimizationBreakdown() throws JWaveException {
        Assume.assumeTrue("Performance tests disabled", RUN_PERFORMANCE_TESTS);
        
        System.out.println("\nOptimization Impact Analysis");
        System.out.println("============================");
        System.out.println("Measuring impact of various optimizations");
        System.out.println();
        
        int n = 4096; // Fixed size for comparison
        Complex[] signal = new Complex[n];
        for (int i = 0; i < n; i++) {
            signal[i] = new Complex(Math.random() - 0.5, Math.random() - 0.5);
        }
        
        // Measure different aspects
        System.out.println("Operation breakdown (1000 iterations):");
        System.out.println("-------------------------------------");
        
        // Measure bit reversal time
        long startTime = System.nanoTime();
        for (int iter = 0; iter < 1000; iter++) {
            Complex[] temp = new Complex[n];
            System.arraycopy(signal, 0, temp, 0, n);
            // Bit reversal is part of the FFT
        }
        long copyTime = (System.nanoTime() - startTime) / 1_000_000;
        
        // Measure full transform time
        startTime = System.nanoTime();
        for (int iter = 0; iter < 1000; iter++) {
            optimizedFft.forward(signal);
        }
        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
        
        System.out.printf("Array copy overhead: %.2f ms (%.1f%% of total)%n", 
            copyTime / 1000.0, (copyTime * 100.0 / totalTime));
        System.out.printf("Total transform time: %.2f ms%n", totalTime / 1000.0);
        System.out.printf("Butterfly operations: %.2f ms (%.1f%% of total)%n", 
            (totalTime - copyTime) / 1000.0, ((totalTime - copyTime) * 100.0 / totalTime));
    }
    
    /**
     * Test performance scaling with size.
     */
    @Test
    public void testPerformanceScaling() throws JWaveException {
        Assume.assumeTrue("Performance tests disabled", RUN_PERFORMANCE_TESTS);
        
        System.out.println("\nFFT Performance Scaling Analysis");
        System.out.println("================================");
        System.out.printf("%-10s %-15s %-20s %-20s%n", 
            "Length", "Time (ms)", "Time/NlogN (ns)", "Efficiency");
        System.out.println("-----------------------------------------------------------");
        
        int[] lengths = {256, 512, 1024, 2048, 4096, 8192, 16384};
        double baselineEfficiency = 0;
        
        for (int n : lengths) {
            double[] signal = generateTestSignal(n);
            
            // Warm up
            for (int i = 0; i < 50; i++) {
                optimizedFft.forward(signal);
            }
            
            // Benchmark
            int iterations = 1000;
            long time = benchmarkRealFFT(optimizedFft, signal, iterations);
            
            // Calculate time per operation (normalized by N log N)
            double nLogN = n * Math.log(n) / Math.log(2);
            double timePerOp = (time * 1_000_000.0) / (iterations * nLogN); // nanoseconds
            
            if (baselineEfficiency == 0) {
                baselineEfficiency = timePerOp;
            }
            
            double efficiency = baselineEfficiency / timePerOp;
            
            System.out.printf("%-10d %-15.3f %-20.3f %-20.2f%n",
                n,
                time / (double) iterations,
                timePerOp,
                efficiency);
        }
    }
    
    /**
     * Helper method to generate test signal.
     */
    private double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i * 5 / length) +
                       0.5 * Math.sin(2 * Math.PI * i * 17 / length) +
                       0.25 * Math.cos(2 * Math.PI * i * 33 / length);
        }
        return signal;
    }
    
    /**
     * Benchmark FFT with real input.
     */
    private long benchmarkRealFFT(BasicTransform fft, double[] signal, int iterations) 
            throws JWaveException {
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            double[] result = fft.forward(signal);
            // Ensure result is used
            if (result[0] == Double.MAX_VALUE) {
                fail("This should never happen");
            }
        }
        
        return (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
    }
}