package jwave.transforms;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

/**
 * Test to verify FFT adjoint convolution implementation correctness.
 */
public class MODWTFFTAdjointVerificationTest {
    
    @Test
    public void testAdjointConvolutionCorrectness() {
        // Test signal and filter
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.5, -0.5, 0.25};
        
        // Direct adjoint convolution
        double[] directResult = circularConvolveAdjoint(signal, filter);
        
        // FFT adjoint convolution using reflection to access private method
        double[] fftResult = null;
        try {
            MODWTTransform modwt = new MODWTTransform(new jwave.transforms.wavelets.haar.Haar1());
            java.lang.reflect.Method method = MODWTTransform.class.getDeclaredMethod(
                "circularConvolveFFTAdjoint", double[].class, double[].class);
            method.setAccessible(true);
            fftResult = (double[]) method.invoke(modwt, signal, filter);
        } catch (Exception e) {
            fail("Failed to invoke FFT adjoint method: " + e.getMessage());
        }
        
        System.out.println("Signal: " + Arrays.toString(signal));
        System.out.println("Filter: " + Arrays.toString(filter));
        System.out.println("Direct adjoint result: " + Arrays.toString(directResult));
        System.out.println("FFT adjoint result:    " + Arrays.toString(fftResult));
        
        // Compare results
        assertArrayEquals("FFT adjoint should match direct adjoint", 
                         directResult, fftResult, 1e-10);
    }
    
    @Test
    public void testAdjointPropertyWithMatrix() {
        // Test that adjoint truly implements the transpose of convolution matrix
        // For a smaller example to visualize
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] filter = {0.5, -0.5};
        
        System.out.println("\n=== Testing Adjoint Property ===");
        System.out.println("Signal: " + Arrays.toString(signal));
        System.out.println("Filter: " + Arrays.toString(filter));
        
        // Build convolution matrix explicitly
        int N = signal.length;
        int M = filter.length;
        double[][] convMatrix = new double[N][N];
        
        // Fill convolution matrix
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int filterIdx = Math.floorMod(i - j, N);
                if (filterIdx < M) {
                    convMatrix[i][j] = filter[filterIdx];
                }
            }
        }
        
        System.out.println("\nConvolution matrix:");
        for (int i = 0; i < N; i++) {
            System.out.print("[ ");
            for (int j = 0; j < N; j++) {
                System.out.printf("%6.2f ", convMatrix[i][j]);
            }
            System.out.println("]");
        }
        
        // Compute direct convolution: y = H * x
        double[] directConv = new double[N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                directConv[i] += convMatrix[i][j] * signal[j];
            }
        }
        
        // Compute adjoint convolution: y = H^T * x
        double[] adjointConv = new double[N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                adjointConv[i] += convMatrix[j][i] * signal[j];  // Note: transposed indices
            }
        }
        
        // Compare with our implementation
        double[] ourAdjoint = circularConvolveAdjoint(signal, filter);
        
        System.out.println("\nDirect convolution (H*x):   " + Arrays.toString(directConv));
        System.out.println("Matrix adjoint (H^T*x):      " + Arrays.toString(adjointConv));
        System.out.println("Our adjoint implementation:  " + Arrays.toString(ourAdjoint));
        
        assertArrayEquals("Our adjoint should match matrix transpose", 
                         adjointConv, ourAdjoint, 1e-10);
    }
    
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
}