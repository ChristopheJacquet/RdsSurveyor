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

import biz.source_code.dsp.util.IntArray;

/**
* An activity detector that can be used to subdivide an audio signal into zones with sound and zones with silence.
*
* <p>
* The following model is used to find active and silent zones:
* The input signal is divided into segments according to whether the signal envelope level is above or
* below the threshold level.
* These segments are classified into the following types:
* <ul>
*  <li>active: The envelope level is above the threshold and the zone is equal or longer than the minimum for activity.
*  <li>silence: The envelope level is below the threshold and the zone is equal or longer than the minimum for silence.
*  <li>undef: The zone is too small. The envelope level of the zone may be above or below the threshold (but not mixed).
* </ul>
* <p>
* "undef" segments are then eliminated using the following rules:
* <ul>
*  <li>Adjacent "undef" segments are combined.
*  <li>An "undef" segment that is adjacent to an "active" segment is converted to "active".
*  <li>An "undef" segment that lies between two "silence" segments is converted to "silence".
*  <li>The start and the end of the signal are regarded as "silence".
* </ul>
* <p>
* The resulting "active" segments are combined into the active zones.
*/
public class ActivityDetector {

private float                thresholdLevel;
private int                  minActivityLen;
private int                  minSilenceLen;

private float[]              signalEnvelope;
private int                  pos;                          // current position in signal
private IntArray             activeZones;

/**
* Creates an activity detector.
*
* @param thresholdLevel
*    The threshold signal envelope level to distinguish activity from silence.
* @param minActivityLen
*    The minimum number of samples for an "active" segment.
* @param minSilenceLen
*    The minimum number of samples for a "silence" segment.
*/
public ActivityDetector (float thresholdLevel, int minActivityLen, int minSilenceLen) {
   this.thresholdLevel = thresholdLevel;
   this.minActivityLen = minActivityLen;
   this.minSilenceLen = minSilenceLen; }

/**
* Processes the signal envelope and returns the positions of the active zones.
*
* @param signalEnvelope
*    The envelope of the input signal.
* @return
*    An array with the start and end positions of the active zones.
*    The size of the array is twice the number of active zones.
*    The sequence is: active1Start, active1End, active2Start, active2End, ...
*/
public int[] process (float[] signalEnvelope) {
   this.signalEnvelope = signalEnvelope;
   pos = 0;
   activeZones = new IntArray(32);
   int activeStartPos = -1;                                // start position of active zone or -1
   int undefStartPos = -1;                                 // start position of undefined zone or -1
   while (pos < signalEnvelope.length) {
      int segmentStartPos = pos;                           // start position of current segment
      SegmentType segmentType = scanSegment();
      switch (segmentType) {
         case silence: {
            if (activeStartPos != -1) {
               addActiveZone(activeStartPos, segmentStartPos);
               activeStartPos = -1; }
            undefStartPos = -1;
            break; }
         case active: {
            if (activeStartPos == -1) {
               activeStartPos = (undefStartPos != -1) ? undefStartPos : segmentStartPos; }
            break; }
         case undef: {
            if (undefStartPos == -1) {
               undefStartPos = segmentStartPos; }
            break; }
         default:
            throw new AssertionError(); }}
   if (activeStartPos != -1) {
      addActiveZone(activeStartPos, pos); }
   return activeZones.toArray(); }

private enum SegmentType { active, silence, undef }

private SegmentType scanSegment() {
   int startPos = pos;
   if (pos >= signalEnvelope.length) {
      throw new AssertionError(); }
   boolean active = signalEnvelope[pos++] >= thresholdLevel;
   while (pos < signalEnvelope.length && (signalEnvelope[pos] >= thresholdLevel) == active) {
      pos++; }
   int minLen = active ? minActivityLen : minSilenceLen;
   if (pos - startPos < minLen) {
      return SegmentType.undef; }
   return active ? SegmentType.active : SegmentType.silence; }

private void addActiveZone (int startPos, int endPos) {
   activeZones.add(startPos);
   activeZones.add(endPos); }

}
