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
import jwave.transforms.FastFourierTransform;
import jwave.transforms.OptimizedFastFourierTransform;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.WaveletOperations;
import jwave.transforms.wavelets.StandardWaveletOperations;
import jwave.transforms.wavelets.OptimizedWaveletOperations;

/**
 * Demonstrates dependency injection patterns in MODWTTransform.
 * 
 * This example shows how to inject different implementations of
 * FFT and WaveletOperations for optimal performance.
 * 
 * @author Stephen Romano
 */
public class MODWTDependencyInjectionExample {
    
    public static void main(String[] args) {
        System.out.println("=== MODWT Dependency Injection Example ===\n");
        
        // Create test signal
        int signalLength = 256;
        double[] signal = createTestSignal(signalLength);
        
        // Example 1: Default configuration
        defaultExample(signal);
        
        // Example 2: Inject optimized FFT only
        optimizedFFTExample(signal);
        
        // Example 3: Inject optimized wavelet operations only
        optimizedWaveletOpsExample(signal);
        
        // Example 4: Fully optimized with both injections
        fullyOptimizedExample(signal);
        
        // Example 5: Performance comparison
        performanceComparison(signal);
    }
    
    /**
     * Example 1: Default MODWT configuration
     */
    private static void defaultExample(double[] signal) {
        System.out.println("Example 1: Default Configuration");
        System.out.println("--------------------------------");
        
        // Create MODWT with default implementations
        Daubechies4 wavelet = new Daubechies4();
        MODWTTransform modwt = new MODWTTransform(wavelet);
        
        // Perform transform
        int levels = 3;
        double[][] coeffs = modwt.forwardMODWT(signal, levels);
        
        System.out.println("Transform completed with:");
        System.out.println("  - FFT: FastFourierTransform (default)");
        System.out.println("  - WaveletOps: StandardWaveletOperations (default)");
        System.out.println("  - Levels: " + levels);
        System.out.println("  - Signal length: " + signal.length);
        System.out.println();
    }
    
    /**
     * Example 2: Inject optimized FFT
     */
    private static void optimizedFFTExample(double[] signal) {
        System.out.println("Example 2: Optimized FFT Injection");
        System.out.println("----------------------------------");
        
        // Create MODWT with optimized FFT
        Daubechies4 wavelet = new Daubechies4();
        OptimizedFastFourierTransform optimizedFFT = new OptimizedFastFourierTransform();
        MODWTTransform modwt = new MODWTTransform(wavelet, optimizedFFT);
        
        // Configure to use FFT method
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        
        // Perform transform
        int levels = 3;
        double[][] coeffs = modwt.forwardMODWT(signal, levels);
        
        System.out.println("Transform completed with:");
        System.out.println("  - FFT: OptimizedFastFourierTransform (injected)");
        System.out.println("  - WaveletOps: StandardWaveletOperations (default)");
        System.out.println("  - Convolution method: FFT");
        System.out.println();
    }
    
    /**
     * Example 3: Inject optimized wavelet operations
     */
    private static void optimizedWaveletOpsExample(double[] signal) {
        System.out.println("Example 3: Optimized WaveletOperations Injection");
        System.out.println("------------------------------------------------");
        
        // Create MODWT with optimized wavelet operations
        Daubechies4 wavelet = new Daubechies4();
        FastFourierTransform standardFFT = new FastFourierTransform();
        OptimizedWaveletOperations optimizedOps = new OptimizedWaveletOperations();
        MODWTTransform modwt = new MODWTTransform(wavelet, standardFFT, optimizedOps);
        
        // Configure to use direct convolution
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.DIRECT);
        
        // Perform transform
        int levels = 3;
        double[][] coeffs = modwt.forwardMODWT(signal, levels);
        
