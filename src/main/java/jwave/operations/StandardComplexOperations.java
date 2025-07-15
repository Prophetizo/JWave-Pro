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

import jwave.datatypes.natives.Complex;

/**
 * Standard implementation of ComplexOperations using Complex objects.
 * 
 * This implementation:
 * - Uses the existing Complex class methods
 * - Is straightforward and easy to understand
 * - Serves as a reference implementation
 * - May have more object allocation overhead
 * 
 * Thread-safe for all operations.
 * 
 * @author Stephen Romano
 */
public class StandardComplexOperations implements ComplexOperations {
    
    @Override
    public void add(Complex[] array1, Complex[] array2, Complex[] result, int length) {
        for (int i = 0; i < length; i++) {
            result[i] = array1[i].add(array2[i]);
        }
    }
    
    @Override
    public void subtract(Complex[] array1, Complex[] array2, Complex[] result, int length) {
        for (int i = 0; i < length; i++) {
            result[i] = array1[i].sub(array2[i]);
        }
    }
    
    @Override
    public void multiply(Complex[] array1, Complex[] array2, Complex[] result, int length) {
        for (int i = 0; i < length; i++) {
            result[i] = array1[i].mul(array2[i]);
        }
    }
    
    @Override
    public void multiplyScalar(Complex[] array, double scalar, Complex[] result, int length) {
        for (int i = 0; i < length; i++) {
            result[i] = array[i].mul(scalar);
        }
    }
    
    @Override
    public void conjugate(Complex[] array, Complex[] result, int length) {
        for (int i = 0; i < length; i++) {
            result[i] = array[i].conjugate();
        }
    }
    
    @Override
    public void magnitude(Complex[] array, double[] result, int length) {
        for (int i = 0; i < length; i++) {
            result[i] = array[i].getMag();
        }
    }
    
    @Override
    public Complex multiplyAccumulate(Complex[] array1, Complex[] array2, int length) {
        Complex sum = new Complex(0, 0);
        for (int i = 0; i < length; i++) {
            sum = sum.add(array1[i].mul(array2[i]));
        }
        return sum;
    }
    
    @Override
    public void toSeparateArrays(Complex[] complexArray, double[] realOut, double[] imagOut) {
        for (int i = 0; i < complexArray.length; i++) {
            realOut[i] = complexArray[i].getReal();
            imagOut[i] = complexArray[i].getImag();
        }
    }
    
    @Override
    public void fromSeparateArrays(double[] real, double[] imag, Complex[] complexOut, int length) {
        for (int i = 0; i < length; i++) {
            complexOut[i] = new Complex(real[i], imag[i]);
        }
    }
    
    @Override
    public String getImplementationName() {
        return "Standard Complex Operations";
    }
    
    @Override
    public boolean isOptimized() {
        return false;
    }
}