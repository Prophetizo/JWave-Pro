/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

/**
 * Configuration for streaming transforms.
 * 
 * This class uses the builder pattern to provide flexible configuration
 * of streaming transform parameters.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class StreamingTransformConfig {
    
    public enum UpdateStrategy {
        /**
         * Update all coefficients on each new sample.
         * Most accurate but slowest.
         */
        FULL,
        
        /**
         * Update only affected coefficients based on new samples.
         * Good balance of accuracy and performance.
         */
        INCREMENTAL,
        
        /**
         * Update coefficients lazily on demand.
         * Fastest but may have slight delay in coefficient availability.
         */
        LAZY
    }
    
    private final int bufferSize;
    private final int maxLevel;
    private final UpdateStrategy updateStrategy;
    private final boolean cacheIntermediateResults;
    private final boolean enableParallelProcessing;
    private final int updateBatchSize;
    
    private StreamingTransformConfig(Builder builder) {
        this.bufferSize = builder.bufferSize;
        this.maxLevel = builder.maxLevel;
        this.updateStrategy = builder.updateStrategy;
        this.cacheIntermediateResults = builder.cacheIntermediateResults;
        this.enableParallelProcessing = builder.enableParallelProcessing;
        this.updateBatchSize = builder.updateBatchSize;
    }
    
    public int getBufferSize() {
        return bufferSize;
    }
    
    public int getMaxLevel() {
        return maxLevel;
    }
    
    public UpdateStrategy getUpdateStrategy() {
        return updateStrategy;
    }
    
    public boolean isCacheIntermediateResults() {
        return cacheIntermediateResults;
    }
    
    public boolean isEnableParallelProcessing() {
        return enableParallelProcessing;
    }
    
    public int getUpdateBatchSize() {
        return updateBatchSize;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int bufferSize = 1024;
        private int maxLevel = -1; // Auto-detect based on buffer size
        private UpdateStrategy updateStrategy = UpdateStrategy.INCREMENTAL;
        private boolean cacheIntermediateResults = true;
        private boolean enableParallelProcessing = false;
        private int updateBatchSize = 1;
        
        public Builder bufferSize(int bufferSize) {
            if (bufferSize <= 0) {
                throw new IllegalArgumentException("Buffer size must be positive: " + bufferSize);
            }
            this.bufferSize = bufferSize;
            return this;
        }
        
        public Builder maxLevel(int maxLevel) {
            if (maxLevel < -1) {
                throw new IllegalArgumentException("Max level must be >= -1: " + maxLevel);
            }
            this.maxLevel = maxLevel;
            return this;
        }
        
        public Builder updateStrategy(UpdateStrategy strategy) {
            if (strategy == null) {
                throw new IllegalArgumentException("Update strategy cannot be null");
            }
            this.updateStrategy = strategy;
            return this;
        }
        
        public Builder cacheIntermediateResults(boolean cache) {
            this.cacheIntermediateResults = cache;
            return this;
        }
        
        public Builder enableParallelProcessing(boolean enable) {
            this.enableParallelProcessing = enable;
            return this;
        }
        
        public Builder updateBatchSize(int batchSize) {
            if (batchSize <= 0) {
                throw new IllegalArgumentException("Update batch size must be positive: " + batchSize);
            }
            this.updateBatchSize = batchSize;
            return this;
        }
        
        public StreamingTransformConfig build() {
            // Auto-detect max level if not specified
            if (maxLevel == -1) {
                maxLevel = (int) (Math.log(bufferSize) / Math.log(2));
            }
            
            // Validate configuration
            if (maxLevel > (int) (Math.log(bufferSize) / Math.log(2))) {
                throw new IllegalStateException(
                    "Max level " + maxLevel + " too high for buffer size " + bufferSize
                );
            }
            
            return new StreamingTransformConfig(this);
        }
    }
}