        System.out.println("Transform completed with:");
        System.out.println("  - FFT: FastFourierTransform (standard)");
        System.out.println("  - WaveletOps: OptimizedWaveletOperations (injected)");
        System.out.println("  - Convolution method: DIRECT");
        System.out.println();
    }
    
    /**
     * Example 4: Fully optimized configuration
     */
    private static void fullyOptimizedExample(double[] signal) {
        System.out.println("Example 4: Fully Optimized Configuration");
        System.out.println("----------------------------------------");
        
        // Create MODWT with all optimizations
        Daubechies4 wavelet = new Daubechies4();
        OptimizedFastFourierTransform optimizedFFT = new OptimizedFastFourierTransform();
        OptimizedWaveletOperations optimizedOps = new OptimizedWaveletOperations();
        MODWTTransform modwt = new MODWTTransform(wavelet, optimizedFFT, optimizedOps);
        
        // Let MODWT choose the best method automatically
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.AUTO);
        
        // Perform transform
        int levels = 3;
        double[][] coeffs = modwt.forwardMODWT(signal, levels);
        
        System.out.println("Transform completed with:");
        System.out.println("  - FFT: OptimizedFastFourierTransform (injected)");
        System.out.println("  - WaveletOps: OptimizedWaveletOperations (injected)");
        System.out.println("  - Convolution method: AUTO");
        System.out.println();
    }
    
    /**
     * Example 5: Performance comparison
     */
    private static void performanceComparison(double[] signal) {
        System.out.println("Example 5: Performance Comparison");
        System.out.println("---------------------------------");
        
        int levels = 4;
        int warmupRuns = 10;
        int benchmarkRuns = 100;
        
        // Test different configurations
        Haar1 wavelet = new Haar1(); // Simple wavelet for clear comparison
        
        // Configuration 1: All defaults
        MODWTTransform defaultModwt = new MODWTTransform(wavelet);
        
        // Configuration 2: Optimized FFT only
        MODWTTransform fftOptModwt = new MODWTTransform(wavelet, 
            new OptimizedFastFourierTransform());
        
        // Configuration 3: Optimized WaveletOps only
        MODWTTransform opsOptModwt = new MODWTTransform(wavelet,
            new FastFourierTransform(), new OptimizedWaveletOperations());
        
        // Configuration 4: Fully optimized
        MODWTTransform fullOptModwt = new MODWTTransform(wavelet,
            new OptimizedFastFourierTransform(), new OptimizedWaveletOperations());
        
        // Run benchmarks
        System.out.println("Benchmarking different configurations...");
        System.out.println("Signal length: " + signal.length + ", Levels: " + levels);
        System.out.println();
        
        // Benchmark each configuration
        long defaultTime = benchmark(defaultModwt, signal, levels, warmupRuns, benchmarkRuns);
        long fftOptTime = benchmark(fftOptModwt, signal, levels, warmupRuns, benchmarkRuns);
        long opsOptTime = benchmark(opsOptModwt, signal, levels, warmupRuns, benchmarkRuns);
        long fullOptTime = benchmark(fullOptModwt, signal, levels, warmupRuns, benchmarkRuns);
        
        // Display results
        System.out.printf("%-30s: %6.2f ms (baseline)\n", 
            "Default configuration", defaultTime / 1_000_000.0);
        System.out.printf("%-30s: %6.2f ms (%.1fx speedup)\n", 
            "Optimized FFT only", fftOptTime / 1_000_000.0, 
            (double)defaultTime / fftOptTime);
        System.out.printf("%-30s: %6.2f ms (%.1fx speedup)\n", 
            "Optimized WaveletOps only", opsOptTime / 1_000_000.0, 
            (double)defaultTime / opsOptTime);
        System.out.printf("%-30s: %6.2f ms (%.1fx speedup)\n", 
            "Fully optimized", fullOptTime / 1_000_000.0, 
            (double)defaultTime / fullOptTime);
        
        System.out.println("\nDependency Injection Benefits:");
        System.out.println("- Flexible configuration without changing core code");
        System.out.println("- Easy performance testing and optimization");
        System.out.println("- Backward compatibility maintained");
        System.out.println("- Clean separation of concerns");
    }
    
    /**
     * Benchmark a MODWT configuration
     */
    private static long benchmark(MODWTTransform modwt, double[] signal, int levels,
                                  int warmupRuns, int benchmarkRuns) {
        // Warmup
        for (int i = 0; i < warmupRuns; i++) {
            modwt.forwardMODWT(signal, levels);
        }
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < benchmarkRuns; i++) {
            modwt.forwardMODWT(signal, levels);
        }
        long endTime = System.nanoTime();
        
        return (endTime - startTime) / benchmarkRuns;
    }
    
    /**
     * Create a test signal with multiple frequency components
     */
    private static double[] createTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.cos(4 * Math.PI * i / 32) +
                       0.25 * Math.sin(8 * Math.PI * i / 32);
        }
        return signal;
    }
}