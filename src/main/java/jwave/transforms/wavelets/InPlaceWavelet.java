package jwave.transforms.wavelets;

/**
 * Interface for wavelets that support in-place operations.
 * This allows wavelets to perform transforms without allocating new arrays,
 * significantly improving performance for large datasets.
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public interface InPlaceWavelet {
    
    /**
     * Performs an in-place forward wavelet transform.
     * The input array is modified directly.
     * 
     * @param arrTime The time domain signal (will be modified)
     * @param arrTimeLength The length of data to process
     * @param workspace Optional workspace array for temporary calculations
     */
    void forwardInPlace(double[] arrTime, int arrTimeLength, double[] workspace);
    
    /**
     * Performs an in-place reverse wavelet transform.
     * The input array is modified directly.
     * 
     * @param arrHilb The wavelet coefficients (will be modified)
     * @param arrHilbLength The length of data to process
     * @param workspace Optional workspace array for temporary calculations
     */
    void reverseInPlace(double[] arrHilb, int arrHilbLength, double[] workspace);
    
    /**
     * Gets the minimum workspace size required for in-place operations.
     * 
     * @param dataLength The length of data to be processed
     * @return The minimum workspace array size needed
     */
    int getWorkspaceSize(int dataLength);
}