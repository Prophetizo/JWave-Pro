package jwave.examples;

import jwave.transforms.MODWTTransform;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.haar.Haar1;

/**
 * Comprehensive examples demonstrating the Maximal Overlap Discrete Wavelet Transform (MODWT).
 * 
 * <p>This class provides practical examples of MODWT usage for common signal processing tasks
 * including denoising, trend extraction, and multi-resolution analysis.</p>
 * 
 * <h2>Key MODWT Properties Demonstrated:</h2>
 * <ul>
 *   <li>Shift invariance: Results don't depend on signal alignment</li>
 *   <li>Perfect reconstruction: Signal can be exactly recovered</li>
 *   <li>No downsampling: All levels have same length as input</li>
 *   <li>Energy preservation: Total variance equals sum of scale variances</li>
 *   <li>Circular boundary handling: No edge artifacts</li>
 * </ul>
 * 
 * <h2>Mathematical Background:</h2>
 * <p>The MODWT decomposes a signal X into J detail components D_j and one
 * approximation component A_J:</p>
 * <pre>
 *   X = D_1 + D_2 + ... + D_J + A_J
 * </pre>
 * <p>where each D_j captures variations at scale 2^j.</p>
 * 
 * @author Stephen Romano
 * @see MODWTTransform
 */
public class MODWTExample {
    
    public static void main(String[] args) {
        // Example 1: Basic MODWT and inverse
        basicExample();
        
        // Example 2: Denoising with MODWT
        denoisingExample();
        
        // Example 3: Multi-resolution analysis
        multiResolutionExample();
    }
    
    /**
     * Example 1: Basic MODWT decomposition and perfect reconstruction.
     * 
     * <p>Demonstrates:</p>
     * <ul>
     *   <li>Creating a multi-frequency test signal</li>
     *   <li>Performing MODWT decomposition</li>
     *   <li>Understanding the coefficient structure</li>
     *   <li>Verifying perfect reconstruction property</li>
     * </ul>
     */
    private static void basicExample() {
        System.out.println("=== Basic MODWT Example ===\n");
        
        // Create a test signal with two frequency components
        // This represents a common scenario in signal processing where
        // we have a primary signal (32-sample period) contaminated
        // with higher frequency oscillations (8-sample period)
        int N = 128;
        double[] signal = new double[N];
        for (int i = 0; i < N; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) +     // ~3.9 Hz component
                       0.5 * Math.sin(2 * Math.PI * i / 8.0);  // ~15.6 Hz component
        }
        
        // Create MODWT transform with Haar wavelet
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // Forward MODWT - decompose to 3 levels
        // Level 1: captures 8-16 Hz (will contain our 15.6 Hz component)
        // Level 2: captures 4-8 Hz
        // Level 3: captures 2-4 Hz (will contain our 3.9 Hz component)
        double[][] coeffs = modwt.forwardMODWT(signal, 3);
        
        System.out.println("Forward MODWT produced " + coeffs.length + " coefficient arrays:");
        System.out.println("(Note: All arrays have the same length as the input signal)\n");
        for (int i = 0; i < coeffs.length - 1; i++) {
            double freqRange = N / Math.pow(2, i+1);
            System.out.printf("  D_%d (Detail level %d): %d coefficients, captures %.1f-%.1f Hz\n", 
                            i+1, i+1, coeffs[i].length, freqRange/2, freqRange);
        }
        System.out.printf("  A_%d (Approximation): %d coefficients, captures < %.1f Hz\n", 
                         coeffs.length-1, coeffs[coeffs.length-1].length, 
                         N / Math.pow(2, coeffs.length));
        
        // Inverse MODWT
        double[] reconstructed = modwt.inverseMODWT(coeffs);
        
        // Check reconstruction error
        double error = 0.0;
        for (int i = 0; i < signal.length; i++) {
            error += Math.pow(signal[i] - reconstructed[i], 2);
        }
        error = Math.sqrt(error / signal.length);
        
