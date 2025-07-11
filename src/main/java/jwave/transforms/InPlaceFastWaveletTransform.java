package jwave.transforms;

import jwave.exceptions.JWaveException;
import jwave.exceptions.JWaveFailure;
import jwave.transforms.wavelets.Wavelet;

/**
 * In-place API for the Fast Wavelet Transform (FWT) that modifies
 * the input array directly. This provides an in-place interface while
 * internally using the standard transform implementation.
 * 
 * <p>WARNING: This implementation modifies the input array. If you need to
 * preserve the original data, make a copy before calling these methods.</p>
 * 
 * <p><b>Current Implementation Note:</b></p>
 * <p>This is currently a wrapper that uses the standard FWT internally and
 * copies results back to the input array. While this doesn't reduce memory
 * allocations yet, it provides the API for future true in-place implementations.
 * A true in-place implementation would require modifying the base wavelet
 * classes to support in-place operations at the algorithm level.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * double[] signal = getSignalData(); // Your input data
 * InPlaceFastWaveletTransform fwt = new InPlaceFastWaveletTransform(new Daubechies4());
 * 
 * // Option 1: Modify the original array
 * fwt.forwardInPlace(signal); // signal now contains coefficients
 * 
 * // Option 2: Work with a copy if you need to preserve original
 * double[] workCopy = Arrays.copyOf(signal, signal.length);
 * fwt.forwardInPlace(workCopy);
 * }</pre>
 * 
 * <p><b>Future Performance Goals:</b></p>
 * <ul>
 *   <li>True in-place operations without intermediate allocations</li>
 *   <li>50-75% reduction in memory allocations</li>
 *   <li>20-30% faster due to reduced GC pressure</li>
 *   <li>Better cache locality for large datasets</li>
 * </ul>
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class InPlaceFastWaveletTransform extends FastWaveletTransform {
    
    // Note: Thread-local workspace buffer removed as current implementation
    // uses the parent class methods and copies results back.
    // A true in-place implementation would require modifying the wavelet
    // base classes to support in-place operations.
    
    /**
     * Constructor with a Wavelet object.
     * 
     * @param wavelet Wavelet object
     */
    public InPlaceFastWaveletTransform(Wavelet wavelet) {
        super(wavelet);
    }
    
    /**
     * Performs an in-place forward Fast Wavelet Transform (FWT).
     * The input array is modified to contain the wavelet coefficients.
     * 
     * @param arrTime The input signal array (will be modified)
     * @return The same array reference containing wavelet coefficients
     * @throws JWaveException if array length is not a power of 2
     */
    public double[] forwardInPlace(double[] arrTime) throws JWaveException {
        // For now, use the standard implementation until we have true in-place wavelets
        double[] result = super.forward(arrTime);
        System.arraycopy(result, 0, arrTime, 0, arrTime.length);
        return arrTime;
    }
    
    /**
     * Performs an in-place forward Fast Wavelet Transform (FWT) with specified level.
     * 
     * @param arrTime The input signal array (will be modified)
     * @param level The number of decomposition levels
     * @return The same array reference containing wavelet coefficients
     * @throws JWaveException if parameters are invalid
     */
    public double[] forwardInPlace(double[] arrTime, int level) throws JWaveException {
        // For now, use the standard implementation until we have true in-place wavelets
        double[] result = super.forward(arrTime, level);
        System.arraycopy(result, 0, arrTime, 0, arrTime.length);
        return arrTime;
    }
    
    /**
     * Performs an in-place reverse Fast Wavelet Transform (FWT).
     * The input array is modified to contain the reconstructed signal.
     * 
     * @param arrHilb The wavelet coefficients array (will be modified)
     * @return The same array reference containing reconstructed signal
     * @throws JWaveException if array length is not a power of 2
     */
    public double[] reverseInPlace(double[] arrHilb) throws JWaveException {
        // For now, use the standard implementation until we have true in-place wavelets
        double[] result = super.reverse(arrHilb);
        System.arraycopy(result, 0, arrHilb, 0, arrHilb.length);
        return arrHilb;
    }
    
    /**
     * Performs an in-place reverse Fast Wavelet Transform (FWT) from specified level.
     * 
     * @param arrHilb The wavelet coefficients array (will be modified)
     * @param level The decomposition level to start reconstruction from
     * @return The same array reference containing reconstructed signal
     * @throws JWaveException if parameters are invalid
     */
    public double[] reverseInPlace(double[] arrHilb, int level) throws JWaveException {
        // For now, use the standard implementation until we have true in-place wavelets
        double[] result = super.reverse(arrHilb, level);
        System.arraycopy(result, 0, arrHilb, 0, arrHilb.length);
        return arrHilb;
    }
    
    
    /**
     * Standard forward transform that creates a copy (for API compatibility).
     * Delegates to parent class implementation.
     */
    @Override
    public double[] forward(double[] arrTime) throws JWaveException {
        return super.forward(arrTime);
    }
    
    /**
     * Standard reverse transform that creates a copy (for API compatibility).
     * Delegates to parent class implementation.
     */
    @Override
    public double[] reverse(double[] arrHilb) throws JWaveException {
        return super.reverse(arrHilb);
    }
}