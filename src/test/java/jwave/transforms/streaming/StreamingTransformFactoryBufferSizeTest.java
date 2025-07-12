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

/**
 * Targeted unit tests for StreamingTransformFactory.getRecommendedBufferSize().
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class StreamingTransformFactoryBufferSizeTest {
    
    @Test
    public void testFWTRecommendedBufferSizes() {
        StreamingTransformFactory.TransformType type = StreamingTransformFactory.TransformType.FWT;
        
        // Test minimum buffer size enforcement
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 0));
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 1));
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 2));
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 3));
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 4));
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 5));
        
        // Test larger levels
        assertEquals(512, StreamingTransformFactory.getRecommendedBufferSize(type, 6));
        assertEquals(1024, StreamingTransformFactory.getRecommendedBufferSize(type, 7));
        assertEquals(2048, StreamingTransformFactory.getRecommendedBufferSize(type, 8));
        assertEquals(4096, StreamingTransformFactory.getRecommendedBufferSize(type, 9));
        assertEquals(8192, StreamingTransformFactory.getRecommendedBufferSize(type, 10));
        
        // Verify power-of-2 results
        for (int level = 0; level <= 15; level++) {
            int bufferSize = StreamingTransformFactory.getRecommendedBufferSize(type, level);
            assertTrue("Buffer size should be power of 2", (bufferSize & (bufferSize - 1)) == 0);
        }
    }
    
    @Test
    public void testWPTRecommendedBufferSizes() {
        StreamingTransformFactory.TransformType type = StreamingTransformFactory.TransformType.WPT;
        
        // WPT should behave the same as FWT
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 0));
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 5));
        assertEquals(512, StreamingTransformFactory.getRecommendedBufferSize(type, 6));
        assertEquals(1024, StreamingTransformFactory.getRecommendedBufferSize(type, 7));
        
        // Verify power-of-2 results
        for (int level = 0; level <= 15; level++) {
            int bufferSize = StreamingTransformFactory.getRecommendedBufferSize(type, level);
            assertTrue("Buffer size should be power of 2", (bufferSize & (bufferSize - 1)) == 0);
        }
    }
    
    @Test
    public void testMODWTRecommendedBufferSizes() {
        StreamingTransformFactory.TransformType type = StreamingTransformFactory.TransformType.MODWT;
        
        // Test minimum buffer size enforcement
        assertEquals(512, StreamingTransformFactory.getRecommendedBufferSize(type, 0));
        assertEquals(512, StreamingTransformFactory.getRecommendedBufferSize(type, 1));
        assertEquals(512, StreamingTransformFactory.getRecommendedBufferSize(type, 2));
        assertEquals(512, StreamingTransformFactory.getRecommendedBufferSize(type, 3));
        assertEquals(512, StreamingTransformFactory.getRecommendedBufferSize(type, 4));
        
        // Test scaling with level
        assertEquals(640, StreamingTransformFactory.getRecommendedBufferSize(type, 5));
        assertEquals(768, StreamingTransformFactory.getRecommendedBufferSize(type, 6));
        assertEquals(896, StreamingTransformFactory.getRecommendedBufferSize(type, 7));
        assertEquals(1024, StreamingTransformFactory.getRecommendedBufferSize(type, 8));
        assertEquals(1152, StreamingTransformFactory.getRecommendedBufferSize(type, 9));
        assertEquals(1280, StreamingTransformFactory.getRecommendedBufferSize(type, 10));
        
        // MODWT doesn't require power-of-2
        assertEquals(1920, StreamingTransformFactory.getRecommendedBufferSize(type, 15));
        assertEquals(2560, StreamingTransformFactory.getRecommendedBufferSize(type, 20));
    }
    
    @Test
    public void testCWTRecommendedBufferSizes() {
        StreamingTransformFactory.TransformType type = StreamingTransformFactory.TransformType.CWT;
        
        // Test minimum buffer size enforcement
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 0));
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 1));
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 2));
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 3));
        
        // Test scaling with scale
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(type, 4));
        assertEquals(320, StreamingTransformFactory.getRecommendedBufferSize(type, 5));
        assertEquals(384, StreamingTransformFactory.getRecommendedBufferSize(type, 6));
        assertEquals(448, StreamingTransformFactory.getRecommendedBufferSize(type, 7));
        assertEquals(512, StreamingTransformFactory.getRecommendedBufferSize(type, 8));
        assertEquals(640, StreamingTransformFactory.getRecommendedBufferSize(type, 10));
        
        // CWT doesn't require power-of-2
        assertEquals(960, StreamingTransformFactory.getRecommendedBufferSize(type, 15));
        assertEquals(1280, StreamingTransformFactory.getRecommendedBufferSize(type, 20));
    }
    
    @Test
    public void testFFTRecommendedBufferSizes() {
        StreamingTransformFactory.TransformType type = StreamingTransformFactory.TransformType.FFT;
        
        // Test minimum buffer size enforcement
        for (int level = 0; level <= 10; level++) {
            assertEquals(1024, StreamingTransformFactory.getRecommendedBufferSize(type, level));
        }
        
        // Test larger levels
        assertEquals(2048, StreamingTransformFactory.getRecommendedBufferSize(type, 11));
        assertEquals(4096, StreamingTransformFactory.getRecommendedBufferSize(type, 12));
        assertEquals(8192, StreamingTransformFactory.getRecommendedBufferSize(type, 13));
        assertEquals(16384, StreamingTransformFactory.getRecommendedBufferSize(type, 14));
        assertEquals(32768, StreamingTransformFactory.getRecommendedBufferSize(type, 15));
        
        // Verify power-of-2 results
        for (int level = 0; level <= 20; level++) {
            int bufferSize = StreamingTransformFactory.getRecommendedBufferSize(type, level);
            assertTrue("FFT buffer size should be power of 2", 
                      (bufferSize & (bufferSize - 1)) == 0);
        }
    }
    
    @Test
    public void testDFTRecommendedBufferSizes() {
        StreamingTransformFactory.TransformType type = StreamingTransformFactory.TransformType.DFT;
        
        // DFT should behave the same as FFT
        for (int level = 0; level <= 10; level++) {
            assertEquals(1024, StreamingTransformFactory.getRecommendedBufferSize(type, level));
        }
        
        assertEquals(2048, StreamingTransformFactory.getRecommendedBufferSize(type, 11));
        assertEquals(4096, StreamingTransformFactory.getRecommendedBufferSize(type, 12));
        
        // Verify power-of-2 results
        for (int level = 0; level <= 20; level++) {
            int bufferSize = StreamingTransformFactory.getRecommendedBufferSize(type, level);
            assertTrue("DFT buffer size should be power of 2", 
                      (bufferSize & (bufferSize - 1)) == 0);
        }
    }
    
    @Test
    public void testEdgeCases() {
        // Test with very large levels
        assertEquals(1048576, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FWT, 17)); // 2^20
            
        // MODWT and CWT can handle very large levels without overflow
        assertEquals(12800, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.MODWT, 100));
        assertEquals(6400, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.CWT, 100));
    }
    
    @Test
    public void testOverflowProtection() {
        // Test that very large levels don't cause bit shift overflow
        // Should cap at 2^30 instead of overflowing
        int maxExpected = 1 << 30; // 2^30 = 1,073,741,824
        
        // FWT with huge level should cap at MAX_BUFFER_POWER
        assertEquals(maxExpected, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FWT, 50));
        assertEquals(maxExpected, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FWT, 100));
        assertEquals(maxExpected, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FWT, Integer.MAX_VALUE));
        
        // WPT should behave the same
        assertEquals(maxExpected, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.WPT, 50));
        
        // FFT/DFT with huge level should also cap
        assertEquals(maxExpected, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FFT, 40));
        assertEquals(maxExpected, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.DFT, 40));
        
        // Edge case: exactly at the boundary
        assertEquals(maxExpected, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FWT, 27)); // 27 + 3 = 30
        assertEquals(maxExpected, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FFT, 30)); // exactly 30
    }
    
    @Test
    public void testConsistencyAcrossTypes() {
        // FWT and WPT should always return the same values
        for (int level = 0; level <= 15; level++) {
            assertEquals(
                StreamingTransformFactory.getRecommendedBufferSize(
                    StreamingTransformFactory.TransformType.FWT, level),
                StreamingTransformFactory.getRecommendedBufferSize(
                    StreamingTransformFactory.TransformType.WPT, level)
            );
        }
        
        // FFT and DFT should always return the same values
        for (int level = 0; level <= 15; level++) {
            assertEquals(
                StreamingTransformFactory.getRecommendedBufferSize(
                    StreamingTransformFactory.TransformType.FFT, level),
                StreamingTransformFactory.getRecommendedBufferSize(
                    StreamingTransformFactory.TransformType.DFT, level)
            );
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeDesiredLevel() {
        StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FWT, -1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeDesiredLevelForMODWT() {
        StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.MODWT, -5);
    }
    
    @Test
    public void testBufferSizeFormulas() {
        // Verify the formulas match the constants
        
        // FWT/WPT: 1 << max(level + 3, 8)
        assertEquals(1 << 8, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FWT, 5));
        assertEquals(1 << 9, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FWT, 6));
        
        // MODWT: max(level * 128, 512)
        assertEquals(512, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.MODWT, 3));
        assertEquals(5 * 128, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.MODWT, 5));
        
        // CWT: max(level * 64, 256)
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.CWT, 3));
        assertEquals(5 * 64, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.CWT, 5));
        
        // FFT/DFT: 1 << max(level, 10)
        assertEquals(1 << 10, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FFT, 9));
        assertEquals(1 << 11, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FFT, 11));
    }
}