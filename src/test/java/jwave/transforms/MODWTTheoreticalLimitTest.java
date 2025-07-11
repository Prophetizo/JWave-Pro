package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test that MODWT properly validates decomposition levels against
 * the theoretical limit floor(log2(N)) based on signal length.
 */
public class MODWTTheoreticalLimitTest {
    
    @Test
    public void testTheoreticalLimitValidation() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // Test various signal lengths
        testSignalLengthLimit(modwt, 16, 4);     // 2^4 = 16, max level = 4
        testSignalLengthLimit(modwt, 32, 5);     // 2^5 = 32, max level = 5
        testSignalLengthLimit(modwt, 64, 6);     // 2^6 = 64, max level = 6
        testSignalLengthLimit(modwt, 128, 7);    // 2^7 = 128, max level = 7
        testSignalLengthLimit(modwt, 256, 8);    // 2^8 = 256, max level = 8
        testSignalLengthLimit(modwt, 512, 9);    // 2^9 = 512, max level = 9
        testSignalLengthLimit(modwt, 1024, 10);  // 2^10 = 1024, max level = 10
        
        // Test non-power-of-2 lengths
        testSignalLengthLimit(modwt, 100, 6);    // floor(log2(100)) = 6
        testSignalLengthLimit(modwt, 200, 7);    // floor(log2(200)) = 7
        testSignalLengthLimit(modwt, 1000, 9);   // floor(log2(1000)) = 9
    }
    
    private void testSignalLengthLimit(MODWTTransform modwt, int signalLength, int expectedMaxLevel) {
        double[] signal = new double[signalLength];
        
        // Should succeed at the theoretical limit
        try {
            double[][] result = modwt.forwardMODWT(signal, expectedMaxLevel);
            assertNotNull("Level " + expectedMaxLevel + " should succeed for length " + signalLength, result);
            assertEquals("Should return correct number of arrays", expectedMaxLevel + 1, result.length);
        } catch (Exception e) {
            fail("Level " + expectedMaxLevel + " should not throw for length " + signalLength + ": " + e.getMessage());
        }
        
        // Should fail above the theoretical limit
        try {
            modwt.forwardMODWT(signal, expectedMaxLevel + 1);
            fail("Level " + (expectedMaxLevel + 1) + " should throw for length " + signalLength);
        } catch (IllegalArgumentException e) {
            assertTrue("Exception should mention theoretical limit", 
                      e.getMessage().contains("exceeds theoretical limit"));
            assertTrue("Exception should mention the limit value " + expectedMaxLevel,
                      e.getMessage().contains(String.valueOf(expectedMaxLevel)));
            assertTrue("Exception should mention signal length " + signalLength,
                      e.getMessage().contains(String.valueOf(signalLength)));
        }
    }
    
    @Test
    public void testVerySmallSignals() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // Signal of length 2: floor(log2(2)) = 1
        double[] signal2 = new double[2];
        try {
            double[][] result = modwt.forwardMODWT(signal2, 1);
            assertNotNull("Level 1 should work for length 2", result);
        } catch (Exception e) {
            fail("Should not throw for valid level: " + e.getMessage());
        }
        
        try {
            modwt.forwardMODWT(signal2, 2);
            fail("Level 2 should fail for length 2");
        } catch (IllegalArgumentException e) {
            assertTrue("Should mention theoretical limit", e.getMessage().contains("theoretical limit"));
        }
        
        // Signal of length 3: floor(log2(3)) = 1
        double[] signal3 = new double[3];
        try {
            double[][] result = modwt.forwardMODWT(signal3, 1);
            assertNotNull("Level 1 should work for length 3", result);
        } catch (Exception e) {
            fail("Should not throw for valid level: " + e.getMessage());
        }
        
        try {
            modwt.forwardMODWT(signal3, 2);
            fail("Level 2 should fail for length 3");
        } catch (IllegalArgumentException e) {
            assertTrue("Should mention theoretical limit", e.getMessage().contains("theoretical limit"));
        }
    }
    
    @Test
    public void testEdgeCaseWithMaxDecompositionLevel() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // Create a signal where theoretical limit equals MAX_DECOMPOSITION_LEVEL
        // 2^13 = 8192
        double[] signal = new double[8192];
        int theoreticalLimit = 13;
        
        assertEquals("Theoretical limit should equal MAX_DECOMPOSITION_LEVEL", 
                    MODWTTransform.getMaxDecompositionLevel(), theoreticalLimit);
        
        // Should succeed at level 13
        try {
            double[][] result = modwt.forwardMODWT(signal, 13);
            assertNotNull("Level 13 should succeed", result);
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
        
        // Create a smaller signal where theoretical limit < MAX_DECOMPOSITION_LEVEL
        double[] smallSignal = new double[1024]; // 2^10
        
        // Should fail if we request MAX_DECOMPOSITION_LEVEL
        try {
            modwt.forwardMODWT(smallSignal, MODWTTransform.getMaxDecompositionLevel());
            fail("Should throw when requesting level 13 for length 1024");
        } catch (IllegalArgumentException e) {
            assertTrue("Should mention theoretical limit", e.getMessage().contains("theoretical limit"));
            assertTrue("Should mention limit is 10", e.getMessage().contains("10"));
        }
    }
    
    @Test
    public void testPrioritizationOfValidationMessages() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        double[] signal = new double[16]; // Theoretical limit = 4
        
        // Test that basic validation comes first
        try {
            modwt.forwardMODWT(signal, 0);
            fail("Should throw for level 0");
        } catch (IllegalArgumentException e) {
            assertTrue("Should mention minimum level requirement", 
                      e.getMessage().contains("at least 1"));
        }
        
        // Test that MAX_DECOMPOSITION_LEVEL check comes before theoretical limit
        try {
            modwt.forwardMODWT(signal, 20); // Way above both limits
            fail("Should throw for level 20");
        } catch (IllegalArgumentException e) {
            assertTrue("Should mention MAX_DECOMPOSITION_LEVEL first", 
                      e.getMessage().contains("maximum supported decomposition level is 13"));
            assertFalse("Should not mention theoretical limit", 
                       e.getMessage().contains("theoretical limit"));
        }
        
        // Test that theoretical limit is checked when within MAX_DECOMPOSITION_LEVEL
        try {
            modwt.forwardMODWT(signal, 5); // Within MAX but above theoretical
            fail("Should throw for level 5 with length 16");
        } catch (IllegalArgumentException e) {
            assertTrue("Should mention theoretical limit", 
                      e.getMessage().contains("exceeds theoretical limit"));
        }
    }
}