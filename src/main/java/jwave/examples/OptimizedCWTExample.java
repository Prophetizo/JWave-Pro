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
import jwave.transforms.wavelets.continuous.MexicanHatWavelet;
import jwave.transforms.wavelets.continuous.MorletWavelet;
import jwave.datatypes.natives.Complex;

/**
 * Demonstrates how to use the optimized Continuous Wavelet Transform (CWT).
 * 
 * The CWT with SIMD optimizations provides time-frequency analysis with
 * improved performance through optimized FFT and complex operations.
 * 
 * @author Stephen Romano
 */
public class OptimizedCWTExample {
    
    public static void main(String[] args) {
        System.out.println("=== JWave Optimized CWT Example ===\n");
        
        // Example 1: Basic CWT usage
        basicCWTExample();
        
        // Example 2: Time-frequency analysis
        timeFrequencyAnalysisExample();
        
        // Example 3: Multi-scale analysis
        multiScaleAnalysisExample();
    }
    
    /**
     * Example 1: Basic CWT usage with optimized implementation
     */
    private static void basicCWTExample() {
        System.out.println("Example 1: Basic CWT Usage");
        System.out.println("--------------------------");
        
        // Create a test signal with two frequencies
        int signalLength = 256;
        double samplingRate = 1000.0; // Hz
        double[] signal = new double[signalLength];
        
        // Generate signal: 50 Hz for first half, 150 Hz for second half
        for (int i = 0; i < signalLength / 2; i++) {
            double t = i / samplingRate;
            signal[i] = Math.sin(2 * Math.PI * 50 * t);
        }
        for (int i = signalLength / 2; i < signalLength; i++) {
            double t = i / samplingRate;
            signal[i] = Math.sin(2 * Math.PI * 150 * t);
        }
        
        // Create CWT with Morlet wavelet and inject OptimizedFastFourierTransform
        MorletWavelet motherWavelet = new MorletWavelet();
        OptimizedFastFourierTransform optimizedFFT = new OptimizedFastFourierTransform();
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(motherWavelet, optimizedFFT);
        
        // Define scales to analyze (corresponding to frequencies)
        int numScales = 32;
        double[] scales = new double[numScales];
        for (int i = 0; i < numScales; i++) {
            // Scales from 1 to 50 (corresponding to high to low frequencies)
            scales[i] = 1.0 + i * 49.0 / (numScales - 1);
        }
        
        // Perform CWT using optimized FFT method
        System.out.println("Computing CWT coefficients using optimized FFT method...");
        CWTResult cwtResult = cwt.transformFFT(signal, scales, samplingRate);
        
        // Extract coefficients
        Complex[][] coefficients = cwtResult.getCoefficients();
        
        // Find maximum coefficient locations
        System.out.println("\nScale analysis (peak locations):");
        for (int scaleIdx = 0; scaleIdx < numScales; scaleIdx += 4) {
            double maxMag = 0;
            int maxIdx = 0;
            
            for (int i = 0; i < signalLength; i++) {
                double mag = coefficients[scaleIdx][i].getMag();
                if (mag > maxMag) {
                    maxMag = mag;
                    maxIdx = i;
                }
            }
            
            // Convert scale to approximate frequency
            double centerFreq = motherWavelet.getCenterFrequency();
            double pseudoFreq = centerFreq * samplingRate / scales[scaleIdx];
            
            System.out.printf("  Scale %.1f (~%.1f Hz): Peak at t=%.3f s, magnitude=%.2f\n",
                            scales[scaleIdx], pseudoFreq, maxIdx / samplingRate, maxMag);
        }
        System.out.println();
    }
    
