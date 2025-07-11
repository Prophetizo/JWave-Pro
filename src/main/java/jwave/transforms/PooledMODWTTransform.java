package jwave.transforms;

import jwave.transforms.wavelets.Wavelet;
import jwave.utils.ArrayBufferPool;
import jwave.datatypes.natives.Complex;
import java.util.Arrays;

/**
 * A pooled version of MODWTTransform that uses buffer pooling to reduce GC pressure.
 * 
 * <p>This implementation extends MODWTTransform and overrides the convolution methods
 * to use pooled arrays instead of allocating new arrays for each operation. This can
 * significantly reduce garbage collection overhead in applications that perform many
 * transforms.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Use pooled version for better performance
 * MODWTTransform modwt = new PooledMODWTTransform(new Daubechies4());
 * 
 * // Perform many transforms without GC pressure
 * for (int i = 0; i < 10000; i++) {
 *     double[][] coeffs = modwt.forwardMODWT(signal, 5);
 *     // Process coefficients...
 * }
 * 
 * // Clean up thread-local pools when done
 * ArrayBufferPool.remove();
 * }</pre>
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class PooledMODWTTransform extends MODWTTransform {
    
    /**
     * Constructor for pooled MODWT transform.
     * 
     * @param wavelet The wavelet to use for the transform
     */
    public PooledMODWTTransform(Wavelet wavelet) {
        super(wavelet);
        // Replace the standard FFT with a pooled version
        this.fft = new PooledFastFourierTransform();
    }
    
    /**
     * Constructor for pooled MODWT transform with custom FFT threshold.
     * 
     * @param wavelet The wavelet to use for the transform
     * @param fftThreshold Custom threshold for FFT-based convolution
     */
    public PooledMODWTTransform(Wavelet wavelet, int fftThreshold) {
        super(wavelet, fftThreshold);
        // Replace the standard FFT with a pooled version
        this.fft = new PooledFastFourierTransform();
    }
    
    /**
     * Performs convolution with a caller-provided output buffer to eliminate the final copy.
     * This is a more efficient API that avoids the extra allocation.
     * 
     * @param signal The signal to convolve
     * @param filter The filter to convolve with
     * @param adjoint True for adjoint convolution, false for forward convolution
     * @param output Pre-allocated output buffer (must be at least signal.length)
     * @return The output buffer filled with results
     */
    public double[] performConvolutionInto(double[] signal, double[] filter, boolean adjoint, double[] output) {
        if (output == null || output.length < signal.length) {
            throw new IllegalArgumentException("Output buffer must be at least signal.length");
        }
        
        boolean useFFT = false;
        switch (getConvolutionMethod()) {
            case AUTO:
                useFFT = (signal.length * filter.length) > fftConvolutionThreshold;
                break;
            case FFT:
                useFFT = true;
                break;
            case DIRECT:
                useFFT = false;
                break;
        }
        
        if (useFFT) {
            if (adjoint) {
                circularConvolveFFTAdjointInto(signal, filter, output);
            } else {
                circularConvolveFFTInto(signal, filter, output);
            }
        } else {
            if (adjoint) {
                circularConvolveDirectAdjointInto(signal, filter, output);
            } else {
                circularConvolveDirectInto(signal, filter, output);
            }
        }
        
        return output;
    }
    
    /**
     * Override performConvolution to use pooled arrays based on the method selection.
     */
    @Override
    protected double[] performConvolution(double[] signal, double[] filter, boolean adjoint) {
        boolean useFFT = false;
        
        switch (getConvolutionMethod()) {
            case FFT:
                useFFT = true;
                break;
            case DIRECT:
                useFFT = false;
                break;
            case AUTO:
                // Use parent's threshold logic
                useFFT = (signal.length * filter.length) > super.fftConvolutionThreshold;
                break;
        }
        
        if (useFFT) {
            return adjoint ? circularConvolveFFTAdjointPooled(signal, filter) 
                          : circularConvolveFFTPooled(signal, filter);
        } else {
            return adjoint ? this.circularConvolveAdjointPooled(signal, filter) 
                          : this.circularConvolvePooled(signal, filter);
        }
    }
    
    /**
     * Performs circular convolution using pooled arrays.
     */
    private double[] circularConvolvePooled(double[] signal, double[] filter) {
        return circularConvolvePooledHelper(signal, filter, false);
    }
    
    /**
     * Performs adjoint circular convolution using pooled arrays.
     */
    private double[] circularConvolveAdjointPooled(double[] signal, double[] filter) {
        return circularConvolvePooledHelper(signal, filter, true);
    }
    
    /**
     * Helper method for both forward and adjoint circular convolution using pooled arrays.
     * 
     * @param signal The input signal
     * @param filter The filter coefficients
     * @param adjoint If true, performs adjoint convolution (n+m indexing), otherwise forward (n-m indexing)
     * @return The convolution result
     */
    private double[] circularConvolvePooledHelper(double[] signal, double[] filter, boolean adjoint) {
        int N = signal.length;
        int M = filter.length;
        
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        double[] output = pool.borrowDoubleArray(N);
        
        try {
            // Clear the array in case it has old data
            Arrays.fill(output, 0, N, 0.0);
            
            for (int n = 0; n < N; n++) {
                double sum = 0.0;
                for (int m = 0; m < M; m++) {
                    // The only difference between forward and adjoint is the sign in the index calculation
                    int signalIndex = adjoint ? Math.floorMod(n + m, N) : Math.floorMod(n - m, N);
                    sum += signal[signalIndex] * filter[m];
                }
                output[n] = sum;
            }
            
            // Return a copy and return the buffer to the pool
            return Arrays.copyOf(output, N);
        } finally {
            pool.returnDoubleArray(output);
        }
    }
    
    /**
     * Performs circular convolution using FFT with pooled arrays.
     */
    private double[] circularConvolveFFTPooled(double[] signal, double[] filter) {
        return circularConvolveFFTPooledHelper(signal, filter, false);
    }
    
    /**
     * Performs the adjoint of circular convolution using FFT with pooled arrays.
     */
    private double[] circularConvolveFFTAdjointPooled(double[] signal, double[] filter) {
        return circularConvolveFFTPooledHelper(signal, filter, true);
    }
    
    /**
     * Helper method for FFT-based circular convolution (both forward and adjoint).
     * 
     * @param signal The input signal
     * @param filter The filter coefficients
     * @param adjoint If true, performs adjoint operation (multiply by conjugate)
     * @return The convolution result
     */
    private double[] circularConvolveFFTPooledHelper(double[] signal, double[] filter, boolean adjoint) {
        int N = signal.length;
        
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        // Borrow arrays from pool
        double[] paddedFilter = pool.borrowDoubleArray(N);
        Complex[] signalComplex = pool.borrowComplexArray(N);
        Complex[] filterComplex = pool.borrowComplexArray(N);
        Complex[] productFFT = pool.borrowComplexArray(N);
        double[] output = pool.borrowDoubleArray(N);
        
        Complex[] signalFFT = null;
        Complex[] filterFFT = null;
        Complex[] inversed = null;
        
        try {
            // Clear arrays
            Arrays.fill(paddedFilter, 0, N, 0.0);
            
            // Wrap filter to signal length
            for (int i = 0; i < filter.length; i++) {
                paddedFilter[i % N] += filter[i];
            }
            
            // Convert to complex arrays
            for (int i = 0; i < N; i++) {
                signalComplex[i].setReal(signal[i]);
                signalComplex[i].setImag(0);
                filterComplex[i].setReal(paddedFilter[i]);
                filterComplex[i].setImag(0);
            }
            
            // Compute FFTs
            signalFFT = fft.forward(signalComplex);
            filterFFT = fft.forward(filterComplex);
            
            // Pointwise multiplication in frequency domain
            // IMPORTANT: Perform in-place to avoid allocating new Complex objects.
            // Using mul() would create new Complex instances, replacing our pooled objects
            // and defeating the purpose of the pool.
            for (int i = 0; i < N; i++) {
                double sigReal = signalFFT[i].getReal();
                double sigImag = signalFFT[i].getImag();
                double filReal = filterFFT[i].getReal();
                double filImag = filterFFT[i].getImag();
                
                double real, imag;
                if (adjoint) {
                    // For adjoint: multiply by conjugate of filter (real, -imag)
                    real = sigReal * filReal + sigImag * filImag;
                    imag = sigImag * filReal - sigReal * filImag;
                } else {
                    // For forward: standard complex multiplication
                    real = sigReal * filReal - sigImag * filImag;
                    imag = sigReal * filImag + sigImag * filReal;
                }
                
                productFFT[i].setReal(real);
                productFFT[i].setImag(imag);
            }
            
            // Inverse FFT - reuse productFFT array for result
            // Note: FFT.reverse() returns a new array internally. By using PooledFastFourierTransform
            // in the constructor, we minimize allocations in the conversion between double[] and Complex[].
            // The core FFT algorithm allocations are harder to eliminate without modifying the FFT itself.
            inversed = fft.reverse(productFFT);
            
            // Extract real part directly
            for (int i = 0; i < N; i++) {
                output[i] = inversed[i].getReal();
            }
            
            // Return a copy
            // TODO: The Arrays.copyOf() allocates a new array, partially defeating the purpose
            // of pooling. A better API would allow the caller to provide an output buffer or
            // use a callback pattern. However, this would require changing the parent class API.
            // For now, we still benefit from pooling the intermediate arrays used in computation.
            return Arrays.copyOf(output, N);
            
        } finally {
            // Return all borrowed arrays to the pool
            pool.returnDoubleArray(paddedFilter);
            pool.returnComplexArray(signalComplex);
            pool.returnComplexArray(filterComplex);
            pool.returnComplexArray(productFFT);
            pool.returnDoubleArray(output);
            
            // Return FFT-allocated arrays if they were created
            // These arrays are allocated by FFT operations and need to be returned to prevent memory leaks
            if (signalFFT != null) {
                pool.returnComplexArray(signalFFT);
            }
            if (filterFFT != null) {
                pool.returnComplexArray(filterFFT);
            }
            if (inversed != null) {
                pool.returnComplexArray(inversed);
            }
        }
    }
    
    /**
     * Performs inverse MODWT with pooled arrays for vNext allocation.
     */
    @Override
    public double[] inverseMODWT(double[][] coefficients) {
        if (coefficients == null || coefficients.length == 0) {
            return new double[0];
        }

        int maxLevel = coefficients.length - 1;
        if (maxLevel <= 0) {
            return new double[0];
        }

        int N = coefficients[0].length;
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        // Initialize cache if needed
        initializeFilterCache();

        double[] vCurrent = Arrays.copyOf(coefficients[maxLevel], N);
        double[] vNext = pool.borrowDoubleArray(N);

        try {
            for (int j = maxLevel; j >= 1; j--) {
                // Clear vNext
                Arrays.fill(vNext, 0, N, 0.0);
                
                // Use cached filters
                double[] gUpsampled = getCachedGFilter(j);
                double[] hUpsampled = getCachedHFilter(j);

                double[] wCurrent = coefficients[j - 1];

                // Use the adjoint convolution for the inverse transform
                double[] vFromApprox = performConvolution(vCurrent, gUpsampled, true);
                double[] vFromDetail = performConvolution(wCurrent, hUpsampled, true);

                for (int i = 0; i < N; i++) {
                    vNext[i] = vFromApprox[i] + vFromDetail[i];
                }

                // Swap arrays to avoid allocation
                double[] temp = vCurrent;
                vCurrent = vNext;
                vNext = temp;
            }

            // Return a copy of the final result
            return Arrays.copyOf(vCurrent, N);
            
        } finally {
            pool.returnDoubleArray(vNext);
            if (vCurrent != coefficients[maxLevel]) {
                pool.returnDoubleArray(vCurrent);
            }
        }
    }
    
    /**
     * Performs direct circular convolution into a provided output buffer.
     */
    private void circularConvolveDirectInto(double[] signal, double[] filter, double[] output) {
        circularConvolveDirectIntoHelper(signal, filter, output, false);
    }
    
    /**
     * Performs direct circular convolution adjoint into a provided output buffer.
     */
    private void circularConvolveDirectAdjointInto(double[] signal, double[] filter, double[] output) {
        circularConvolveDirectIntoHelper(signal, filter, output, true);
    }
    
    /**
     * Helper method for direct circular convolution into a provided buffer.
     * 
     * @param signal The input signal
     * @param filter The filter coefficients
     * @param output The output buffer to write results into
     * @param adjoint If true, performs adjoint convolution
     */
    private void circularConvolveDirectIntoHelper(double[] signal, double[] filter, double[] output, boolean adjoint) {
        int N = signal.length;
        int M = filter.length;
        
        if (adjoint) {
            // For adjoint, we need to clear the output first as we're accumulating
            Arrays.fill(output, 0, N, 0.0);
            for (int n = 0; n < N; n++) {
                for (int m = 0; m < M; m++) {
                    int outputIndex = Math.floorMod(n + m, N);
                    output[outputIndex] += signal[n] * filter[m];
                }
            }
        } else {
            // For forward convolution, we can write directly
            for (int n = 0; n < N; n++) {
                double sum = 0.0;
                for (int m = 0; m < M; m++) {
                    int signalIndex = Math.floorMod(n - m, N);
                    sum += signal[signalIndex] * filter[m];
                }
                output[n] = sum;
            }
        }
    }
    
    /**
     * Performs FFT-based circular convolution into a provided output buffer.
     */
    private void circularConvolveFFTInto(double[] signal, double[] filter, double[] output) {
        circularConvolveFFTIntoHelper(signal, filter, output, false);
    }
    
    /**
     * Performs FFT-based circular convolution adjoint into a provided output buffer.
     */
    private void circularConvolveFFTAdjointInto(double[] signal, double[] filter, double[] output) {
        circularConvolveFFTIntoHelper(signal, filter, output, true);
    }
    
    /**
     * Helper method for FFT-based circular convolution that writes directly to output buffer.
     * This avoids the memory leak from not returning the pooled array from performConvolution.
     * 
     * @param signal The input signal
     * @param filter The filter coefficients
     * @param output The output buffer to write results to
     * @param adjoint If true, performs adjoint operation (multiply by conjugate)
     */
    private void circularConvolveFFTIntoHelper(double[] signal, double[] filter, double[] output, boolean adjoint) {
        int N = signal.length;
        
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        // Borrow arrays from pool
        double[] paddedFilter = pool.borrowDoubleArray(N);
        Complex[] signalComplex = pool.borrowComplexArray(N);
        Complex[] filterComplex = pool.borrowComplexArray(N);
        Complex[] productFFT = pool.borrowComplexArray(N);
        
        Complex[] signalFFT = null;
        Complex[] filterFFT = null;
        Complex[] inversed = null;
        
        try {
            // Clear arrays
            Arrays.fill(paddedFilter, 0, N, 0.0);
            
            // Wrap filter to signal length
            for (int i = 0; i < filter.length; i++) {
                paddedFilter[i % N] += filter[i];
            }
            
            // Convert to complex arrays
            for (int i = 0; i < N; i++) {
                signalComplex[i].setReal(signal[i]);
                signalComplex[i].setImag(0);
                filterComplex[i].setReal(paddedFilter[i]);
                filterComplex[i].setImag(0);
            }
            
            // Compute FFTs
            signalFFT = fft.forward(signalComplex);
            filterFFT = fft.forward(filterComplex);
            
            // Pointwise multiplication in frequency domain
            for (int i = 0; i < N; i++) {
                double sigReal = signalFFT[i].getReal();
                double sigImag = signalFFT[i].getImag();
                double filReal = filterFFT[i].getReal();
                double filImag = filterFFT[i].getImag();
                
                double real, imag;
                if (adjoint) {
                    // For adjoint: multiply by conjugate of filter (real, -imag)
                    real = sigReal * filReal + sigImag * filImag;
                    imag = sigImag * filReal - sigReal * filImag;
                } else {
                    // For forward: standard complex multiplication
                    real = sigReal * filReal - sigImag * filImag;
                    imag = sigReal * filImag + sigImag * filReal;
                }
                
                productFFT[i].setReal(real);
                productFFT[i].setImag(imag);
            }
            
            // Inverse FFT
            inversed = fft.reverse(productFFT);
            
            // Extract real part directly to output buffer
            for (int i = 0; i < N; i++) {
                output[i] = inversed[i].getReal();
            }
            
        } finally {
            // Return all borrowed arrays to the pool
            pool.returnDoubleArray(paddedFilter);
            pool.returnComplexArray(signalComplex);
            pool.returnComplexArray(filterComplex);
            pool.returnComplexArray(productFFT);
            
            // Return FFT-allocated arrays if they were created
            if (signalFFT != null) {
                pool.returnComplexArray(signalFFT);
            }
            if (filterFFT != null) {
                pool.returnComplexArray(filterFFT);
            }
            if (inversed != null) {
                pool.returnComplexArray(inversed);
            }
        }
    }
}