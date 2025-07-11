package jwave.transforms;

import jwave.datatypes.natives.Complex;
import jwave.utils.ArrayBufferPool;
import jwave.exceptions.JWaveException;
import java.util.Arrays;

/**
 * Pooled version of FastFourierTransform that uses buffer pooling to reduce GC pressure.
 * This is particularly beneficial for applications that perform many FFT operations.
 * 
 * @author Stephen Romano
 * @since 2.1.0
 */
public class PooledFastFourierTransform extends FastFourierTransform {
    
    @Override
    public double[] forward(double[] input) throws JWaveException {
        if (input == null || input.length == 0)
            throw new IllegalArgumentException("Input array cannot be null or empty");
            
        int n = input.length;
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        // Borrow Complex array from pool
        Complex[] complex = pool.borrowComplexArray(n);
        
        try {
            // Convert to complex
            for (int i = 0; i < n; i++) {
                complex[i].setReal(input[i]);
                complex[i].setImag(0.0);
            }
            
            // Perform FFT using parent implementation
            Complex[] result = super.forward(complex);
            
            // Borrow output array
            double[] output = pool.borrowDoubleArray(2 * n);
            try {
                // Convert back to double array (real and imaginary parts)
                for (int i = 0; i < n; i++) {
                    output[2 * i] = result[i].getReal();
                    output[2 * i + 1] = result[i].getImag();
                }
                
                // Return a copy
                return Arrays.copyOf(output, 2 * n);
            } finally {
                pool.returnDoubleArray(output);
                // Return the result array to the pool to prevent memory leak
                pool.returnComplexArray(result);
            }
            
        } finally {
            pool.returnComplexArray(complex);
        }
    }
    
    @Override
    public double[] reverse(double[] input) throws JWaveException {
        if (input == null || input.length == 0 || input.length % 2 != 0)
            throw new IllegalArgumentException("Input array must have even length");
            
        int n = input.length / 2;
        ArrayBufferPool pool = ArrayBufferPool.getInstance();
        
        // Borrow Complex array from pool
        Complex[] complex = pool.borrowComplexArray(n);
        
        try {
            // Convert to complex
            for (int i = 0; i < n; i++) {
                complex[i].setReal(input[2 * i]);
                complex[i].setImag(input[2 * i + 1]);
            }
            
            // Perform inverse FFT using parent implementation
            Complex[] result = super.reverse(complex);
            
            // Borrow output array
            double[] output = pool.borrowDoubleArray(n);
            try {
                // Extract real parts only
                for (int i = 0; i < n; i++) {
                    output[i] = result[i].getReal();
                }
                
                // Return a copy
                return Arrays.copyOf(output, n);
            } finally {
                pool.returnDoubleArray(output);
                // Return the result array to the pool to prevent memory leak
                pool.returnComplexArray(result);
            }
            
        } finally {
            pool.returnComplexArray(complex);
        }
    }
}