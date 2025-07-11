package jwave.transforms;

import jwave.transforms.wavelets.Wavelet;
import jwave.exceptions.JWaveException;
import jwave.exceptions.JWaveFailure;
import jwave.datatypes.natives.Complex;
import jwave.utils.MathUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of the Maximal Overlap Discrete Wavelet Transform (MODWT)
 * and its inverse, designed to integrate with the JWave library structure.
 * 
 * <h2>Overview</h2>
 * The MODWT is a shift-invariant, redundant wavelet transform that addresses
 * limitations of the standard Discrete Wavelet Transform (DWT):
 * <ul>
 *   <li>Translation invariance: shifting the input signal results in a shifted output</li>
 *   <li>No downsampling: all levels have the same length as the input signal</li>
 *   <li>Works with any signal length (not restricted to powers of 2)</li>
 *   <li>Increased redundancy provides better analysis capabilities</li>
 * </ul>
 * 
 * <h2>Mathematical Foundation</h2>
 * The MODWT modifies the DWT by:
 * <ol>
 *   <li>Rescaling the wavelet and scaling filters by 1/√2</li>
 *   <li>Using circular convolution instead of downsampling</li>
 *   <li>Upsampling filters at each level j by inserting 2^(j-1)-1 zeros</li>
 * </ol>
 * 
 * For level j, the MODWT filters are:
 * <pre>
 *   h̃_j,l = h_l / 2^(j/2)  (wavelet filter)
 *   g̃_j,l = g_l / 2^(j/2)  (scaling filter)
 * </pre>
 * 
 * <h2>Algorithm</h2>
 * Forward MODWT for J levels:
 * <pre>
 *   V_0 = X (input signal)
 *   For j = 1 to J:
 *     W_j = h̃_j ⊛ V_{j-1}  (detail coefficients)
 *     V_j = g̃_j ⊛ V_{j-1}  (approximation coefficients)
 * </pre>
 * 
 * Inverse MODWT:
 * <pre>
 *   Ṽ_J = V_J
 *   For j = J down to 1:
 *     Ṽ_{j-1} = g̃_j* ⊛ Ṽ_j + h̃_j* ⊛ W_j
 * </pre>
 * Where ⊛ denotes circular convolution and * denotes the adjoint operation.
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create MODWT instance with Daubechies-4 wavelet
 * MODWTTransform modwt = new MODWTTransform(new Daubechies4());
 * 
 * // Example 1: Decompose a signal to 3 levels
 * double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
 * double[][] coeffs = modwt.forwardMODWT(signal, 3);
 * // coeffs[0] = W_1 (level 1 details)
 * // coeffs[1] = W_2 (level 2 details)
 * // coeffs[2] = W_3 (level 3 details)
 * // coeffs[3] = V_3 (level 3 approximation)
 * 
 * // Example 2: Using the 1D interface
 * double[] flatCoeffs = modwt.forward(signal, 2);
 * // Returns flattened array: [W_1[0..7], W_2[0..7], V_2[0..7]]
 * double[] reconstructed = modwt.reverse(flatCoeffs, 2);
 * 
 * // Example 3: Multi-resolution analysis
 * int maxLevel = 4;
 * double[][] mra = modwt.forwardMODWT(signal, maxLevel);
 * 
 * // Denoise by thresholding level 1 details
 * for (int i = 0; i < mra[0].length; i++) {
 *     if (Math.abs(mra[0][i]) < threshold) {
 *         mra[0][i] = 0.0;
 *     }
 * }
 * double[] denoised = modwt.inverseMODWT(mra);
 * }</pre>
 * 
 * <h2>References</h2>
 * <ul>
 *   <li>Percival, D. B., & Walden, A. T. (2000). Wavelet Methods for Time Series Analysis. 
 *       Cambridge University Press. ISBN: 0-521-68508-7</li>
 *   <li>Cornish, C. R., Bretherton, C. S., & Percival, D. B. (2006). Maximal overlap 
 *       wavelet statistical analysis with application to atmospheric turbulence. 
 *       Boundary-Layer Meteorology, 119(2), 339-374.</li>
 *   <li>Quilty, J., & Adamowski, J. (2018). Addressing the incorrect usage of wavelet-based 
 *       hydrological and water resources forecasting models for real-world applications with 
 *       best practices and a new forecasting framework. Journal of Hydrology, 563, 336-353.</li>
 * </ul>
 * 
 * @author Stephen Romano
 * @see jwave.transforms.wavelets.Wavelet
 */
