/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import java.util.Arrays;

/**
 * A circular buffer implementation optimized for streaming transforms.
 * 
 * This buffer maintains a fixed-size array and overwrites old data as new
 * data is added. It provides efficient access patterns for wavelet transforms
 * including window extraction and wraparound handling.
 * 
 * Thread-safety: This class is NOT thread-safe. External synchronization is
 * required for concurrent access.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class CircularBuffer {
    
    private final double[] buffer;
    private final int capacity;
    private int writeIndex;
    private int size;
    private boolean hasWrapped;
    
    /**
     * Create a new circular buffer with the specified capacity.
     * 
     * @param capacity The fixed size of the buffer
     * @throws IllegalArgumentException if capacity <= 0
     */
    public CircularBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Buffer capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
        this.buffer = new double[capacity];
        this.writeIndex = 0;
        this.size = 0;
        this.hasWrapped = false;
    }
    
    /**
     * Append new samples to the buffer, overwriting oldest data if necessary.
     * 
     * @param samples Array of samples to append
     * @throws IllegalArgumentException if samples is null
     */
    public void append(double[] samples) {
        if (samples == null) {
            throw new IllegalArgumentException("Samples array cannot be null");
        }
        
        for (double sample : samples) {
            append(sample);
        }
    }
    
    /**
     * Append a single sample to the buffer.
     * 
     * @param sample The sample to append
     */
    public void append(double sample) {
        buffer[writeIndex] = sample;
        writeIndex = (writeIndex + 1) % capacity;
        
        if (size < capacity) {
            size++;
        } else {
            hasWrapped = true;
        }
    }
    
    /**
     * Get a window of data from the buffer.
     * 
     * The window is extracted in chronological order, handling wraparound
     * transparently. If the requested window extends beyond available data,
     * it will be padded with zeros at the beginning.
     * 
     * @param offset Offset from the most recent sample (0 = most recent)
     * @param length Length of the window to extract
     * @return Array containing the requested window
     * @throws IllegalArgumentException if offset < 0 or length <= 0
     */
    public double[] getWindow(int offset, int length) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative: " + offset);
        }
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive: " + length);
        }
        
        double[] window = new double[length];
        
        // Calculate how many samples we can actually provide
        int availableSamples = Math.min(size - offset, length);
        if (availableSamples <= 0) {
            // Requested window is entirely before our data
            return window; // All zeros
        }
        
        // Calculate the ending position in chronological order
        int endPos = size - offset;
        int startPos = endPos - length;
        
        // If startPos is negative, we need zero padding at the beginning
        int zeroPadding = 0;
        if (startPos < 0) {
            zeroPadding = -startPos;
            startPos = 0;
        }
        
        // Copy the data, accounting for circular buffer wraparound
        for (int i = 0; i < availableSamples; i++) {
            int chronologicalIdx = startPos + i;
            int bufferIdx;
            
            if (!hasWrapped) {
                // Simple case: no wrap yet
                bufferIdx = chronologicalIdx;
            } else {
                // Wrapped case: translate chronological index to buffer index
                bufferIdx = (writeIndex + chronologicalIdx) % capacity;
            }
            
            window[zeroPadding + i] = buffer[bufferIdx];
        }
        
        return window;
    }
    
    /**
     * Get the entire buffer contents as an array in chronological order.
     * 
     * @return Array containing all valid samples in chronological order
     */
    public double[] toArray() {
        if (size == 0) {
            return new double[0];
        }
        
        double[] result = new double[size];
        
        if (!hasWrapped) {
            // Simple case: data hasn't wrapped yet
            System.arraycopy(buffer, 0, result, 0, size);
        } else {
            // Data has wrapped: need to reconstruct chronological order
            int oldestIndex = writeIndex;
            int newestIndex = (writeIndex - 1 + capacity) % capacity;
            
            // Copy from oldest to end of buffer
            System.arraycopy(buffer, oldestIndex, result, 0, capacity - oldestIndex);
            
            // Copy from beginning to newest
            if (writeIndex > 0) {
                System.arraycopy(buffer, 0, result, capacity - oldestIndex, writeIndex);
            }
        }
        
        return result;
    }
    
    /**
     * Get a single sample at the specified index.
     * 
     * @param index Index from the oldest sample (0 = oldest)
     * @return The sample at the specified index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public double get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of range [0, " + size + ")");
        }
        
        int actualIndex;
        if (!hasWrapped) {
            actualIndex = index;
        } else {
            actualIndex = (writeIndex + index) % capacity;
        }
        
        return buffer[actualIndex];
    }
    
    /**
     * Get the most recent sample.
     * 
     * @return The most recently added sample
     * @throws IllegalStateException if buffer is empty
     */
    public double getLast() {
        if (size == 0) {
            throw new IllegalStateException("Buffer is empty");
        }
        int lastIndex = (writeIndex - 1 + capacity) % capacity;
        return buffer[lastIndex];
    }
    
    /**
     * Clear all data from the buffer.
     */
    public void clear() {
        Arrays.fill(buffer, 0.0);
        writeIndex = 0;
        size = 0;
        hasWrapped = false;
    }
    
    /**
     * Get the current number of samples in the buffer.
     * 
     * @return Number of valid samples
     */
    public int size() {
        return size;
    }
    
    /**
     * Get the capacity of the buffer.
     * 
     * @return Maximum number of samples the buffer can hold
     */
    public int capacity() {
        return capacity;
    }
    
    /**
     * Check if the buffer is empty.
     * 
     * @return true if no samples have been added
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Check if the buffer is full.
     * 
     * @return true if the buffer has reached capacity
     */
    public boolean isFull() {
        return size == capacity;
    }
    
    /**
     * Check if the buffer has wrapped around at least once.
     * 
     * @return true if old data has been overwritten
     */
    public boolean hasWrapped() {
        return hasWrapped;
    }
}