/**
 * JWave Enhanced Edition
 * <p>
 * Copyright 2025 Prophetizo and original authors
 * <p>
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for CircularBuffer.
 *
 * @author Prophetizo
 * @date 2025-07-12
 */
public class CircularBufferTest {

    private static final double DELTA = 1e-10;

    @Test
    public void testBasicOperations() {
        CircularBuffer buffer = new CircularBuffer(5);

        assertEquals(5, buffer.capacity());
        assertEquals(0, buffer.size());
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertFalse(buffer.hasWrapped());
    }

    @Test
    public void testSingleAppend() {
        CircularBuffer buffer = new CircularBuffer(5);

        buffer.append(1.0);
        assertEquals(1, buffer.size());
        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertEquals(1.0, buffer.get(0), DELTA);
        assertEquals(1.0, buffer.getLast(), DELTA);
    }

    @Test
    public void testMultipleAppend() {
        CircularBuffer buffer = new CircularBuffer(5);

        buffer.append(new double[]{1.0, 2.0, 3.0});
        assertEquals(3, buffer.size());
        assertEquals(1.0, buffer.get(0), DELTA);
        assertEquals(2.0, buffer.get(1), DELTA);
        assertEquals(3.0, buffer.get(2), DELTA);
        assertEquals(3.0, buffer.getLast(), DELTA);
    }

    @Test
    public void testBufferWrap() {
        CircularBuffer buffer = new CircularBuffer(3);

        // Fill buffer
        buffer.append(new double[]{1.0, 2.0, 3.0});
        assertTrue(buffer.isFull());
        assertFalse(buffer.hasWrapped());

        // Cause wrap
        buffer.append(4.0);
        assertTrue(buffer.hasWrapped());
        assertEquals(3, buffer.size()); // Size stays at capacity

        // Check contents: should be [2.0, 3.0, 4.0]
        double[] contents = buffer.toArray();
        assertArrayEquals(new double[]{2.0, 3.0, 4.0}, contents, DELTA);
    }

    @Test
    public void testToArray() {
        CircularBuffer buffer = new CircularBuffer(4);

        // Test empty buffer
        assertArrayEquals(new double[0], buffer.toArray(), DELTA);

        // Test partially filled
        buffer.append(new double[]{1.0, 2.0});
        assertArrayEquals(new double[]{1.0, 2.0}, buffer.toArray(), DELTA);

        // Test after wrap
        buffer.append(new double[]{3.0, 4.0, 5.0, 6.0});
        assertArrayEquals(new double[]{3.0, 4.0, 5.0, 6.0}, buffer.toArray(), DELTA);
    }

    @Test
    public void testGetWindow() {
        CircularBuffer buffer = new CircularBuffer(5);
        buffer.append(new double[]{1.0, 2.0, 3.0, 4.0, 5.0});

        // Get most recent 3
        double[] window = buffer.getWindow(0, 3);
        assertArrayEquals(new double[]{3.0, 4.0, 5.0}, window, DELTA);

        // Get with offset
        window = buffer.getWindow(2, 3);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, window, DELTA);

