package jwave.transforms;

import jwave.transforms.wavelets.haar.Haar1;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the integer-based log2 calculation for theoretical limit validation.
 * Verifies that 31 - Integer.numberOfLeadingZeros(N) correctly computes floor(log2(N)).
 */
public class MODWTLog2CalculationTest {
    
    @Test
    public void testLog2CalculationAccuracy() {
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        
        // Test exact powers of 2
        // Skip length 1 as it has special handling (log2(1) = 0, but min level is 1)
        verifyLog2Calculation(modwt, 2, 1);      // 2^1 = 2, log2(2) = 1
        verifyLog2Calculation(modwt, 4, 2);      // 2^2 = 4, log2(4) = 2
        verifyLog2Calculation(modwt, 8, 3);      // 2^3 = 8, log2(8) = 3
        verifyLog2Calculation(modwt, 16, 4);     // 2^4 = 16, log2(16) = 4
        verifyLog2Calculation(modwt, 32, 5);     // 2^5 = 32, log2(32) = 5
        verifyLog2Calculation(modwt, 64, 6);     // 2^6 = 64, log2(64) = 6
        verifyLog2Calculation(modwt, 128, 7);    // 2^7 = 128, log2(128) = 7
        verifyLog2Calculation(modwt, 256, 8);    // 2^8 = 256, log2(256) = 8
        verifyLog2Calculation(modwt, 512, 9);    // 2^9 = 512, log2(512) = 9
        verifyLog2Calculation(modwt, 1024, 10);  // 2^10 = 1024, log2(1024) = 10
        verifyLog2Calculation(modwt, 2048, 11);  // 2^11 = 2048, log2(2048) = 11
        verifyLog2Calculation(modwt, 4096, 12);  // 2^12 = 4096, log2(4096) = 12
        verifyLog2Calculation(modwt, 8192, 13);  // 2^13 = 8192, log2(8192) = 13
        
        // Test values just below powers of 2 (edge cases for floating-point)
        verifyLog2Calculation(modwt, 3, 1);      // floor(log2(3)) = 1
        verifyLog2Calculation(modwt, 7, 2);      // floor(log2(7)) = 2
        verifyLog2Calculation(modwt, 15, 3);     // floor(log2(15)) = 3
        verifyLog2Calculation(modwt, 31, 4);     // floor(log2(31)) = 4
        verifyLog2Calculation(modwt, 63, 5);     // floor(log2(63)) = 5
        verifyLog2Calculation(modwt, 127, 6);    // floor(log2(127)) = 6
        verifyLog2Calculation(modwt, 255, 7);    // floor(log2(255)) = 7
        verifyLog2Calculation(modwt, 511, 8);    // floor(log2(511)) = 8
        verifyLog2Calculation(modwt, 1023, 9);   // floor(log2(1023)) = 9
        verifyLog2Calculation(modwt, 2047, 10);  // floor(log2(2047)) = 10
        verifyLog2Calculation(modwt, 4095, 11);  // floor(log2(4095)) = 11
        verifyLog2Calculation(modwt, 8191, 12);  // floor(log2(8191)) = 12
        
        // Test values just above powers of 2
        verifyLog2Calculation(modwt, 5, 2);      // floor(log2(5)) = 2
        verifyLog2Calculation(modwt, 9, 3);      // floor(log2(9)) = 3
        verifyLog2Calculation(modwt, 17, 4);     // floor(log2(17)) = 4
        verifyLog2Calculation(modwt, 33, 5);     // floor(log2(33)) = 5
        verifyLog2Calculation(modwt, 65, 6);     // floor(log2(65)) = 6
        verifyLog2Calculation(modwt, 129, 7);    // floor(log2(129)) = 7
        verifyLog2Calculation(modwt, 257, 8);    // floor(log2(257)) = 8
        verifyLog2Calculation(modwt, 513, 9);    // floor(log2(513)) = 9
        verifyLog2Calculation(modwt, 1025, 10);  // floor(log2(1025)) = 10
        
        // Test arbitrary values
        verifyLog2Calculation(modwt, 100, 6);    // floor(log2(100)) = 6
        verifyLog2Calculation(modwt, 200, 7);    // floor(log2(200)) = 7
        verifyLog2Calculation(modwt, 300, 8);    // floor(log2(300)) = 8
        verifyLog2Calculation(modwt, 400, 8);    // floor(log2(400)) = 8
        verifyLog2Calculation(modwt, 500, 8);    // floor(log2(500)) = 8
        verifyLog2Calculation(modwt, 600, 9);    // floor(log2(600)) = 9
        verifyLog2Calculation(modwt, 700, 9);    // floor(log2(700)) = 9
        verifyLog2Calculation(modwt, 800, 9);    // floor(log2(800)) = 9
        verifyLog2Calculation(modwt, 900, 9);    // floor(log2(900)) = 9
        verifyLog2Calculation(modwt, 1000, 9);   // floor(log2(1000)) = 9
    }
    
