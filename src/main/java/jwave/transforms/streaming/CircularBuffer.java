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
    
    /**
     * Threshold for switching between element-wise and bulk copy.
     * For arrays smaller than this, the overhead of System.arraycopy
     * may not be worth it compared to a simple loop.
     */
    private static final int SMALL_ARRAY_THRESHOLD = 8;
    
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
        
        if (samples.length == 0) {
            return;
        }
        
        // For small arrays, the overhead of bulk copy might not be worth it
        if (samples.length < SMALL_ARRAY_THRESHOLD) {
            for (double sample : samples) {
                append(sample);
            }
            return;
        }
        
        // Bulk copy optimization for larger arrays
        int samplesLength = samples.length;
        
        // If appending more samples than capacity, only keep the most recent
        if (samplesLength >= capacity) {
            // Copy only the last 'capacity' samples
            System.arraycopy(samples, samplesLength - capacity, buffer, 0, capacity);
            writeIndex = 0;
            size = capacity;
            hasWrapped = true;
            return;
        }
        
        int remainingCapacity = capacity - writeIndex;
        
        if (samplesLength <= remainingCapacity) {
            // All samples fit without wrapping
            System.arraycopy(samples, 0, buffer, writeIndex, samplesLength);
            writeIndex = (writeIndex + samplesLength) % capacity;
        } else {
            // Need to wrap around - copy in two segments
            // First segment: fill to end of buffer
            System.arraycopy(samples, 0, buffer, writeIndex, remainingCapacity);
            
            // Second segment: copy remaining samples from beginning
            int remainingSamples = samplesLength - remainingCapacity;
            System.arraycopy(samples, remainingCapacity, buffer, 0, remainingSamples);
            writeIndex = remainingSamples;
            hasWrapped = true;
        }
        
        // Update size
        size = Math.min(size + samplesLength, capacity);
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
        
        // Calculate the position of the most recent sample in the window
        int windowEndPos = size - offset;
        
        // Calculate how many samples we can actually provide
        int availableSamples = Math.min(windowEndPos, length);
        if (availableSamples <= 0) {
            // Requested window is entirely before our data
            return window; // All zeros
        }
        
        // Calculate where in the result array to start writing data
        // If we need fewer samples than the window length, pad with zeros at the beginning
        int zeroPadding = length - availableSamples;
        
        // Calculate the starting position in the buffer (oldest sample in window)
        // This is derived from availableSamples to avoid confusion
        int startPos = windowEndPos - availableSamples;
        
        // Copy the data, accounting for circular buffer wraparound
        if (!hasWrapped) {
            // Simple case: no wrap yet - use bulk copy for efficiency
            System.arraycopy(buffer, startPos, window, zeroPadding, availableSamples);
        } else {
            // Wrapped case: optimize with bulk copies for contiguous segments
            int oldestIndex = writeIndex;
            int chronologicalStart = (oldestIndex + startPos) % capacity;
            
            if (chronologicalStart + availableSamples <= capacity) {
                // All data is in one contiguous segment
                System.arraycopy(buffer, chronologicalStart, window, zeroPadding, availableSamples);
            } else {
                // Data spans the wrap point - copy in two segments
                int firstSegmentSize = capacity - chronologicalStart;
                int secondSegmentSize = availableSamples - firstSegmentSize;
                
                // Copy from chronologicalStart to end of buffer
                System.arraycopy(buffer, chronologicalStart, window, zeroPadding, firstSegmentSize);
                
                // Copy from beginning of buffer
                System.arraycopy(buffer, 0, window, zeroPadding + firstSegmentSize, secondSegmentSize);
            }
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
        // No need to zero the array - just reset the indices
        // The old data will be overwritten as new data arrives
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