public class MODWTTransform extends WaveletTransform {

    /**
     * Maximum supported decomposition level.
     * Set to 13 (a Fibonacci number) to balance flexibility with memory constraints.
     * At level 13, filter sizes can reach ~77K coefficients for longer wavelets.
     */
    private static final int MAX_DECOMPOSITION_LEVEL = 13;

    /**
     * Threshold for switching between direct and FFT-based convolution.
     * When signal length * filter length exceeds this value, FFT is more efficient.
     * 
     * <p>This value was empirically derived through performance benchmarking across
     * various signal sizes and wavelet filter lengths. The threshold represents the
     * break-even point where the O(N log N) FFT approach becomes faster than the
     * O(N*M) direct convolution, despite FFT's overhead.</p>
     * 
     * <p><b>Performance characteristics observed:</b></p>
     * <ul>
     *   <li>Short filters (Haar, length 2): FFT rarely wins due to overhead</li>
     *   <li>Medium filters (Daubechies-4, length 8): FFT beneficial for N > 512</li>
     *   <li>Long filters (Daubechies-20, length 40): FFT beneficial for N > 128</li>
     * </ul>
     * 
     * <p><b>Factors influencing the optimal threshold:</b></p>
     * <ul>
     *   <li>FFT implementation efficiency (using JWave's FastFourierTransform)</li>
     *   <li>Memory allocation overhead for complex number arrays</li>
     *   <li>CPU cache effects and memory bandwidth</li>
     *   <li>JVM optimization and hardware architecture</li>
     * </ul>
     * 
     * <p>The default value of 4096 provides a conservative threshold that ensures FFT 
     * is used only when there's a clear performance benefit. This value can be 
     * configured via the constructor or setter method. For specific applications,
     * users can also override this behavior using {@link #setConvolutionMethod(ConvolutionMethod)}.</p>
     * 
     * @see #performConvolution(double[], double[], boolean)
     */
    protected int fftConvolutionThreshold = 4096;
    
    /**
     * Enum to specify convolution method.
     */
    public enum ConvolutionMethod {
        AUTO,    // Automatically choose based on problem size
        DIRECT,  // Always use direct O(N*M) convolution
        FFT      // Always use FFT-based O(N log N) convolution
    }

    // Cache for upsampled filters, keyed by level
    private transient volatile ConcurrentHashMap<Integer, double[]> gFilterCache;
    private transient volatile ConcurrentHashMap<Integer, double[]> hFilterCache;
    
    // Base MODWT filters (computed once from wavelet)
    private transient volatile double[] g_modwt_base;
    private transient volatile double[] h_modwt_base;
    
    // Flag to track if cache is valid (volatile for thread visibility)
    private transient volatile boolean cacheInitialized = false;
    
    // Convolution method selection
    private ConvolutionMethod convolutionMethod = ConvolutionMethod.AUTO;
    
    // Reference to FFT transform for FFT-based convolution
    protected transient FastFourierTransform fft;

    /**
     * Constructor for the MODWTTransform.
     * 
     * @param wavelet The mother wavelet to use for the transform. Common choices include:
     *                - Haar1: Simple, good for piecewise constant signals
     *                - Daubechies4: Smooth, good for general purpose analysis
     *                - Symlet8: Nearly symmetric, good for signal processing
     */
    public MODWTTransform(Wavelet wavelet) {
        super(wavelet);
        this.fft = new FastFourierTransform();
    }
    
    /**
     * Constructor for MODWTTransform with custom FFT threshold.
     * 
     * @param wavelet the wavelet to use for the transform
     * @param fftThreshold custom threshold for FFT-based convolution (N*M threshold)
     */
    public MODWTTransform(Wavelet wavelet, int fftThreshold) {
        super(wavelet);
        this.fftConvolutionThreshold = fftThreshold;
        this.fft = new FastFourierTransform();
    }
    
    /**
     * Sets the convolution method for the MODWT transform.
     * 
     * @param method The convolution method to use
     */
    public void setConvolutionMethod(ConvolutionMethod method) {
        this.convolutionMethod = method;
    }
    
