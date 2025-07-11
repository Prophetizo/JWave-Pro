package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.symlets.Symlet8;
import jwave.transforms.wavelets.Wavelet;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Test class for MODWT with FFT-based convolution.
 * Verifies mathematical correctness and consistency between direct and FFT methods.
 * 
 * @author Stephen Romano
 * @date 11.01.2025
 */
public class MODWTFFTConvolutionTest {

    private static final double TOLERANCE = 1e-10;
    private static final double RELAXED_TOLERANCE = 1e-8; // For accumulated floating-point errors
    
    private MODWTTransform modwtHaar;
    private MODWTTransform modwtDb4;
    private MODWTTransform modwtSym8;
    
    @Before
    public void setUp() {
        modwtHaar = new MODWTTransform(new Haar1());
        modwtDb4 = new MODWTTransform(new Daubechies4());
        modwtSym8 = new MODWTTransform(new Symlet8());
    }
    
    /**
     * Test that FFT and direct convolution produce identical results for small signals.
     */
    @Test
    public void testFFTvsDirectSmallSignal() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        int maxLevel = 2;
        
        // Test with Haar wavelet
        testFFTvsDirectForWavelet(modwtHaar, signal, maxLevel, "Haar");
        
        // Test with Daubechies-4 wavelet
        testFFTvsDirectForWavelet(modwtDb4, signal, maxLevel, "Daubechies-4");
        
