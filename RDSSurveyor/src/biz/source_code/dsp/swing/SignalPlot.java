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

package biz.source_code.dsp.swing;

import java.awt.Color;

/**
* A Swing component for plotting a sampled signal.
* Somewhat similar to an oscilloscope.
*/
public class SignalPlot extends FunctionPlot {

private static final long    serialVersionUID = 1;
private static final Color   gridLineColor = new Color(0xC0C0C0);

public SignalPlot (float[] signal, double yMin, double yMax) {
   this(signal, 0, 0, signal.length, yMin, yMax); }

public SignalPlot (float[] signal, int signalOffset, double xMin, double xMax, double yMin, double yMax) {
   super(new SignalPlotFunction(signal, signalOffset), xMin, xMax, yMin, yMax); }

//--- Plot function ------------------------------------------------------------

private static class SignalPlotFunction implements PlotFunction {

private static double        sampleOffset = 0.5;           // x value of the center of the first sample

private float[]              signal;
private int                  signalOffset;                 // offset of the x=0 sample within the signal array
private int                  signalEnd;                    // x value of last sample in signal array

public SignalPlotFunction (float[] signal, int signalOffset) {
   this.signal = signal;
   this.signalOffset = signalOffset;
   this.signalEnd = signal.length - signalOffset; }

@Override
public double[] getMinMaxY (double xa1, double xa2) {
   if (xa1 >= xa2) {
      throw new AssertionError(); }
   double xf1 = xa1 + signalOffset;
   double xf2 = xa2 + signalOffset;
   if (xf1 < 0 && xf2 < 0 || xf1 > signal.length && xf2 > signal.length || signal.length == 0) {
      return null; }
   double x1 = Math.max(0, xf1 - sampleOffset);
   double x2 = Math.max(0, Math.min(signal.length - 1, xf2 - sampleOffset));
   double d = x2 - x1;
   if (d > 5) {
      return findMinMaxNearestNeighbour(x1, x2); }
   if (d > 0.3) {
      return findMinMaxLinearInterpolated(x1, x2); }
   return findMinMaxCubicInterpolated(x1, x2); }

private double[] findMinMaxNearestNeighbour (double x1, double x2) {
   return findMinMaxSamples(nearestSamplePos(x1), nearestSamplePos(x2)); }

private double[] findMinMaxLinearInterpolated (double x1, double x2) {
   double y1 = linearInterpolation(x1);
   double y2 = linearInterpolation(x2);
   return findMinMaxSamples(x1, x2, y1, y2); }

private double[] findMinMaxCubicInterpolated (double x1, double x2) {
   double y1 = cubicHermiteInterpolation(x1);
   double y2 = cubicHermiteInterpolation(x2);
   return findMinMaxSamples(x1, x2, y1, y2); }

private int nearestSamplePos (double x) {
   return (int)Math.round(Math.max(0, Math.min(signal.length - 1, x))); }

private double linearInterpolation (double x) {
   int p = (int)Math.floor(x);
   if (p < 0) {
      return signal[0]; }
   if (p >= signal.length - 1) {
      return signal[signal.length - 1]; }
   double f = x - p;
   if (f == 0) {
      return signal[p]; }
   return signal[p] * (1 - f) + signal[p + 1] * f; }

// 4 point third order Hermite interpolation.
private double cubicHermiteInterpolation (double x) {
   int p = (int)Math.floor(x);
   if (p < 1 || p + 2 > signal.length - 1) {
      return linearInterpolation(x); }                     // fallback to linear interpolation near the edges
   double f = x - p;
   if (f == 0) {
      return signal[p]; }
   double ym1 = signal[p - 1];
   double y0  = signal[p    ];
   double y1  = signal[p + 1];
   double y2  = signal[p + 2];
   double a = (3 * (y0 - y1) - ym1 + y2) / 2;
   double b = 2 * y1 + ym1 - (5 * y0 + y2) / 2;
   double c = (y1 - ym1) / 2;
   return ((a * f + b) * f + c) * f + y0; }

private double[] findMinMaxSamples (double x1, double x2, double y1, double y2) {
   double[] a = findMinMaxSamples((int)Math.ceil(x1), (int)Math.floor(x2));
   double yMin = Math.min(y1, Math.min(y2, a[0]));
   double yMax = Math.max(y1, Math.max(y2, a[1]));
   return new double[]{yMin, yMax}; }

private double[] findMinMaxSamples (int x1, int x2) {
   double yMin = Double.MAX_VALUE;
   double yMax = -Double.MAX_VALUE;
   for (int x = x1; x <= x2; x++) {
      float y = signal[x];
      yMin = Math.min(yMin, y);
      yMax = Math.max(yMax, y); }
   return new double[]{yMin, yMax}; }

@Override
public GridLine[] getHorizontalGridLines (double yMin, double yMax) {
   if (yMin < -1 || yMax > 1) {
      return new GridLine[]{new GridLine(-1, gridLineColor), new GridLine(0, gridLineColor), new GridLine(1, gridLineColor)}; }
   return new GridLine[]{new GridLine(0, gridLineColor)}; }

@Override
public GridLine[] getVerticalGridLines (double xMin, double xMax) {
   if (xMax - xMin <= 50.499) {
      return genSampleGridLines(xMin, xMax); }
   if (xMin < 0 || xMax > signalEnd) {
      return new GridLine[]{new GridLine(0, gridLineColor), new GridLine(signalEnd, gridLineColor)}; }
   return null; }

private GridLine[] genSampleGridLines (double xMin, double xMax) {
   int x1 = (int)Math.floor(xMin);
   int x2 = (int)Math.ceil(xMax);
   GridLine[] a = new GridLine[x2 - x1 + 1];
   for (int x = x1; x <= x2; x++) {
      a[x - x1] = new GridLine(x, gridLineColor); }
   return a; }

} // end class SignalPlotFunction
} // end class SignalPlot
