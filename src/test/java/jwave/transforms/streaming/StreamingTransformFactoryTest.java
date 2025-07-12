/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.wavelets.haar.Haar1;
import jwave.transforms.wavelets.daubechies.Daubechies4;
import jwave.transforms.wavelets.symlets.Symlet8;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for StreamingTransformFactory.
 * 
 * @author Prophetizo
 * @date 2025-07-12
 */
public class StreamingTransformFactoryTest {
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithNullType() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        StreamingTransformFactory.create(null, new Haar1(), config);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithNullConfig() {
        StreamingTransformFactory.create(
            StreamingTransformFactory.TransformType.FWT, 
            new Haar1(), 
            null
        );
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFWTWithoutWavelet() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        StreamingTransformFactory.create(
            StreamingTransformFactory.TransformType.FWT, 
            null, 
            config
        );
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateWPTWithoutWavelet() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        StreamingTransformFactory.create(
            StreamingTransformFactory.TransformType.WPT, 
            null, 
            config
        );
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateMODWTWithoutWavelet() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        StreamingTransformFactory.create(
            StreamingTransformFactory.TransformType.MODWT, 
            null, 
            config
        );
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateCWTWithoutWavelet() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        StreamingTransformFactory.create(
            StreamingTransformFactory.TransformType.CWT, 
            null, 
            config
        );
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testCreateFWTNotImplemented() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        StreamingTransformFactory.create(
            StreamingTransformFactory.TransformType.FWT, 
            new Haar1(), 
            config
        );
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testCreateWPTNotImplemented() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        StreamingTransformFactory.create(
            StreamingTransformFactory.TransformType.WPT, 
            new Daubechies4(), 
            config
        );
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testCreateMODWTNotImplemented() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        StreamingTransformFactory.create(
            StreamingTransformFactory.TransformType.MODWT, 
            new Symlet8(), 
            config
        );
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testCreateCWTNotImplemented() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        StreamingTransformFactory.create(
            StreamingTransformFactory.TransformType.CWT, 
            new Haar1(), 
            config
        );
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testCreateFFTNotImplemented() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        StreamingTransformFactory.create(
            StreamingTransformFactory.TransformType.FFT, 
            null, // FFT doesn't require wavelet
            config
        );
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testCreateDFTNotImplemented() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        StreamingTransformFactory.create(
            StreamingTransformFactory.TransformType.DFT, 
            null, // DFT doesn't require wavelet
            config
        );
    }
    
    @Test
    public void testCreateWithBufferSize() {
        // Test the convenience method
        try {
            StreamingTransformFactory.create(
                StreamingTransformFactory.TransformType.FWT,
                new Haar1(),
                1024
            );
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Streaming FWT not yet implemented"));
        }
    }
    
    @Test
    public void testFFTDoesNotRequireWavelet() {
        // FFT/DFT should not throw for null wavelet, only for not implemented
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        try {
            StreamingTransformFactory.create(
                StreamingTransformFactory.TransformType.FFT, 
                null, 
                config
            );
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Streaming FFT not yet implemented"));
        } catch (IllegalArgumentException e) {
            fail("FFT should not require a wavelet");
        }
    }
    
    @Test
    public void testDFTDoesNotRequireWavelet() {
        StreamingTransformConfig config = StreamingTransformConfig.builder().build();
        try {
            StreamingTransformFactory.create(
                StreamingTransformFactory.TransformType.DFT, 
                null, 
                config
            );
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Streaming DFT not yet implemented"));
        } catch (IllegalArgumentException e) {
            fail("DFT should not require a wavelet");
        }
    }
    
    @Test
    public void testGetRecommendedBufferSize() {
        // Test FWT/WPT recommendations
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FWT, 5));
        assertEquals(512, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FWT, 6));
        assertEquals(2048, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.WPT, 8));
        
        // Test MODWT recommendations
        assertEquals(512, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.MODWT, 3));
        assertEquals(640, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.MODWT, 5));
        
        // Test CWT recommendations
        assertEquals(256, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.CWT, 3));
        assertEquals(320, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.CWT, 5));
        
        // Test FFT/DFT recommendations
        assertEquals(1024, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FFT, 5));
        assertEquals(2048, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.FFT, 11));
        assertEquals(1024, StreamingTransformFactory.getRecommendedBufferSize(
            StreamingTransformFactory.TransformType.DFT, 10));
    }
    
    @Test
    public void testTransformTypeEnum() {
        // Ensure all enum values are handled
        StreamingTransformFactory.TransformType[] types = 
            StreamingTransformFactory.TransformType.values();
        assertEquals(6, types.length);
        
        // Verify enum names
        assertEquals("FWT", types[0].name());
        assertEquals("WPT", types[1].name());
        assertEquals("MODWT", types[2].name());
        assertEquals("CWT", types[3].name());
        assertEquals("FFT", types[4].name());
        assertEquals("DFT", types[5].name());
    }
}