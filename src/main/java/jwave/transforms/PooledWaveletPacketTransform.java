package jwave.transforms;

import jwave.transforms.wavelets.Wavelet;
import jwave.utils.ArrayBufferPool;
import jwave.exceptions.JWaveException;
import jwave.exceptions.JWaveFailure;
import java.util.Arrays;

/**
 * Pooled version of WaveletPacketTransform that eliminates loop allocations.
 * This is critical for performance as the standard version allocates arrays
 * inside tight nested loops.
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class PooledWaveletPacketTransform extends WaveletPacketTransform {
    
    public PooledWaveletPacketTransform(Wavelet wavelet) {
        super(wavelet);
    }
    
    @Override
    public double[] forward(double[] arrTime, int level) throws JWaveException {
        if (!isBinary(arrTime.length))
            throw new JWaveFailure("PooledWaveletPacketTransform#forward - array length is not 2^p");
            
        int maxLevel = calcExponent(arrTime.length);
        if (level <= 0 || level > maxLevel)
            throw new JWaveFailure("PooledWaveletPacketTransform#forward - invalid level");
            
        int length = arrTime.length;
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        // Allocate result array
        double[] arrHilb = Arrays.copyOf(arrTime, length);
        
        int transformWavelength = _wavelet.getTransformWavelength();
        int h = length;
        int l = 0;
        
        // Pre-allocate the largest buffer we'll need (first level uses full length)
        double[] iBuf = pool.borrowDoubleArray(length);
        
        try {
            while (h >= transformWavelength && l < level) {
                int g = length / h; // number of packets at this level
                
                for (int p = 0; p < g; p++) {
                    // Clear only the portion we're using
                    Arrays.fill(iBuf, 0, h, 0.0);
                    
                    int offset = p * h;
                    for (int i = 0; i < h; i++)
                        iBuf[i] = arrHilb[offset + i];
                        
                    double[] oBuf = _wavelet.forward(iBuf, h);
                    
                    for (int i = 0; i < h; i++)
                        arrHilb[offset + i] = oBuf[i];
                }
                
                h = h >> 1;
                l++;
            }
            
            return arrHilb;
        } finally {
            pool.returnDoubleArray(iBuf);
        }
    }
    
    @Override
    public double[] reverse(double[] arrHilb, int level) throws JWaveException {
        if (!isBinary(arrHilb.length))
            throw new JWaveFailure("PooledWaveletPacketTransform#reverse - array length is not 2^p");
            
        int maxLevel = calcExponent(arrHilb.length);
        if (level < 0 || level > maxLevel)
            throw new JWaveFailure("PooledWaveletPacketTransform#reverse - invalid level");
            
        int length = arrHilb.length;
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        double[] arrTime = Arrays.copyOf(arrHilb, length);
        
        int transformWavelength = _wavelet.getTransformWavelength();
        int h = transformWavelength;
        
        int steps = calcExponent(length);
        for (int l = level; l < steps; l++)
            h = h << 1; // begin reverse transform at certain level
        
        // Pre-allocate buffer for the largest packet size we'll process
        // In reverse, we start small and go large, so we need to calculate max size
        int maxH = h;
        while (maxH << 1 <= arrTime.length && maxH << 1 >= transformWavelength) {
            maxH = maxH << 1;
        }
        double[] iBuf = pool.borrowDoubleArray(maxH);
        
        try {
            while (h <= arrTime.length && h >= transformWavelength) {
                int g = length / h; // number of packets at this level
                
                for (int p = 0; p < g; p++) {
                    // Clear only the portion we're using
                    Arrays.fill(iBuf, 0, h, 0.0);
                    
                    int offset = p * h;
                    for (int i = 0; i < h; i++)
                        iBuf[i] = arrTime[offset + i];
                        
                    double[] oBuf = _wavelet.reverse(iBuf, h);
                    
                    for (int i = 0; i < h; i++)
                        arrTime[offset + i] = oBuf[i];
                }
                
                h = h << 1;
            }
            
            return arrTime;
        } finally {
            pool.returnDoubleArray(iBuf);
        }
    }
}