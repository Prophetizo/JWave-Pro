package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.exceptions.JWaveException;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test class for the 1D interface of MODWTTransform.
 * Verifies that the flattening/unflattening works correctly
 * and maintains compatibility with JWave conventions.
 */
public class MODWT1DInterfaceTest {
    
    private static final double TOLERANCE = 1e-10;
    
    @Test
    public void testForward1DInterface() throws JWaveException {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        // Test forward transform with default (max) levels
        double[] flatCoeffs = modwt.forward(signal);
        
        // Should have (maxLevel + 1) * N coefficients
        int maxLevel = 3; // log2(8) = 3
        assertEquals("Flattened coefficients length incorrect", 
                    signal.length * (maxLevel + 1), flatCoeffs.length);
    }
    
    @Test
    public void testForwardWithLevel() throws JWaveException {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16.0);
        }
        
        // Test with different levels
        for (int level = 1; level <= 6; level++) {
            double[] flatCoeffs = modwt.forward(signal, level);
            assertEquals("Incorrect length for level " + level,
                        signal.length * (level + 1), flatCoeffs.length);
        }
    }
    
    @Test
    public void testReverse1DInterface() throws JWaveException {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        // Forward transform
        double[] flatCoeffs = modwt.forward(signal);
        
        // Reverse transform
        double[] reconstructed = modwt.reverse(flatCoeffs);
        
        // Check perfect reconstruction
        assertArrayEquals("1D interface reconstruction failed",
                         signal, reconstructed, TOLERANCE);
    }
    
    @Test
    public void testReverseWithLevel() throws JWaveException {
        MODWTTransform modwt = new MODWTTransform(new Daubechies4());
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 8.0);
        }
        
        // Test with different decomposition levels
        for (int level = 1; level <= 5; level++) {
            double[] flatCoeffs = modwt.forward(signal, level);
            double[] reconstructed = modwt.reverse(flatCoeffs, level);
            
            assertArrayEquals("Reconstruction failed for level " + level,
                             signal, reconstructed, TOLERANCE);
        }
    }
    
    @Test
    public void testConsistencyWith2DInterface() throws JWaveException {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        int level = 2;
        
        // Get coefficients via 2D interface
        double[][] coeffs2D = modwt.forwardMODWT(signal, level);
        
        // Get coefficients via 1D interface
        double[] flatCoeffs = modwt.forward(signal, level);
        
        // Verify they contain the same data
        for (int lev = 0; lev <= level; lev++) {
            for (int i = 0; i < signal.length; i++) {
                assertEquals("Coefficient mismatch at level " + lev + ", index " + i,
                            coeffs2D[lev][i], flatCoeffs[lev * signal.length + i], TOLERANCE);
            }
        }
    }
    
    @Test
    public void testInvalidInputHandling() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // Test non-power-of-2 length
        try {
            modwt.forward(new double[10]);
            fail("Should throw exception for non-power-of-2 length");
        } catch (JWaveException e) {
            assertTrue(e.getMessage().contains("2^p"));
        }
        
        // Test invalid level
        try {
            modwt.forward(new double[8], 5); // max level for length 8 is 3
            fail("Should throw exception for invalid level");
        } catch (JWaveException e) {
            assertTrue(e.getMessage().contains("out of range"));
        }
        
        // Test invalid reverse input
        try {
            modwt.reverse(new double[15], 2); // 15 is not divisible by 3
            fail("Should throw exception for invalid coefficient array");
        } catch (JWaveException e) {
            // Check for either error message since different conditions may be triggered
            assertTrue(e.getMessage().contains("does not match") || 
                      e.getMessage().contains("Invalid coefficient array"));
        }
    }
    
    @Test
    public void testEmptyAndNullHandling() throws JWaveException {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // Test empty array
        double[] emptyResult = modwt.forward(new double[0]);
        assertEquals(0, emptyResult.length);
        
        emptyResult = modwt.reverse(new double[0]);
        assertEquals(0, emptyResult.length);
        
        // Test null
        double[] nullInput = null;
        double[] nullResult = modwt.forward(nullInput);
        assertEquals(0, nullResult.length);
        
        nullResult = modwt.reverse(nullInput);
        assertEquals(0, nullResult.length);
    }
    
    @Test
    public void testEnergyConservationWith1DInterface() throws JWaveException {
        MODWTTransform modwt = new MODWTTransform(new Daubechies4());
        double[] signal = new double[256];
        
        // Create test signal
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64.0) + 
                       Math.random() * 0.1;
        }
        
        // Calculate signal energy
        double signalEnergy = 0.0;
        for (double val : signal) {
            signalEnergy += val * val;
        }
        
        // Forward and reverse transform
        double[] coeffs = modwt.forward(signal);
        double[] reconstructed = modwt.reverse(coeffs);
        
        // Calculate reconstructed energy
        double reconstructedEnergy = 0.0;
        for (double val : reconstructed) {
            reconstructedEnergy += val * val;
        }
        
        // Check energy conservation
        assertEquals("Energy not conserved through 1D interface",
                    signalEnergy, reconstructedEnergy, TOLERANCE * signalEnergy);
    }
}