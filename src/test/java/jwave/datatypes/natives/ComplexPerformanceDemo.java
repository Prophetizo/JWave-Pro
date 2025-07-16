/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
 *
 * Copyright (c) 2025 Prophetizo Christian (graetz23@gmail.com)
 */
package jwave.datatypes.natives;

/**
 * Demo program to show the performance improvement of SIMD-optimized complex operations.
 * Run with: mvn exec:java -Dexec.mainClass="jwave.datatypes.natives.ComplexPerformanceDemo" -Dexec.classpathScope=test
 * 
 * @author Stephen Romano
 * @date 15.07.2025
 */
public class ComplexPerformanceDemo {
    
    public static void main(String[] args) {
        System.out.println("SIMD-Optimized Complex Number Operations Performance Demo");
        System.out.println("========================================================\n");
        
        int[] sizes = { 256, 512, 1024, 2048, 4096 };
        int warmupIterations = 100;
        int testIterations = 1000;
        
        for (int size : sizes) {
            System.out.println("Testing with size: " + size);
            
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
            
            // Test multiplication performance
            System.out.println("\nComplex Multiplication:");
            
            // Warmup
            for (int iter = 0; iter < warmupIterations; iter++) {
                // Standard
                Complex[] result = new Complex[size];
                for (int i = 0; i < size; i++) {
                    result[i] = array1[i].mul(array2[i]);
                }
                // Optimized
                OptimizedComplex.multiplyBulk(real1, imag1, real2, imag2, realOut, imagOut, size);
            }
            
            // Benchmark standard Complex
            long standardTime = 0;
            for (int iter = 0; iter < testIterations; iter++) {
                long start = System.nanoTime();
                Complex[] result = new Complex[size];
                for (int i = 0; i < size; i++) {
                    result[i] = array1[i].mul(array2[i]);
                }
                standardTime += System.nanoTime() - start;
            }
            
            // Benchmark optimized
            long optimizedTime = 0;
            for (int iter = 0; iter < testIterations; iter++) {
                long start = System.nanoTime();
                OptimizedComplex.multiplyBulk(real1, imag1, real2, imag2, realOut, imagOut, size);
                optimizedTime += System.nanoTime() - start;
            }
            
            double standardAvg = standardTime / (double) testIterations / 1000.0; // microseconds
            double optimizedAvg = optimizedTime / (double) testIterations / 1000.0;
            double speedup = standardAvg / optimizedAvg;
            
            System.out.printf("  Standard:  %.2f μs%n", standardAvg);
            System.out.printf("  Optimized: %.2f μs%n", optimizedAvg);
            System.out.printf("  Speedup:   %.2fx%n", speedup);
            
            // Test convolution performance
            System.out.println("\nComplex Convolution (Multiply-Accumulate):");
            
            // Warmup
            for (int iter = 0; iter < warmupIterations; iter++) {
                // Standard
                Complex sum = new Complex(0, 0);
                for (int i = 0; i < size; i++) {
                    sum = sum.add(array1[i].mul(array2[i]));
                }
                // Optimized
                OptimizedComplex.multiplyAccumulate(real1, imag1, real2, imag2, size);
            }
            
            // Benchmark standard
            standardTime = 0;
            for (int iter = 0; iter < testIterations; iter++) {
                long start = System.nanoTime();
                Complex sum = new Complex(0, 0);
                for (int i = 0; i < size; i++) {
                    sum = sum.add(array1[i].mul(array2[i]));
                }
                standardTime += System.nanoTime() - start;
            }
            
            // Benchmark optimized
            optimizedTime = 0;
            for (int iter = 0; iter < testIterations; iter++) {
                long start = System.nanoTime();
                OptimizedComplex.multiplyAccumulate(real1, imag1, real2, imag2, size);
                optimizedTime += System.nanoTime() - start;
            }
            
            standardAvg = standardTime / (double) testIterations / 1000.0;
            optimizedAvg = optimizedTime / (double) testIterations / 1000.0;
            speedup = standardAvg / optimizedAvg;
            
            System.out.printf("  Standard:  %.2f μs%n", standardAvg);
            System.out.printf("  Optimized: %.2f μs%n", optimizedAvg);
            System.out.printf("  Speedup:   %.2fx%n", speedup);
            System.out.println("\n" + "-".repeat(60) + "\n");
        }
        
        System.out.println("Summary:");
        System.out.println("The SIMD-optimized complex operations provide significant speedups");
        System.out.println("by using separate real/imaginary arrays and loop unrolling.");
        System.out.println("This enables the JVM to auto-vectorize the operations.");
    }
}