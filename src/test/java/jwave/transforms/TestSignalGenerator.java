package jwave.transforms;

import java.util.Random;

/**
 * Common test signal generation utilities for wavelet transform tests.
 * Provides various types of test signals commonly used in signal processing.
 * 
 * @author Stephen Romano
 */
public class TestSignalGenerator {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private TestSignalGenerator() {}
    
    /**
     * Generates a composite sinusoidal signal with noise.
     * This is the most commonly used test signal across MODWT tests.
     * 
     * @param length The desired length of the signal
     * @return A signal containing multiple frequency components and noise
     */
    public static double[] generateCompositeSignal(int length) {
        Random rnd = new Random(123456789); // Fixed seed for reproducibility
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 8.0) +
                       0.1 * rnd.nextDouble();
        }
        return signal;
    }
    
    /**
     * Generates a composite sinusoidal signal with additional low frequency component.
     * Useful for testing multi-resolution analysis.
     * 
     * @param length The desired length of the signal
     * @return A signal with high, medium, and low frequency components plus noise
     */
    public static double[] generateMultiFrequencySignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 8.0) +
                       0.25 * Math.sin(2 * Math.PI * i / 128.0) +
                       0.1 * Math.random();
        }
        return signal;
    }
    
    /**
     * Generates a clean composite sinusoidal signal without noise.
     * Useful for testing exact reconstruction properties.
     * 
     * @param length The desired length of the signal
     * @return A deterministic signal with multiple frequency components
     */
    public static double[] generateCleanSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 8.0) +
                       0.25 * Math.cos(2 * Math.PI * i / 64.0);
        }
        return signal;
    }
    
    /**
     * Generates a simple sinusoidal signal without noise.
     * Useful for thread safety tests where deterministic behavior is needed.
     * 
     * @param length The desired length of the signal
     * @return A clean sinusoidal signal
     */
    public static double[] generateSimpleSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 8.0);
        }
        return signal;
    }
    
    /**
     * Generates a constant signal.
     * Useful for testing edge cases.
     * 
     * @param length The desired length of the signal
     * @param value The constant value
     * @return A signal with all elements equal to the specified value
     */
    public static double[] generateConstantSignal(int length, double value) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = value;
        }
        return signal;
    }
    
    /**
     * Generates a linear trend signal.
     * Useful for testing trend preservation.
     * 
     * @param length The desired length of the signal
     * @param slope The slope of the linear trend
     * @return A signal with linear trend
     */
    public static double[] generateLinearSignal(int length, double slope) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = slope * i;
        }
        return signal;
    }
    
    /**
     * Generates a random signal with uniform distribution.
     * 
     * @param length The desired length of the signal
     * @return A signal with random values between 0 and 1
     */
    public static double[] generateRandomSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.random();
        }
        return signal;
    }
    
    /**
     * Generates a signal with a step function (Heaviside).
     * Useful for testing edge detection capabilities.
     * 
     * @param length The desired length of the signal
     * @param stepPosition The position where the step occurs
     * @param lowValue The value before the step
     * @param highValue The value after the step
     * @return A signal with a step discontinuity
     */
    public static double[] generateStepSignal(int length, int stepPosition, 
                                            double lowValue, double highValue) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = (i < stepPosition) ? lowValue : highValue;
        }
        return signal;
    }
}