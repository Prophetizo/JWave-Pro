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
 * Example demonstrating the DOG (Derivative of Gaussian) wavelet and its
 * applications in edge detection, singularity analysis, and multi-scale
 * feature extraction.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class DOGWaveletExample {

    public static void main(String[] args) {
        System.out.println("=== DOG Wavelet Demonstration ===\n");
        
        demonstrateWaveletProperties();
        demonstrateEdgeDetection();
        demonstrateSingularityAnalysis();
        compareDifferentOrders();
        demonstrateMexicanHatEquivalence();
    }
    
    /**
     * Demonstrate basic properties of DOG wavelets.
     */
    private static void demonstrateWaveletProperties() {
        System.out.println("1. DOG Wavelet Properties:");
        System.out.println("-------------------------");
        
        // Create DOG wavelets with different orders
        int[] orders = {1, 2, 3, 4};
        double sigma = 1.0;
        
        for (int n : orders) {
            DOGWavelet dog = new DOGWavelet(n, sigma);
            
            System.out.printf("\nDOG wavelet (n=%d, σ=%.1f):\n", n, sigma);
            System.out.printf("  Center frequency: %.3f Hz\n", dog.getCenterFrequency());
            System.out.printf("  Admissibility constant: %.3f\n", dog.getAdmissibilityConstant());
            
            double[] support = dog.getEffectiveSupport();
            System.out.printf("  Effective support: [%.1f, %.1f]\n", support[0], support[1]);
            
            double[] bandwidth = dog.getBandwidth();
            System.out.printf("  Bandwidth: [%.3f, %.3f] Hz\n", bandwidth[0], bandwidth[1]);
            
            // Check symmetry
            String symmetry = (n % 2 == 0) ? "symmetric" : "antisymmetric";
            System.out.printf("  Symmetry: %s\n", symmetry);
            
            // Sample values
            Complex val0 = dog.wavelet(0);
            Complex val1 = dog.wavelet(1);
            System.out.printf("  ψ(0) = %.4f, ψ(1) = %.4f\n", val0.getReal(), val1.getReal());
        }
    }
    
    /**
     * Demonstrate edge detection using DOG n=1.
     */
    private static void demonstrateEdgeDetection() {
        System.out.println("\n2. Edge Detection with DOG (n=1):");
        System.out.println("--------------------------------");
        
        // Create a signal with multiple edges
        int n = 512;
        double[] signal = new double[n];
        
        // Create step edges at different locations
        for (int i = 0; i < n; i++) {
            if (i < n/4) {
                signal[i] = 0;
            } else if (i < n/2) {
                signal[i] = 1;
            } else if (i < 3*n/4) {
                signal[i] = 0.5;
            } else {
                signal[i] = 0.8;
            }
        }
        
        // Add smooth edge
        for (int i = n/3; i < n/3 + 20; i++) {
            double t = (i - n/3) / 20.0;
            signal[i] = 0.3 * (1 + Math.tanh(5 * (t - 0.5)));
        }
        
        // Use DOG n=1 for edge detection
        DOGWavelet edgeDetector = DOGWavelet.createStandard(DOGWavelet.WaveletType.EDGE, 2.0);
        ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(edgeDetector);
        
        // Single scale analysis
        double[] scales = {5.0};
        CWTResult result = cwt.transformFFT(signal, scales, 1.0);
        
        // Find edges (local maxima of magnitude)
        double[][] magnitude = result.getMagnitude();
        System.out.println("\nDetected edges (magnitude > 0.1):");
        
        for (int i = 1; i < n - 1; i++) {
            if (magnitude[0][i] > 0.1 && 
                magnitude[0][i] > magnitude[0][i-1] && 
                magnitude[0][i] > magnitude[0][i+1]) {
                System.out.printf("  Edge at index %d (%.1f%% of signal)\n", 
                                i, 100.0 * i / n);
            }
        }
    }
    
    /**
     * Demonstrate singularity analysis using different DOG orders.
     */
    private static void demonstrateSingularityAnalysis() {
        System.out.println("\n3. Singularity Analysis:");
        System.out.println("-----------------------");
        
        // Create signal with different types of singularities
        int n = 256;
        double[] signal = new double[n];
        
        // Add different singularities
        for (int i = 0; i < n; i++) {
            double t = (i - n/2) / 50.0;
            
            // Cusp singularity at t=0
            if (Math.abs(t) < 2) {
                signal[i] += Math.pow(Math.abs(t), 0.5);
            }
            
            // Jump discontinuity at i=3n/4
            if (i == 3*n/4) {
                signal[i] = 2.0;
            }
            
            // Smooth background
            signal[i] += 0.1 * Math.sin(2 * Math.PI * i / n);
        }
        
        // Analyze with different DOG orders
        System.out.println("\nAnalyzing singularities with different DOG orders:");
        
        for (int order = 1; order <= 4; order++) {
            DOGWavelet dog = new DOGWavelet(order, 1.0);
            ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(dog);
            
            // Multi-scale analysis
            double[] scales = ContinuousWaveletTransform.generateLogScales(1.0, 20.0, 10);
            CWTResult result = cwt.transformFFT(signal, scales, 1.0);
            
            // Find maximum response across scales
            double[][] magnitude = result.getMagnitude();
            double maxResponse = 0;
            int maxScale = 0;
            int maxTime = 0;
            
            for (int s = 0; s < scales.length; s++) {
                for (int t = 0; t < n; t++) {
                    if (magnitude[s][t] > maxResponse) {
                        maxResponse = magnitude[s][t];
                        maxScale = s;
                        maxTime = t;
                    }
                }
            }
            
            System.out.printf("  DOG n=%d: Max response %.3f at scale %.1f, time %d\n",
                            order, maxResponse, scales[maxScale], maxTime);
        }
    }
    
    /**
     * Compare different DOG orders for feature detection.
     */
    private static void compareDifferentOrders() {
        System.out.println("\n4. Comparing Different DOG Orders:");
        System.out.println("---------------------------------");
        
        // Create test signal with mixed features
        int n = 512;
        double[] signal = new double[n];
        double samplingRate = 256.0; // Hz
        
        for (int i = 0; i < n; i++) {
            double t = i / samplingRate;
            
            // Low frequency oscillation
            signal[i] = Math.sin(2 * Math.PI * 5 * t);
            
            // Add high frequency burst
            if (t > 0.5 && t < 0.7) {
                signal[i] += 0.5 * Math.sin(2 * Math.PI * 50 * t);
            }
            
            // Add spike
            if (Math.abs(i - n/3) < 2) {
                signal[i] += 2.0 * Math.exp(-Math.pow((i - n/3) / 5.0, 2));
            }
        }
        
        // Standard DOG wavelets
        DOGWavelet.WaveletType[] types = {DOGWavelet.WaveletType.EDGE, DOGWavelet.WaveletType.MEXICAN_HAT,
                                  DOGWavelet.WaveletType.ZERO_CROSSING, DOGWavelet.WaveletType.RIDGE};
        
        System.out.println("\nFeature detection with standard DOG wavelets:");
        
        for (DOGWavelet.WaveletType type : types) {
            DOGWavelet dog = DOGWavelet.createStandard(type, 2.0);
            ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(dog);
            
            // Use appropriate scale for each order
            double scale = 10.0 / Math.sqrt(type.getOrder());
            double[] scales = {scale};
            
            long start = System.nanoTime();
            CWTResult result = cwt.transformFFT(signal, scales, samplingRate);
            long elapsed = System.nanoTime() - start;
            
            // Find feature locations
            double[][] magnitude = result.getMagnitude();
            int spikeLocation = 0;
            double maxMag = 0;
            
            for (int i = 0; i < n; i++) {
                if (magnitude[0][i] > maxMag) {
                    maxMag = magnitude[0][i];
                    spikeLocation = i;
                }
            }
            
            System.out.printf("  %-15s (n=%d): Spike at t=%.3f s, computation time: %.2f ms\n",
                            type.name(), type.getOrder(), 
                            spikeLocation / samplingRate, 
                            elapsed / 1_000_000.0);
        }
    }
    
    /**
     * Demonstrate equivalence with Mexican Hat wavelet.
     */
    private static void demonstrateMexicanHatEquivalence() {
        System.out.println("\n5. DOG n=2 vs Mexican Hat Wavelet:");
        System.out.println("----------------------------------");
        
        // Create DOG n=2 and Mexican Hat
        DOGWavelet dog2 = new DOGWavelet(2, 1.0);
        MexicanHatWavelet mexican = new MexicanHatWavelet(1.0);
        
        System.out.println("\nComparing wavelet values:");
        double[] testPoints = {0.0, 0.5, 1.0, 1.5, 2.0};
        
        System.out.println("  t     DOG(n=2)    Mexican Hat   Ratio");
        System.out.println("  ---   ---------   -----------   -----");
        
        double ratio = 0;
        for (double t : testPoints) {
            Complex dogVal = dog2.wavelet(t);
            Complex mexVal = mexican.wavelet(t);
            
            if (Math.abs(mexVal.getReal()) > 1e-10) {
                ratio = dogVal.getReal() / mexVal.getReal();
            }
            
            System.out.printf("  %.1f   %9.6f   %11.6f   %.3f\n",
                            t, dogVal.getReal(), mexVal.getReal(), ratio);
        }
        
        System.out.printf("\nNormalization ratio: %.3f\n", ratio);
        System.out.println("(DOG n=2 and Mexican Hat differ only by a normalization constant)");
        
        // Compare in frequency domain
        System.out.println("\nFrequency domain comparison:");
        double[] frequencies = {0.1, 0.5, 1.0, 2.0, 5.0};
        
        System.out.println("  ω     |DOG(ω)|    |Mexican(ω)|");
        System.out.println("  ---   --------    ------------");
        
        for (double omega : frequencies) {
            Complex dogFT = dog2.fourierTransform(omega);
            Complex mexFT = mexican.fourierTransform(omega);
            
            System.out.printf("  %.1f   %.6f    %.6f\n",
                            omega, dogFT.getMag(), mexFT.getMag());
        }
        
        // Performance comparison
        System.out.println("\nPerformance comparison (512 points, 30 scales):");
        
        int n = 512;
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            signal[i] = Math.random() - 0.5;
        }
        
        double[] scales = ContinuousWaveletTransform.generateLogScales(0.5, 10.0, 30);
        
        // DOG n=2
        ContinuousWaveletTransform cwtDOG = new ContinuousWaveletTransform(dog2);
        long startDOG = System.nanoTime();
        CWTResult resultDOG = cwtDOG.transformFFT(signal, scales, 1.0);
        long timeDOG = System.nanoTime() - startDOG;
        
        // Mexican Hat
        ContinuousWaveletTransform cwtMex = new ContinuousWaveletTransform(mexican);
        long startMex = System.nanoTime();
        CWTResult resultMex = cwtMex.transformFFT(signal, scales, 1.0);
        long timeMex = System.nanoTime() - startMex;
        
        System.out.printf("  DOG n=2:      %.2f ms\n", timeDOG / 1_000_000.0);
        System.out.printf("  Mexican Hat:  %.2f ms\n", timeMex / 1_000_000.0);
        System.out.printf("  Performance ratio: %.2fx\n", (double)timeDOG / timeMex);
    }
}