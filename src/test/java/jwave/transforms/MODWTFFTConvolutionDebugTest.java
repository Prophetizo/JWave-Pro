package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import org.junit.Test;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Debug test to understand the FFT convolution issue.
 */
public class MODWTFFTConvolutionDebugTest {
    
    @Test
    public void debugHaarConvolution() {
        // Simple test case
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] filter = {0.5, -0.5};
        
        System.out.println("Signal: " + Arrays.toString(signal));
        System.out.println("Filter: " + Arrays.toString(filter));
        
        // Direct convolution
        double[] directResult = circularConvolve(signal, filter);
        System.out.println("\nDirect convolution: " + Arrays.toString(directResult));
        
        // Direct adjoint convolution
        double[] directAdjointResult = circularConvolveAdjoint(signal, filter);
        System.out.println("Direct adjoint convolution: " + Arrays.toString(directAdjointResult));
        
        // Now test with MODWT
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // Test forward transform with both methods
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.DIRECT);
        double[][] directCoeffs = modwt.forwardMODWT(signal, 1);
        System.out.println("\nMODWT Direct - Detail: " + Arrays.toString(directCoeffs[0]));
        System.out.println("MODWT Direct - Approx: " + Arrays.toString(directCoeffs[1]));
        
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        double[][] fftCoeffs = modwt.forwardMODWT(signal, 1);
        System.out.println("\nMODWT FFT - Detail: " + Arrays.toString(fftCoeffs[0]));
        System.out.println("MODWT FFT - Approx: " + Arrays.toString(fftCoeffs[1]));
    }
    
    private static double[] circularConvolve(double[] signal, double[] filter) {
        int N = signal.length;
        int M = filter.length;
        double[] output = new double[N];
        for (int n = 0; n < N; n++) {
            double sum = 0.0;
            for (int m = 0; m < M; m++) {
                int signalIndex = Math.floorMod(n - m, N);
                sum += signal[signalIndex] * filter[m];
            }
            output[n] = sum;
        }
        return output;
    }
    
    private static double[] circularConvolveAdjoint(double[] signal, double[] filter) {
        int N = signal.length;
        int M = filter.length;
        double[] output = new double[N];
        for (int n = 0; n < N; n++) {
            double sum = 0.0;
            for (int m = 0; m < M; m++) {
                int signalIndex = Math.floorMod(n + m, N);
                sum += signal[signalIndex] * filter[m];
            }
            output[n] = sum;
        }
        return output;
    }
    
    @Test
    public void debugDaubechies4Issue() {
        // Test case from the failing test
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        MODWTTransform modwtDb4 = new MODWTTransform(new Daubechies4());
        
        // Test with direct convolution
        modwtDb4.setConvolutionMethod(MODWTTransform.ConvolutionMethod.DIRECT);
        double[][] directCoeffs = modwtDb4.forwardMODWT(signal, 2);
        
        // Test with FFT convolution
        modwtDb4.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        double[][] fftCoeffs = modwtDb4.forwardMODWT(signal, 2);
        
        System.out.println("Daubechies-4 Level 1:");
        System.out.println("Direct: " + Arrays.toString(directCoeffs[0]));
        System.out.println("FFT:    " + Arrays.toString(fftCoeffs[0]));
        System.out.println("Diff:   " + (directCoeffs[0][0] - fftCoeffs[0][0]));
        
        System.out.println("\nDaubechies-4 Level 2:");
        System.out.println("Direct: " + Arrays.toString(directCoeffs[1]));
        System.out.println("FFT:    " + Arrays.toString(fftCoeffs[1]));
        
        // Test inverse transform
        System.out.println("\nTesting inverse transform:");
        double[] directReconstructed = modwtDb4.inverseMODWT(directCoeffs);
        
        modwtDb4.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        double[] fftReconstructed = modwtDb4.inverseMODWT(fftCoeffs);
        
        System.out.println("Original:           " + Arrays.toString(signal));
        System.out.println("Direct reconstructed: " + Arrays.toString(directReconstructed));
        System.out.println("FFT reconstructed:    " + Arrays.toString(fftReconstructed));
        
        // Debug: Check the filters being used
        System.out.println("\nChecking filter sizes at each level:");
        modwtDb4.clearFilterCache();
        modwtDb4.precomputeFilters(2);
    }
}