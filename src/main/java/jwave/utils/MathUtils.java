/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
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
package jwave.utils;

/**
 * Utility class for common mathematical operations used throughout JWave.
 * 
 * @author Stephen Romano
 * @date 07.01.2025
 */
public final class MathUtils {
    
    /**
     * Private constructor to prevent instantiation.
     */
    private MathUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Computes the next power of two greater than or equal to n.
     * Uses efficient bit manipulation for performance.
     * 
     * @param n input value
     * @return next power of two greater than or equal to n
     */
    public static int nextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        return 1 << (32 - Integer.numberOfLeadingZeros(n - 1));
    }
    
    /**
     * Checks if a number is a power of two.
     * 
     * @param n the number to check
     * @return true if n is a power of two, false otherwise
     */
    public static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}