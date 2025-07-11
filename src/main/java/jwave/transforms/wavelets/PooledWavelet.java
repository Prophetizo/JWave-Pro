package jwave.transforms.wavelets;

import jwave.utils.ArrayBufferPool;
import java.util.Arrays;

/**
 * A pooled wavelet wrapper that uses buffer pooling for the forward and reverse operations.
 * This can be used to wrap any existing wavelet implementation to reduce GC pressure.
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class PooledWavelet extends Wavelet {
    
    private final Wavelet wrappedWavelet;
    
    /**
     * Creates a pooled wrapper around an existing wavelet.
     * 
     * @param wavelet The wavelet to wrap with pooling
     */
    public PooledWavelet(Wavelet wavelet) {
        super();
        this.wrappedWavelet = wavelet;
        
        // Copy all the wavelet properties
        this._name = "Pooled" + wavelet.getName();
        this._motherWavelength = wavelet.getMotherWavelength();
        this._transformWavelength = wavelet.getTransformWavelength();
        
        // Copy the wavelet coefficients
        this._scalingDeCom = Arrays.copyOf(wavelet.getScalingDeComposition(), 
                                          wavelet.getScalingDeComposition().length);
        this._waveletDeCom = Arrays.copyOf(wavelet.getWaveletDeComposition(), 
                                          wavelet.getWaveletDeComposition().length);
        this._scalingReCon = Arrays.copyOf(wavelet.getScalingReConstruction(), 
                                          wavelet.getScalingReConstruction().length);
        this._waveletReCon = Arrays.copyOf(wavelet.getWaveletReConstruction(), 
                                          wavelet.getWaveletReConstruction().length);
    }
    
    @Override
    public double[] forward(double[] arrTime, int arrTimeLength) {
        if (arrTimeLength > arrTime.length) {
            throw new IllegalArgumentException("arrTimeLength cannot exceed arrTime.length");
        }
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        double[] arrHilb = pool.borrowDoubleArray(arrTimeLength);
        
        try {
            int h = arrTimeLength >> 1; // half length
            
            for (int i = 0; i < h; i++) {
                arrHilb[i] = arrHilb[i + h] = 0.0; // set to zero before sum up
                
                for (int j = 0; j < _motherWavelength; j++) {
                    int k = (i << 1) + j; // k = (i * 2) + j
                    while (k >= arrTimeLength)
                        k -= arrTimeLength; // circulate over arrays
                        
                    arrHilb[i] += arrTime[k] * _scalingDeCom[j]; // low pass
                    arrHilb[i + h] += arrTime[k] * _waveletDeCom[j]; // high pass
                }
            }
            
            // Return a copy
            return Arrays.copyOf(arrHilb, arrTimeLength);
            
        } finally {
            pool.returnDoubleArray(arrHilb);
        }
    }
    
    @Override
    public double[] reverse(double[] arrHilb, int arrHilbLength) {
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        double[] arrTime = pool.borrowDoubleArray(arrHilbLength);
        
        try {
            // Clear the array first
            Arrays.fill(arrTime, 0, arrHilbLength, 0.0);
            
            int h = arrHilbLength >> 1; // half length
            
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < _motherWavelength; j++) {
                    int k = (i << 1) + j; // k = (i * 2) + j
                    while (k >= arrHilbLength)
                        k -= arrHilbLength; // circulate over arrays
                        
                    // Reconstruction from low and high pass
                    arrTime[k] += (arrHilb[i] * _scalingReCon[j]) + 
                                  (arrHilb[i + h] * _waveletReCon[j]);
                }
            }
            
            // Return a copy
            return Arrays.copyOf(arrTime, arrHilbLength);
            
        } finally {
            pool.returnDoubleArray(arrTime);
        }
    }
    
}