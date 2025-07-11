package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test edge cases for inverseMODWT with invalid coefficient arrays.
 */
public class MODWTInverseSingleArrayTest {
    
    @Test
    public void testInverseWithSingleArray() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // Single array input (only 1 coefficient array) should return empty
        double[][] singleArray = new double[][] {
            {1.0, 2.0, 3.0, 4.0}
        };
        
        double[] result = modwt.inverseMODWT(singleArray);
        assertNotNull("Result should not be null", result);
        assertEquals("Single array input should return empty array", 0, result.length);
    }
    
    @Test
    public void testInverseWithValidMinimumArrays() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // Minimum valid input: 2 arrays (W_1 and V_1)
        double[][] validCoeffs = new double[][] {
            {1.0, 2.0, 3.0, 4.0},  // W_1 (details)
            {5.0, 6.0, 7.0, 8.0}   // V_1 (approximation)
        };
        
        double[] result = modwt.inverseMODWT(validCoeffs);
        assertNotNull("Result should not be null", result);
        assertEquals("Result should have same length as input", 4, result.length);
        
        // The result should be a valid reconstruction (not checking exact values
        // as that would require implementing the inverse transform logic)
        for (double value : result) {
            assertFalse("Result should not contain NaN", Double.isNaN(value));
            assertFalse("Result should not contain Infinity", Double.isInfinite(value));
        }
    }
    
    @Test
    public void testInverseWithEmptyArrays() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // Empty coefficient arrays
        double[][] emptyCoeffs = new double[][] {
            new double[0],
            new double[0]
        };
        
        double[] result = modwt.inverseMODWT(emptyCoeffs);
        assertNotNull("Result should not be null", result);
        assertEquals("Empty coefficient arrays should return empty result", 0, result.length);
    }
    
    @Test
    public void testForwardInverseConsistency() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        // Test that forward with level 1 produces 2 arrays
        double[][] coeffs = modwt.forwardMODWT(signal, 1);
        assertEquals("Level 1 forward should produce 2 arrays", 2, coeffs.length);
        
        // And inverse should work with these 2 arrays
        double[] reconstructed = modwt.inverseMODWT(coeffs);
        assertNotNull("Reconstructed signal should not be null", reconstructed);
        assertEquals("Reconstructed length should match original", signal.length, reconstructed.length);
        
        // Check perfect reconstruction
        for (int i = 0; i < signal.length; i++) {
            assertEquals("Perfect reconstruction at index " + i, 
                        signal[i], reconstructed[i], 1e-10);
        }
    }
}