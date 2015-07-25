// Copyright 2014 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
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

/**
* A simple signal level normalizer.
*
* <p>
* To determine the amplification factor, the signal is divided into segments and for each segment the
* <a href="http://en.wikipedia.org/wiki/Root_mean_square">RMS</a> value is computed.
* The maximum RMS value is used to adjust the aplitude of the signal.
* For speech audio, a segment size of 100 ms might be reasonable.
*/
public class RmsNormalizer {

/**
* Adjusts the amplitude level of a signal to match a specified target RMS amplitude value.
*
* @param signals
*    The multi-channel signal values.
* @param targetRms
*    Target RMS amplitude.
* @param segmentSize
*    Number of signal samples to be used per RMS measurement.
*/
public static void normalize (float[][] signals, float targetRms, int segmentSize) {
   double maxRms = findMaxRmsLevel(signals, segmentSize);
   if (maxRms == 0) {
      return; }
   double factor = targetRms / maxRms;
   amplifySignal(signals, factor); }

private static double findMaxRmsLevel (float[][] signals, int segmentSize) {
   double maxRms = 0;
   for (int channel = 0; channel < signals.length; channel++) {
      double rms = findMaxRmsLevel(signals[channel], segmentSize);
      if (rms > maxRms) {
         maxRms = rms; }}
   return maxRms; }

private static double findMaxRmsLevel (float[] signal, int segmentSize) {
   double maxRms = 0;
   int p = 0;
   while (p < signal.length) {
      int endP = (p + segmentSize * 5 / 3 > signal.length) ? signal.length : p + segmentSize;
         // If the last segment is less than 2/3 of the segment size, we include it in the previous segment.
      double rms = computeRms(signal, p, endP - p);
      if (rms > maxRms) {
         maxRms = rms; }
      p = endP; }
   return maxRms; }

private static double computeRms (float[] signal, int startPos, int len) {
   double a = 0;
   for (int p = startPos; p < startPos + len; p++) {
      a += signal[p] * signal[p]; }
   return Math.sqrt(a / len); }

private static void amplifySignal (float[][] signals, double factor) {
   for (int channel = 0; channel < signals.length; channel++) {
      amplifySignal(signals[channel], factor); }}
private static void amplifySignal (float[] signal, double factor) {
   for (int p = 0; p < signal.length; p++) {
      signal[p] *= factor; }}

}
