/**
 * JWave Enhanced Edition
 * <p>
 * Copyright 2025 Prophetizo and original authors
 * <p>
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

/**
 * Interface for streaming wavelet transforms that process data incrementally.
 *
 * Implementations maintain a circular buffer of fixed size and update transform
 * coefficients as new data arrives, avoiding the need to reprocess the entire
 * signal for each update.
 *
 * @param <T> The type of coefficients returned by the transform
 *
 * @author Prophetizo
 * @date 2025-07-12
 */
public interface StreamingTransform<T> {

    /**
     * Initialize the streaming transform with buffer size and parameters.
     *
     * @param bufferSize The size of the circular buffer (lookback window)
     * @param maxLevel Maximum decomposition level for transforms that support it
     * @throws IllegalArgumentException if bufferSize or maxLevel are invalid
     */
    void initialize(int bufferSize, int maxLevel);

    /**
     * Process new samples and return updated transform coefficients.
     *
     * The new samples are added to the circular buffer, potentially overwriting
     * the oldest samples, and the transform is updated incrementally where possible.
     *
     * @param newSamples Array of new samples to process
     * @return Updated transform coefficients
     * @throws IllegalStateException if not initialized
     * @throws IllegalArgumentException if newSamples is null or empty
     */
    T update(double[] newSamples);

    /**
     * Process a single new sample and return updated transform coefficients.
     *
     * Convenience method for single-sample updates.
     *
     * @param newSample The new sample to process
     * @return Updated transform coefficients
     * @throws IllegalStateException if not initialized
     */
    T update(double newSample);

    /**
     * Get the current transform coefficients without adding new data.
     *
     * @return Current transform coefficients
     * @throws IllegalStateException if not initialized
     */
    T getCurrentCoefficients();

    /**
     * Get the current buffer contents as an array.
     *
     * @return Copy of the current circular buffer contents in chronological order
     * @throws IllegalStateException if not initialized
     */
    double[] getCurrentBuffer();

    /**
     * Reset the buffer and transform state.
     *
     * Clears all data and resets to initial state.
     */
    void reset();

    /**
     * Get the configured buffer size.
     *
     * @return The size of the circular buffer
     */
    int getBufferSize();

    /**
     * Get the maximum decomposition level.
     *
     * @return Maximum level for multi-level transforms, or -1 if not applicable
     */
    int getMaxLevel();

    /**
     * Check if the buffer is full.
     *
     * @return true if the buffer has been filled at least once
     */
    boolean isBufferFull();
}