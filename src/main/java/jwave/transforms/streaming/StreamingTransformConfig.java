/**
 * JWave Enhanced Edition
 * <p>
 * Copyright 2025 Prophetizo and original authors
 * <p>
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

    public static Builder builder() {
        return new Builder();
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

    public boolean isParallelProcessingEnabled() {
        return enableParallelProcessing;
    }

    public int getUpdateBatchSize() {
        return updateBatchSize;
    }

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

        public Builder parallelProcessingEnabled(boolean enabled) {
            this.enableParallelProcessing = enabled;
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
            // Store original maxLevel for potential reuse
            int originalMaxLevel = this.maxLevel;

            // Auto-detect max level if not specified
            if (this.maxLevel == -1) {
                this.maxLevel = (int) (Math.log(bufferSize) / StreamingConstants.LOG_2);
            }

            // Validate configuration
            int maxPossibleLevel = (int) (Math.log(bufferSize) / StreamingConstants.LOG_2);
            if (this.maxLevel > maxPossibleLevel) {
                throw new IllegalArgumentException(
                        "Max level " + this.maxLevel + " too high for buffer size " + bufferSize +
                                " (max possible: " + maxPossibleLevel + ")"
                );
            }

            // Create config with current values
            StreamingTransformConfig config = new StreamingTransformConfig(this);

            // Restore original maxLevel for builder reuse
            this.maxLevel = originalMaxLevel;

            return config;
        }
    }
}