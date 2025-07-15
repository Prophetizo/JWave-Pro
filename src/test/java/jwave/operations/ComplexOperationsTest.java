/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
 *
 * Copyright (c) 2025 Prophetizo
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
package jwave.operations;

import static org.junit.Assert.*;
import jwave.datatypes.natives.Complex;
import org.junit.Test;

/**
 * Unit tests for ComplexOperations implementations.
 * 
 * Tests both StandardComplexOperations and SIMDComplexOperations
 * to ensure they produce identical results.
 * 
 * @author Stephen Romano
 */
public class ComplexOperationsTest {
    
    private static final double EPSILON = 1e-12;
    
    @Test
    public void testAddOperation() {
        int length = 100;
        Complex[] array1 = createComplexArray(length, 1.0, 2.0);
        Complex[] array2 = createComplexArray(length, 3.0, 4.0);
        
        // Test standard implementation
        ComplexOperations standardOps = new StandardComplexOperations();
        Complex[] resultStandard = new Complex[length];
        standardOps.add(array1, array2, resultStandard, length);
        
        // Test SIMD implementation
        ComplexOperations simdOps = new SIMDComplexOperations();
        Complex[] resultSIMD = new Complex[length];
        simdOps.add(array1, array2, resultSIMD, length);
        
        // Compare results
        for (int i = 0; i < length; i++) {
            assertEquals(resultStandard[i].getReal(), resultSIMD[i].getReal(), EPSILON);
            assertEquals(resultStandard[i].getImag(), resultSIMD[i].getImag(), EPSILON);
            // Verify actual values
            assertEquals(4.0, resultStandard[i].getReal(), EPSILON);
            assertEquals(6.0, resultStandard[i].getImag(), EPSILON);
        }
    }
    
    @Test
    public void testSubtractOperation() {
        int length = 100;
        Complex[] array1 = createComplexArray(length, 5.0, 7.0);
        Complex[] array2 = createComplexArray(length, 2.0, 3.0);
        
        // Test both implementations
        ComplexOperations standardOps = new StandardComplexOperations();
        ComplexOperations simdOps = new SIMDComplexOperations();
        
        Complex[] resultStandard = new Complex[length];
        Complex[] resultSIMD = new Complex[length];
        
        standardOps.subtract(array1, array2, resultStandard, length);
        simdOps.subtract(array1, array2, resultSIMD, length);
        
        // Compare results
        for (int i = 0; i < length; i++) {
            assertEquals(resultStandard[i].getReal(), resultSIMD[i].getReal(), EPSILON);
            assertEquals(resultStandard[i].getImag(), resultSIMD[i].getImag(), EPSILON);
            // Verify actual values
            assertEquals(3.0, resultStandard[i].getReal(), EPSILON);
            assertEquals(4.0, resultStandard[i].getImag(), EPSILON);
        }
    }
    
    @Test
    public void testMultiplyOperation() {
        int length = 100;
        Complex[] array1 = createComplexArray(length, 2.0, 3.0);
        Complex[] array2 = createComplexArray(length, 4.0, 5.0);
        
        ComplexOperations standardOps = new StandardComplexOperations();
        ComplexOperations simdOps = new SIMDComplexOperations();
        
        Complex[] resultStandard = new Complex[length];
        Complex[] resultSIMD = new Complex[length];
        
        standardOps.multiply(array1, array2, resultStandard, length);
        simdOps.multiply(array1, array2, resultSIMD, length);
        
        // Compare results
        for (int i = 0; i < length; i++) {
            assertEquals(resultStandard[i].getReal(), resultSIMD[i].getReal(), EPSILON);
            assertEquals(resultStandard[i].getImag(), resultSIMD[i].getImag(), EPSILON);
            // Verify actual values: (2+3i)(4+5i) = 8-15+12i+20i = -7+22i
            assertEquals(-7.0, resultStandard[i].getReal(), EPSILON);
            assertEquals(22.0, resultStandard[i].getImag(), EPSILON);
        }
    }
    
    @Test
    public void testMultiplyScalarOperation() {
        int length = 100;
        Complex[] array = createComplexArray(length, 2.0, 3.0);
        double scalar = 5.0;
        
        ComplexOperations standardOps = new StandardComplexOperations();
        ComplexOperations simdOps = new SIMDComplexOperations();
        
        Complex[] resultStandard = new Complex[length];
        Complex[] resultSIMD = new Complex[length];
        
        standardOps.multiplyScalar(array, scalar, resultStandard, length);
        simdOps.multiplyScalar(array, scalar, resultSIMD, length);
        
        // Compare results
        for (int i = 0; i < length; i++) {
            assertEquals(resultStandard[i].getReal(), resultSIMD[i].getReal(), EPSILON);
            assertEquals(resultStandard[i].getImag(), resultSIMD[i].getImag(), EPSILON);
            // Verify actual values
            assertEquals(10.0, resultStandard[i].getReal(), EPSILON);
            assertEquals(15.0, resultStandard[i].getImag(), EPSILON);
        }
    }
    