        // Get with padding (request more than available)
        window = buffer.getWindow(3, 5);
        assertArrayEquals(new double[]{0.0, 0.0, 0.0, 1.0, 2.0}, window, DELTA);
    }

    @Test
    public void testGetWindowAfterWrap() {
        CircularBuffer buffer = new CircularBuffer(3);
        buffer.append(new double[]{1.0, 2.0, 3.0, 4.0, 5.0});

        // Buffer now contains [3.0, 4.0, 5.0]
        double[] window = buffer.getWindow(0, 3);
        assertArrayEquals(new double[]{3.0, 4.0, 5.0}, window, DELTA);

        window = buffer.getWindow(1, 2);
        assertArrayEquals(new double[]{3.0, 4.0}, window, DELTA);
    }

    @Test
    public void testGetWindowOffsetExceedsSize() {
        CircularBuffer buffer = new CircularBuffer(5);
        buffer.append(new double[]{1.0, 2.0, 3.0});

        // Request window with offset beyond available data
        double[] window = buffer.getWindow(5, 3);
        assertArrayEquals(new double[]{0.0, 0.0, 0.0}, window, DELTA);

        // Request window with offset at boundary
        window = buffer.getWindow(3, 3);
        assertArrayEquals(new double[]{0.0, 0.0, 0.0}, window, DELTA);

        // Request window with offset partially beyond available data
        window = buffer.getWindow(2, 3);
        assertArrayEquals(new double[]{0.0, 0.0, 1.0}, window, DELTA);
    }

    @Test
    public void testClear() {
        CircularBuffer buffer = new CircularBuffer(3);
        buffer.append(new double[]{1.0, 2.0, 3.0});

        buffer.clear();
        assertEquals(0, buffer.size());
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.hasWrapped());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCapacity() {
        new CircularBuffer(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullAppend() {
        CircularBuffer buffer = new CircularBuffer(5);
        buffer.append(null);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetOutOfBounds() {
        CircularBuffer buffer = new CircularBuffer(5);
        buffer.append(1.0);
        buffer.get(1); // Only index 0 is valid
    }

    @Test(expected = IllegalStateException.class)
    public void testGetLastEmpty() {
        CircularBuffer buffer = new CircularBuffer(5);
        buffer.getLast();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetWindowNegativeOffset() {
        CircularBuffer buffer = new CircularBuffer(5);
        buffer.append(new double[]{1.0, 2.0, 3.0});
        buffer.getWindow(-1, 2); // Should throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetWindowZeroLength() {
        CircularBuffer buffer = new CircularBuffer(5);
        buffer.append(new double[]{1.0, 2.0, 3.0});
        buffer.getWindow(0, 0); // Should throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetWindowNegativeLength() {
        CircularBuffer buffer = new CircularBuffer(5);
        buffer.append(new double[]{1.0, 2.0, 3.0});
        buffer.getWindow(0, -5); // Should throw
    }

    @Test
    public void testBulkAppendOptimization() {
        CircularBuffer buffer = new CircularBuffer(10);

        // Test bulk append without wrap
        double[] data1 = {1.0, 2.0, 3.0, 4.0, 5.0};
        buffer.append(data1);
        assertArrayEquals(data1, buffer.toArray(), DELTA);

        // Test bulk append with wrap
        double[] data2 = {6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0};
        buffer.append(data2);
        // Should contain: [3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
        assertArrayEquals(new double[]{3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0},
                buffer.toArray(), DELTA);
    }

    @Test
    public void testBulkAppendExceedsCapacity() {
        CircularBuffer buffer = new CircularBuffer(5);

        // Append more samples than capacity
        double[] largeSamples = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        buffer.append(largeSamples);

        // Should only keep the last 5 samples
        assertArrayEquals(new double[]{6.0, 7.0, 8.0, 9.0, 10.0}, buffer.toArray(), DELTA);
        assertEquals(5, buffer.size());
        assertTrue(buffer.hasWrapped());
    }

    @Test
    public void testBulkAppendExactCapacity() {
        CircularBuffer buffer = new CircularBuffer(5);

        // Append exactly capacity samples
        double[] samples = {1.0, 2.0, 3.0, 4.0, 5.0};
        buffer.append(samples);

        assertArrayEquals(samples, buffer.toArray(), DELTA);
        assertEquals(5, buffer.size());
        assertTrue(buffer.isFull());
        assertFalse(buffer.hasWrapped()); // Haven't overwritten anything yet
    }

    @Test
    public void testBulkAppendEmpty() {
        CircularBuffer buffer = new CircularBuffer(5);
        buffer.append(1.0);

        // Append empty array should not change buffer
        buffer.append(new double[0]);

        assertEquals(1, buffer.size());
        assertEquals(1.0, buffer.get(0), DELTA);
    }

    @Test
    public void testSmallArrayFallback() {
        CircularBuffer buffer = new CircularBuffer(10);

        // Small arrays should use element-wise append
        double[] small = {1.0, 2.0, 3.0};
        buffer.append(small);

        assertArrayEquals(small, buffer.toArray(), DELTA);
        assertEquals(3, buffer.size());
    }
}