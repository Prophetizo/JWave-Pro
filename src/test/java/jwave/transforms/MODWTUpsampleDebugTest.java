package jwave.transforms;

import org.junit.Test;
import java.util.Arrays;

/**
 * Debug test to understand upsampled filter issue.
 */
public class MODWTUpsampleDebugTest {
    
    @Test
    public void testUpsampledFilterConvolution() {
        // Test signal
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        // Original filter (e.g., Haar)
        double[] filter = {0.5, -0.5};
        
        // Upsample filter for level 2 (insert 1 zero between coefficients)
        double[] upsampledFilter = upsample(filter, 2);
        System.out.println("Original filter: " + Arrays.toString(filter));
        System.out.println("Upsampled filter (level 2): " + Arrays.toString(upsampledFilter));
        
        // Direct convolution
        double[] directResult = circularConvolve(signal, upsampledFilter);
        System.out.println("\nDirect convolution result: " + Arrays.toString(directResult));
        
        // Now test what happens with FFT
        // The issue is that upsampled filters have special frequency characteristics
    }
    
    @Test 
    public void compareFilterUpsampling() {
        // Test the actual filter upsampling used in MODWT
        double[] baseFilter = {0.5, -0.5};
        
        System.out.println("Base filter: " + Arrays.toString(baseFilter));
        
        for (int level = 1; level <= 3; level++) {
            double[] upsampled = upsample(baseFilter, level);
            System.out.println("Level " + level + " upsampled: " + Arrays.toString(upsampled));
            System.out.println("  Length: " + upsampled.length);
            
            // Count non-zero elements
            int nonZeros = 0;
            for (double v : upsampled) {
                if (v != 0) nonZeros++;
            }
            System.out.println("  Non-zero elements: " + nonZeros);
        }
    }
    
    private static double[] upsample(double[] filter, int level) {
        if (level <= 1) return filter;
        int gap = (1 << (level - 1)) - 1;
        int newLength = filter.length + (filter.length - 1) * gap;
        double[] upsampled = new double[newLength];
        for (int i = 0; i < filter.length; i++) {
            upsampled[i * (gap + 1)] = filter[i];
        }
        return upsampled;
    }
    
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
}