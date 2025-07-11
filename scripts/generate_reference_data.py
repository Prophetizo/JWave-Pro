#!/usr/bin/env python3
"""
Generate reference test data for JWave using established Python libraries.
This script creates test vectors using NumPy, SciPy, and PyWavelets.
"""

import numpy as np
import scipy.signal
import scipy.fft
import pywt
import os
import sys

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

def save_matrix(data, filename, header=None):
    """Save a 2D array to a text file."""
    filepath = os.path.join(output_dir, filename)
    with open(filepath, 'w') as f:
        if header:
            f.write(f"# {header}\n")
        for row in data:
            f.write(" ".join(f"{value:.16e}" for value in row) + "\n")
    print(f"Generated: {filename}")

def save_complex_matrix(data, filename, header=None):
    """Save a 2D complex array to a text file."""
    filepath = os.path.join(output_dir, filename)
    with open(filepath, 'w') as f:
        if header:
            f.write(f"# {header}\n")
        for row in data:
            f.write(" ".join(f"{value.real:.16e},{value.imag:.16e}" for value in row) + "\n")
    print(f"Generated: {filename}")

def generate_fft_reference():
    """Generate FFT reference data."""
    print("\n=== Generating FFT Reference Data ===")
    
    # Test signal 1: Simple sinusoid
    n = 64
    fs = 1000
    f = 50
    t = np.arange(n) / fs
    signal1 = np.sin(2 * np.pi * f * t)
    fft1 = scipy.fft.fft(signal1)
    
    save_vector(signal1, "fft_sine_input.txt", "64-point sine wave at 50Hz, fs=1000Hz")
    save_vector(fft1.real, "fft_sine_output_real.txt", "FFT real part")
    save_vector(fft1.imag, "fft_sine_output_imag.txt", "FFT imaginary part")
    
    # Test signal 2: Complex signal
    signal2 = np.array([1+2j, 3+4j, 5+6j, 7+8j, 9+10j, 11+12j, 13+14j, 15+16j])
    fft2 = scipy.fft.fft(signal2)
    
    with open(os.path.join(output_dir, "fft_complex_input.txt"), 'w') as f:
        f.write("# Complex test signal\n")
        for val in signal2:
            f.write(f"{val.real:.16e},{val.imag:.16e}\n")
    
    with open(os.path.join(output_dir, "fft_complex_output.txt"), 'w') as f:
        f.write("# FFT of complex signal\n")
        for val in fft2:
            f.write(f"{val.real:.16e},{val.imag:.16e}\n")
    print("Generated: fft_complex_input.txt, fft_complex_output.txt")

def generate_dwt_reference():
    """Generate DWT reference data using PyWavelets."""
    print("\n=== Generating DWT Reference Data ===")
    
    # Test with standard signal
    signal = np.array([1, 2, 3, 4, 5, 6, 7, 8])
    
    # Haar wavelet
    coeffs_haar = pywt.dwt(signal, 'haar')
    save_vector(signal, "dwt_haar_input.txt", "Test signal [1,2,3,4,5,6,7,8]")
    save_vector(coeffs_haar[0], "dwt_haar_approx.txt", "Haar approximation coefficients")
    save_vector(coeffs_haar[1], "dwt_haar_detail.txt", "Haar detail coefficients")
    
    # Daubechies 4
    coeffs_db4 = pywt.dwt(signal, 'db4')
    save_vector(coeffs_db4[0], "dwt_db4_approx.txt", "Daubechies 4 approximation coefficients")
    save_vector(coeffs_db4[1], "dwt_db4_detail.txt", "Daubechies 4 detail coefficients")
    
    # Multi-level decomposition
    coeffs_multi = pywt.wavedec(signal, 'haar', level=3)
    for i, level_coeffs in enumerate(coeffs_multi):
        save_vector(level_coeffs, f"dwt_haar_multilevel_{i}.txt", 
                   f"Haar multi-level decomposition level {i}")

def generate_modwt_reference():
    """Generate MODWT reference data."""
    print("\n=== Generating MODWT Reference Data ===")
    
    # Test signal
    signal = np.array([1, 2, 3, 4, 5, 6, 7, 8])
    
    # MODWT with Haar wavelet
    coeffs = pywt.swt(signal, 'haar', level=3, trim_approx=False)
    
    save_vector(signal, "modwt_haar_input.txt", "Test signal for MODWT")
    
    # Save each level
    for i, (approx, detail) in enumerate(coeffs):
        save_vector(detail, f"modwt_haar_detail_level{i+1}.txt", 
                   f"MODWT Haar detail coefficients level {i+1}")
        save_vector(approx, f"modwt_haar_approx_level{i+1}.txt", 
                   f"MODWT Haar approximation coefficients level {i+1}")

