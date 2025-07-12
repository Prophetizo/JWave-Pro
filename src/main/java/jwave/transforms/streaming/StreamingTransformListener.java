/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

/**
 * Listener interface for streaming transform events.
 * 
 * Implementations can be registered with streaming transforms to receive
 * notifications when new coefficients are computed or when the buffer
 * state changes.
 * 
 * @param <T> The type of coefficients produced by the transform
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public interface StreamingTransformListener<T> {
    
    /**
     * Called when new transform coefficients are available.
     * 
     * This method is invoked after each update() call on the streaming
     * transform. Implementations should process the coefficients quickly
     * to avoid blocking subsequent updates.
     * 
     * @param coefficients The updated transform coefficients
     * @param newSamplesCount Number of new samples that triggered this update
     */
    void onCoefficientsUpdated(T coefficients, int newSamplesCount);
    
    /**
     * Called when the circular buffer becomes full for the first time.
     * 
     * This event indicates that the transform is now operating on a
     * complete window of data rather than a partially filled buffer.
     */
    default void onBufferFull() {
        // Default empty implementation
    }
    
    /**
     * Called when the transform is reset.
     * 
     * This event indicates that all data has been cleared and the
     * transform has returned to its initial state.
     */
    default void onReset() {
        // Default empty implementation
    }
    
    /**
     * Called when an error occurs during transform processing.
     * 
     * @param error The exception that occurred
     * @param recoverable true if processing can continue, false if fatal
     */
    default void onError(Exception error, boolean recoverable) {
        // Default: log to stderr
        System.err.println("Streaming transform error: " + error.getMessage());
        if (!recoverable) {
            error.printStackTrace();
        }
    }
}