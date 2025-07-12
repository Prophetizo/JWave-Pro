/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.BasicTransform;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base class for streaming transform implementations.
 * 
 * This class provides common functionality for all streaming transforms including
 * buffer management, initialization validation, and helper methods. Subclasses
 * must implement the actual transform update logic.
 * 
 * @param <T> The type of coefficients returned by the transform
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public abstract class AbstractStreamingTransform<T> implements StreamingTransform<T> {
    
    protected final BasicTransform transform;
    protected CircularBuffer buffer;
    protected int bufferSize;
    protected int maxLevel;
    protected boolean initialized;
    protected final List<StreamingTransformListener<T>> listeners;
    
    /**
     * Create a new streaming transform wrapper.
     * 
     * @param transform The underlying transform implementation
     * @throws IllegalArgumentException if transform is null
     */
    protected AbstractStreamingTransform(BasicTransform transform) {
        if (transform == null) {
            throw new IllegalArgumentException("Transform cannot be null");
        }
        this.transform = transform;
        this.initialized = false;
        this.listeners = new CopyOnWriteArrayList<>();
    }
    
    @Override
    public synchronized void initialize(int bufferSize, int maxLevel) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive: " + bufferSize);
        }
        if (maxLevel < 0) {
            throw new IllegalArgumentException("Max level cannot be negative: " + maxLevel);
        }
        
        // Validate buffer size for specific transform requirements
        validateBufferSize(bufferSize);
        
        // Validate decomposition level
        validateMaxLevel(bufferSize, maxLevel);
        
        this.bufferSize = bufferSize;
        this.maxLevel = maxLevel;
        this.buffer = new CircularBuffer(bufferSize);
        
        // Allow subclasses to perform additional initialization
        initializeTransformState();
        
        this.initialized = true;
    }
    
    @Override
    public T update(double newSample) {
        return update(new double[] { newSample });
    }
    
    @Override
    public synchronized T update(double[] newSamples) {
        if (!initialized) {
            throw new IllegalStateException("Transform not initialized. Call initialize() first.");
        }
        if (newSamples == null) {
            throw new IllegalArgumentException("New samples array cannot be null");
        }
        if (newSamples.length == 0) {
            // Empty array is allowed, just return current coefficients
            return getCachedCoefficients();
        }
        
        // Track if buffer becomes full
        boolean wasFullBefore = buffer.isFull();
        
        // Add samples to buffer
        buffer.append(newSamples);
        
        // Check if buffer just became full
        if (!wasFullBefore && buffer.isFull()) {
            notifyBufferFull();
        }
        
        // Perform transform-specific update
        T coefficients = performUpdate(newSamples);
        
        // Notify listeners
        notifyCoefficientsUpdated(coefficients, newSamples.length);
        
        return coefficients;
    }
    
    @Override
    public synchronized T getCurrentCoefficients() {
        if (!initialized) {
            throw new IllegalStateException("Transform not initialized. Call initialize() first.");
        }
        
        // Return cached coefficients without update
        return getCachedCoefficients();
    }
    
    @Override
    public double[] getCurrentBuffer() {
        if (!initialized) {
            throw new IllegalStateException("Transform not initialized. Call initialize() first.");
        }
        
        return buffer.toArray();
    }
    
    @Override
    public synchronized void reset() {
        if (buffer != null) {
            buffer.clear();
        }
        resetTransformState();
        notifyReset();
        // Don't reset initialized flag - keep configuration
    }
    
    @Override
    public int getBufferSize() {
        return bufferSize;
    }
    
    @Override
    public int getMaxLevel() {
        return maxLevel;
    }
    
    @Override
    public boolean isBufferFull() {
        return initialized && buffer.isFull();
    }
    
    /**
     * Get the underlying transform implementation.
     * 
     * @return The wrapped transform
     */
    public BasicTransform getTransform() {
        return transform;
    }
    
    /**
     * Check if the transform has been initialized.
     * 
     * @return true if initialize() has been called
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Validate the buffer size for the specific transform type.
     * 
     * Subclasses should override this to enforce transform-specific constraints
     * (e.g., power-of-2 for FFT).
     * 
     * @param bufferSize The requested buffer size
     * @throws IllegalArgumentException if buffer size is invalid
     */
    protected void validateBufferSize(int bufferSize) {
        // Default: no additional validation
    }
    
    /**
     * Validate the maximum decomposition level.
     * 
     * @param bufferSize The buffer size
     * @param maxLevel The requested maximum level
     * @throws IllegalArgumentException if level is invalid for the buffer size
     */
    protected void validateMaxLevel(int bufferSize, int maxLevel) {
        // Calculate maximum possible level based on buffer size
        int maxPossibleLevel = (int) (Math.log(bufferSize) / StreamingConstants.LOG_2);
        if (maxLevel > maxPossibleLevel) {
            throw new IllegalArgumentException(
                "Maximum level " + maxLevel + " too high for buffer size " + bufferSize +
                " (max possible: " + maxPossibleLevel + ")"
            );
        }
    }
    
    /**
     * Perform transform-specific initialization.
     * 
     * Called after buffer creation but before marking as initialized.
     * Subclasses should create coefficient storage and other state here.
     */
    protected abstract void initializeTransformState();
    
    /**
     * Perform the incremental transform update.
     * 
     * @param newSamples The new samples that were added to the buffer
     * @return Updated transform coefficients
     */
    protected abstract T performUpdate(double[] newSamples);
    
    /**
     * Get the cached coefficients without performing an update.
     * 
     * @return Current cached coefficients
     */
    protected abstract T getCachedCoefficients();
    
    /**
     * Reset transform-specific state.
     * 
     * Called by reset() to clear coefficient caches and other state.
     */
    protected abstract void resetTransformState();
    
    /**
     * Add a listener for transform events.
     * 
     * @param listener The listener to add
     * @throws IllegalArgumentException if listener is null
     */
    public void addListener(StreamingTransformListener<T> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        listeners.add(listener);
    }
    
    /**
     * Remove a listener.
     * 
     * @param listener The listener to remove
     * @return true if the listener was removed
     */
    public boolean removeListener(StreamingTransformListener<T> listener) {
        return listeners.remove(listener);
    }
    
    /**
     * Remove all listeners.
     */
    public void clearListeners() {
        listeners.clear();
    }
    
    /**
     * Get the number of registered listeners.
     * 
     * @return The listener count
     */
    public int getListenerCount() {
        return listeners.size();
    }
    
    /**
     * Notify listeners that coefficients have been updated.
     * 
     * @param coefficients The new coefficients
     * @param newSamplesCount Number of new samples
     */
    protected void notifyCoefficientsUpdated(T coefficients, int newSamplesCount) {
        for (StreamingTransformListener<T> listener : listeners) {
            try {
                listener.onCoefficientsUpdated(coefficients, newSamplesCount);
            } catch (Exception e) {
                notifyError(e, true);
            }
        }
    }
    
    /**
     * Notify listeners that the buffer is full.
     */
    protected void notifyBufferFull() {
        for (StreamingTransformListener<T> listener : listeners) {
            try {
                listener.onBufferFull();
            } catch (Exception e) {
                notifyError(e, true);
            }
        }
    }
    
    /**
     * Notify listeners that the transform has been reset.
     */
    protected void notifyReset() {
        for (StreamingTransformListener<T> listener : listeners) {
            try {
                listener.onReset();
            } catch (Exception e) {
                notifyError(e, true);
            }
        }
    }
    
    /**
     * Notify listeners of an error.
     * 
     * @param error The error that occurred
     * @param recoverable Whether processing can continue
     */
    protected void notifyError(Exception error, boolean recoverable) {
        for (StreamingTransformListener<T> listener : listeners) {
            try {
                listener.onError(error, recoverable);
            } catch (Exception e) {
                // Prevent infinite recursion - silently ignore
                // Applications should ensure their error handlers don't throw
            }
        }
    }
}