def generate_cwt_reference():
    """Generate CWT reference data."""
    print("\n=== Generating CWT Reference Data ===")
    
    # Test signal: chirp from 10 Hz to 100 Hz
    fs = 1000
    t = np.linspace(0, 1, fs)
    signal = scipy.signal.chirp(t, 10, 1, 100)
    
    # Morlet wavelet CWT
    widths = np.arange(1, 31)  # scales from 1 to 30
    cwt_matrix = scipy.signal.cwt(signal, scipy.signal.morlet2, widths)
    
    save_vector(signal, "cwt_chirp_input.txt", "Chirp signal 10-100Hz, 1 second, fs=1000Hz")
    save_vector(widths, "cwt_scales.txt", "CWT scales")
    save_matrix(np.abs(cwt_matrix), "cwt_morlet_magnitude.txt", "CWT magnitude (Morlet)")
    save_matrix(np.angle(cwt_matrix), "cwt_morlet_phase.txt", "CWT phase (Morlet)")
    
    # Smaller test for direct validation
    small_signal = np.array([1, 2, 3, 4, 5, 6, 7, 8])
    small_scales = np.array([1, 2, 3, 4])
    small_cwt = scipy.signal.cwt(small_signal, scipy.signal.morlet2, small_scales)
    
    save_vector(small_signal, "cwt_small_input.txt", "Small test signal")
    save_vector(small_scales, "cwt_small_scales.txt", "Small test scales")
    save_complex_matrix(small_cwt, "cwt_small_output.txt", "CWT output (complex)")

def generate_wavelet_filter_reference():
    """Generate wavelet filter coefficients."""
    print("\n=== Generating Wavelet Filter Reference Data ===")
    
    wavelets = ['haar', 'db2', 'db4', 'db8', 'sym4', 'coif2']
    
    for wavelet_name in wavelets:
        wavelet = pywt.Wavelet(wavelet_name)
        
        # Decomposition filters
        save_vector(wavelet.dec_lo, f"filter_{wavelet_name}_dec_lo.txt", 
                   f"{wavelet_name} decomposition low-pass filter")
        save_vector(wavelet.dec_hi, f"filter_{wavelet_name}_dec_hi.txt", 
                   f"{wavelet_name} decomposition high-pass filter")
        
        # Reconstruction filters
        save_vector(wavelet.rec_lo, f"filter_{wavelet_name}_rec_lo.txt", 
                   f"{wavelet_name} reconstruction low-pass filter")
        save_vector(wavelet.rec_hi, f"filter_{wavelet_name}_rec_hi.txt", 
                   f"{wavelet_name} reconstruction high-pass filter")

def generate_special_cases():
    """Generate reference data for special test cases."""
    print("\n=== Generating Special Test Cases ===")
    
    # Edge cases
    edge_signals = {
        "empty": np.array([]),
        "single": np.array([42.0]),
        "power_of_2": np.arange(16),
        "non_power_of_2": np.arange(13),
        "zeros": np.zeros(8),
        "ones": np.ones(8),
        "impulse": np.array([0, 0, 0, 1, 0, 0, 0, 0])
    }
    
    for name, signal in edge_signals.items():
        if len(signal) > 0:
            save_vector(signal, f"edge_case_{name}_input.txt", f"Edge case: {name}")
            
            # FFT if applicable
            if len(signal) > 0:
                fft_result = scipy.fft.fft(signal)
                save_vector(fft_result.real, f"edge_case_{name}_fft_real.txt", f"FFT real part of {name}")
                save_vector(fft_result.imag, f"edge_case_{name}_fft_imag.txt", f"FFT imag part of {name}")
            
            # DWT if length > 1
            if len(signal) > 1:
                try:
                    coeffs = pywt.dwt(signal, 'haar')
                    save_vector(coeffs[0], f"edge_case_{name}_dwt_approx.txt", f"DWT approx of {name}")
                    save_vector(coeffs[1], f"edge_case_{name}_dwt_detail.txt", f"DWT detail of {name}")
                except:
                    pass

def main():
    """Generate all reference data."""
    print("Generating reference test data for JWave...")
    print(f"Output directory: {os.path.abspath(output_dir)}")
    
    try:
        generate_fft_reference()
        generate_dwt_reference()
        generate_modwt_reference()
        generate_cwt_reference()
        generate_wavelet_filter_reference()
        generate_special_cases()
        
        print("\n=== Summary ===")
        print(f"Reference data generated successfully in {output_dir}")
        print("Total files created:", len(os.listdir(output_dir)))
        
    except ImportError as e:
        print(f"\nError: Missing required Python package.")
        print(f"Please install required packages:")
        print("pip install numpy scipy pywavelets")
        sys.exit(1)
    except Exception as e:
        print(f"\nError generating reference data: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()