    /**
     * Gets the current convolution method.
     * 
     * @return The current convolution method
     */
    public ConvolutionMethod getConvolutionMethod() {
        return this.convolutionMethod;
    }
    
    /**
     * Returns the maximum supported decomposition level.
     * 
     * @return The maximum decomposition level (currently 13)
     */
    public static int getMaxDecompositionLevel() {
        return MAX_DECOMPOSITION_LEVEL;
    }

    /**
     * Performs a full forward Maximal Overlap Discrete Wavelet Transform.
     * 
     * <p>This is the primary method for MODWT decomposition. Unlike the DWT, the MODWT
     * preserves the length of the signal at each decomposition level, making it ideal
     * for time series analysis where temporal alignment is important.</p>
     * 
     * <p><b>Mathematical Description:</b><br>
     * At each level j, the transform computes:
     * <ul>
     *   <li>W_j = h̃_j ⊛ V_{j-1} (wavelet/detail coefficients)</li>
     *   <li>V_j = g̃_j ⊛ V_{j-1} (scaling/approximation coefficients)</li>
     * </ul>
     * where h̃_j and g̃_j are the upsampled and rescaled filters.</p>
     * 
     * @param data      The input time series data of any length (not restricted to 2^n)
     * @param maxLevel  The maximum level of decomposition (1 ≤ maxLevel ≤ log2(N))
     * @return A 2D array where:
     *         - coeffs[0] through coeffs[maxLevel-1] contain detail coefficients W_j
     *         - coeffs[maxLevel] contains approximation coefficients V_maxLevel
     *         - Each row has the same length as the input signal
     * 
     * @example
     * <pre>{@code
     * double[] ecgSignal = loadECGData();
     * MODWTTransform modwt = new MODWTTransform(new Daubechies4());
     * double[][] coeffs = modwt.forwardMODWT(ecgSignal, 5);
     * // Analyze heart rate variability at different scales
     * double[] scale1Details = coeffs[0]; // 2-4 sample periods
     * double[] scale2Details = coeffs[1]; // 4-8 sample periods
     * }</pre>
     */
    public double[][] forwardMODWT(double[] data, int maxLevel) {
        if (maxLevel < 1) {
            throw new IllegalArgumentException("MODWTTransform#forwardMODWT - " +
                "decomposition level must be at least 1, requested: " + maxLevel);
        }
        if (maxLevel > MAX_DECOMPOSITION_LEVEL) {
            throw new IllegalArgumentException("MODWTTransform#forwardMODWT - " +
                "maximum supported decomposition level is " + MAX_DECOMPOSITION_LEVEL + 
                ", requested: " + maxLevel);
        }
        if (data == null || data.length == 0) {
            // Return the expected structure but with empty arrays
            double[][] emptyResult = new double[maxLevel + 1][];
            for (int i = 0; i <= maxLevel; i++) {
                emptyResult[i] = new double[0];
            }
            return emptyResult;
        }
        int N = data.length;
        
        // Check theoretical limit based on signal length
        // Use integer-based approach for efficiency and precision: floor(log2(N)) = 31 - numberOfLeadingZeros(N)
        int theoreticalMaxLevel = N > 0 ? 31 - Integer.numberOfLeadingZeros(N) : 0;
        if (maxLevel > theoreticalMaxLevel) {
            throw new IllegalArgumentException("Decomposition level " + maxLevel + " exceeds theoretical limit " + 
                theoreticalMaxLevel + " for signal length " + N);
        }
        
        // Initialize cache if needed
        initializeFilterCache();

        double[][] modwtCoeffs = new double[maxLevel + 1][N];
        double[] vCurrent = Arrays.copyOf(data, N);

        for (int j = 1; j <= maxLevel; j++) {
            // Use cached filters instead of creating new ones
            double[] gUpsampled = getCachedGFilter(j);
            double[] hUpsampled = getCachedHFilter(j);

            double[] wNext = performConvolution(vCurrent, hUpsampled, false);
            double[] vNext = performConvolution(vCurrent, gUpsampled, false);

            modwtCoeffs[j - 1] = wNext;
            vCurrent = vNext;

            if (j == maxLevel) {
                modwtCoeffs[j] = vNext;
            }
        }
        return modwtCoeffs;
    }

