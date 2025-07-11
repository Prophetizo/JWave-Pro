#!/usr/bin/env python3
"""
Generate basic reference test data for JWave using only standard Python.
This creates simple test cases with known analytical solutions.
"""

import math
import os

# Ensure output directory exists
output_dir = "../src/test/resources/testdata"
os.makedirs(output_dir, exist_ok=True)

def save_vector(data, filename, header=None):
    """Save a 1D array to a text file."""
    filepath = os.path.join(output_dir, filename)
    with open(filepath, 'w') as f:
        if header:
            f.write(f"# {header}\n")
        for value in data:
            f.write(f"{value:.16e}\n")
    print(f"Generated: {filename}")

def generate_haar_reference():
    """Generate Haar wavelet transform reference for simple cases."""
    print("\n=== Generating Haar Wavelet Reference Data ===")
    
    # Simple test signal
    signal = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    
    # Manual Haar transform (normalized)
    # Level 1: averaging and differencing pairs
    sqrt2 = math.sqrt(2)
    approx1 = []
    detail1 = []
    
    for i in range(0, len(signal), 2):
        approx1.append((signal[i] + signal[i+1]) / sqrt2)
        detail1.append((signal[i] - signal[i+1]) / sqrt2)
    
    save_vector(signal, "haar_simple_input.txt", "Simple test signal [1,2,3,4,5,6,7,8]")
    save_vector(approx1, "haar_level1_approx_manual.txt", "Haar level 1 approximation (manual calc)")
    save_vector(detail1, "haar_level1_detail_manual.txt", "Haar level 1 detail (manual calc)")
    
    # Known constant signal (all coefficients except first approx should be 0)
    constant_signal = [5.0] * 8
    save_vector(constant_signal, "haar_constant_input.txt", "Constant signal [5,5,5,5,5,5,5,5]")
    
    # Known linear signal
    linear_signal = [float(i) for i in range(8)]
    save_vector(linear_signal, "haar_linear_input.txt", "Linear signal [0,1,2,3,4,5,6,7]")

def generate_fft_reference():
    """Generate FFT reference for simple cases."""
    print("\n=== Generating FFT Reference Data ===")
    
    # DC signal
    dc_signal = [1.0] * 8
    dc_fft_real = [8.0] + [0.0] * 7  # DC component = sum of signal
    dc_fft_imag = [0.0] * 8
    
    save_vector(dc_signal, "fft_dc_input.txt", "DC signal (all ones)")
    save_vector(dc_fft_real, "fft_dc_output_real.txt", "FFT of DC signal (real part)")
    save_vector(dc_fft_imag, "fft_dc_output_imag.txt", "FFT of DC signal (imaginary part)")
    
    # Simple sinusoid (1 cycle over 8 points)
    sine_signal = []
    for i in range(8):
        sine_signal.append(math.sin(2 * math.pi * i / 8))
    
    save_vector(sine_signal, "fft_sine_simple_input.txt", "One cycle sine wave over 8 points")
    
    # Impulse signal
    impulse = [0.0] * 8
    impulse[0] = 1.0
    impulse_fft_real = [1.0] * 8  # FFT of impulse is all ones
    impulse_fft_imag = [0.0] * 8
    
    save_vector(impulse, "fft_impulse_input.txt", "Impulse signal")
    save_vector(impulse_fft_real, "fft_impulse_output_real.txt", "FFT of impulse (real)")
    save_vector(impulse_fft_imag, "fft_impulse_output_imag.txt", "FFT of impulse (imag)")

def generate_wavelet_filters():
    """Generate known wavelet filter coefficients."""
    print("\n=== Generating Wavelet Filter Coefficients ===")
    
    # Haar wavelet filters (normalized)
    sqrt2 = math.sqrt(2)
    haar_dec_lo = [1/sqrt2, 1/sqrt2]
    haar_dec_hi = [1/sqrt2, -1/sqrt2]
    haar_rec_lo = [1/sqrt2, 1/sqrt2]
    haar_rec_hi = [-1/sqrt2, 1/sqrt2]
    
    save_vector(haar_dec_lo, "filter_haar_dec_lo.txt", "Haar decomposition low-pass")
    save_vector(haar_dec_hi, "filter_haar_dec_hi.txt", "Haar decomposition high-pass")
    save_vector(haar_rec_lo, "filter_haar_rec_lo.txt", "Haar reconstruction low-pass")
    save_vector(haar_rec_hi, "filter_haar_rec_hi.txt", "Haar reconstruction high-pass")
    
    # Daubechies 2 (same as Haar)
    save_vector(haar_dec_lo, "filter_db2_dec_lo.txt", "Daubechies 2 = Haar")
    
    # Daubechies 4 coefficients (from literature)
    db4_dec_lo = [
        0.48296291314469025,
        0.83651630373746899,
        0.22414386804185735,
        -0.12940952255092145
    ]
    db4_dec_hi = [
        -0.12940952255092145,
        -0.22414386804185735,
        0.83651630373746899,
        -0.48296291314469025
    ]
    
    save_vector(db4_dec_lo, "filter_db4_dec_lo.txt", "Daubechies 4 decomposition low-pass")
    save_vector(db4_dec_hi, "filter_db4_dec_hi.txt", "Daubechies 4 decomposition high-pass")

def generate_test_parameters():
    """Generate parameter files for tests."""
    print("\n=== Generating Test Parameters ===")
    
    # CWT test parameters
    with open(os.path.join(output_dir, "cwt_test_params.txt"), 'w') as f:
        f.write("# CWT test parameters\n")
        f.write("sampling_rate=1000.0\n")
        f.write("signal_length=256\n")
        f.write("num_scales=20\n")
        f.write("scale_min=1.0\n")
        f.write("scale_max=50.0\n")
    
    print("Generated: cwt_test_params.txt")

def main():
    """Generate all reference data."""
    print("Generating basic reference test data for JWave...")
    print(f"Output directory: {os.path.abspath(output_dir)}")
    
    generate_haar_reference()
    generate_fft_reference()
    generate_wavelet_filters()
    generate_test_parameters()
    
    print("\n=== Summary ===")
    print(f"Basic reference data generated successfully in {output_dir}")
    print("Total files created:", len(os.listdir(output_dir)))
    print("\nNote: For comprehensive reference data, install numpy, scipy, and pywavelets:")
    print("pip install numpy scipy pywavelets")

if __name__ == "__main__":
    main()