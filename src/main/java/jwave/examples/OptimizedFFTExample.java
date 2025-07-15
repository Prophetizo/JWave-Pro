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

import jwave.Transform;
import jwave.datatypes.natives.Complex;
import jwave.transforms.FastFourierTransform;
import jwave.transforms.OptimizedFastFourierTransform;

/**
 * Demonstrates how to use the SIMD-optimized Fast Fourier Transform.
 * 
 * The OptimizedFastFourierTransform provides 2-3x speedup over the standard
 * FFT through SIMD-friendly algorithms and loop unrolling.
 * 
 * @author Stephen Romano
 */
public class OptimizedFFTExample {
    
    public static void main(String[] args) {
        System.out.println("=== JWave Optimized FFT Example ===\n");
        
        // Example 1: Basic usage with real signals
        basicUsageExample();
        
        // Example 2: Complex signal processing
        complexSignalExample();
        
        // Example 3: Performance comparison
        performanceComparisonExample();
        
        // Example 4: Spectral analysis example
        spectralAnalysisExample();
    }
    
    /**
     * Example 1: Basic usage with real-valued signals
     */
    private static void basicUsageExample() {
        System.out.println("Example 1: Basic Usage with Real Signals");
        System.out.println("----------------------------------------");
        
        // Create a test signal (sum of two sinusoids)
        int signalLength = 1024;
        double samplingRate = 1000.0; // Hz
        double[] signal = new double[signalLength];
        
        // Generate signal: 50 Hz + 120 Hz sinusoids
        for (int i = 0; i < signalLength; i++) {
            double t = i / samplingRate;
            signal[i] = Math.sin(2 * Math.PI * 50 * t) + 
                       0.5 * Math.sin(2 * Math.PI * 120 * t);
        }
        
        // Method 1: Using Transform wrapper (recommended for consistency)
        Transform transform = new Transform(new OptimizedFastFourierTransform());
        double[] spectrum = transform.forward(signal);
        double[] reconstructed = transform.reverse(spectrum);
        
        // Verify reconstruction
        double error = 0.0;
        for (int i = 0; i < signalLength; i++) {
            error += Math.pow(signal[i] - reconstructed[i], 2);
        }
        System.out.printf("Reconstruction error: %.2e\n", Math.sqrt(error / signalLength));
        
        // Method 2: Direct usage (for more control)
        OptimizedFastFourierTransform fft = new OptimizedFastFourierTransform();
        
        // Convert to complex for direct FFT usage
        Complex[] complexSignal = new Complex[signalLength];
        for (int i = 0; i < signalLength; i++) {
            complexSignal[i] = new Complex(signal[i], 0);
        }
        
        // Forward FFT
        Complex[] complexSpectrum = fft.forward(complexSignal);
        
        // Find dominant frequencies
        System.out.println("\nDominant frequencies:");
        for (int i = 0; i < signalLength / 2; i++) {
            double magnitude = complexSpectrum[i].getMag();
            if (magnitude > 100) { // Threshold for significant peaks
                double frequency = i * samplingRate / signalLength;
                System.out.printf("  %.1f Hz (magnitude: %.1f)\n", frequency, magnitude);
            }
        }
        
        System.out.println();
    }
    
    /**
     * Example 2: Complex signal processing
     */
    private static void complexSignalExample() {
        System.out.println("Example 2: Complex Signal Processing");
        System.out.println("------------------------------------");
        
        // Create optimized FFT instance
        OptimizedFastFourierTransform fft = new OptimizedFastFourierTransform();
        
        // Generate a complex signal (e.g., from I/Q data)
        int length = 512;
        Complex[] complexSignal = new Complex[length];
        
        // Create a complex exponential with linear frequency sweep (chirp)
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double phase = 2 * Math.PI * (10 * t + 20 * t * t); // Linear chirp
            complexSignal[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }
        
        // Apply window function (Hamming window)
        for (int i = 0; i < length; i++) {
            double window = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (length - 1));
            complexSignal[i] = complexSignal[i].mul(window);
        }
        
        // Forward FFT
        Complex[] spectrum = fft.forward(complexSignal);
        
        // Apply frequency domain filter (low-pass)
        int cutoff = length / 4;
        for (int i = cutoff; i < length - cutoff; i++) {
            spectrum[i] = new Complex(0, 0);
        }
        
        // Inverse FFT
        Complex[] filtered = fft.reverse(spectrum);
        
