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
package jwave.transforms;

import static org.junit.Assert.*;
import org.junit.Test;
import jwave.utils.MathUtils;

/**
 * Test to verify nextPowerOfTwo implementations.
 *
 * @author Stephen Romano
 * @date 07.01.2025
 */
public class NextPowerOfTwoTest {
    
    @Test
    public void testNextPowerOfTwo() {
        // Test the bit-twiddling approach from MathUtils
        assertEquals(1, MathUtils.nextPowerOfTwo(0));
        assertEquals(1, MathUtils.nextPowerOfTwo(1));
        assertEquals(2, MathUtils.nextPowerOfTwo(2));
        assertEquals(4, MathUtils.nextPowerOfTwo(3));
        assertEquals(4, MathUtils.nextPowerOfTwo(4));
        assertEquals(8, MathUtils.nextPowerOfTwo(5));
        assertEquals(8, MathUtils.nextPowerOfTwo(6));
        assertEquals(8, MathUtils.nextPowerOfTwo(7));
        assertEquals(8, MathUtils.nextPowerOfTwo(8));
        assertEquals(16, MathUtils.nextPowerOfTwo(9));
        assertEquals(16, MathUtils.nextPowerOfTwo(15));
        assertEquals(16, MathUtils.nextPowerOfTwo(16));
        assertEquals(32, MathUtils.nextPowerOfTwo(17));
        assertEquals(64, MathUtils.nextPowerOfTwo(63));
        assertEquals(64, MathUtils.nextPowerOfTwo(64));
        assertEquals(128, MathUtils.nextPowerOfTwo(65));
        assertEquals(256, MathUtils.nextPowerOfTwo(255));
        assertEquals(256, MathUtils.nextPowerOfTwo(256));
        assertEquals(512, MathUtils.nextPowerOfTwo(257));
        assertEquals(1024, MathUtils.nextPowerOfTwo(1000));
        assertEquals(2048, MathUtils.nextPowerOfTwo(2000));
        assertEquals(4096, MathUtils.nextPowerOfTwo(3000));
        assertEquals(4096, MathUtils.nextPowerOfTwo(4096));
        assertEquals(8192, MathUtils.nextPowerOfTwo(4097));
        assertEquals(65536, MathUtils.nextPowerOfTwo(50000));
        assertEquals(1048576, MathUtils.nextPowerOfTwo(1000000));
    }
    
    @Test
    public void testIsPowerOfTwo() {
        // Test isPowerOfTwo from MathUtils
        assertFalse(MathUtils.isPowerOfTwo(0));
        assertTrue(MathUtils.isPowerOfTwo(1));
        assertTrue(MathUtils.isPowerOfTwo(2));
        assertFalse(MathUtils.isPowerOfTwo(3));
        assertTrue(MathUtils.isPowerOfTwo(4));
        assertFalse(MathUtils.isPowerOfTwo(5));
        assertFalse(MathUtils.isPowerOfTwo(6));
        assertFalse(MathUtils.isPowerOfTwo(7));
        assertTrue(MathUtils.isPowerOfTwo(8));
        assertFalse(MathUtils.isPowerOfTwo(9));
        assertTrue(MathUtils.isPowerOfTwo(16));
        assertTrue(MathUtils.isPowerOfTwo(32));
        assertTrue(MathUtils.isPowerOfTwo(64));
        assertTrue(MathUtils.isPowerOfTwo(128));
        assertTrue(MathUtils.isPowerOfTwo(256));
        assertTrue(MathUtils.isPowerOfTwo(512));
        assertTrue(MathUtils.isPowerOfTwo(1024));
        assertFalse(MathUtils.isPowerOfTwo(1000));
        assertFalse(MathUtils.isPowerOfTwo(-1));
        assertFalse(MathUtils.isPowerOfTwo(-8));
    }
    
    // The compareBitTwiddlingVsFloatingPoint test has been moved to a dedicated performance test suite.
}