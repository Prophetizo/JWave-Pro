# Continuous Wavelet Transform (CWT) in JWave

## Overview

The Continuous Wavelet Transform (CWT) provides time-frequency analysis of signals, making it particularly useful for analyzing non-stationary signals where frequency content changes over time. Unlike the discrete wavelet transform, CWT operates at all scales and positions, providing a complete time-frequency representation.

## Mathematical Foundation

The CWT of a signal f(t) is defined as:

```
CWT(a,b) = (1/√a) ∫ f(t) ψ*((t-b)/a) dt
```

Where:
- `a` is the scale parameter (related to frequency)
- `b` is the translation parameter (time position)
- `ψ` is the mother wavelet
- `ψ*` denotes the complex conjugate

## Implementation Details

JWave's CWT implementation uses FFT-based convolution for efficient computation:

1. Signal and wavelet are transformed to frequency domain using FFT
2. Pointwise multiplication in frequency domain (equivalent to convolution)
3. Inverse FFT to obtain time-domain CWT coefficients

This approach provides O(N log N) complexity instead of O(N²) for direct convolution.

## Available Continuous Wavelets

### 1. Morlet Wavelet
- **Purpose**: Time-frequency analysis with good balance
- **Parameters**: `omega0` (center frequency, default: 6.0)
- **Best for**: General signal analysis, feature extraction

```java
Transform cwt = new Transform(
    new ContinuousWaveletTransform(new MorletWavelet(6.0))
);
```

### 2. Mexican Hat Wavelet (Ricker)
- **Purpose**: Edge and singularity detection
- **Parameters**: None (sigma=1.0)
- **Best for**: Finding discontinuities, peaks

```java
Transform cwt = new Transform(
    new ContinuousWaveletTransform(new MexicanHatWavelet())
);
```

### 3. Paul Wavelet
- **Purpose**: Oscillatory pattern detection
- **Parameters**: `order` (default: 4)
- **Best for**: Analyzing oscillatory behavior

```java
Transform cwt = new Transform(
    new ContinuousWaveletTransform(new PaulWavelet(4))
);
```

### 4. DOG Wavelet (Derivative of Gaussian)
- **Purpose**: Multi-scale feature detection
- **Parameters**: `order` (derivative order, default: 2)
- **Best for**: Multi-resolution analysis

```java
Transform cwt = new Transform(
    new ContinuousWaveletTransform(new DOGWavelet(2))
);
```

### 5. Meyer Wavelet
- **Purpose**: Smooth frequency localization
- **Parameters**: None
- **Best for**: Frequency band analysis

```java
Transform cwt = new Transform(
    new ContinuousWaveletTransform(new MeyerWavelet())
);
```

## Usage Examples

### Basic Time-Frequency Analysis

```java
// Create signal with changing frequency
double[] signal = new double[1024];
for (int i = 0; i < signal.length; i++) {
    double t = i / 1024.0;
    // Chirp signal: frequency increases with time
    signal[i] = Math.sin(2 * Math.PI * (10 + 40 * t) * t);
}

// Perform CWT with Morlet wavelet
Transform cwt = new Transform(
    new ContinuousWaveletTransform(new MorletWavelet(6.0))
);

// Analyze at multiple scales
double[] scales = {1, 2, 4, 8, 16, 32, 64};
Complex[][] coefficients = new Complex[scales.length][signal.length];

for (int i = 0; i < scales.length; i++) {
    coefficients[i] = cwt.forward(signal, scales[i]);
}

// Extract magnitude for visualization
double[][] magnitude = new double[scales.length][signal.length];
for (int i = 0; i < scales.length; i++) {
    for (int j = 0; j < signal.length; j++) {
        magnitude[i][j] = coefficients[i][j].abs();
    }
}
```

### Edge Detection Example

```java
// Use Mexican Hat wavelet for edge detection
Transform cwt = new Transform(
    new ContinuousWaveletTransform(new MexicanHatWavelet())
);

// Detect edges at different scales
double[] scales = {1, 2, 4};
for (double scale : scales) {
    Complex[] coeffs = cwt.forward(signal, scale);
    
    // Find local maxima in coefficient magnitude
    for (int i = 1; i < coeffs.length - 1; i++) {
        double curr = coeffs[i].abs();
        double prev = coeffs[i-1].abs();
        double next = coeffs[i+1].abs();
        
        if (curr > prev && curr > next && curr > threshold) {
            System.out.println("Edge detected at position " + i + 
                             " scale " + scale);
        }
    }
}
```

### Creating a Scalogram

```java
// Generate scalogram for visualization
public double[][] createScalogram(double[] signal, 
                                 double minScale, 
                                 double maxScale, 
                                 int numScales) {
    Transform cwt = new Transform(
        new ContinuousWaveletTransform(new MorletWavelet(6.0))
    );
    
    double[][] scalogram = new double[numScales][signal.length];
    
    for (int i = 0; i < numScales; i++) {
        // Logarithmic scale spacing
        double scale = minScale * Math.pow(maxScale/minScale, 
                                          (double)i/(numScales-1));
        
        Complex[] coeffs = cwt.forward(signal, scale);
        
        // Store magnitude
        for (int j = 0; j < signal.length; j++) {
            scalogram[i][j] = coeffs[j].abs();
        }
    }
    
    return scalogram;
}
```

## Performance Considerations

1. **Signal Length**: CWT is O(N log N) per scale due to FFT implementation
2. **Number of Scales**: Total complexity is O(S × N log N) for S scales
3. **Memory Usage**: Requires complex array storage for each scale
4. **Optimization Tips**:
   - Use power-of-2 signal lengths for optimal FFT performance
   - Limit number of scales to what's needed for analysis
   - Consider MODWT for shift-invariant discrete analysis if CWT is too intensive

## Applications

### Signal Analysis
- Identify frequency components and their time locations
- Detect transient events and their duration
- Analyze frequency modulation patterns

### Pattern Recognition
- Extract features at multiple scales
- Detect specific waveform patterns
- Classify signals based on time-frequency characteristics

### Denoising
- Identify noise components in time-frequency domain
- Selective filtering based on scale and time location
- Preserve important transient features

### Music and Audio
- Pitch detection and tracking
- Beat and rhythm analysis
- Audio feature extraction

## References

1. Mallat, S. (2008). A Wavelet Tour of Signal Processing: The Sparse Way
2. Torrence, C., & Compo, G. P. (1998). A practical guide to wavelet analysis
3. Addison, P. S. (2002). The Illustrated Wavelet Transform Handbook

## See Also

- [CWTExample.java](../src/main/java/jwave/examples/CWTExample.java) - Basic CWT usage
- [ContinuousWaveletTransformTest.java](../src/test/java/jwave/transforms/ContinuousWaveletTransformTest.java) - Unit tests
- Individual wavelet examples in the examples directory