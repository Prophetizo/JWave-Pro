# JWave - Enhanced Edition
## High-Performance Java Wavelet Transform Library

![Java Version](https://img.shields.io/badge/Java-21-blue)
![Maven](https://img.shields.io/badge/Maven-3.9+-orange)
![License](https://img.shields.io/badge/License-MIT-green)
![Status](https://img.shields.io/badge/Status-Production_Ready-brightgreen)

## Overview

JWave Enhanced Edition is a comprehensive Java library for wavelet transforms, building upon the original JWave by Christian Scheiblich (Graetz23) with significant performance improvements, new transform types, and production-ready features.

### Key Enhancements Over Original JWave

- **47x faster MODWT** with FFT-based convolution
- **Parallel Wavelet Packet Transform** with 1.2-1.3x speedup
- **Continuous Wavelet Transform (CWT)** implementation
- **Memory-efficient buffer pooling** reducing GC pressure by 36-70%
- **50+ wavelets** including new continuous wavelet families
- **Thread-safe implementations** for production use
- **Comprehensive documentation** with mathematical foundations

## Features

### Transform Types

1. **Fast Wavelet Transform (FWT)** - Standard discrete wavelet transform
   - 1D, 2D, and 3D support
   - Power-of-2 signal lengths
   - All orthogonal wavelet families

2. **Wavelet Packet Transform (WPT)** - Full decomposition tree
   - Parallel implementation available
   - Memory-pooled version for efficiency
   - Optimal basis selection support

3. **Continuous Wavelet Transform (CWT)** - Time-frequency analysis
   - Morlet, Mexican Hat, Paul, DOG, Meyer wavelets
   - Scale-based analysis
   - Complex wavelet support for phase information

4. **Maximal Overlap Discrete Wavelet Transform (MODWT)** - Shift-invariant transform
   - Handles arbitrary signal lengths
   - FFT-accelerated (up to 47x faster)
   - Perfect for time series analysis

5. **Discrete Fourier Transform (DFT/FFT)** - Frequency domain analysis
   - Fast Fourier Transform implementation
   - 1D, 2D, and 3D support

### Performance Optimizations

- **Parallel Processing**: Multi-threaded WPT using ForkJoinPool
- **Memory Pooling**: Thread-local buffer reuse via `ArrayBufferPool`
- **FFT Convolution**: Automatic method selection for optimal performance
- **Filter Caching**: Lazy initialization with thread-safe access
- **In-Place API**: Foundation for future zero-copy operations

### Wavelet Families

#### Orthogonal Wavelets
- Haar
- Daubechies (1-20)
- Symlets (1-20)
- Coiflets (1-5)
- Discrete Meyer

#### Biorthogonal Wavelets
- BiOrthogonal (various configurations)
- CDF wavelets

#### Continuous Wavelets
- Morlet (complex-valued)
- Mexican Hat (Ricker)
- Paul
- Derivative of Gaussian (DOG)
- Meyer

#### Other Wavelets
- Legendre (1-3)
- Battle-Lemarie
- Custom wavelets via extension

## Getting Started

### Maven Dependency

```xml
<dependency>
    <groupId>com.prophetizo</groupId>
    <artifactId>jwave-pro</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Basic Examples

#### Fast Wavelet Transform
```java
Transform t = new Transform(new FastWaveletTransform(new Daubechies4()));

double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
double[] coeffs = t.forward(signal);  // Forward transform
double[] recon = t.reverse(coeffs);   // Inverse transform
```

#### Parallel Wavelet Packet Transform
```java
// Use parallel implementation for better performance
Transform t = new Transform(new ParallelWaveletPacketTransform(new Haar1()));

double[] largeSignal = new double[65536];
// ... fill signal ...
double[] coeffs = t.forward(largeSignal, 6); // 6-level decomposition
```

#### MODWT for Arbitrary Length Signals
```java
MODWTTransform modwt = new MODWTTransform(new Daubechies4());

// Works with any length - no power-of-2 restriction!
double[] signal = {1.5, 0.8, -0.3, 2.1, -1.2, 0.7};

// Decompose to 3 levels
double[][] coeffs = modwt.forwardMODWT(signal, 3);
// coeffs[0-2] = detail coefficients at levels 1-3
// coeffs[3] = approximation coefficients at level 3

// Perfect reconstruction
double[] reconstructed = modwt.inverseMODWT(coeffs);
```

#### Continuous Wavelet Transform
```java
Transform t = new Transform(new ContinuousWaveletTransform(
    new MorletWavelet(6.0))); // Morlet with ω₀ = 6.0

double[] signal = generateChirpSignal(); // Your signal

// Analyze at multiple scales
double[] scales = {1.0, 2.0, 4.0, 8.0, 16.0, 32.0};
Complex[][] cwt = new Complex[scales.length][signal.length];

for (int i = 0; i < scales.length; i++) {
    cwt[i] = t.forward(signal, scales[i]);
}
```

#### Memory-Efficient Processing
```java
// Use pooled implementations to reduce GC pressure
Transform t = new Transform(new PooledWaveletPacketTransform(new Symlet8()));

// Process many signals efficiently
for (double[] signal : largeDataset) {
    double[] coeffs = t.forward(signal);
    processCoefficients(coeffs);
}

// Clean up thread-local pools when done
ArrayBufferPool.remove();
```

## Advanced Usage

### Signal Denoising with MODWT
```java
MODWTTransform modwt = new MODWTTransform(new Daubechies4());
double[] noisySignal = loadNoisySignal();

// Decompose
double[][] coeffs = modwt.forwardMODWT(noisySignal, 5);

// Soft threshold detail coefficients
double threshold = calculateNoiseThreshold(coeffs[0]);
for (int level = 0; level < 5; level++) {
    for (int i = 0; i < coeffs[level].length; i++) {
        coeffs[level][i] = softThreshold(coeffs[level][i], threshold);
    }
}

// Reconstruct denoised signal
double[] denoised = modwt.inverseMODWT(coeffs);
```

### Time-Frequency Analysis with CWT
```java
// Analyze a signal with changing frequency content
ContinuousWaveletTransform cwt = new ContinuousWaveletTransform(
    new MorletWavelet(6.0));

double[] signal = loadECGSignal();
int numScales = 64;
double[][] scalogram = cwt.computeScalogram(signal, numScales);

// Find ridges in the scalogram for feature extraction
List<Feature> features = extractRidges(scalogram);
```

### Handling Odd-Length Signals
```java
// Option 1: Use MODWT (recommended)
MODWTTransform modwt = new MODWTTransform(new Haar1());
double[] oddSignal = {1, 2, 3, 4, 5, 6, 7}; // length = 7
double[][] coeffs = modwt.forwardMODWT(oddSignal, 2);

// Option 2: Use Ancient Egyptian Decomposition
Transform t = new Transform(
    new AncientEgyptianDecomposition(
        new FastWaveletTransform(new Haar1())));
double[] result = t.forward(oddSignal);
```

## Performance Characteristics

| Operation | Performance Gain | Use Case |
|-----------|-----------------|----------|
| MODWT with FFT | Up to 47x faster | Large signals, multi-level decomposition |
| Parallel WPT | 1.2-1.3x speedup | Multi-core systems, large transforms |
| Buffer Pooling | 36-70% less GC | Repeated transforms, real-time processing |
| Filter Caching | 10-20% faster | Multiple transforms with same wavelet |

## Requirements

- Java 21 or higher
- Maven 3.9+ for building
- No external runtime dependencies

## Building from Source

```bash
git clone https://github.com/prophetizo/jwave.git
cd jwave
mvn clean install
```

Run tests:
```bash
mvn test
```

Run performance benchmarks:
```bash
mvn test -Dtest=*PerformanceTest -DenablePerformanceTests=true
```

## Documentation

- Comprehensive JavaDoc with mathematical foundations
- Example code in unit tests
- Performance benchmarks in test suite
- [CLAUDE.md](CLAUDE.md) for AI-assisted development guidelines

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

JWave is distributed under the MIT License. See [LICENSE](LICENSE.md) for details.

Original JWave Copyright (c) 2008-2024 Christian Scheiblich (graetz23@gmail.com)  
Enhanced Edition Copyright (c) 2024-2025 Prophetizo

## Acknowledgments

This project builds upon the excellent foundation of [JWave](https://github.com/graetz23/JWave) by Christian Scheiblich. The enhancements focus on performance, additional transform types, and production readiness while maintaining backward compatibility where possible.

## Version History

See [CHANGELOG.md](CHANGELOG.md) for detailed version history and changes.

**Current Version: 2.1.0-SNAPSHOT**