        System.out.println("Applied frequency domain filtering:");
        System.out.printf("  Original signal energy: %.2f\n", calculateEnergy(complexSignal));
        System.out.printf("  Filtered signal energy: %.2f\n", calculateEnergy(filtered));
        System.out.println();
    }
    
    /**
     * Example 3: Performance comparison between standard and optimized FFT
     */
    private static void performanceComparisonExample() {
        System.out.println("Example 3: Performance Comparison");
        System.out.println("---------------------------------");
        
        // Test different signal sizes
        int[] sizes = {256, 1024, 4096, 16384};
        int warmupRuns = 100;
        int benchmarkRuns = 1000;
        
        for (int size : sizes) {
            // Generate test signal
            double[] signal = new double[size];
            for (int i = 0; i < size; i++) {
                signal[i] = Math.random();
            }
            
            // Standard FFT
            Transform standardTransform = new Transform(new FastFourierTransform());
            
            // Warmup
            for (int i = 0; i < warmupRuns; i++) {
                standardTransform.forward(signal);
            }
            
            // Benchmark standard FFT
            long standardTime = System.nanoTime();
            for (int i = 0; i < benchmarkRuns; i++) {
                standardTransform.forward(signal);
            }
            standardTime = System.nanoTime() - standardTime;
            
            // Optimized FFT
            Transform optimizedTransform = new Transform(new OptimizedFastFourierTransform());
            
            // Warmup
            for (int i = 0; i < warmupRuns; i++) {
                optimizedTransform.forward(signal);
            }
            
            // Benchmark optimized FFT
            long optimizedTime = System.nanoTime();
            for (int i = 0; i < benchmarkRuns; i++) {
                optimizedTransform.forward(signal);
            }
            optimizedTime = System.nanoTime() - optimizedTime;
            
            // Calculate speedup
            double speedup = (double) standardTime / optimizedTime;
            double standardMs = standardTime / 1_000_000.0 / benchmarkRuns;
            double optimizedMs = optimizedTime / 1_000_000.0 / benchmarkRuns;
            
            System.out.printf("Size %5d: Standard: %.3f ms, Optimized: %.3f ms, Speedup: %.2fx\n",
                            size, standardMs, optimizedMs, speedup);
        }
        System.out.println();
    }
    
    /**
     * Example 4: Spectral analysis with optimized FFT
     */
    private static void spectralAnalysisExample() {
        System.out.println("Example 4: Spectral Analysis");
        System.out.println("----------------------------");
        
        // Signal parameters
        int signalLength = 2048;
        double samplingRate = 8000.0; // Hz
        
        // Generate a multi-component signal
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            double t = i / samplingRate;
            signal[i] = 1.0 * Math.sin(2 * Math.PI * 440 * t)    // A4 note
                      + 0.5 * Math.sin(2 * Math.PI * 554 * t)    // C#5 note
                      + 0.3 * Math.sin(2 * Math.PI * 659 * t)    // E5 note
                      + 0.1 * (Math.random() - 0.5);             // Noise
        }
        
        // Use optimized FFT for spectral analysis
        OptimizedFastFourierTransform fft = new OptimizedFastFourierTransform();
        
        // Convert to complex
        Complex[] complexSignal = new Complex[signalLength];
        for (int i = 0; i < signalLength; i++) {
            complexSignal[i] = new Complex(signal[i], 0);
        }
        
        // Compute spectrum
        Complex[] spectrum = fft.forward(complexSignal);
        
        // Calculate power spectral density
        double[] psd = new double[signalLength / 2];
        for (int i = 0; i < psd.length; i++) {
            psd[i] = spectrum[i].getMag() * spectrum[i].getMag() / signalLength;
        }
        
        // Find peaks
        System.out.println("Detected frequency peaks:");
        double threshold = 10.0; // Minimum power for peak detection
        
        for (int i = 1; i < psd.length - 1; i++) {
            if (psd[i] > threshold && psd[i] > psd[i-1] && psd[i] > psd[i+1]) {
                double frequency = i * samplingRate / signalLength;
                double power = 10 * Math.log10(psd[i]); // Convert to dB
                System.out.printf("  %.1f Hz (%.1f dB)\n", frequency, power);
            }
        }
        
        // Calculate total harmonic distortion (THD)
        double fundamentalPower = psd[55]; // ~440 Hz bin
        double harmonicPower = 0;
        for (int harmonic = 2; harmonic <= 5; harmonic++) {
            int bin = 55 * harmonic;
            if (bin < psd.length) {
                harmonicPower += psd[bin];
            }
        }
        double thd = 100 * Math.sqrt(harmonicPower / fundamentalPower);
        System.out.printf("\nTotal Harmonic Distortion: %.2f%%\n", thd);
    }
    
    /**
     * Helper method to calculate signal energy
     */
    private static double calculateEnergy(Complex[] signal) {
        double energy = 0;
        for (Complex c : signal) {
            energy += c.getMag() * c.getMag();
        }
        return energy;
    }
}