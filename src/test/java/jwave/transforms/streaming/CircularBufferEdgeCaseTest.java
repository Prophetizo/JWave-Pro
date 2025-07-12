/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Random;

/**
 * Edge case tests for CircularBuffer.
 * 
 * These tests verify correct behavior in unusual or boundary conditions
 * that might not be covered by the standard unit tests.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class CircularBufferEdgeCaseTest {
    
    @Test
    public void testSingleCapacityBuffer() {
        CircularBuffer buffer = new CircularBuffer(1);
        
        // Add multiple values
        buffer.append(1.0);
        assertEquals(1.0, buffer.getLast(), 1e-10);
        
        buffer.append(2.0);
        assertEquals(2.0, buffer.getLast(), 1e-10);
        assertEquals(1, buffer.size());
        
        // Verify window extraction
        double[] window = buffer.getWindow(0, 1);
        assertEquals(1, window.length);
        assertEquals(2.0, window[0], 1e-10);
    }
    
    @Test
    public void testLargeCapacityBuffer() {
        int capacity = 1_000_000;
        CircularBuffer buffer = new CircularBuffer(capacity);
        
        // Fill partially
        for (int i = 0; i < 1000; i++) {
            buffer.append(i);
        }
        
        assertEquals(1000, buffer.size());
        assertFalse(buffer.isFull());
        assertFalse(buffer.hasWrapped());
        
        // Fill completely
        for (int i = 1000; i < capacity; i++) {
            buffer.append(i);
        }
        
        assertTrue(buffer.isFull());
        assertFalse(buffer.hasWrapped());
        
        // Cause wrap
        buffer.append(capacity);
        assertTrue(buffer.hasWrapped());
        assertEquals(capacity, buffer.size());
    }
    
    @Test
    public void testAlternatingPatterns() {
        CircularBuffer buffer = new CircularBuffer(100);
        
        // Add alternating positive/negative
        for (int i = 0; i < 200; i++) {
            buffer.append((i % 2 == 0) ? 1.0 : -1.0);
        }
        
        // Verify pattern is preserved
        double[] data = buffer.toArray();
        for (int i = 0; i < data.length; i++) {
            double expected = (i % 2 == 0) ? 1.0 : -1.0;
            assertEquals(expected, data[i], 1e-10);
        }
    }
    
    @Test
    public void testExtremeBulkAppends() {
        CircularBuffer buffer = new CircularBuffer(100);
        
        // Append array larger than capacity
        double[] huge = new double[1000];
        for (int i = 0; i < huge.length; i++) {
            huge[i] = i;
        }
        
        buffer.append(huge);
        
        // Should only keep last 100 elements
        assertEquals(100, buffer.size());
        assertTrue(buffer.isFull());
        assertTrue(buffer.hasWrapped());
        
        // Verify correct elements are kept
        double[] result = buffer.toArray();
        for (int i = 0; i < result.length; i++) {
            assertEquals(900 + i, result[i], 1e-10);
        }
    }
    
    @Test
    public void testWindowExtractionEdgeCases() {
        CircularBuffer buffer = new CircularBuffer(10);
        
        // Fill buffer
        for (int i = 0; i < 10; i++) {
            buffer.append(i);
        }
        
        // Request window larger than buffer
        double[] window = buffer.getWindow(0, 20);
        assertEquals(20, window.length);
        
        // First 10 should be zeros (padding)
        for (int i = 0; i < 10; i++) {
            assertEquals(0.0, window[i], 1e-10);
        }
        
        // Last 10 should be buffer contents
        for (int i = 10; i < 20; i++) {
            assertEquals(i - 10, window[i], 1e-10);
        }
        
        // Request window with large offset
        window = buffer.getWindow(20, 5);
        assertEquals(5, window.length);
        
        // Should be all zeros (beyond buffer)
        for (int i = 0; i < 5; i++) {
            assertEquals(0.0, window[i], 1e-10);
        }
    }
    
    @Test
    public void testRapidClearAndRefill() {
        CircularBuffer buffer = new CircularBuffer(50);
        Random random = new Random(42);
        
        // Rapid clear/fill cycles
        for (int cycle = 0; cycle < 100; cycle++) {
            // Fill with random data
            for (int i = 0; i < 50; i++) {
                buffer.append(random.nextDouble());
            }
            
            assertTrue(buffer.isFull());
            assertEquals(50, buffer.size());
            
            // Clear
            buffer.clear();
            
            assertTrue(buffer.isEmpty());
            assertEquals(0, buffer.size());
            assertFalse(buffer.hasWrapped());
        }
    }
    
    @Test
    public void testMixedSingleAndBulkAppends() {
        CircularBuffer buffer = new CircularBuffer(100);
        
        // Mix single and bulk appends
        buffer.append(1.0);
        buffer.append(new double[]{2.0, 3.0, 4.0});
        buffer.append(5.0);
        buffer.append(new double[]{6.0, 7.0, 8.0, 9.0, 10.0});
        
        double[] result = buffer.toArray();
        assertEquals(10, result.length);
        
        for (int i = 0; i < 10; i++) {
            assertEquals(i + 1.0, result[i], 1e-10);
        }
    }
    
    @Test
    public void testBoundaryValueAppends() {
        CircularBuffer buffer = new CircularBuffer(10);
        
        // Test special floating point values
        buffer.append(Double.MAX_VALUE);
        buffer.append(Double.MIN_VALUE);
        buffer.append(Double.MIN_NORMAL);
        buffer.append(-Double.MAX_VALUE);
        buffer.append(0.0);
        buffer.append(-0.0);
        buffer.append(Double.POSITIVE_INFINITY);
        buffer.append(Double.NEGATIVE_INFINITY);
        buffer.append(Double.NaN);
        
        double[] result = buffer.toArray();
        assertEquals(9, result.length);
        
        // Verify special values are preserved
        assertEquals(Double.MAX_VALUE, result[0], 0);
        assertEquals(Double.MIN_VALUE, result[1], 0);
        assertEquals(Double.MIN_NORMAL, result[2], 0);
        assertEquals(-Double.MAX_VALUE, result[3], 0);
        assertEquals(0.0, result[4], 0);
        assertEquals(-0.0, result[5], 0);
        assertEquals(Double.POSITIVE_INFINITY, result[6], 0);
        assertEquals(Double.NEGATIVE_INFINITY, result[7], 0);
        assertTrue(Double.isNaN(result[8]));
    }
    
    @Test
    public void testGetWindowWithWrappedBuffer() {
        CircularBuffer buffer = new CircularBuffer(10);
        
        // Fill buffer beyond capacity to ensure wrapping
        for (int i = 0; i < 25; i++) {
            buffer.append(i);
        }
        
        assertTrue(buffer.hasWrapped());
        
        // Get various windows
        double[] window1 = buffer.getWindow(0, 5); // Most recent 5
        double[] window2 = buffer.getWindow(5, 5); // Next 5
        double[] window3 = buffer.getWindow(0, 10); // Full buffer
        
        // Verify window1 (should be 20-24)
        for (int i = 0; i < 5; i++) {
            assertEquals(20 + i, window1[i], 1e-10);
        }
        
        // Verify window2 (should be 15-19)
        for (int i = 0; i < 5; i++) {
            assertEquals(15 + i, window2[i], 1e-10);
        }
        
        // Verify window3 (should be 15-24)
        for (int i = 0; i < 10; i++) {
            assertEquals(15 + i, window3[i], 1e-10);
        }
    }
    
    @Test
    public void testPrecisionPreservation() {
        CircularBuffer buffer = new CircularBuffer(100);
        
        // Add values with high precision requirements
        double[] preciseValues = {
            Math.PI,
            Math.E,
            Math.sqrt(2),
            Math.log(10),
            1.0 / 3.0,
            7.0 / 13.0
        };
        
        buffer.append(preciseValues);
        
        double[] result = buffer.toArray();
        
        for (int i = 0; i < preciseValues.length; i++) {
            assertEquals("Precision should be preserved",
                        preciseValues[i], result[i], 0.0);
        }
    }
}