    /**
     * Performs the inverse Maximal Overlap Discrete Wavelet Transform (iMODWT).
     * 
     * <p>Reconstructs the original signal from MODWT coefficients using the
     * reconstruction formula:</p>
     * <pre>
     * X = Σ(j=1 to J) D_j + A_J
     * </pre>
     * where D_j are the detail components and A_J is the approximation at level J.
     * 
     * <p><b>Perfect Reconstruction Property:</b><br>
     * The MODWT satisfies the perfect reconstruction property, meaning
     * inverseMODWT(forwardMODWT(X)) = X (within numerical precision).</p>
     * 
     * @param coefficients A 2D array of MODWT coefficients as returned by forwardMODWT
     * @return The reconstructed time-domain signal with the same length as the original
     * 
     * @example
     * <pre>{@code
     * // Perform multi-resolution analysis
     * double[][] coeffs = modwt.forwardMODWT(signal, 4);
     * 
     * // Remove high-frequency noise (zero out level 1 details)
     * coeffs[0] = new double[coeffs[0].length]; // zeros
     * 
     * // Reconstruct denoised signal
     * double[] denoised = modwt.inverseMODWT(coeffs);
     * }</pre>
     */
    public double[] inverseMODWT(double[][] coefficients) {
        if (coefficients == null || coefficients.length == 0) {
            return new double[0];
        }

        int maxLevel = coefficients.length - 1;
        if (maxLevel <= 0) {
            // Need at least level 1 (2 arrays: W_1 and V_1)
            return new double[0];
        }

        int N = coefficients[0].length;
        
        // Initialize cache if needed
        initializeFilterCache();

        double[] vCurrent = Arrays.copyOf(coefficients[maxLevel], N);

        for (int j = maxLevel; j >= 1; j--) {
            // Use cached filters instead of creating new ones
            double[] gUpsampled = getCachedGFilter(j);
            double[] hUpsampled = getCachedHFilter(j);

            double[] wCurrent = coefficients[j - 1];

            // Use the adjoint convolution for the inverse transform.
            double[] vFromApprox = performConvolution(vCurrent, gUpsampled, true);
            double[] vFromDetail = performConvolution(wCurrent, hUpsampled, true);

            double[] vNext = new double[N];
            for (int i = 0; i < N; i++) {
                vNext[i] = vFromApprox[i] + vFromDetail[i];
            }

            vCurrent = vNext;
        }

        return vCurrent;
    }

    /**
     * Performs a forward MODWT to a specified decomposition level.
     * 
     * <p>This method allows control over the decomposition depth, useful when
     * analyzing specific frequency bands or limiting computational cost.</p>
     * 
     * @param arrTime The input signal (must be power of 2 length)
     * @param level The desired decomposition level (1 ≤ level ≤ log2(N))
     * @return Flattened array containing MODWT coefficients up to the specified level
     * @throws JWaveException if input is invalid or level is out of range
     */
    @Override
    public double[] forward(double[] arrTime, int level) throws JWaveException {
        if (arrTime == null || arrTime.length == 0) return new double[0];
        
        if (!isBinary(arrTime.length))
            throw new JWaveFailure("MODWTTransform#forward - " +
                "given array length is not 2^p | p E N ... = 1, 2, 4, 8, 16, 32, .. ");
        
        int maxLevel = calcExponent(arrTime.length);
        if (level < 0 || level > maxLevel)
            throw new JWaveFailure("MODWTTransform#forward - " +
                "given level is out of range for given array");
        if (level > MAX_DECOMPOSITION_LEVEL)
            throw new JWaveFailure("MODWTTransform#forward - " +
                "maximum supported decomposition level is " + MAX_DECOMPOSITION_LEVEL + 
                ", requested: " + level);
        
        // Perform MODWT decomposition to specified level
        double[][] coeffs2D = forwardMODWT(arrTime, level);
        
        // Flatten the 2D coefficient array into 1D
        int N = arrTime.length;
        double[] flatCoeffs = new double[N * (level + 1)];
        
        for (int lev = 0; lev <= level; lev++) {
            System.arraycopy(coeffs2D[lev], 0, flatCoeffs, lev * N, N);
        }
        
        return flatCoeffs;
    }
    
