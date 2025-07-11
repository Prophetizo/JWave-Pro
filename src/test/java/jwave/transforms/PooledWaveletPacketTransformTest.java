package jwave.transforms;

import jwave.exceptions.JWaveException;
import jwave.transforms.wavelets.biorthogonal.BiOrthogonal35;
import jwave.transforms.wavelets.haar.Haar1;
import jwave.utils.ArrayBufferPool;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for PooledWaveletPacketTransform to verify the ArrayIndexOutOfBoundsException fix.
 */
public class PooledWaveletPacketTransformTest {
    
    private static final double DELTA = 1e-10;
    
    @After
    public void tearDown() {
        ArrayBufferPool.remove();
    }
    
    @Test
    public void testBiOrthogonal35Level2Length64() throws JWaveException {
        // This is the test case that was failing with ArrayIndexOutOfBoundsException
        BiOrthogonal35 wavelet = new BiOrthogonal35();
        PooledWaveletPacketTransform wpt = new PooledWaveletPacketTransform(wavelet);
        
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        
        // This should not throw ArrayIndexOutOfBoundsException
        double[] coeffs = wpt.forward(signal, 2);
        double[] reconstructed = wpt.reverse(coeffs, 2);
        
        assertArrayEquals("Signal should be reconstructed", signal, reconstructed, DELTA);
    }
    
    @Test
    public void testMultipleLevelsWithHaar() throws JWaveException {
        Haar1 wavelet = new Haar1();
        PooledWaveletPacketTransform wpt = new PooledWaveletPacketTransform(wavelet);
        
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i;
        }
        
        // Test different levels
        for (int level = 1; level <= 6; level++) {
            double[] coeffs = wpt.forward(signal, level);
            double[] reconstructed = wpt.reverse(coeffs, level);
            
            assertArrayEquals("Level " + level + " reconstruction failed", 
                            signal, reconstructed, DELTA);
        }
    }
    
    @Test
    public void testCompareWithRegularWPT() throws JWaveException {
        BiOrthogonal35 wavelet = new BiOrthogonal35();
        
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random();
        }
        
        // Regular WPT
        WaveletPacketTransform regularWpt = new WaveletPacketTransform(wavelet);
        double[] regularCoeffs = regularWpt.forward(signal, 3);
        double[] regularReconstructed = regularWpt.reverse(regularCoeffs, 3);
        
        // Pooled WPT
        PooledWaveletPacketTransform pooledWpt = new PooledWaveletPacketTransform(wavelet);
        double[] pooledCoeffs = pooledWpt.forward(signal, 3);
        double[] pooledReconstructed = pooledWpt.reverse(pooledCoeffs, 3);
        
        // Results should be identical
        assertArrayEquals("Forward transforms should be identical", 
                         regularCoeffs, pooledCoeffs, DELTA);
        assertArrayEquals("Reconstructions should be identical", 
                         regularReconstructed, pooledReconstructed, DELTA);
    }
}