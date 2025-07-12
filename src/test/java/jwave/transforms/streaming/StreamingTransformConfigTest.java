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
 * Unit tests for StreamingTransformConfig and its Builder.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class StreamingTransformConfigTest {
    
    @Test
    public void testDefaultConfiguration() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        
        assertEquals(1024, config.getBufferSize());
        assertEquals(10, config.getMaxLevel()); // Auto-detected: log2(1024) = 10
        assertEquals(StreamingTransformConfig.UpdateStrategy.INCREMENTAL, config.getUpdateStrategy());
        assertTrue(config.isCacheIntermediateResults());
        assertFalse(config.isParallelProcessingEnabled());
        assertEquals(1, config.getUpdateBatchSize());
    }
    
    @Test
    public void testCustomConfiguration() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(2048)
            .maxLevel(5)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.FULL)
            .cacheIntermediateResults(false)
            .parallelProcessingEnabled(true)
            .updateBatchSize(10)
            .build();
        
        assertEquals(2048, config.getBufferSize());
        assertEquals(5, config.getMaxLevel());
        assertEquals(StreamingTransformConfig.UpdateStrategy.FULL, config.getUpdateStrategy());
        assertFalse(config.isCacheIntermediateResults());
        assertTrue(config.isParallelProcessingEnabled());
        assertEquals(10, config.getUpdateBatchSize());
    }
    
    @Test
    public void testMaxLevelAutoDetection() {
        // Test various buffer sizes
        StreamingTransformConfig config1 = StreamingTransformConfig.builder()
            .bufferSize(256)
            .build();
        assertEquals(8, config1.getMaxLevel()); // log2(256) = 8
        
        StreamingTransformConfig config2 = StreamingTransformConfig.builder()
            .bufferSize(512)
            .build();
        assertEquals(9, config2.getMaxLevel()); // log2(512) = 9
        
        StreamingTransformConfig config3 = StreamingTransformConfig.builder()
            .bufferSize(4096)
            .build();
        assertEquals(12, config3.getMaxLevel()); // log2(4096) = 12
        
        // Non-power-of-2 buffer size
        StreamingTransformConfig config4 = StreamingTransformConfig.builder()
            .bufferSize(1000)
            .build();
        assertEquals(9, config4.getMaxLevel()); // floor(log2(1000)) = 9
    }
    
    @Test
    public void testBuilderReuse() {
        StreamingTransformConfig.Builder builder = StreamingTransformConfig.builder()
            .bufferSize(512);
        
        // Build first config with auto-detected maxLevel
        StreamingTransformConfig config1 = builder.build();
        assertEquals(9, config1.getMaxLevel());
        
        // Reuse builder with explicit maxLevel
        StreamingTransformConfig config2 = builder.maxLevel(5).build();
        assertEquals(5, config2.getMaxLevel());
        
        // Build again - should still use explicit maxLevel
        StreamingTransformConfig config3 = builder.build();
        assertEquals(5, config3.getMaxLevel());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBufferSize() {
        StreamingTransformConfig.builder()
            .bufferSize(0)
            .build();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeBufferSize() {
        StreamingTransformConfig.builder()
            .bufferSize(-100)
            .build();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMaxLevel() {
        StreamingTransformConfig.builder()
            .maxLevel(-2) // -1 is valid (auto-detect), -2 is not
            .build();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMaxLevelTooHighForBufferSize() {
        StreamingTransformConfig.builder()
            .bufferSize(256)  // max possible level = 8
            .maxLevel(10)     // too high!
            .build();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullUpdateStrategy() {
        StreamingTransformConfig.builder()
            .updateStrategy(null)
            .build();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUpdateBatchSize() {
        StreamingTransformConfig.builder()
            .updateBatchSize(0)
            .build();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeUpdateBatchSize() {
        StreamingTransformConfig.builder()
            .updateBatchSize(-5)
            .build();
    }
    
    @Test
    public void testBoundaryMaxLevel() {
        // Test exact boundary case
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(1024)
            .maxLevel(10) // Exactly log2(1024)
            .build();
        assertEquals(10, config.getMaxLevel());
    }
    
    @Test
    public void testUpdateStrategies() {
        for (StreamingTransformConfig.UpdateStrategy strategy : 
             StreamingTransformConfig.UpdateStrategy.values()) {
            StreamingTransformConfig config = StreamingTransformConfig.builder()
                .updateStrategy(strategy)
                .build();
            assertEquals(strategy, config.getUpdateStrategy());
        }
    }
}