    @Override
    public double[] reverse(double[] arrHilb, int level) throws JWaveException {
        if (arrHilb == null || arrHilb.length == 0) return new double[0];
        
        // For MODWT, we need the full coefficient set to reconstruct
        // The level parameter indicates how many levels were used in decomposition
        int N = arrHilb.length / (level + 1);
        
        if (!isBinary(N))
            throw new JWaveFailure("MODWTTransform#reverse - " +
                "Invalid coefficient array for given level");
        
        if (arrHilb.length != N * (level + 1))
            throw new JWaveFailure("MODWTTransform#reverse - " +
                "Coefficient array length does not match expected size for given level");
        
        // Unflatten the 1D array back to 2D structure
        double[][] coeffs2D = new double[level + 1][N];
        for (int lev = 0; lev <= level; lev++) {
            System.arraycopy(arrHilb, lev * N, coeffs2D[lev], 0, N);
        }
        
        // Perform inverse MODWT
        return inverseMODWT(coeffs2D);
    }

    // --- Helper and Overridden Methods ---
    
    /**
     * Initializes the filter cache if not already initialized.
     * Computes the base MODWT filters from the wavelet coefficients.
     * Thread-safe through double-checked locking pattern.
     */
    protected void initializeFilterCache() {
        if (!cacheInitialized || g_modwt_base == null) {
            synchronized (this) {
                // Double-check inside synchronized block
                if (!cacheInitialized || g_modwt_base == null) {
                    // Initialize FFT if needed
                    if (fft == null) {
                        fft = new FastFourierTransform();
                    }
                    // Compute base MODWT filters
                    double[] g_dwt = Arrays.copyOf(_wavelet.getScalingDeComposition(), 
                                                   _wavelet.getScalingDeComposition().length);
                    double[] h_dwt = Arrays.copyOf(_wavelet.getWaveletDeComposition(), 
                                                   _wavelet.getWaveletDeComposition().length);
                    normalize(g_dwt);
                    normalize(h_dwt);
                    
                    double scaleFactor = Math.sqrt(2.0);
                    g_modwt_base = new double[g_dwt.length];
                    h_modwt_base = new double[h_dwt.length];
                    for (int i = 0; i < g_dwt.length; i++) {
                        g_modwt_base[i] = g_dwt[i] / scaleFactor;
                        h_modwt_base[i] = h_dwt[i] / scaleFactor;
                    }
                    
                    // Initialize cache maps with ConcurrentHashMap for thread safety
                    gFilterCache = new ConcurrentHashMap<>();
                    hFilterCache = new ConcurrentHashMap<>();
                    cacheInitialized = true;
                }
            }
        }
    }
    
    /**
     * Gets the cached upsampled G filter for the specified level.
     * Creates and caches it if not already present.
     */
    protected double[] getCachedGFilter(int level) {
        initializeFilterCache();
        
        // Check if already cached
        double[] cached = gFilterCache.get(level);
        if (cached != null) {
            return cached;
        }
        
        // Compute under synchronization to avoid race with clearFilterCache
        synchronized (this) {
            // Double-check after acquiring lock
            cached = gFilterCache.get(level);
            if (cached != null) {
                return cached;
            }
            
            // Ensure base filter is initialized
            if (g_modwt_base == null) {
                initializeFilterCache();
            }
            
            // Compute and cache
            double[] filter = upsample(g_modwt_base, level);
            gFilterCache.put(level, filter);
            return filter;
        }
    }
    
    /**
     * Gets the cached upsampled H filter for the specified level.
     * Creates and caches it if not already present.
     */
    protected double[] getCachedHFilter(int level) {
        initializeFilterCache();
        
        // Check if already cached
        double[] cached = hFilterCache.get(level);
        if (cached != null) {
            return cached;
        }
        
        // Compute under synchronization to avoid race with clearFilterCache
        synchronized (this) {
            // Double-check after acquiring lock
            cached = hFilterCache.get(level);
            if (cached != null) {
                return cached;
            }
            
            // Ensure base filter is initialized
            if (h_modwt_base == null) {
                initializeFilterCache();
            }
            
            // Compute and cache
            double[] filter = upsample(h_modwt_base, level);
            hFilterCache.put(level, filter);
            return filter;
        }
    }
    
