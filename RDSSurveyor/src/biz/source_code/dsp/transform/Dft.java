// Copyright 2015 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
// www.source-code.biz, www.inventec.ch/chdh
//
// This module is multi-licensed and may be used under the terms
// of any of the following licenses:
//
//  EPL, Eclipse Public License, V1.0 or later, http://www.eclipse.org/legal
//  LGPL, GNU Lesser General Public License, V2.1 or later, http://www.gnu.org/licenses/lgpl.html
//
// Please contact the author if you need another license.
// This module is provided "as is", without warranties of any kind.

package biz.source_code.dsp.transform;

import biz.source_code.dsp.math.Complex;

/**
* Discrete Fourier transform (DFT).
*
* <p>This class contains a collection of DFT-related methods.
* The current implementation focuses on the conversion between a real signal and a complex spectrum.
* It's not yet a complete implementation of all the classic DFT functions.
*
* <p>See <a href="http://dsp.stackexchange.com/questions/11376/why-are-magnitudes-normalised-during-synthesis-idft-not-analysis-dft" target="_top">this Stackexchange entry</a>
* for a discussion of magnitude normalization.
*/
public class Dft {

/**
* Computes the DFT on real numbers for a single frequency.
*
* <p>This is a reference implementation without any optimization.
* It's simple to understand, but slow.
*
* @param x
*    The input values (samples).
* @param pos
*    The index of the first value in <code>x</code> to be processed.
* @param len
*    The number of values in <code>x</code> to be processed, starting at <code>pos</code>.
* @param relativeFrequency
*    A frequency relative to <code>len</code>.
*    It represents the number of sinusoidal oscillations within <code>len</code>
*    and is normally within the range 0 (for DC) to <code>len / 2</code>.
*    The absolute frequency is <code>relativeFrequency * samplingRate / len</code>.
* @param normalize
*    <code>true</code> to normalize the magnitude of the result,
*    so that it represents the amplitude of the sinusoidal frequency component.
* @return
*    A complex number that corresponds to the amplitude and phase of a sinusoidal frequency component.
*/
public static Complex directDftSingle (double[] x, int pos, int len, int relativeFrequency, boolean normalize) {
   Complex acc = Complex.ZERO;
   double w = -2 * Math.PI / len * relativeFrequency;
   for (int p = 0; p < len; p++) {
      Complex c = Complex.fromPolar(x[pos + p], w * p);
      acc = acc.add(c); }
   if (normalize) {
      boolean half = relativeFrequency > 0 && 2 * relativeFrequency < len;
         // For the frequencies between 0 and len/2 we have to duplicate the
         // magnitudes, when we use only one of the two complex conjugate values.
      acc = acc.div(half ? len / 2.0 : len); }
   return acc; }

/**
* Computes the DFT on real numbers for a single frequency.
*
* <p>This version uses the <a href="http://en.wikipedia.org/wiki/Goertzel_algorithm">Goertzel algorithm</a>
* to compute the DFT.
* It produces the same results as {@link #directDft}, but it's much faster.
*
* @param x
*    The input values (samples).
* @param pos
*    The index of the first value in <code>x</code> to be processed.
* @param len
*    The number of values in <code>x</code> to be processed, starting at <code>pos</code>.
* @param relativeFrequency
*    A frequency relative to <code>len</code>.
*    It represents the number of sinusoidal oscillations within <code>len</code>
*    and is normally within the range 0 (for DC) to <code>len / 2</code>.
*    The absolute frequency is <code>relativeFrequency * samplingRate / len</code>.
* @param normalize
*    <code>true</code> to normalize the magnitude of the result,
*    so that it represents the amplitude of the sinusoidal frequency component.
* @return
*    A complex number that corresponds to the amplitude and phase of a sinusoidal frequency component.
*/
public static Complex goertzelSingle (double[] x, int pos, int len, int relativeFrequency, boolean normalize) {
   double w = 2 * Math.PI / len * relativeFrequency;
   Complex c = Complex.expj(w);
   Double cr2 = c.re() * 2;
   double s1 = 0;
   double s2 = 0;
   for (int p = 0; p < len; p++) {
      double s0 = x[pos + p] + cr2 * s1 - s2;
      s2 = s1;
      s1 = s0; }
   Complex r = new Complex(c.re() * s1 - s2, c.im() * s1);
   if (normalize) {
      boolean half = relativeFrequency > 0 && 2 * relativeFrequency < len;
         // For the frequencies between 0 and len/2 we have to duplicate the
         // magnitudes, when we use only one of the two complex conjugate values.
      r = r.div(half ? len / 2.0 : len); }
   return r; }

/**
* Computes the DFT on an array of real numbers and returns the complex result.
*
* <p>This method calls {@link #directDftSingle} with <code>normalize = false</code>.
*
* @param x
*    The input values (samples).
* @return
*    An array of complex numbers. It has the same size as the input array.
*    The upper half of the array contains complex conjugates of the lower half.
*/
public static Complex[] directDft (double[] x) {
   Complex[] r = new Complex[x.length];
   for (int frequency = 0; frequency < x.length; frequency++) {
      r[frequency] = directDftSingle(x, 0, x.length, frequency, false); }
   return r; }

/**
* Computes the DFT on an array of real numbers and returns the complex result.
*
* <p>This method calls {@link #goertzelSingle} with <code>normalize = false</code>.
*
* @param x
*    The input values (samples).
* @return
*    An array of complex numbers. It has the same size as the input array.
*    The upper half of the array contains complex conjugates of the lower half.
*/
public static Complex[] goertzel (double[] x) {
   Complex[] r = new Complex[x.length];
   for (int frequency = 0; frequency < x.length; frequency++) {
      r[frequency] = goertzelSingle(x, 0, x.length, frequency, false); }
   return r; }

/**
* Computes the DFT on an array of real numbers and returns the complex spectrum.
*
* <p>This method calls {@link #directDftSingle} with <code>normalize = true</code>
* for the frequencies from 0 to <code>x.length / 2</code>.
*
* <p>See {@link #synthesizeFromSpectrum} for the inverse function.
*
* @param x
*    The input values (samples).
* @return
*    An array of complex numbers that represent the amplitudes and phases of the sinusoidal frequency components.
*    This is the normalized lower half of the DFT output.
*/
public static Complex[] directDftSpectrum (double[] x) {
   int maxFrequency = x.length / 2;
   Complex[] r = new Complex[maxFrequency + 1];
   for (int frequency = 0; frequency <= maxFrequency; frequency++) {
      r[frequency] = directDftSingle(x, 0, x.length, frequency, true); }
   return r; }

/**
* Computes the DFT on an array of real numbers and returns the complex spectrum.
*
* <p>This method calls {@link #goertzelSingle} with <code>normalize = true</code>
* for the frequencies from 0 to <code>x.length / 2</code>.
*
* <p>See {@link #synthesizeFromSpectrum} for the inverse function.
*
* @param x
*    The input values (samples).
* @return
*    An array of complex numbers that represent the amplitudes and phases of the sinusoidal frequency components.
*    This is the normalized lower half of the DFT output.
*/
public static Complex[] goertzelSpectrum (double[] x) {
   int maxFrequency = x.length / 2;
   Complex[] r = new Complex[maxFrequency + 1];
   for (int frequency = 0; frequency <= maxFrequency; frequency++) {
      r[frequency] = goertzelSingle(x, 0, x.length, frequency, true); }
   return r; }

/**
* Computes a kind of inverse DFT on an array of complex numbers that represent a spectrum,
* and returns the result as an array of real numbers.
*
* <p>This is a reference implementation without any optimization.
* It's simple to understand, but slow.
*
* <p>This is the inverse function for {@link #directDftSpectrum} and {@link #goertzelSpectrum}.
*
* @param x
*    Complex numbers that define the amplitudes and phases of the sinusoidal frequency components.
* @param odd
*    If <code>odd</code> is <code>true</code>, <code>2 * x.length - 1</code> output values are generated.
*    Otherwise <code>2 * x.length - 2</code> output values are generated.
* @return
*    The sampled signal that is the sum of the sinusoidal components.
*/
public static double[] synthesizeFromSpectrum (Complex[] x, boolean odd) {
   int len = x.length * 2 - (odd ? 1 : 2);
   double[] r = new double[len];
   for (int frequency = 0; frequency < x.length; frequency++) {
      Complex f = x[frequency];
      synthesizeSinusoidal(r, frequency, f.abs(), f.arg()); }
   return r; }

private static void synthesizeSinusoidal (double[] a, int frequency, double amplitude, double phase) {
   double w = 2 * Math.PI / a.length * frequency;
   for (int p = 0; p < a.length; p++) {
      a[p] += amplitude * Math.cos(phase + w * p); }}

}
