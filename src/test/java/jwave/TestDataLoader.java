/**
 * JWave is distributed under the MIT License (MIT); this file is part of.
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
package jwave;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import jwave.datatypes.natives.Complex;

/**
 * Utility class for loading test data from external files.
 * Supports loading reference data from MATLAB, Python (NumPy/SciPy), and other tools.
 *
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 10.01.2025
 */
public class TestDataLoader {

  /**
   * Load a 1D array of doubles from a text file.
   * Expected format: one value per line
   * 
   * @param filename the name of the file in src/test/resources/testdata/
   * @return array of doubles
   * @throws IOException if file cannot be read
   */
  public static double[] loadVector(String filename) throws IOException {
    List<Double> values = new ArrayList<>();
    
    try (InputStream is = TestDataLoader.class.getResourceAsStream("/testdata/" + filename)) {
      if (is == null) {
        throw new FileNotFoundException("Test data file not found: " + filename);
      }
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty() && !line.startsWith("#")) { // Skip empty lines and comments
          values.add(Double.parseDouble(line));
        }
      }
    }
    
    return values.stream().mapToDouble(Double::doubleValue).toArray();
  }

  /**
   * Load a 2D array of doubles from a text file.
   * Expected format: space or comma separated values, one row per line
   * 
   * @param filename the name of the file in src/test/resources/testdata/
   * @return 2D array of doubles
   * @throws IOException if file cannot be read
   */
  public static double[][] loadMatrix(String filename) throws IOException {
    List<double[]> rows = new ArrayList<>();
    
    try (InputStream is = TestDataLoader.class.getResourceAsStream("/testdata/" + filename);
         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      
      if (is == null) {
        throw new FileNotFoundException("Test data file not found: " + filename);
      }
      
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty() && !line.startsWith("#")) {
          String[] values = line.split("[,\\s]+");
          double[] row = new double[values.length];
          for (int i = 0; i < values.length; i++) {
            row[i] = Double.parseDouble(values[i]);
          }
          rows.add(row);
        }
      }
    }
    
    return rows.toArray(new double[0][]);
  }

  /**
   * Load complex-valued data from a text file.
   * Expected format: real,imag per line or real imag per line
   * 
   * @param filename the name of the file in src/test/resources/testdata/
   * @return array of Complex numbers
   * @throws IOException if file cannot be read
   */
  public static Complex[] loadComplexVector(String filename) throws IOException {
    List<Complex> values = new ArrayList<>();
    
    try (InputStream is = TestDataLoader.class.getResourceAsStream("/testdata/" + filename);
         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      
      if (is == null) {
        throw new FileNotFoundException("Test data file not found: " + filename);
      }
      
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty() && !line.startsWith("#")) {
          String[] parts = line.split("[,\\s]+");
          if (parts.length >= 2) {
            double real = Double.parseDouble(parts[0]);
            double imag = Double.parseDouble(parts[1]);
            values.add(new Complex(real, imag));
          } else if (parts.length == 1) {
            // Real-only value
            values.add(new Complex(Double.parseDouble(parts[0]), 0));
          }
        }
      }
    }
    
    return values.toArray(new Complex[0]);
  }

  /**
   * Load complex-valued matrix from a text file.
   * Expected format: real1,imag1 real2,imag2 ... per row
   * 
   * @param filename the name of the file in src/test/resources/testdata/
   * @return 2D array of Complex numbers
   * @throws IOException if file cannot be read
   */
  public static Complex[][] loadComplexMatrix(String filename) throws IOException {
    List<Complex[]> rows = new ArrayList<>();
    
    try (InputStream is = TestDataLoader.class.getResourceAsStream("/testdata/" + filename);
         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      
      if (is == null) {
        throw new FileNotFoundException("Test data file not found: " + filename);
      }
      
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty() && !line.startsWith("#")) {
          // Split by spaces to get complex number pairs
          String[] complexPairs = line.split("\\s+");
          Complex[] row = new Complex[complexPairs.length];
          
          for (int i = 0; i < complexPairs.length; i++) {
            String[] parts = complexPairs[i].split(",");
            if (parts.length >= 2) {
              double real = Double.parseDouble(parts[0]);
              double imag = Double.parseDouble(parts[1]);
              row[i] = new Complex(real, imag);
            } else {
              // Real-only value
              row[i] = new Complex(Double.parseDouble(parts[0]), 0);
            }
          }
          rows.add(row);
        }
      }
    }
    
    return rows.toArray(new Complex[0][]);
  }

  /**
   * Load test parameters from a properties-style file.
   * 
   * @param filename the name of the file in src/test/resources/testdata/
   * @return array of parameters as strings
   * @throws IOException if file cannot be read
   */
  public static String[] loadParameters(String filename) throws IOException {
    List<String> params = new ArrayList<>();
    
    try (InputStream is = TestDataLoader.class.getResourceAsStream("/testdata/" + filename);
         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      
      if (is == null) {
        throw new FileNotFoundException("Test data file not found: " + filename);
      }
      
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty() && !line.startsWith("#")) {
          // Handle key=value format
          if (line.contains("=")) {
            String value = line.substring(line.indexOf('=') + 1).trim();
            params.add(value);
          } else {
            params.add(line);
          }
        }
      }
    }
    
    return params.toArray(new String[0]);
  }

  /**
   * Save a vector to a file for reference data generation.
   * 
   * @param data the data to save
   * @param filename the output filename
   * @throws IOException if file cannot be written
   */
  public static void saveVector(double[] data, String filename) throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
      for (double value : data) {
        writer.println(value);
      }
    }
  }

  /**
   * Save a matrix to a file for reference data generation.
   * 
   * @param data the data to save
   * @param filename the output filename
   * @throws IOException if file cannot be written
   */
  public static void saveMatrix(double[][] data, String filename) throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
      for (double[] row : data) {
        for (int i = 0; i < row.length; i++) {
          if (i > 0) writer.print(" ");
          writer.print(row[i]);
        }
        writer.println();
      }
    }
  }
}