    /**
     * Clears the filter cache. Call this if memory is a concern
     * or before changing wavelets. Thread-safe.
     */
    public void clearFilterCache() {
        synchronized (this) {
            // Set flag first to prevent new threads from using stale data
            cacheInitialized = false;
            
            // Clear caches
            if (gFilterCache != null) gFilterCache.clear();
            if (hFilterCache != null) hFilterCache.clear();
            
            // Clear base filters to force recomputation
            g_modwt_base = null;
            h_modwt_base = null;
        }
    }
    
    /**
     * Pre-computes filters for specified levels to avoid
     * computation during time-critical operations.
     * 
     * @param maxLevel The maximum decomposition level to pre-compute (1 ≤ maxLevel ≤ MAX_DECOMPOSITION_LEVEL)
     * @throws IllegalArgumentException if maxLevel is out of valid range
     */
    public void precomputeFilters(int maxLevel) {
        if (maxLevel < 1) {
            throw new IllegalArgumentException("MODWTTransform#precomputeFilters - " +
                "decomposition level must be at least 1, requested: " + maxLevel);
        }
        if (maxLevel > MAX_DECOMPOSITION_LEVEL) {
            throw new IllegalArgumentException("MODWTTransform#precomputeFilters - " +
                "maximum supported decomposition level is " + MAX_DECOMPOSITION_LEVEL + 
                ", requested: " + maxLevel);
        }
        initializeFilterCache();
        for (int j = 1; j <= maxLevel; j++) {
            getCachedGFilter(j);
            getCachedHFilter(j);
        }
    }

    /**
     * Normalizes a filter to have unit energy (L2 norm = 1).
     * This ensures the transform preserves signal energy.
     */
    private void normalize(double[] filter) {
        double energy = 0.0;
        for (double c : filter) { energy += c * c; }
        double norm = Math.sqrt(energy);
        if (norm > 1e-12) {
            for (int i = 0; i < filter.length; i++) { filter[i] /= norm; }
        }
    }

    /**
     * Upsamples a filter for a specific decomposition level.
     * 
     * <p>At level j, inserts 2^(j-1) - 1 zeros between each filter coefficient.
     * This is a key operation that makes the MODWT shift-invariant.</p>
     * 
     * @param filter The base filter coefficients
     * @param level The decomposition level (1, 2, 3, ...)
     * @return The upsampled filter
     */
    private static double[] upsample(double[] filter, int level) {
        if (level <= 1) return filter;
        if (level > MAX_DECOMPOSITION_LEVEL) throw new IllegalArgumentException("MODWTTransform#upsample - maximum supported decomposition level is " + MAX_DECOMPOSITION_LEVEL + ", requested: " + level);
        int gap = (1 << (level - 1)) - 1;
        int newLength = filter.length + (filter.length - 1) * gap;
        if (newLength < 0 || newLength < filter.length) throw new IllegalArgumentException("Upsampling would result in array too large");

        double[] upsampled = new double[newLength];
        for (int i = 0; i < filter.length; i++) {
            upsampled[i * (gap + 1)] = filter[i];
        }
        return upsampled;
    }

    /**
     * Performs circular convolution or its adjoint based on the selected method.
     * 
     * @param signal The input signal
     * @param filter The filter to convolve with
     * @param adjoint If true, performs adjoint (transpose) convolution
     * @return The convolution result with the same length as the signal
     */
    protected double[] performConvolution(double[] signal, double[] filter, boolean adjoint) {
        boolean useFFT = false;
        
        switch (convolutionMethod) {
            case FFT:
                useFFT = true;
                break;
            case DIRECT:
                useFFT = false;
                break;
            case AUTO:
                // Use FFT when the product of signal and filter lengths exceeds threshold
                // See fftConvolutionThreshold documentation for performance analysis
                useFFT = (signal.length * filter.length) > fftConvolutionThreshold;
                break;
        }
        
        if (useFFT) {
            return adjoint ? circularConvolveFFTAdjoint(signal, filter) 
                          : circularConvolveFFT(signal, filter);
        } else {
            return adjoint ? circularConvolveAdjoint(signal, filter) 
                          : circularConvolve(signal, filter);
        }
    }