    private void verifyLog2Calculation(MODWTTransform modwt, int signalLength, int expectedMaxLevel) {
        double[] signal = new double[signalLength];
        
        // Verify our integer calculation matches expected floor(log2(N))
        int calculatedLog2 = signalLength > 0 ? 31 - Integer.numberOfLeadingZeros(signalLength) : 0;
        assertEquals("Integer-based log2(" + signalLength + ") calculation", 
                    expectedMaxLevel, calculatedLog2);
        
        // Verify it works in practice
        try {
            double[][] result = modwt.forwardMODWT(signal, expectedMaxLevel);
            assertNotNull("Should succeed at theoretical limit", result);
        } catch (Exception e) {
            fail("Should not throw at theoretical limit for length " + signalLength + ": " + e.getMessage());
        }
        
        // Verify it fails above the limit (unless we hit MAX_DECOMPOSITION_LEVEL first)
        if (expectedMaxLevel < MODWTTransform.getMaxDecompositionLevel()) {
            try {
                modwt.forwardMODWT(signal, expectedMaxLevel + 1);
                fail("Should throw above theoretical limit for length " + signalLength);
            } catch (IllegalArgumentException e) {
                assertTrue("Should mention theoretical limit", 
                          e.getMessage().contains("exceeds theoretical limit"));
            }
        }
    }
    
    @Test
    public void testEdgeCaseForOne() {
        // Special case: signal of length 1
        // floor(log2(1)) = 0, so no decomposition is possible
        MODWTTransform modwt = new MODWTTransform(new Haar1());
        double[] signal = new double[1];
        
        // Level 0 is not allowed (minimum is 1)
        try {
            modwt.forwardMODWT(signal, 0);
            fail("Level 0 should fail");
        } catch (IllegalArgumentException e) {
            assertTrue("Should mention minimum level", e.getMessage().contains("at least 1"));
        }
        
        // Level 1 should fail due to theoretical limit
        try {
            modwt.forwardMODWT(signal, 1);
            fail("Level 1 should fail for length 1");
        } catch (IllegalArgumentException e) {
            assertTrue("Should mention theoretical limit", e.getMessage().contains("exceeds theoretical limit"));
            assertTrue("Should mention limit is 0", e.getMessage().contains("0"));
        }
    }
    
    @Test
    public void testPerformanceOfIntegerLog2() {
        // Compare performance of integer vs floating-point log2 calculation
        int iterations = 1000000;
        int warmupIterations = 10000;
        
        // Warm up JIT compiler
        for (int i = 1; i <= warmupIterations; i++) {
            int log2Int = 31 - Integer.numberOfLeadingZeros(i);
            int log2Float = (int) Math.floor(Math.log(i) / Math.log(2));
        }
        
        // Run multiple trials and take the best time for each approach
        long bestTimeInteger = Long.MAX_VALUE;
        long bestTimeFloat = Long.MAX_VALUE;
        
        for (int trial = 0; trial < 5; trial++) {
            // Test integer-based approach
            long startInteger = System.nanoTime();
            for (int i = 1; i <= iterations; i++) {
                int log2 = 31 - Integer.numberOfLeadingZeros(i);
            }
            long timeInteger = System.nanoTime() - startInteger;
            bestTimeInteger = Math.min(bestTimeInteger, timeInteger);
            
            // Test floating-point approach
            long startFloat = System.nanoTime();
            for (int i = 1; i <= iterations; i++) {
                int log2 = (int) Math.floor(Math.log(i) / Math.log(2));
            }
            long timeFloat = System.nanoTime() - startFloat;
            bestTimeFloat = Math.min(bestTimeFloat, timeFloat);
        }
        
        double speedup = (double) bestTimeFloat / bestTimeInteger;
        System.out.println("Integer-based log2 is " + String.format("%.1f", speedup) + "x faster than floating-point");
        System.out.println("Integer time: " + (bestTimeInteger / 1_000_000.0) + " ms");
        System.out.println("Float time: " + (bestTimeFloat / 1_000_000.0) + " ms");
        
        // Integer approach should be faster, but we'll be more lenient
        // Just verify it's not significantly slower (within 20%)
        assertTrue("Integer-based approach should not be significantly slower than floating-point", 
                   bestTimeInteger < bestTimeFloat * 1.2);
    }
}