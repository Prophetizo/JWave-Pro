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
package jwave.datatypes.natives;

import jwave.Base;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Performance tests for OptimizedComplex vs standard Complex operations.
 * 
 * @author Stephen Romano
 */
public class OptimizedComplexPerformanceTest extends Base {
    
    private static final int WARMUP_ITERATIONS = 100;
    private static final int TEST_ITERATIONS = 1000;
    private static final int[] SIZES = { 256, 512, 1024, 2048, 4096 };
    
    @Test
    @Ignore("Performance test - run manually")
    public void testComplexAdditionPerformance() {
        System.out.println("Complex Addition Performance Test");
        System.out.println("=================================");
        
        for (int size : SIZES) {
            // Prepare data
            Complex[] array1 = new Complex[size];
            Complex[] array2 = new Complex[size];
            double[] real1 = new double[size];
            double[] imag1 = new double[size];
            double[] real2 = new double[size];
            double[] imag2 = new double[size];
            double[] realOut = new double[size];
            double[] imagOut = new double[size];
            
            // Initialize with random values
            for (int i = 0; i < size; i++) {
                real1[i] = Math.random();
                imag1[i] = Math.random();
                real2[i] = Math.random();
                imag2[i] = Math.random();
                array1[i] = new Complex(real1[i], imag1[i]);
                array2[i] = new Complex(real2[i], imag2[i]);
            }
            
            // Warmup
            for (int iter = 0; iter < WARMUP_ITERATIONS; iter++) {
                // Standard Complex
                Complex[] result = new Complex[size];
                for (int i = 0; i < size; i++) {
                    result[i] = array1[i].add(array2[i]);
                }
                
                // Optimized
                OptimizedComplex.addBulk(real1, imag1, real2, imag2, realOut, imagOut, size);
            }
            
            // Benchmark standard Complex
            long standardTime = 0;
            for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                long start = System.nanoTime();
                Complex[] result = new Complex[size];
                for (int i = 0; i < size; i++) {
                    result[i] = array1[i].add(array2[i]);
                }
                standardTime += System.nanoTime() - start;
            }
            
            // Benchmark optimized
            long optimizedTime = 0;
            for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                long start = System.nanoTime();
                OptimizedComplex.addBulk(real1, imag1, real2, imag2, realOut, imagOut, size);
                optimizedTime += System.nanoTime() - start;
            }
            
            double standardAvg = standardTime / (double) TEST_ITERATIONS / 1000.0; // microseconds
            double optimizedAvg = optimizedTime / (double) TEST_ITERATIONS / 1000.0;
            double speedup = standardAvg / optimizedAvg;
            