    /**
     * Performs circular convolution between a signal and filter.
     * 
     * <p>Uses periodic boundary conditions, treating the signal as if it
     * wraps around at the boundaries. This preserves the signal length
     * and is essential for the MODWT's shift-invariance property.</p>
     * 
     * @param signal The input signal
     * @param filter The filter to convolve with
     * @return The convolution result with the same length as the signal
     */
    private static double[] circularConvolve(double[] signal, double[] filter) {
        int N = signal.length;
        int M = filter.length;
        double[] output = new double[N];
        for (int n = 0; n < N; n++) {
            double sum = 0.0;
            for (int m = 0; m < M; m++) {
                int signalIndex = Math.floorMod(n - m, N);
                sum += signal[signalIndex] * filter[m];
            }
            output[n] = sum;
        }
        return output;
    }

    /**
     * Performs the adjoint (transpose) of circular convolution.
     * 
     * <p>This operation is crucial for the inverse MODWT. If H is the
     * convolution matrix, this computes H^T * signal. The adjoint
     * operation reverses the time-reversal in standard convolution.</p>
     * 
     * @param signal The input signal
     * @param filter The filter for adjoint convolution
     * @return The adjoint convolution result
     */
    private static double[] circularConvolveAdjoint(double[] signal, double[] filter) {
        int N = signal.length;
        int M = filter.length;
        double[] output = new double[N];
        for (int n = 0; n < N; n++) {
            double sum = 0.0;
            for (int m = 0; m < M; m++) {
                int signalIndex = Math.floorMod(n + m, N);
                sum += signal[signalIndex] * filter[m];
            }
            output[n] = sum;
        }
        return output;
    }
    
    /**
     * Wraps a filter to the signal length with circular indexing.
     * 
     * <p>When the filter is longer than the signal (common with upsampled filters),
     * this method wraps the filter coefficients by accumulating values that map
     * to the same position after modulo operation.</p>
     * 
     * @param filter The filter to wrap
     * @param signalLength The target length for the wrapped filter
     * @return The wrapped filter with length equal to signalLength
     */
    private static double[] wrapFilterToSignalLength(double[] filter, int signalLength) {
        double[] wrappedFilter = new double[signalLength];
        
        // Using addition (+=) instead of assignment (=) ensures that overlapping
        // coefficients are accumulated correctly when the filter length exceeds
        // the signal length. This is necessary for circular convolution, where
        // filter coefficients wrap around and contribute to multiple positions.
        for (int i = 0; i < filter.length; i++) {
            wrappedFilter[i % signalLength] += filter[i];
        }
        
        return wrappedFilter;
    }
    
    /**
     * Performs circular convolution using FFT for improved performance.
     * 
     * <p>Circular convolution theorem: circular_conv(x,h) = IFFT(FFT(x) * FFT(h))</p>
     * 
     * @param signal The input signal
     * @param filter The filter to convolve with
     * @return The convolution result with the same length as the signal
     */
    private double[] circularConvolveFFT(double[] signal, double[] filter) {
        int N = signal.length;
        
        // Wrap filter to signal length if necessary
        double[] paddedFilter = wrapFilterToSignalLength(filter, N);
        
        // Convert to complex arrays
        Complex[] signalComplex = new Complex[N];
        Complex[] filterComplex = new Complex[N];
        for (int i = 0; i < N; i++) {
            signalComplex[i] = new Complex(signal[i], 0);
            filterComplex[i] = new Complex(paddedFilter[i], 0);
        }
        
        // Compute FFTs
        Complex[] signalFFT = fft.forward(signalComplex);
        Complex[] filterFFT = fft.forward(filterComplex);
        
        // Pointwise multiplication in frequency domain
        Complex[] productFFT = new Complex[N];
        for (int i = 0; i < N; i++) {
            productFFT[i] = signalFFT[i].mul(filterFFT[i]);
        }
        
        // Inverse FFT
        Complex[] result = fft.reverse(productFFT);
        
        // Extract real part (imaginary part should be near zero for real inputs)
        double[] output = new double[N];
        for (int i = 0; i < N; i++) {
            output[i] = result[i].getReal();
        }
        
        return output;
    }
    
