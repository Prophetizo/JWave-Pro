/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
 *
 * Copyright (c) 2025 Prophetizo Christian (graetz23@gmail.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jwave.datatypes.natives;

import jwave.Base;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for OptimizedComplex operations.
 * 
 * @author Stephen Romano
 * @date 15.07.2025
 */
public class OptimizedComplexTest extends Base {
    
    private static final double DELTA = 1e-10;
    
    @Test
    public void testAddBulk() {
        int size = 10;
        double[] real1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        double[] imag1 = { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
        double[] real2 = { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
        double[] imag2 = { 11, 10, 9, 8, 7, 6, 5, 4, 3, 2 };
        double[] realOut = new double[size];
        double[] imagOut = new double[size];
        
        OptimizedComplex.addBulk(real1, imag1, real2, imag2, realOut, imagOut, size);
        
        // Verify results
        for (int i = 0; i < size; i++) {
            assertEquals(real1[i] + real2[i], realOut[i], DELTA);
            assertEquals(imag1[i] + imag2[i], imagOut[i], DELTA);
        }
    }
    
    @Test
    public void testSubtractBulk() {
        int size = 10;
        double[] real1 = { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
        double[] imag1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        double[] real2 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        double[] imag2 = { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
        double[] realOut = new double[size];
        double[] imagOut = new double[size];
        
        OptimizedComplex.subtractBulk(real1, imag1, real2, imag2, realOut, imagOut, size);
        
        // Verify results
        for (int i = 0; i < size; i++) {
            assertEquals(real1[i] - real2[i], realOut[i], DELTA);
            assertEquals(imag1[i] - imag2[i], imagOut[i], DELTA);
        }
    }
    
    @Test
    public void testMultiplyBulk() {
        int size = 5;
        double[] real1 = { 1, 2, 3, 4, 5 };
        double[] imag1 = { 2, 3, 4, 5, 6 };
        double[] real2 = { 2, 3, 4, 5, 6 };
        double[] imag2 = { 1, 2, 3, 4, 5 };
        double[] realOut = new double[size];
        double[] imagOut = new double[size];
        
        OptimizedComplex.multiplyBulk(real1, imag1, real2, imag2, realOut, imagOut, size);
        
        // Verify results using complex multiplication formula
        // (a + bi)(c + di) = (ac - bd) + (ad + bc)i
        for (int i = 0; i < size; i++) {
            double expectedReal = real1[i] * real2[i] - imag1[i] * imag2[i];
            double expectedImag = real1[i] * imag2[i] + imag1[i] * real2[i];
            assertEquals(expectedReal, realOut[i], DELTA);
            assertEquals(expectedImag, imagOut[i], DELTA);
        }
    }
    
    @Test
    public void testMultiplyScalarBulk() {
        int size = 7;
        double[] real = { 1, 2, 3, 4, 5, 6, 7 };
        double[] imag = { 7, 6, 5, 4, 3, 2, 1 };
        double scalar = 2.5;
        double[] realOut = new double[size];
        double[] imagOut = new double[size];
        
        OptimizedComplex.multiplyScalarBulk(real, imag, scalar, realOut, imagOut, size);
        
        // Verify results
        for (int i = 0; i < size; i++) {
            assertEquals(real[i] * scalar, realOut[i], DELTA);
            assertEquals(imag[i] * scalar, imagOut[i], DELTA);
        }
    }
    
    @Test
    public void testConjugateBulk() {
        int size = 6;
        double[] real = { 1, -2, 3, -4, 5, -6 };
        double[] imag = { -1, 2, -3, 4, -5, 6 };
        double[] realOut = new double[size];
        double[] imagOut = new double[size];
        
        OptimizedComplex.conjugateBulk(real, imag, realOut, imagOut, size);
        
        // Verify results
        for (int i = 0; i < size; i++) {
            assertEquals(real[i], realOut[i], DELTA);
            assertEquals(-imag[i], imagOut[i], DELTA);
        }
    }
    
    @Test
    public void testMagnitudeBulk() {
        int size = 4;
        double[] real = { 3, 0, 4, 1 };
        double[] imag = { 4, 5, 0, 1 };
        double[] magOut = new double[size];
        
        OptimizedComplex.magnitudeBulk(real, imag, magOut, size);
        
        // Verify results
        assertEquals(5.0, magOut[0], DELTA); // 3-4-5 triangle
        assertEquals(5.0, magOut[1], DELTA); // 0+5i
        assertEquals(4.0, magOut[2], DELTA); // 4+0i
        assertEquals(Math.sqrt(2), magOut[3], DELTA); // 1+1i
    }
    
    @Test
    public void testMultiplyAccumulate() {
        int size = 4;
        double[] real1 = { 1, 2, 3, 4 };
        double[] imag1 = { 1, 2, 3, 4 };
        double[] real2 = { 1, 1, 1, 1 };
        double[] imag2 = { 0, 0, 0, 0 };
        
        double[] result = OptimizedComplex.multiplyAccumulate(real1, imag1, real2, imag2, size);
        
        // Verify result
        // Sum of (1+1i)*1 + (2+2i)*1 + (3+3i)*1 + (4+4i)*1 = 10+10i
        assertEquals(10.0, result[0], DELTA);
        assertEquals(10.0, result[1], DELTA);
    }
    
    @Test
    public void testConversionToFromSeparateArrays() {
        int size = 5;
        Complex[] complexArray = new Complex[size];
        for (int i = 0; i < size; i++) {
            complexArray[i] = new Complex(i + 1.0, (i + 1.0) * 2);
        }
        
        double[] real = new double[size];
        double[] imag = new double[size];
        
        // Convert to separate arrays
        OptimizedComplex.toSeparateArrays(complexArray, real, imag);
        
        // Verify conversion
        for (int i = 0; i < size; i++) {
            assertEquals(complexArray[i].getReal(), real[i], DELTA);
            assertEquals(complexArray[i].getImag(), imag[i], DELTA);
        }
        
        // Convert back
        Complex[] reconstructed = new Complex[size];
        OptimizedComplex.fromSeparateArrays(real, imag, reconstructed);
        
        // Verify reconstruction
        for (int i = 0; i < size; i++) {
            assertEquals(complexArray[i].getReal(), reconstructed[i].getReal(), DELTA);
            assertEquals(complexArray[i].getImag(), reconstructed[i].getImag(), DELTA);
        }
    }
    
    @Test
    public void testEdgeCases() {
        // Test with size 1
        double[] real1 = { 5.0 };
        double[] imag1 = { 3.0 };
        double[] real2 = { 2.0 };
        double[] imag2 = { 1.0 };
        double[] realOut = new double[1];
        double[] imagOut = new double[1];
        
        OptimizedComplex.addBulk(real1, imag1, real2, imag2, realOut, imagOut, 1);
        assertEquals(7.0, realOut[0], DELTA);
        assertEquals(4.0, imagOut[0], DELTA);
        
        // Test with size that's not multiple of unroll factor
        int oddSize = 7;
        double[] realOdd = new double[oddSize];
        double[] imagOdd = new double[oddSize];
        double[] realOdd2 = new double[oddSize];
        double[] imagOdd2 = new double[oddSize];
        double[] realOutOdd = new double[oddSize];
        double[] imagOutOdd = new double[oddSize];
        
        for (int i = 0; i < oddSize; i++) {
            realOdd[i] = i;
            imagOdd[i] = i;
            realOdd2[i] = 1;
            imagOdd2[i] = 1;
        }
        
        OptimizedComplex.addBulk(realOdd, imagOdd, realOdd2, imagOdd2, realOutOdd, imagOutOdd, oddSize);
        
        for (int i = 0; i < oddSize; i++) {
            assertEquals(i + 1, realOutOdd[i], DELTA);
            assertEquals(i + 1, imagOutOdd[i], DELTA);
        }
    }
}