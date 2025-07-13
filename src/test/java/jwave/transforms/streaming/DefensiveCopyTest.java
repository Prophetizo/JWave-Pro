/**
 * JWave Enhanced Edition
 *
 * Copyright 2025 Prophetizo and original authors
 *
 * Licensed under the MIT License
 */
package jwave.transforms.streaming;

import jwave.transforms.wavelets.haar.Haar1;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test suite to verify that streaming transforms properly use defensive copying
 * to prevent external modification of internal state.
 * 
 * @author Prophetizo
 * @date 2025-07-13
 */
public class DefensiveCopyTest {
    
    @Test
    public void testStreamingWPTDefensiveCopy() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(2)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Add some data
        double[] signal = new double[16];
        for (int i = 0; i < 16; i++) {
            signal[i] = i;
        }
        wpt.update(signal);
        
        // Get coefficients
        double[] coeffs1 = wpt.getCurrentCoefficients();
        double originalValue = coeffs1[0];
        
        // Try to modify the returned array
        coeffs1[0] = 999.0;
        
        // Get coefficients again
        double[] coeffs2 = wpt.getCurrentCoefficients();
        
        // Verify internal state was not modified
        assertEquals("Internal coefficients should not be modified", 
                    originalValue, coeffs2[0], 1e-10);
        assertNotEquals("External modification should not affect internal state",
                       999.0, coeffs2[0], 1e-10);
    }
    
    @Test
    public void testStreamingFWTDefensiveCopy() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(2)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        
        // Add some data
        double[] signal = new double[16];
        for (int i = 0; i < 16; i++) {
            signal[i] = i;
        }
        fwt.update(signal);
        
        // Get coefficients
        double[] coeffs1 = fwt.getCurrentCoefficients();
        double originalValue = coeffs1[0];
        
        // Try to modify the returned array
        coeffs1[0] = 999.0;
        
        // Get coefficients again
        double[] coeffs2 = fwt.getCurrentCoefficients();
        
        // Verify internal state was not modified
        assertEquals("Internal coefficients should not be modified", 
                    originalValue, coeffs2[0], 1e-10);
        assertNotEquals("External modification should not affect internal state",
                       999.0, coeffs2[0], 1e-10);
    }
    
    @Test
    public void testStreamingWPTLazyStrategyDefensiveCopy() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(2)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Add some data
        double[] signal = new double[16];
        for (int i = 0; i < 16; i++) {
            signal[i] = i;
        }
        wpt.update(signal);
        
        // Force computation by getting coefficients
        double[] coeffs1 = wpt.getCurrentCoefficients();
        double originalValue = coeffs1[0];
        
        // Modify returned array
        coeffs1[0] = 888.0;
        
        // Add more data (should trigger LAZY behavior)
        wpt.update(new double[]{1.0});
        
        // Get coefficients again (should still be cached due to LAZY)
        double[] coeffs2 = wpt.getCurrentCoefficients();
        
        // Verify the modification didn't affect internal state
        assertNotEquals("External modification should not affect internal state",
                       888.0, coeffs2[0], 1e-10);
    }
    
    @Test
    public void testStreamingFWTLazyStrategyDefensiveCopy() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(2)
            .updateStrategy(StreamingTransformConfig.UpdateStrategy.LAZY)
            .build();
        
        StreamingFWT fwt = new StreamingFWT(new Haar1(), config);
        
        // Add some data
        double[] signal = new double[16];
        for (int i = 0; i < 16; i++) {
            signal[i] = i;
        }
        fwt.update(signal);
        
        // Force computation by getting coefficients
        double[] coeffs1 = fwt.getCurrentCoefficients();
        double originalValue = coeffs1[0];
        
        // Modify returned array
        coeffs1[0] = 777.0;
        
        // Add more data (should trigger LAZY behavior)
        fwt.update(new double[]{1.0});
        
        // Get coefficients again (should still be cached due to LAZY)
        double[] coeffs2 = fwt.getCurrentCoefficients();
        
        // Verify the modification didn't affect internal state
        assertNotEquals("External modification should not affect internal state",
                       777.0, coeffs2[0], 1e-10);
    }
    
    @Test
    public void testPacketExtractionDefensiveCopy() {
        StreamingTransformConfig config = StreamingTransformConfig.builder()
            .bufferSize(16)
            .maxLevel(2)
            .build();
        
        StreamingWPT wpt = new StreamingWPT(new Haar1(), config);
        
        // Add some data
        double[] signal = new double[16];
        for (int i = 0; i < 16; i++) {
            signal[i] = i;
        }
        wpt.update(signal);
        
        // Get a packet
        double[] packet = wpt.getPacket(1, 0);
        double originalValue = packet[0];
        
        // Modify the packet
        packet[0] = 555.0;
        
        // Get the same packet again
        double[] packet2 = wpt.getPacket(1, 0);
        
        // Verify internal state was not modified
        assertEquals("Packet modification should not affect internal state",
                    originalValue, packet2[0], 1e-10);
        assertNotEquals("External modification should not affect packet data",
                       555.0, packet2[0], 1e-10);
    }
}