            System.out.printf("Size %d: Standard %.2f μs, Optimized %.2f μs, Speedup: %.2fx%n",
                    size, standardAvg, optimizedAvg, speedup);
        }
        System.out.println();
    }
    
    @Test
    @Ignore("Performance test - run manually")
    public void testComplexMultiplicationPerformance() {
        System.out.println("Complex Multiplication Performance Test");
        System.out.println("======================================");
        
        for (int size : SIZES) {
            // Prepare data
            Complex[] array1 = new Complex[size];
            Complex[] array2 = new Complex[size];
            double[] real1 = new double[size];
            double[] imag1 = new double[size];
            double[] real2 = new double[size];
            double[] imag2 = new double[size];
            double[] realOut = new double[size];
            double[] imagOut = new double[size];
            
            // Initialize with random values
            for (int i = 0; i < size; i++) {
                real1[i] = Math.random();
                imag1[i] = Math.random();
                real2[i] = Math.random();
                imag2[i] = Math.random();
                array1[i] = new Complex(real1[i], imag1[i]);
                array2[i] = new Complex(real2[i], imag2[i]);
            }
            
            // Warmup
            for (int iter = 0; iter < WARMUP_ITERATIONS; iter++) {
                // Standard Complex
                Complex[] result = new Complex[size];
                for (int i = 0; i < size; i++) {
                    result[i] = array1[i].mul(array2[i]);
                }
                
                // Optimized
                OptimizedComplex.multiplyBulk(real1, imag1, real2, imag2, realOut, imagOut, size);
            }
            
            // Benchmark standard Complex
            long standardTime = 0;
            for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                long start = System.nanoTime();
                Complex[] result = new Complex[size];
                for (int i = 0; i < size; i++) {
                    result[i] = array1[i].mul(array2[i]);
                }
                standardTime += System.nanoTime() - start;
            }
            
            // Benchmark optimized
            long optimizedTime = 0;
            for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                long start = System.nanoTime();
                OptimizedComplex.multiplyBulk(real1, imag1, real2, imag2, realOut, imagOut, size);
                optimizedTime += System.nanoTime() - start;
            }
            
            double standardAvg = standardTime / (double) TEST_ITERATIONS / 1000.0; // microseconds
            double optimizedAvg = optimizedTime / (double) TEST_ITERATIONS / 1000.0;
            double speedup = standardAvg / optimizedAvg;
            
            System.out.printf("Size %d: Standard %.2f μs, Optimized %.2f μs, Speedup: %.2fx%n",
                    size, standardAvg, optimizedAvg, speedup);
        }
        System.out.println();
    }
    
    @Test
    @Ignore("Performance test - run manually")
    public void testConvolutionPerformance() {
        System.out.println("Complex Convolution (Multiply-Accumulate) Performance Test");
        System.out.println("=========================================================");
        
        for (int size : SIZES) {
            // Prepare data
            Complex[] array1 = new Complex[size];
            Complex[] array2 = new Complex[size];
            double[] real1 = new double[size];
            double[] imag1 = new double[size];
            double[] real2 = new double[size];
            double[] imag2 = new double[size];
            
            // Initialize with random values
            for (int i = 0; i < size; i++) {
                real1[i] = Math.random();
                imag1[i] = Math.random();
                real2[i] = Math.random();
                imag2[i] = Math.random();
                array1[i] = new Complex(real1[i], imag1[i]);
                array2[i] = new Complex(real2[i], imag2[i]);
            }
            
            // Warmup
            for (int iter = 0; iter < WARMUP_ITERATIONS; iter++) {
                // Standard Complex
                Complex sum = new Complex(0, 0);
                for (int i = 0; i < size; i++) {
                    sum = sum.add(array1[i].mul(array2[i]));
                }
                
                // Optimized
                OptimizedComplex.multiplyAccumulate(real1, imag1, real2, imag2, size);
            }
            
            // Benchmark standard Complex
            long standardTime = 0;
            for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                long start = System.nanoTime();
                Complex sum = new Complex(0, 0);
                for (int i = 0; i < size; i++) {
                    sum = sum.add(array1[i].mul(array2[i]));
                }
                standardTime += System.nanoTime() - start;
            }
            
            // Benchmark optimized
            long optimizedTime = 0;
            for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                long start = System.nanoTime();
                OptimizedComplex.multiplyAccumulate(real1, imag1, real2, imag2, size);
                optimizedTime += System.nanoTime() - start;
            }
            
            double standardAvg = standardTime / (double) TEST_ITERATIONS / 1000.0; // microseconds
            double optimizedAvg = optimizedTime / (double) TEST_ITERATIONS / 1000.0;
            double speedup = standardAvg / optimizedAvg;
            
            System.out.printf("Size %d: Standard %.2f μs, Optimized %.2f μs, Speedup: %.2fx%n",
                    size, standardAvg, optimizedAvg, speedup);
        }
        System.out.println();
    }
    
    @Test
    @Ignore("Performance test - run manually")
    public void testCompletePerformanceSuite() {
        testComplexAdditionPerformance();
        testComplexMultiplicationPerformance();
        testConvolutionPerformance();
    }
}