        System.out.println("\nReconstruction RMS error: " + error);
        System.out.println("Perfect reconstruction: " + (error < 1e-10 ? "YES" : "NO"));
    }
    
    /**
     * Example 2: Signal denoising using MODWT with soft thresholding.
     * 
     * <p>This example demonstrates a practical application of MODWT for
     * removing noise from signals. The technique exploits the fact that
     * noise typically manifests as small coefficients across all scales,
     * while signal features produce larger coefficients at specific scales.</p>
     * 
     * <p>Soft thresholding formula: η(x,λ) = sign(x)·max(|x|-λ, 0)</p>
     */
    private static void denoisingExample() {
        System.out.println("\n\n=== Denoising Example ===\n");
        
        // Create a clean signal with two harmonic components
        // and contaminate it with white Gaussian noise
        int N = 256;
        double[] cleanSignal = new double[N];
        double[] noisySignal = new double[N];
        
        for (int i = 0; i < N; i++) {
            cleanSignal[i] = Math.sin(2 * Math.PI * i / 64.0) + 
                            0.3 * Math.cos(2 * Math.PI * i / 16.0);
            noisySignal[i] = cleanSignal[i] + 0.2 * (Math.random() - 0.5); // Add noise
        }
        
        // Create MODWT transform with Daubechies-4 wavelet
        MODWTTransform modwt = new MODWTTransform(new Daubechies4());
        
        // Forward transform
        double[][] coeffs = modwt.forwardMODWT(noisySignal, 4);
        
        // Apply soft thresholding to detail coefficients
        // Threshold selection: Can use universal threshold λ = σ√(2 log N)
        // where σ is noise standard deviation (here we use fixed threshold)
        double threshold = 0.1;
        int coeffsThresholded = 0;
        
        // Only threshold detail coefficients, not the approximation
        for (int level = 0; level < coeffs.length - 1; level++) {
            for (int i = 0; i < coeffs[level].length; i++) {
                double coeff = coeffs[level][i];
                if (Math.abs(coeff) <= threshold) {
                    coeffs[level][i] = 0.0;
                    coeffsThresholded++;
                } else if (coeff > 0) {
                    coeffs[level][i] = coeff - threshold;
                } else {
                    coeffs[level][i] = coeff + threshold;
                }
            }
        }
        
        System.out.printf("Thresholded %d out of %d detail coefficients (%.1f%%)\n",
                         coeffsThresholded, N * (coeffs.length - 1),
                         100.0 * coeffsThresholded / (N * (coeffs.length - 1)));
        
        // Inverse transform to get denoised signal
        double[] denoised = modwt.inverseMODWT(coeffs);
        
        // Calculate signal-to-noise ratio improvement
        double noiseBeforeMSE = calculateMSE(cleanSignal, noisySignal);
        double noiseAfterMSE = calculateMSE(cleanSignal, denoised);
        
        System.out.println("Noise level before denoising: " + Math.sqrt(noiseBeforeMSE));
        System.out.println("Noise level after denoising: " + Math.sqrt(noiseAfterMSE));
        System.out.println("Noise reduction: " + 
                          String.format("%.1f%%", (1 - noiseAfterMSE/noiseBeforeMSE) * 100));
    }
    
    /**
     * Example 3: Multi-resolution analysis for frequency component separation.
     * 
     * <p>This example shows how MODWT can decompose a complex signal into
     * its constituent frequency components. Each level of the transform
     * acts as a band-pass filter with a specific frequency range.</p>
     * 
     * <p>Frequency bands for each level j:</p>
     * <pre>
     *   Level j captures: [fs/(2^(j+1)), fs/2^j] Hz
     *   where fs is the sampling frequency
     * </pre>
     */
    private static void multiResolutionExample() {
        System.out.println("\n\n=== Multi-Resolution Analysis Example ===\n");
        
        // Create a signal with multiple frequency components
        // simulating a complex real-world signal
        int N = 512;
        double[] signal = new double[N];
        for (int i = 0; i < N; i++) {
            signal[i] = 2.0 * Math.sin(2 * Math.PI * i / 128.0) +    // Very low freq
                       1.0 * Math.sin(2 * Math.PI * i / 32.0) +      // Low freq
                       0.5 * Math.sin(2 * Math.PI * i / 8.0) +       // Medium freq
                       0.25 * Math.sin(2 * Math.PI * i / 2.0);       // High freq
        }
        
        // Create MODWT transform
        MODWTTransform modwt = new MODWTTransform(new Daubechies4());
        
        // Perform 5-level decomposition
        double[][] coeffs = modwt.forwardMODWT(signal, 5);
        
        // Analyze energy distribution across scales
        // According to Parseval's theorem, the total energy is preserved
        System.out.println("Energy distribution across scales:");
        System.out.println("(Demonstrating energy conservation property of MODWT)\n");
        double totalEnergy = calculateEnergy(signal);
        
        System.out.println("Scale | Frequency Range | Energy | Percentage");
        System.out.println("------|-----------------|--------|------------");
        
        for (int i = 0; i < coeffs.length - 1; i++) {
            double levelEnergy = calculateEnergy(coeffs[i]);
            double percentage = (levelEnergy / totalEnergy) * 100;
            double freqHigh = N / Math.pow(2, i+1);
            double freqLow = N / Math.pow(2, i+2);
            System.out.printf("  D_%d  | %3.0f - %3.0f Hz    | %6.2f | %5.1f%%\n", 
                            i+1, freqLow, freqHigh, levelEnergy, percentage);
        }
        
        double approxEnergy = calculateEnergy(coeffs[coeffs.length - 1]);
        double approxPercentage = (approxEnergy / totalEnergy) * 100;
        System.out.printf("  Approximation level %d: %.1f%% of total energy\n", 
                         coeffs.length - 1, approxPercentage);
        
        // Reconstruct signal using only certain levels
        System.out.println("\nSelective reconstruction:");
        
        // Keep only low-frequency components (approximation + last 2 detail levels)
        double[][] lowFreqCoeffs = new double[coeffs.length][coeffs[0].length];
        for (int i = 0; i < coeffs.length; i++) {
            if (i < 3) {
                // Zero out high-frequency details
                lowFreqCoeffs[i] = new double[coeffs[i].length];
            } else {
                // Keep low-frequency components
                lowFreqCoeffs[i] = coeffs[i].clone();
            }
        }
        
        double[] lowFreqSignal = modwt.inverseMODWT(lowFreqCoeffs);
        System.out.println("Low-frequency reconstruction completed");
        
        // Keep only high-frequency components (first 3 detail levels)
        double[][] highFreqCoeffs = new double[coeffs.length][coeffs[0].length];
        for (int i = 0; i < coeffs.length; i++) {
            if (i < 3) {
                // Keep high-frequency details
                highFreqCoeffs[i] = coeffs[i].clone();
            } else {
                // Zero out low-frequency components
                highFreqCoeffs[i] = new double[coeffs[i].length];
            }
        }
        
        double[] highFreqSignal = modwt.inverseMODWT(highFreqCoeffs);
        System.out.println("High-frequency reconstruction completed");
    }
    
    private static double calculateMSE(double[] signal1, double[] signal2) {
        double sum = 0.0;
        for (int i = 0; i < signal1.length; i++) {
            double diff = signal1[i] - signal2[i];
            sum += diff * diff;
        }
        return sum / signal1.length;
    }
    
    private static double calculateEnergy(double[] signal) {
        double energy = 0.0;
        for (double value : signal) {
            energy += value * value;
        }
        return energy;
    }
}