package jwave.transforms;

import jwave.transforms.wavelets.daubechies.Daubechies8;
import jwave.exceptions.JWaveException;
import org.junit.Test;

/**
 * Simple test to understand parallel WPT performance characteristics.
 */
public class ParallelWPTSimpleTest {
    
    @Test
    public void testParallelSpeedup() throws JWaveException {
        System.out.println("\n=== Simple Parallel WPT Performance Test ===");
        
        // Large signal for meaningful parallelization
        int size = 32768;
        int level = 8;
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64);
        }
        
        Daubechies8 wavelet = new Daubechies8();
        
        // Warmup
        WaveletPacketTransform wptSeq = new WaveletPacketTransform(wavelet);
        ParallelWaveletPacketTransform wptPar = new ParallelWaveletPacketTransform(wavelet);
        
        for (int i = 0; i < 3; i++) {
            wptSeq.forward(signal, level);
            wptPar.forward(signal, level);
        }
        
        // Sequential timing
        long seqStart = System.nanoTime();
        double[] seqResult = wptSeq.forward(signal, level);
        long seqTime = System.nanoTime() - seqStart;
        
        // Parallel timing
        long parStart = System.nanoTime();
        double[] parResult = wptPar.forward(signal, level);
        long parTime = System.nanoTime() - parStart;
        
        // Results
        double seqMs = seqTime / 1_000_000.0;
        double parMs = parTime / 1_000_000.0;
        double speedup = seqMs / parMs;
        
        System.out.printf("Signal size: %d, Level: %d\n", size, level);
        System.out.printf("Sequential: %.2f ms\n", seqMs);
        System.out.printf("Parallel: %.2f ms\n", parMs);
        System.out.printf("Speedup: %.2fx\n", speedup);
        
        // Calculate packets at each level
        System.out.println("\nPackets per level:");
        int h = size;
        for (int l = 1; l <= level; l++) {
            int packets = size / h;
            System.out.printf("Level %d: %d packets of size %d\n", l, packets, h);
            h = h >> 1;
        }
        
        wptPar.shutdown();
    }
}