    /**
     * Performs the adjoint of circular convolution using FFT.
     * 
     * <p>For the adjoint operation, the convolution matrix is transposed.
     * This is different from correlation and requires careful handling.</p>
     * 
     * @param signal The input signal
     * @param filter The filter for adjoint convolution
     * @return The adjoint convolution result
     */
    private double[] circularConvolveFFTAdjoint(double[] signal, double[] filter) {
        int N = signal.length;
        
        // Wrap filter to signal length if necessary
        double[] paddedFilter = wrapFilterToSignalLength(filter, N);
        
        // Convert to complex arrays
        Complex[] signalComplex = new Complex[N];
        Complex[] filterComplex = new Complex[N];
        for (int i = 0; i < N; i++) {
            signalComplex[i] = new Complex(signal[i], 0);
            filterComplex[i] = new Complex(paddedFilter[i], 0);
        }
        
        // Compute FFTs
        Complex[] signalFFT = fft.forward(signalComplex);
        Complex[] filterFFT = fft.forward(filterComplex);
        
        // For the adjoint operation in circular convolution,
        // we conjugate the filter FFT (which time-reverses it)
        // then multiply and take IFFT
        Complex[] productFFT = new Complex[N];
        for (int i = 0; i < N; i++) {
            // Conjugate the filter FFT for adjoint operation
            productFFT[i] = signalFFT[i].mul(filterFFT[i].conjugate());
        }
        
        // Inverse FFT
        Complex[] result = fft.reverse(productFFT);
        
        // Extract real part 
        double[] output = new double[N];
        for (int i = 0; i < N; i++) {
            // The conjugation of the filter FFT already implements the adjoint operation correctly
            // No additional circular shift is needed
            output[i] = result[i].getReal();
        }
        
        return output;
    }

    /**
     * Performs a forward MODWT with automatic level selection.
     * 
     * <p>Computes the maximum possible decomposition level based on the
     * signal length and performs a full decomposition. The output is
     * flattened into a 1D array for compatibility with JWave's interface.</p>
     * 
     * <p><b>Output Structure:</b><br>
     * [W_1[0..N-1], W_2[0..N-1], ..., W_J[0..N-1], V_J[0..N-1]]</p>
     * 
     * @param arrTime The input signal (must be power of 2 length)
     * @return Flattened array containing all MODWT coefficients
     * @throws JWaveException if input is null, empty, or not power of 2
     */
    @Override
    public double[] forward(double[] arrTime) throws JWaveException {
        if (arrTime == null || arrTime.length == 0) return new double[0];
        
        // Calculate maximum decomposition level
        int maxLevel = calcExponent(arrTime.length);
        
        // Perform full MODWT decomposition
        double[][] coeffs2D = forwardMODWT(arrTime, maxLevel);
        
        // Flatten the 2D coefficient array into 1D
        // Structure: [D1, D2, ..., D_maxLevel, A_maxLevel]
        // Each has the same length as the input signal
        int N = arrTime.length;
        double[] flatCoeffs = new double[N * (maxLevel + 1)];
        
        for (int level = 0; level <= maxLevel; level++) {
            System.arraycopy(coeffs2D[level], 0, flatCoeffs, level * N, N);
        }
        
        return flatCoeffs;
    }

    @Override
    public double[] reverse(double[] arrHilb) throws JWaveException {
        if (arrHilb == null || arrHilb.length == 0) return new double[0];
        
        // Determine the signal length and number of levels from the flattened array
        // The flattened array contains (maxLevel + 1) segments of equal length
        // We need to find N such that arrHilb.length = N * (levels + 1)
        int totalLength = arrHilb.length;
        int N = 0;
        int levels = 0;
        
        // Find the signal length by trying different possibilities
        for (int testN = 1; testN <= totalLength; testN++) {
            if (totalLength % testN == 0) {
                int testLevels = (totalLength / testN) - 1;
                if (testLevels >= 0 && isBinary(testN) && testLevels <= calcExponent(testN)) {
                    N = testN;
                    levels = testLevels;
                    break;
                }
            }
        }
        
        if (N == 0) {
            throw new JWaveFailure("MODWTTransform#reverse - " +
                "Invalid flattened coefficient array length. Cannot determine original signal dimensions.");
        }
        
        // Unflatten the 1D array back to 2D structure
        double[][] coeffs2D = new double[levels + 1][N];
        for (int level = 0; level <= levels; level++) {
            System.arraycopy(arrHilb, level * N, coeffs2D[level], 0, N);
        }
        
        // Perform inverse MODWT
        return inverseMODWT(coeffs2D);
    }
}