    @Test
    public void testConjugateOperation() {
        int length = 100;
        Complex[] array = createComplexArray(length, 2.0, 3.0);
        
        ComplexOperations standardOps = new StandardComplexOperations();
        ComplexOperations simdOps = new SIMDComplexOperations();
        
        Complex[] resultStandard = new Complex[length];
        Complex[] resultSIMD = new Complex[length];
        
        standardOps.conjugate(array, resultStandard, length);
        simdOps.conjugate(array, resultSIMD, length);
        
        // Compare results
        for (int i = 0; i < length; i++) {
            assertEquals(resultStandard[i].getReal(), resultSIMD[i].getReal(), EPSILON);
            assertEquals(resultStandard[i].getImag(), resultSIMD[i].getImag(), EPSILON);
            // Verify actual values
            assertEquals(2.0, resultStandard[i].getReal(), EPSILON);
            assertEquals(-3.0, resultStandard[i].getImag(), EPSILON);
        }
    }
    
    @Test
    public void testMagnitudeOperation() {
        int length = 100;
        Complex[] array = createComplexArray(length, 3.0, 4.0);
        
        ComplexOperations standardOps = new StandardComplexOperations();
        ComplexOperations simdOps = new SIMDComplexOperations();
        
        double[] resultStandard = new double[length];
        double[] resultSIMD = new double[length];
        
        standardOps.magnitude(array, resultStandard, length);
        simdOps.magnitude(array, resultSIMD, length);
        
        // Compare results
        for (int i = 0; i < length; i++) {
            assertEquals(resultStandard[i], resultSIMD[i], EPSILON);
            // Verify actual values: |3+4i| = 5
            assertEquals(5.0, resultStandard[i], EPSILON);
        }
    }
    
    @Test
    public void testMultiplyAccumulate() {
        int length = 5;
        Complex[] array1 = new Complex[] {
            new Complex(1, 0), new Complex(2, 0), new Complex(3, 0),
            new Complex(4, 0), new Complex(5, 0)
        };
        Complex[] array2 = new Complex[] {
            new Complex(1, 0), new Complex(1, 0), new Complex(1, 0),
            new Complex(1, 0), new Complex(1, 0)
        };
        
        ComplexOperations standardOps = new StandardComplexOperations();
        ComplexOperations simdOps = new SIMDComplexOperations();
        
        Complex resultStandard = standardOps.multiplyAccumulate(array1, array2, length);
        Complex resultSIMD = simdOps.multiplyAccumulate(array1, array2, length);
        
        // Compare results
        assertEquals(resultStandard.getReal(), resultSIMD.getReal(), EPSILON);
        assertEquals(resultStandard.getImag(), resultSIMD.getImag(), EPSILON);
        // Verify actual value: 1+2+3+4+5 = 15
        assertEquals(15.0, resultStandard.getReal(), EPSILON);
        assertEquals(0.0, resultStandard.getImag(), EPSILON);
    }
    
    @Test
    public void testArrayConversion() {
        int length = 100;
        Complex[] complexArray = createComplexArray(length, 7.0, 11.0);
        
        ComplexOperations standardOps = new StandardComplexOperations();
        ComplexOperations simdOps = new SIMDComplexOperations();
        
        // Test conversion to separate arrays
        double[] realStandard = new double[length];
        double[] imagStandard = new double[length];
        double[] realSIMD = new double[length];
        double[] imagSIMD = new double[length];
        
        standardOps.toSeparateArrays(complexArray, realStandard, imagStandard);
        simdOps.toSeparateArrays(complexArray, realSIMD, imagSIMD);
        
        // Compare conversion results
        for (int i = 0; i < length; i++) {
            assertEquals(realStandard[i], realSIMD[i], EPSILON);
            assertEquals(imagStandard[i], imagSIMD[i], EPSILON);
            assertEquals(7.0, realStandard[i], EPSILON);
            assertEquals(11.0, imagStandard[i], EPSILON);
        }
        
        // Test conversion back to Complex array
        Complex[] resultStandard = new Complex[length];
        Complex[] resultSIMD = new Complex[length];
        
        standardOps.fromSeparateArrays(realStandard, imagStandard, resultStandard, length);
        simdOps.fromSeparateArrays(realSIMD, imagSIMD, resultSIMD, length);
        
        // Compare results
        for (int i = 0; i < length; i++) {
            assertEquals(resultStandard[i].getReal(), resultSIMD[i].getReal(), EPSILON);
            assertEquals(resultStandard[i].getImag(), resultSIMD[i].getImag(), EPSILON);
            assertEquals(7.0, resultStandard[i].getReal(), EPSILON);
            assertEquals(11.0, resultStandard[i].getImag(), EPSILON);
        }
    }
    
    @Test
    public void testImplementationInfo() {
        ComplexOperations standardOps = new StandardComplexOperations();
        ComplexOperations simdOps = new SIMDComplexOperations();
        
        assertNotNull(standardOps.getImplementationName());
        assertNotNull(simdOps.getImplementationName());
        
        assertFalse(standardOps.isOptimized());
        assertTrue(simdOps.isOptimized());
    }
    
    /**
     * Helper method to create a complex array with uniform values.
     */
    private Complex[] createComplexArray(int length, double real, double imag) {
        Complex[] array = new Complex[length];
        for (int i = 0; i < length; i++) {
            array[i] = new Complex(real, imag);
        }
        return array;
    }
}