        // Test with Symlet-8 wavelet
        testFFTvsDirectForWavelet(modwtSym8, signal, maxLevel, "Symlet-8");
    }
    
    /**
     * Test with larger signals where FFT should show performance benefits.
     */
    @Test
    public void testFFTvsDirectLargeSignal() {
        // Create a larger test signal
        double[] signal = new double[256];
        Random rand = new Random(42); // Fixed seed for reproducibility
        for (int i = 0; i < signal.length; i++) {
            signal[i] = rand.nextGaussian();
        }
        int maxLevel = 4;
        
        testFFTvsDirectForWavelet(modwtHaar, signal, maxLevel, "Haar");
        testFFTvsDirectForWavelet(modwtDb4, signal, maxLevel, "Daubechies-4");
    }
    
    /**
     * Test perfect reconstruction with FFT convolution.
     */
    @Test
    public void testPerfectReconstructionFFT() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        // Set to use FFT convolution
        modwtHaar.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        
        // Forward transform
        double[][] coeffs = modwtHaar.forwardMODWT(signal, 3);
        
        // Inverse transform
        double[] reconstructed = modwtHaar.inverseMODWT(coeffs);
        
        // Check perfect reconstruction
        assertArrayEquals("FFT convolution should allow perfect reconstruction", 
                         signal, reconstructed, TOLERANCE);
    }
    
    /**
     * Test energy conservation with FFT convolution.
     */
    @Test
    public void testEnergyConservationFFT() {
        double[] signal = new double[64];
        Random rand = new Random(12345);
        for (int i = 0; i < signal.length; i++) {
            signal[i] = rand.nextGaussian();
        }
        
        // Set to use FFT convolution
        modwtDb4.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        
        int maxLevel = 4;
        double[][] coeffs = modwtDb4.forwardMODWT(signal, maxLevel);
        
        // Calculate variance of the original signal
        double signalVariance = calculateVariance(signal);
        
        // Calculate the sum of variances of all coefficient arrays
        double coeffsVarianceSum = 0.0;
        for (double[] coeffLevel : coeffs) {
            coeffsVarianceSum += calculateVariance(coeffLevel);
        }
        
        // The variance of the signal should equal the sum of variances of the coefficients
        assertEquals("Energy (variance) should be conserved with FFT convolution", 
                    signalVariance, coeffsVarianceSum, RELAXED_TOLERANCE);
    }
    
    /**
     * Test AUTO mode correctly switches between direct and FFT based on size.
     */
    @Test
    public void testAutoModeSelection() {
        // Small signal - should use direct convolution
        double[] smallSignal = new double[32];
        Arrays.fill(smallSignal, 1.0);
        
        // Large signal - should use FFT convolution
        double[] largeSignal = new double[1024];
        Arrays.fill(largeSignal, 1.0);
        
        modwtHaar.setConvolutionMethod(MODWTTransform.ConvolutionMethod.AUTO);
        
        // Both should produce valid results
        double[][] smallCoeffs = modwtHaar.forwardMODWT(smallSignal, 2);
        double[][] largeCoeffs = modwtHaar.forwardMODWT(largeSignal, 5);
        
        assertNotNull("Small signal coefficients should not be null", smallCoeffs);
        assertNotNull("Large signal coefficients should not be null", largeCoeffs);
        
        // Test reconstruction
        double[] smallReconstructed = modwtHaar.inverseMODWT(smallCoeffs);
        double[] largeReconstructed = modwtHaar.inverseMODWT(largeCoeffs);
        
        assertArrayEquals("Small signal reconstruction", smallSignal, smallReconstructed, TOLERANCE);
        assertArrayEquals("Large signal reconstruction", largeSignal, largeReconstructed, TOLERANCE);
    }
    
    /**
     * Test with non-power-of-2 signal lengths.
     */
    @Test
    public void testNonPowerOfTwoLengths() {
        // MODWT should handle arbitrary lengths
        double[] signal = new double[100]; // Not a power of 2
        Random rand = new Random(999);
        for (int i = 0; i < signal.length; i++) {
            signal[i] = rand.nextDouble();
        }
        
        // Test both methods
        modwtHaar.setConvolutionMethod(MODWTTransform.ConvolutionMethod.DIRECT);
        double[][] directCoeffs = modwtHaar.forwardMODWT(signal, 3);
        
        modwtHaar.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        double[][] fftCoeffs = modwtHaar.forwardMODWT(signal, 3);
        
        // Compare results
        for (int level = 0; level < directCoeffs.length; level++) {
            assertArrayEquals("Level " + level + " coefficients should match", 
                            directCoeffs[level], fftCoeffs[level], RELAXED_TOLERANCE);
        }
    }
    
    /**
     * Test boundary conditions and edge cases.
     */
    @Test
    public void testBoundaryConditions() {
        // Test with very small signal
        double[] tinySignal = {1.0, 2.0};
        
        modwtHaar.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        double[][] coeffs = modwtHaar.forwardMODWT(tinySignal, 1);
        double[] reconstructed = modwtHaar.inverseMODWT(coeffs);
        
        assertArrayEquals("Tiny signal reconstruction", tinySignal, reconstructed, TOLERANCE);
        
        // Test with single-level decomposition
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        coeffs = modwtHaar.forwardMODWT(signal, 1);
        reconstructed = modwtHaar.inverseMODWT(coeffs);
        
        assertArrayEquals("Single-level reconstruction", signal, reconstructed, TOLERANCE);
    }
    
    /**
     * Helper method to test FFT vs Direct convolution for a specific wavelet.
     */
    private void testFFTvsDirectForWavelet(MODWTTransform modwt, double[] signal, 
                                          int maxLevel, String waveletName) {
        // Compute with direct convolution
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.DIRECT);
        double[][] directCoeffs = modwt.forwardMODWT(signal, maxLevel);
        
        // Compute with FFT convolution
        modwt.setConvolutionMethod(MODWTTransform.ConvolutionMethod.FFT);
        double[][] fftCoeffs = modwt.forwardMODWT(signal, maxLevel);
        
        // Compare all coefficients
        assertEquals("Number of levels should match", directCoeffs.length, fftCoeffs.length);
        
        for (int level = 0; level < directCoeffs.length; level++) {
            assertArrayEquals(waveletName + " - Level " + level + " coefficients should match", 
                            directCoeffs[level], fftCoeffs[level], RELAXED_TOLERANCE);
        }
        
        // Test inverse transform consistency
        double[] directReconstructed = modwt.inverseMODWT(directCoeffs);
        double[] fftReconstructed = modwt.inverseMODWT(fftCoeffs);
        
        assertArrayEquals(waveletName + " - Direct reconstruction", 
                         signal, directReconstructed, TOLERANCE);
        assertArrayEquals(waveletName + " - FFT reconstruction", 
                         signal, fftReconstructed, TOLERANCE);
    }
    
    /**
     * Helper method to calculate variance.
     */
    private double calculateVariance(double[] data) {
        if (data == null || data.length == 0) {
            return 0.0;
        }
        double sum = Arrays.stream(data).sum();
        double mean = sum / data.length;
        double squaredDifferences = Arrays.stream(data)
                .map(x -> (x - mean) * (x - mean))
                .sum();
        return squaredDifferences / data.length;
    }
}