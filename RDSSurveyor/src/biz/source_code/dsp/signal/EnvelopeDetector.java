// Copyright 2013 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
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

package biz.source_code.dsp.signal;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterCoefficients;
import biz.source_code.dsp.filter.IirFilterDesignFisher;

/**
* An envelope detector.
*
* <p>The input signal is filtered by a bandpass filter before the envelope is detected.
*/
public class EnvelopeDetector {

private IirFilter            iirFilter;
private double               gAttack;
private double               gRelease;
private double               level;

/**
* Constructs an envelope detector with default parameters for audio / speech.
*
* @param samplingRate
*    Sampling rate in Hz.
*/
public EnvelopeDetector (int samplingRate) {
   double attackTime = 0.0015;
   double releaseTime = 0.03;
   double lowerFilterCutoffFreq = 130;
   double upperFilterCutoffFreq = 4700;
   int filterOrder = 4;                                    // higher bandpass filter orders would be instable because of the small lower cutoff frequency
   double filterRipple = -0.5;
   double fcf1Rel = lowerFilterCutoffFreq / samplingRate;
   double fcf2Rel = upperFilterCutoffFreq / samplingRate;
   IirFilterCoefficients coeffs = IirFilterDesignFisher.design(FilterPassType.bandpass, FilterCharacteristicsType.chebyshev, filterOrder, filterRipple, fcf1Rel, fcf2Rel);
   IirFilter iirFilter = new IirFilter(coeffs);
   init(samplingRate, attackTime, releaseTime, iirFilter); }

/**
* Constructs an envelope detector.
*
* @param samplingRate
*    Sampling rate in Hz.
* @param attackTime
*    Attack time of the envelope detector in seconds (time for 1/e convergence).
* @param releaseTime
*    Release time of the envelope detector in seconds (time for 1/e convergence).
* @param iirFilter
*    Filter for pre-processing the signal. May be null to bypass filtering.
*/
public EnvelopeDetector (int samplingRate, double attackTime, double releaseTime, IirFilter iirFilter) {
   init(samplingRate, attackTime, releaseTime, iirFilter); }

private void init (int samplingRate, double attackTime, double releaseTime, IirFilter iirFilter) {
   gAttack  = Math.exp(-1 / (samplingRate * attackTime));
   gRelease = Math.exp(-1 / (samplingRate * releaseTime));
   this.iirFilter = iirFilter; }

/**
* Processes one input signal value and returns the current envelope level.
*/
public double step (double inputValue) {
   double prefiltered = (iirFilter == null) ? inputValue : iirFilter.step(inputValue);
   double inLevel = Math.abs(prefiltered);
   double g = (inLevel > level) ? gAttack : gRelease;
   level = g * level + (1 - g) * inLevel;
   return level; }

/**
* Processes an array of input signal values and returns an array containing the envelope levels.
*/
public float[] process (float[] in) {
   float[] out = new float[in.length];
   for (int i = 0; i < in.length; i++) {
      out[i] = (float)step(in[i]); }
   return out; }

}
