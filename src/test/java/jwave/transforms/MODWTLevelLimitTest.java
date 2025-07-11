package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.exceptions.JWaveFailure;
import jwave.exceptions.JWaveException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the maximum decomposition level limit for MODWTTransform.
 * The limit is set to a Fibonacci number and provides a reasonable
 * balance between flexibility and memory constraints.
 */
public class MODWTLevelLimitTest {
    
    private static final int MAX_LEVEL = MODWTTransform.getMaxDecompositionLevel();
    
    @Test
    public void testMaximumLevelIsEnforced() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        double[] signal = new double[16384]; // 2^14, enough for 14 levels theoretically
        
        // MAX_LEVEL should work
        try {
            double[][] result = modwt.forwardMODWT(signal, MAX_LEVEL);
            assertNotNull("Level " + MAX_LEVEL + " should succeed", result);
            assertEquals("Should return " + (MAX_LEVEL + 1) + " arrays (" + MAX_LEVEL + 
                        " details + 1 approximation)", MAX_LEVEL + 1, result.length);
        } catch (Exception e) {
            fail("Level " + MAX_LEVEL + " should not throw exception: " + e.getMessage());
        }
        
        // MAX_LEVEL + 1 should fail
        try {
            modwt.forwardMODWT(signal, MAX_LEVEL + 1);
            fail("Level " + (MAX_LEVEL + 1) + " should throw exception");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention level " + MAX_LEVEL, 
                      e.getMessage().contains(String.valueOf(MAX_LEVEL)));
        }
    }
    
    @Test
    public void testPrecomputeFiltersLevelLimit() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // MAX_LEVEL should work
        try {
            modwt.precomputeFilters(MAX_LEVEL);
        } catch (Exception e) {
            fail("precomputeFilters(" + MAX_LEVEL + ") should not throw exception: " + e.getMessage());
        }
        
        // MAX_LEVEL + 1 should fail
        try {
            modwt.precomputeFilters(MAX_LEVEL + 1);
            fail("precomputeFilters(" + (MAX_LEVEL + 1) + ") should throw exception");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention level " + MAX_LEVEL, 
                      e.getMessage().contains(String.valueOf(MAX_LEVEL)));
        }
    }
    
    @Test
    public void testForwardMethodWithLevelLimit() throws JWaveException {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        double[] signal = new double[16384]; // 2^14
        
        // MAX_LEVEL should work
        try {
            double[] result = modwt.forward(signal, MAX_LEVEL);
            assertNotNull("Level " + MAX_LEVEL + " should succeed", result);
        } catch (IllegalArgumentException e) {
            fail("forward() with level " + MAX_LEVEL + " should not throw exception: " + e.getMessage());
        }
        
        // MAX_LEVEL + 1 should fail
        try {
            modwt.forward(signal, MAX_LEVEL + 1);
            fail("forward() with level " + (MAX_LEVEL + 1) + " should throw exception");
        } catch (JWaveException e) {
            assertTrue("Exception message should mention level " + MAX_LEVEL, 
                      e.getMessage().contains(String.valueOf(MAX_LEVEL)));
        }
    }
    
    @Test
    public void testUpsamplingAtMaxLevel() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        double[] smallSignal = new double[1024]; // 2^10
        
        // Even with a small signal, we should be able to request MAX_LEVEL
        // (though it may not be meaningful)
        try {
            modwt.precomputeFilters(MAX_LEVEL);
            // The cache should now contain upsampled filters for levels 1-MAX_LEVEL
        } catch (Exception e) {
            fail("Should be able to pre-compute filters up to level " + MAX_LEVEL + ": " + e.getMessage());
        }
    }
    
    @Test
    public void testFibonacciLevelChoice() {
        // Verify that MAX_LEVEL is indeed a Fibonacci number
        int[] fibonacci = {0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144};
        boolean isFibonacci = false;
        for (int fib : fibonacci) {
            if (fib == MAX_LEVEL) {
                isFibonacci = true;
                break;
            }
        }
        assertTrue(MAX_LEVEL + " should be a Fibonacci number", isFibonacci);
        
        // At MAX_LEVEL, filter sizes become quite large
        // For Haar (2 coefficients), upsampled size = 2 + (2-1) * (2^(MAX_LEVEL-1) - 1)
        // For Db4 (8 coefficients), upsampled size = 8 + (8-1) * (2^(MAX_LEVEL-1) - 1)
        // These are still manageable sizes
        
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        modwt.precomputeFilters(MAX_LEVEL);
        
        // The filters should be cached and ready for use
        double[] testSignal = new double[8192];
        double[][] result = modwt.forwardMODWT(testSignal, 10); // Use reasonable level
        assertNotNull("Transform should work with pre-computed filters", result);
    }
}