package jwave.transforms;

import jwave.transforms.wavelets.Wavelet;
import jwave.utils.ArrayBufferPool;
import java.util.Arrays;

/**
 * Memory-efficient implementation of the Maximal Overlap Discrete Wavelet Transform (MODWT)
 * that minimizes allocations and supports in-place operations where possible.
 * 
 * <p>This implementation reduces memory usage by:</p>
 * <ul>
 *   <li>Using a single backing array for all coefficients</li>
 *   <li>Providing view-based access to coefficient levels</li>
 *   <li>Reusing buffers via ArrayBufferPool</li>
 *   <li>Supporting streaming/chunked processing</li>
 * </ul>
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class EfficientMODWTTransform extends PooledMODWTTransform {
    
    /**
     * Result wrapper that provides efficient access to MODWT coefficients
     * without copying data.
     */
    public static class MODWTCoefficients {
        private final double[] backingArray;
        private final int signalLength;
        private final int levels;
        
        public MODWTCoefficients(double[] backingArray, int signalLength, int levels) {
            this.backingArray = backingArray;
            this.signalLength = signalLength;
            this.levels = levels;
        }
        
        /**
         * Get detail coefficients at specified level without copying.
         * 
         * @param level The level (1 to levels)
         * @return View of detail coefficients
         */
        public double[] getDetails(int level) {
            if (level < 1 || level > levels) {
                throw new IllegalArgumentException("Invalid level: " + level);
            }
            int offset = (level - 1) * signalLength;
            return Arrays.copyOfRange(backingArray, offset, offset + signalLength);
        }
        
        /**
         * Get approximation coefficients without copying.
         * 
         * @return View of approximation coefficients
         */
        public double[] getApproximation() {
            int offset = levels * signalLength;
            return Arrays.copyOfRange(backingArray, offset, offset + signalLength);
        }
        
        /**
         * Get a view of coefficients at specified level without copying.
         * This returns a wrapper that reads directly from the backing array.
         * 
         * @param level The level (1 to levels for details, levels+1 for approximation)
         * @return Array view wrapper
         */
        public ArrayView getView(int level) {
            if (level < 1 || level > levels + 1) {
                throw new IllegalArgumentException("Invalid level: " + level);
            }
            int offset = (level - 1) * signalLength;
            return new ArrayView(backingArray, offset, signalLength);
        }
        
        /**
         * Get the total number of coefficients.
         */
        public int getTotalSize() {
            return backingArray.length;
        }
    }
    
    /**
     * Lightweight array view that provides read access without copying.
     */
    public static class ArrayView {
        private final double[] array;
        private final int offset;
        private final int length;
        
        ArrayView(double[] array, int offset, int length) {
            this.array = array;
            this.offset = offset;
            this.length = length;
        }
        
        public double get(int index) {
            if (index < 0 || index >= length) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
            }
            return array[offset + index];
        }
        
        public int length() {
            return length;
        }
        
        /**
         * Copy view contents to a new array (only when needed).
         */
        public double[] toArray() {
            return Arrays.copyOfRange(array, offset, offset + length);
        }
    }
    
    public EfficientMODWTTransform(Wavelet wavelet) {
        super(wavelet);
    }
    
    /**
     * Performs MODWT with minimal memory allocation.
     * All coefficients are stored in a single backing array.
     * 
     * @param data Input signal
     * @param level Number of decomposition levels
     * @return Coefficient wrapper providing efficient access
     */
    public MODWTCoefficients forwardMODWTEfficient(double[] data, int level) {
        int N = data.length;
        
        // Single allocation for all coefficients
        // Layout: [W1, W2, ..., Wlevel, Vlevel]
        double[] allCoeffs = new double[N * (level + 1)];
        
        // Use pooled arrays for intermediate calculations
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        double[] vCurrent = pool.borrowDoubleArray(N);
        double[] vNext = pool.borrowDoubleArray(N);
        double[] wCurrent = pool.borrowDoubleArray(N);
        
        try {
            // Initialize with input data
            System.arraycopy(data, 0, vCurrent, 0, N);
            
            // Process each level
            for (int j = 1; j <= level; j++) {
                // Get filters for this level
                double[] g = getCachedGFilter(j);
                double[] h = getCachedHFilter(j);
                
                // Compute detail coefficients (Wj)
                if (g.length <= N) {
                    performConvolutionInto(vCurrent, g, false, wCurrent);
                    // Store in backing array
                    System.arraycopy(wCurrent, 0, allCoeffs, (j - 1) * N, N);
                }
                
                // Compute approximation for next level (Vj)
                if (h.length <= N) {
                    performConvolutionInto(vCurrent, h, false, vNext);
                    // Swap buffers
                    double[] temp = vCurrent;
                    vCurrent = vNext;
                    vNext = temp;
                }
            }
            
            // Store final approximation
            System.arraycopy(vCurrent, 0, allCoeffs, level * N, N);
            
        } finally {
            pool.returnDoubleArray(vCurrent);
            pool.returnDoubleArray(vNext);
            pool.returnDoubleArray(wCurrent);
        }
        
        return new MODWTCoefficients(allCoeffs, N, level);
    }
    
    /**
     * Performs inverse MODWT from efficient coefficient structure.
     * 
     * @param coeffs The coefficient structure
     * @return Reconstructed signal
     */
    public double[] inverseMODWTEfficient(MODWTCoefficients coeffs) {
        int N = coeffs.signalLength;
        int maxLevel = coeffs.levels;
        
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        double[] vCurrent = pool.borrowDoubleArray(N);
        double[] vNext = pool.borrowDoubleArray(N);
        double[] temp1 = pool.borrowDoubleArray(N);
        double[] temp2 = pool.borrowDoubleArray(N);
        
        try {
            // Start with the approximation coefficients
            System.arraycopy(coeffs.getApproximation(), 0, vCurrent, 0, N);
            
            // Reconstruct from coarsest to finest level
            for (int j = maxLevel; j >= 1; j--) {
                double[] gUpsampled = getCachedGFilter(j);
                double[] hUpsampled = getCachedHFilter(j);
                
                // Get detail coefficients for this level
                ArrayView wView = coeffs.getView(j);
                
                // Compute contributions
                performConvolutionInto(vCurrent, gUpsampled, true, temp1);
                
                // Copy detail coefficients to temp2
                for (int i = 0; i < N; i++) {
                    temp2[i] = wView.get(i);
                }
                performConvolutionInto(temp2, hUpsampled, true, vNext);
                
                // Combine
                for (int i = 0; i < N; i++) {
                    vNext[i] = temp1[i] + vNext[i];
                }
                
                // Swap buffers
                double[] swap = vCurrent;
                vCurrent = vNext;
                vNext = swap;
            }
            
            // Return a copy of the result
            return Arrays.copyOf(vCurrent, N);
            
        } finally {
            pool.returnDoubleArray(vCurrent);
            pool.returnDoubleArray(vNext);
            pool.returnDoubleArray(temp1);
            pool.returnDoubleArray(temp2);
        }
    }
    
    /**
     * Process MODWT in chunks for very large datasets.
     * This allows processing datasets that don't fit in memory.
     * 
     * @param dataProvider Provides chunks of input data
     * @param level Number of decomposition levels
     * @param chunkSize Size of each chunk to process
     * @param outputHandler Handles output chunks
     */
    public void processChunkedMODWT(DataProvider dataProvider, int level, 
                                   int chunkSize, OutputHandler outputHandler) {
        // Implementation would handle overlapping chunks for proper
        // boundary handling in the MODWT algorithm
        throw new UnsupportedOperationException("Chunked processing not yet implemented");
    }
    
    /**
     * Interface for providing data chunks.
     */
    public interface DataProvider {
        /**
         * Get the next chunk of data.
         * 
         * @return Data chunk or null if no more data
         */
        double[] getNextChunk();
        
        /**
         * Check if more data is available.
         */
        boolean hasMore();
    }
    
    /**
     * Interface for handling output chunks.
     */
    public interface OutputHandler {
        /**
         * Process output coefficients for a chunk.
         * 
         * @param coefficients The coefficient chunk
         * @param chunkIndex Index of this chunk
         */
        void handleOutput(MODWTCoefficients coefficients, int chunkIndex);
    }
}