/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
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

import jwave.transforms.wavelets.continuous.*;
import jwave.transforms.ContinuousWaveletTransform;
import jwave.transforms.CWTResult;
import jwave.datatypes.natives.Complex;

/**
 * Example demonstrating the Paul wavelet and its properties compared
 * to other continuous wavelets.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class PaulWaveletExample {

    public static void main(String[] args) {
        System.out.println("=== Paul Wavelet Demonstration ===\n");
        
        demonstrateWaveletProperties();
        demonstrateFrequencyAnalysis();
        demonstratePhaseAnalysis();
        compareWavelets();
    }
    
    /**
     * Demonstrate basic properties of Paul wavelet.
     */
    private static void demonstrateWaveletProperties() {
        System.out.println("1. Paul Wavelet Properties:");
        System.out.println("--------------------------");
        
        // Create Paul wavelets with different orders
        int[] orders = {2, 4, 6, 8};
        
        for (int m : orders) {
            PaulWavelet paul = new PaulWavelet(m);
            
            System.out.printf("\nPaul wavelet (m=%d):\n", m);
            System.out.printf("  Center frequency: %.3f Hz\n", paul.getCenterFrequency());
            System.out.printf("  Admissibility constant: %.3f\n", paul.getAdmissibilityConstant());
            
            double[] support = paul.getEffectiveSupport();
            System.out.printf("  Effective support: [%.1f, %.1f]\n", support[0], support[1]);
            
            double[] bandwidth = paul.getBandwidth();
            System.out.printf("  Bandwidth: [%.3f, %.3f] Hz\n", bandwidth[0], bandwidth[1]);
        }
    }
    
    /**
     * Demonstrate frequency analysis capabilities.
     */
    private static void demonstrateFrequencyAnalysis() {
        System.out.println("\n2. Frequency Analysis with Paul Wavelet:");
        System.out.println("---------------------------------------");
        
        // Create a signal with time-varying frequency
        int n = 512;
        double samplingRate = 256.0; // Hz
        double[] signal = new double[n];
        
        // Create chirp signal (frequency increases from 10 Hz to 50 Hz)
        for (int i = 0; i < n; i++) {
            double t = i / samplingRate;
            double f_instantaneous = 10 + (40 * t / 2.0); // Linear frequency sweep
            double phase = 2 * Math.PI * (10 * t + 20 * t * t);
            signal[i] = Math.sin(phase);
        }
        
        // Analyze with Paul wavelet (high frequency resolution)
        PaulWavelet paul = new PaulWavelet(8); // High order for frequency resolution
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(paul);
        
        // Generate scales corresponding to frequencies 5-60 Hz
        double[] frequencies = new double[20];
        for (int i = 0; i < 20; i++) {
            frequencies[i] = 5 + i * 55.0 / 19.0;
        }
        
        // Convert frequencies to scales
        double[] scales = new double[frequencies.length];
        double fc = paul.getCenterFrequency();
        for (int i = 0; i < frequencies.length; i++) {
            scales[i] = fc * samplingRate / frequencies[i];
        }
        
        // Perform CWT
        long start = System.nanoTime();
        CWTResult result = cwt.transformFFT(signal, scales, samplingRate);
        long elapsed = System.nanoTime() - start;
        
        System.out.printf("\nCWT computation time: %.2f ms\n", elapsed / 1_000_000.0);
        System.out.println("Signal: Linear chirp from 10 Hz to 50 Hz");
        System.out.println("Paul wavelet order: 8 (optimized for frequency resolution)");
        
        // Find ridge (maximum magnitude at each time)
        double[][] magnitude = result.getMagnitude();
        System.out.println("\nInstantaneous frequency detection:");
        
        // Sample at a few time points
        int[] timeIndices = {64, 128, 192, 256, 320, 384, 448};
        for (int tidx : timeIndices) {
            double maxMag = 0;
            int maxScale = 0;
            for (int s = 0; s < scales.length; s++) {
                if (magnitude[s][tidx] > maxMag) {
                    maxMag = magnitude[s][tidx];
                    maxScale = s;
                }
            }
            double detectedFreq = frequencies[maxScale];
            double t = tidx / samplingRate;
            double expectedFreq = 10 + (40 * t / 2.0);  // Must match line 85
            System.out.printf("  t=%.2f s: detected=%.1f Hz, expected=%.1f Hz\n",
                            t, detectedFreq, expectedFreq);
        }
    }
    
    /**
     * Demonstrate phase analysis capabilities.
     */
    private static void demonstratePhaseAnalysis() {
        System.out.println("\n3. Phase Analysis with Paul Wavelet:");
        System.out.println("-----------------------------------");
        
        // Create signal with phase jump
        int n = 256;
        double[] signal = new double[n];
        
        // First half: cos(2*pi*20*t)
        // Second half: cos(2*pi*20*t + pi/2) = -sin(2*pi*20*t)
        for (int i = 0; i < n/2; i++) {
            double t = i / 256.0;
            signal[i] = Math.cos(2 * Math.PI * 20 * t);
        }
        for (int i = n/2; i < n; i++) {
            double t = i / 256.0;
            signal[i] = Math.cos(2 * Math.PI * 20 * t + Math.PI / 2);
        }
        
        // Analyze with Paul wavelet
        PaulWavelet paul = new PaulWavelet(4);
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(paul);
        
        // Use scale corresponding to 20 Hz
        double fc = paul.getCenterFrequency();
        double scale = fc * 256.0 / 20.0;
        double[] scales = {scale};
        
        CWTResult result = cwt.transformFFT(signal, scales, 256.0);
        Complex[][] coeffs = result.getCoefficients();
        
        System.out.println("\nSignal: 20 Hz with π/2 phase jump at t=0.5s");
        System.out.println("Phase analysis around the jump:");
        
        // Check phase around the jump
        int jumpIndex = n/2;
        for (int i = jumpIndex - 5; i <= jumpIndex + 5; i++) {
            double phase = Math.atan2(coeffs[0][i].getImag(), coeffs[0][i].getReal());
            System.out.printf("  t=%.3f s: phase=%.2f rad (%.1f°)\n", 
                            i / 256.0, phase, phase * 180 / Math.PI);
        }
    }
    
    /**
     * Compare Paul wavelet with Morlet and Mexican Hat.
     */
    private static void compareWavelets() {
        System.out.println("\n4. Wavelet Comparison:");
        System.out.println("---------------------");
        
        // Create test signal with multiple components
        int n = 512;
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / 512.0;
            signal[i] = Math.sin(2 * Math.PI * 10 * t) +      // 10 Hz
                       0.5 * Math.sin(2 * Math.PI * 25 * t) + // 25 Hz  
                       0.3 * Math.sin(2 * Math.PI * 40 * t);  // 40 Hz
        }
        
        // Create wavelets
        PaulWavelet paul = new PaulWavelet(4);
        MorletWavelet morlet = new MorletWavelet(1.0, 1.0);
        MexicanHatWavelet mexican = new MexicanHatWavelet(1.0);
        
        // Test each wavelet
        ContinuousWavelet[] wavelets = {paul, morlet, mexican};
        String[] names = {"Paul (m=4)", "Morlet", "Mexican Hat"};
        
        double[] scales = ContinuousWaveletTransform.generateLogScales(0.5, 5.0, 30);
        
        System.out.println("\nPerformance comparison (512 points, 30 scales):");
        
        for (int i = 0; i < wavelets.length; i++) {
            ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(wavelets[i]);
            
            long start = System.nanoTime();
            CWTResult result = cwt.transformFFT(signal, scales, 512.0);
            long elapsed = System.nanoTime() - start;
            
            // Check if complex or real valued
            boolean isComplex = false;
            Complex[][] coeffs = result.getCoefficients();
            for (int j = 0; j < Math.min(10, coeffs[0].length); j++) {
                if (Math.abs(coeffs[0][j].getImag()) > 1e-10) {
                    isComplex = true;
                    break;
                }
            }
            
            System.out.printf("  %-15s: %.2f ms, %s-valued, center freq=%.3f Hz\n",
                            names[i], 
                            elapsed / 1_000_000.0,
                            isComplex ? "complex" : "real",
                            wavelets[i].getCenterFrequency());
        }
        
        System.out.println("\nKey differences:");
        System.out.println("  - Paul: Complex, excellent frequency resolution, analytic (no negative frequencies)");
        System.out.println("  - Morlet: Complex, balanced time-frequency resolution, approximately analytic");
        System.out.println("  - Mexican Hat: Real, good time resolution, symmetric");
    }
}