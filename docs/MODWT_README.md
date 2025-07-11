# MODWT (Maximal Overlap Discrete Wavelet Transform) Implementation

## Overview

This implementation provides a complete MODWT transform for the JWave library, addressing the limitations of the standard DWT by offering shift-invariance and redundancy.

## Key Features

- **Shift Invariance**: Results are consistent regardless of signal alignment
- **No Downsampling**: All decomposition levels maintain the original signal length
- **Perfect Reconstruction**: Signal can be exactly recovered from coefficients
- **Flexible Signal Length**: Works with signals of any length (not restricted to powers of 2)
- **JWave Integration**: Fully compatible with JWave's transform interface

## Mathematical Foundation

The MODWT modifies the DWT filters by:
1. Rescaling by 1/√2 at each level
2. Upsampling by inserting zeros between coefficients
3. Using circular convolution instead of downsampling

For a signal X of length N, the MODWT produces:
- J detail coefficient vectors: W₁, W₂, ..., Wⱼ
- 1 approximation coefficient vector: Vⱼ
- Each vector has length N (no downsampling)

## Usage Examples

### Basic Decomposition
```java
// Create MODWT with Daubechies-4 wavelet
MODWTTransform modwt = new MODWTTransform(new Daubechies4());

// Decompose signal to 4 levels
double[] signal = getTimeSeriesData();
double[][] coeffs = modwt.forwardMODWT(signal, 4);

// coeffs[0] = W₁ (finest details)
// coeffs[1] = W₂ 
// coeffs[2] = W₃
// coeffs[3] = W₄
// coeffs[4] = V₄ (coarsest approximation)
```

### Signal Denoising
```java
// Forward transform
double[][] coeffs = modwt.forwardMODWT(noisySignal, 5);

// Soft threshold detail coefficients
double threshold = calculateUniversalThreshold(signal);
for (int level = 0; level < 3; level++) {
    softThreshold(coeffs[level], threshold);
}

// Reconstruct denoised signal
double[] denoised = modwt.inverseMODWT(coeffs);
```

### Using 1D Interface
```java
// Compatible with JWave's standard interface
double[] flatCoeffs = modwt.forward(signal, 3);
double[] reconstructed = modwt.reverse(flatCoeffs, 3);
```

## Frequency Analysis

Each MODWT level corresponds to a specific frequency band:

| Level | Frequency Range | Period Range |
|-------|----------------|--------------|
| W₁    | fs/4 - fs/2    | 2-4 samples  |
| W₂    | fs/8 - fs/4    | 4-8 samples  |
| W₃    | fs/16 - fs/8   | 8-16 samples |
| Wⱼ    | fs/2^(j+1) - fs/2^j | 2^j - 2^(j+1) samples |

Where fs is the sampling frequency.

## Performance Considerations

- **Computational Complexity**: O(N log N) for N-length signal
- **Memory Usage**: (J+1) × N coefficients for J-level decomposition
- **Optimization**: Consider caching upsampled filters for repeated transforms

## Advantages over Standard DWT

1. **Time Alignment**: Coefficients align with original signal time points
2. **Statistical Analysis**: Better for variance/correlation analysis
3. **Boundary Handling**: Circular convolution avoids edge artifacts
4. **Flexibility**: No power-of-2 length restriction

## Implementation Details

### Filter Modifications
```java
// MODWT filters at level j
h̃ⱼ = h / 2^(j/2)  // Wavelet filter
g̃ⱼ = g / 2^(j/2)  // Scaling filter
```

### Upsampling at Level j
```
Insert 2^(j-1) - 1 zeros between filter coefficients
```

### Reconstruction Formula
```
X = Σ(j=1 to J) Wⱼ + Vⱼ
```

## Test Coverage

The implementation includes comprehensive tests for:
- Perfect reconstruction with various wavelets
- Energy conservation
- Non-power-of-2 signal lengths
- 1D interface compatibility
- Edge cases and error handling

## References

1. Percival, D. B., & Walden, A. T. (2000). *Wavelet Methods for Time Series Analysis*. Cambridge University Press.

2. Cornish, C. R., Bretherton, C. S., & Percival, D. B. (2006). Maximal overlap wavelet statistical analysis with application to atmospheric turbulence. *Boundary-Layer Meteorology*, 119(2), 339-374.

3. Quilty, J., & Adamowski, J. (2018). Addressing the incorrect usage of wavelet-based hydrological and water resources forecasting models for real-world applications. *Journal of Hydrology*, 563, 336-353.

## Author

Stephen Romano

## License

Distributed under the same MIT License as JWave.