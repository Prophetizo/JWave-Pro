/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms;

import jwave.datatypes.natives.Complex;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test suite specifically for phase normalization in CWTResult.
 * Verifies that phase values are correctly normalized to [-π, π] range.
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class CWTResultPhaseTest {
    
    private static final double DELTA = 1e-10;
    
    @Test
    public void testPhaseNormalization() {
        // Create test coefficients with various phase angles
        Complex[][] coefficients = new Complex[1][10];
        
        // Test various angles that should normalize differently
        // Note: Complex.getPhi() returns degrees in [0, 360)
        coefficients[0][0] = new Complex(1, 0);      // 0 degrees -> 0 radians
        coefficients[0][1] = new Complex(0, 1);      // 90 degrees -> π/2 radians
        coefficients[0][2] = new Complex(-1, 0);     // 180 degrees -> π radians (should become -π)
        coefficients[0][3] = new Complex(0, -1);     // 270 degrees -> 3π/2 radians (should become -π/2)
        coefficients[0][4] = new Complex(1, 1);      // 45 degrees -> π/4 radians
        coefficients[0][5] = new Complex(-1, 1);     // 135 degrees -> 3π/4 radians
        coefficients[0][6] = new Complex(-1, -1);    // 225 degrees -> 5π/4 radians (should become -3π/4)
        coefficients[0][7] = new Complex(1, -1);     // 315 degrees -> 7π/4 radians (should become -π/4)
        coefficients[0][8] = createComplexWithAngle(359);  // 359 degrees (should become close to -π/180)
        coefficients[0][9] = createComplexWithAngle(181);  // 181 degrees (should become close to -179π/180)
        
        // Create CWTResult
        double[] scales = {1.0};
        double[] timeAxis = new double[10];
        CWTResult result = new CWTResult(coefficients, scales, timeAxis, 1.0, "Test");
        
        // Get phase
        double[][] phase = result.getPhase();
        
        // Verify all phases are in [-π, π]
        for (int i = 0; i < phase[0].length; i++) {
            assertTrue("Phase[" + i + "] = " + phase[0][i] + " should be in [-π, π]", 
                      phase[0][i] >= -Math.PI - DELTA && phase[0][i] <= Math.PI + DELTA);
        }
        
        // Test specific expected values
        assertEquals("0 degrees should be 0 radians", 0.0, phase[0][0], DELTA);
        assertEquals("90 degrees should be π/2 radians", Math.PI/2, phase[0][1], DELTA);
        assertEquals("180 degrees should normalize to π radians", Math.PI, phase[0][2], DELTA);
        assertEquals("270 degrees should normalize to -π/2 radians", -Math.PI/2, phase[0][3], DELTA);
        assertEquals("45 degrees should be π/4 radians", Math.PI/4, phase[0][4], DELTA);
        assertEquals("135 degrees should be 3π/4 radians", 3*Math.PI/4, phase[0][5], DELTA);
        assertEquals("225 degrees should normalize to -3π/4 radians", -3*Math.PI/4, phase[0][6], DELTA);
        assertEquals("315 degrees should normalize to -π/4 radians", -Math.PI/4, phase[0][7], DELTA);
        
        // 359 degrees = 359π/180 radians ≈ 6.2657 radians
        // After normalization: 6.2657 - 2π ≈ -0.0175 radians (close to -π/180)
        assertTrue("359 degrees should be close to -π/180", 
                  Math.abs(phase[0][8] - (-Math.PI/180)) < 0.001);
        
        // 181 degrees = 181π/180 radians ≈ 3.1590 radians  
        // After normalization using IEEEremainder, should be close to -179π/180
        assertTrue("181 degrees should be close to -179π/180", 
                  Math.abs(phase[0][9] - (-179*Math.PI/180)) < 0.001);
    }
    
    @Test
    public void testPhaseNormalizationEdgeCases() {
        Complex[][] coefficients = new Complex[1][5];
        
        // Test edge cases
        coefficients[0][0] = new Complex(0, 0);     // Zero complex number
        coefficients[0][1] = new Complex(Double.MIN_VALUE, Double.MIN_VALUE);  // Very small values
        coefficients[0][2] = new Complex(1e-15, 1e-15);  // Near zero
        coefficients[0][3] = new Complex(1e15, 1e15);    // Very large values
        coefficients[0][4] = createComplexWithAngle(360); // Exactly 360 degrees
        
        double[] scales = {1.0};
        double[] timeAxis = new double[5];
        CWTResult result = new CWTResult(coefficients, scales, timeAxis, 1.0, "Test");
        
        // Get phase - should not throw any exceptions
        double[][] phase = result.getPhase();
        
        // Verify all phases are in [-π, π]
        for (int i = 0; i < phase[0].length; i++) {
            assertTrue("Phase[" + i + "] = " + phase[0][i] + " should be in [-π, π]", 
                      phase[0][i] >= -Math.PI - DELTA && phase[0][i] <= Math.PI + DELTA);
        }
        
        // Zero complex number should have phase 0
        assertEquals("Zero complex number should have phase 0", 0.0, phase[0][0], DELTA);
        
        // 360 degrees should normalize to 0
        assertEquals("360 degrees should normalize to 0", 0.0, phase[0][4], DELTA);
    }
    
    @Test
    public void testPhaseNormalizationSymmetry() {
        // Test that angles that differ by 2π normalize to the same value
        Complex[][] coefficients = new Complex[1][3];
        
        // Create angles that differ by multiples of 360 degrees
        coefficients[0][0] = createComplexWithAngle(30);    // 30 degrees
        coefficients[0][1] = createComplexWithAngle(390);   // 30 + 360 degrees
        coefficients[0][2] = createComplexWithAngle(750);   // 30 + 2*360 degrees
        
        double[] scales = {1.0};
        double[] timeAxis = new double[3];
        CWTResult result = new CWTResult(coefficients, scales, timeAxis, 1.0, "Test");
        
        double[][] phase = result.getPhase();
        
        // All should normalize to π/6 radians
        double expected = Math.PI / 6;
        assertEquals("30 degrees should be π/6", expected, phase[0][0], DELTA);
        assertEquals("390 degrees should normalize to π/6", expected, phase[0][1], DELTA);
        assertEquals("750 degrees should normalize to π/6", expected, phase[0][2], DELTA);
    }
    
    /**
     * Helper method to create a complex number with a specific angle in degrees.
     */
    private Complex createComplexWithAngle(double degrees) {
        double radians = degrees * Math.PI / 180.0;
        double magnitude = 1.0;
        return new Complex(magnitude * Math.cos(radians), magnitude * Math.sin(radians));
    }
}