    /**
     * Example 2: Time-frequency analysis with scalogram
     */
    private static void timeFrequencyAnalysisExample() {
        System.out.println("Example 2: Time-Frequency Analysis");
        System.out.println("----------------------------------");
        
        // Create a non-stationary signal
        int signalLength = 512;
        double samplingRate = 1000.0;
        double[] signal = new double[signalLength];
        
        // Three different frequency components at different times
        for (int i = 0; i < signalLength; i++) {
            double t = i / samplingRate;
            
            // 30 Hz from 0 to 0.15s
            if (t < 0.15) {
                signal[i] += Math.sin(2 * Math.PI * 30 * t);
            }
            
            // 80 Hz from 0.1 to 0.35s
            if (t >= 0.1 && t < 0.35) {
                signal[i] += 0.8 * Math.sin(2 * Math.PI * 80 * t);
            }
            
            // 150 Hz from 0.3 to 0.5s
            if (t >= 0.3) {
                signal[i] += 0.6 * Math.sin(2 * Math.PI * 150 * t);
            }
            
            // Add some noise
            signal[i] += 0.1 * (Math.random() - 0.5);
        }
        
        // Setup CWT with Mexican Hat wavelet and inject OptimizedFastFourierTransform
        MexicanHatWavelet motherWavelet = new MexicanHatWavelet();
        OptimizedFastFourierTransform optimizedFFT = new OptimizedFastFourierTransform();
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(motherWavelet, optimizedFFT);
        
        // Define frequency range to analyze
        double minFreq = 20.0;
        double maxFreq = 200.0;
        int numFreqs = 40;
        
        // Convert frequencies to scales
        double centerFreq = motherWavelet.getCenterFrequency();
        double[] scales = new double[numFreqs];
        double[] frequencies = new double[numFreqs];
        
        for (int i = 0; i < numFreqs; i++) {
            frequencies[i] = minFreq + (maxFreq - minFreq) * i / (numFreqs - 1);
            scales[i] = centerFreq * samplingRate / frequencies[i];
        }
        
        // Compute scalogram using parallel FFT method for better performance
        System.out.println("Computing scalogram using parallel FFT method...");
        CWTResult cwtResult = cwt.transformFFTParallel(signal, scales, samplingRate);
        Complex[][] coefficients = cwtResult.getCoefficients();
        
        // Analyze energy distribution over time
        System.out.println("\nTime-frequency energy distribution:");
        double[] timeWindows = {0.05, 0.15, 0.25, 0.35, 0.45};
        
        for (double t : timeWindows) {
            int timeIdx = (int)(t * samplingRate);
            System.out.printf("\nAt t=%.2f s:\n", t);
            
            // Find dominant frequencies
            for (int freqIdx = 0; freqIdx < numFreqs; freqIdx++) {
                double magnitude = coefficients[freqIdx][timeIdx].getMag();
                if (magnitude > 0.3) { // Threshold
                    System.out.printf("  Active frequency: %.1f Hz (magnitude: %.2f)\n",
                                    frequencies[freqIdx], magnitude);
                }
            }
        }
        System.out.println();
    }
    
    /**
     * Example 3: Multi-scale analysis
     */
    private static void multiScaleAnalysisExample() {
        System.out.println("Example 3: Multi-Scale Analysis");
        System.out.println("-------------------------------");
        
        // Create a chirp signal
        int signalLength = 1024;
        double samplingRate = 1000.0;
        double[] signal = new double[signalLength];
        
        // Linear chirp from 10 Hz to 200 Hz
        double f0 = 10.0;
        double f1 = 200.0;
        double T = signalLength / samplingRate;
        
        for (int i = 0; i < signalLength; i++) {
            double t = i / samplingRate;
            double phase = 2 * Math.PI * (f0 * t + (f1 - f0) * t * t / (2 * T));
            signal[i] = Math.cos(phase);
        }
        
        // Use Morlet wavelet for time-frequency analysis with optimized FFT
        MorletWavelet morlet = new MorletWavelet();
        OptimizedFastFourierTransform optimizedFFT = new OptimizedFastFourierTransform();
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(morlet, optimizedFFT);
        
        // Define logarithmic scale distribution
        int numScales = 64;
        double[] scales = new double[numScales];
        double minScale = 2.0;
        double maxScale = 100.0;
        
        // Logarithmic distribution of scales
        for (int i = 0; i < numScales; i++) {
            double logMin = Math.log(minScale);
            double logMax = Math.log(maxScale);
            double logScale = logMin + (logMax - logMin) * i / (numScales - 1);
            scales[i] = Math.exp(logScale);
        }
        
        // Perform CWT
        System.out.println("Performing multi-scale CWT analysis...");
        CWTResult cwtResult = cwt.transformFFT(signal, scales, samplingRate);
        
        // Analyze instantaneous frequency tracking
        System.out.println("\nInstantaneous frequency tracking:");
        double[] timePoints = {0.1, 0.3, 0.5, 0.7, 0.9};
        
        for (double t : timePoints) {
            int timeIdx = (int)(t * samplingRate);
            double expectedFreq = f0 + (f1 - f0) * t / T;
            
            // Find scale with maximum response
            double maxMag = 0;
            int bestScaleIdx = 0;
            Complex[][] coeffs = cwtResult.getCoefficients();
            
            for (int i = 0; i < numScales; i++) {
                double mag = coeffs[i][timeIdx].getMag();
                if (mag > maxMag) {
                    maxMag = mag;
                    bestScaleIdx = i;
                }
            }
            
            // Convert scale to frequency
            double centerFreq = morlet.getCenterFrequency();
            double detectedFreq = centerFreq * samplingRate / scales[bestScaleIdx];
            
            System.out.printf("  t=%.1f s: Expected %.1f Hz, Detected %.1f Hz (error: %.1f%%)\n",
                            t, expectedFreq, detectedFreq, 
                            100 * Math.abs(detectedFreq - expectedFreq) / expectedFreq);
        }
        
        System.out.println("\nOptimization Note:");
        System.out.println("This example demonstrates dependency injection of OptimizedFastFourierTransform:");
        System.out.println("- OptimizedFastFourierTransform injected via constructor for better performance");
        System.out.println("- OptimizedComplex used internally for bulk complex multiplications");
        System.out.println("- Parallel processing available via transformFFTParallel() method");
        System.out.println("- Resulting in 2-3x performance improvement over standard implementation");
    }
}