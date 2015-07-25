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

import biz.source_code.dsp.math.Complex;
import biz.source_code.dsp.math.PolynomialUtils;
import biz.source_code.dsp.math.PolynomialUtils.RationalFraction;

/**
* A Swing component for plotting the transfer curve of a signal filter.
*
* <p>
* The current implementation can display linear gain or phase.
* Logarithmic gain is not yet implemented.
*/
public class TransferFunctionPlot extends FunctionPlot {

private static final long    serialVersionUID = 1;

private static final double  borderFactor = 0.05;

/**
/* Constructs a plot component.
*
* @param tf
*    Coefficients of the z-plane transfer function.
* @param gainOrPhase
*    true = plot gain, false = plot phase
*/
public TransferFunctionPlot (RationalFraction tf, boolean gainOrPhase) {
   super(new TransferFunctionPlotFunction(tf, gainOrPhase), 0, 0.5,
      gainOrPhase ? -borderFactor     : -Math.PI * (1+borderFactor*2),
      gainOrPhase ?  1 + borderFactor :  Math.PI * (1+borderFactor*2)); }

//--- Plot function ------------------------------------------------------------

private static class TransferFunctionPlotFunction extends SimplePlotFunction {

private RationalFraction     tf;
private boolean              gainOrPhase;

public TransferFunctionPlotFunction (RationalFraction tf, boolean gainOrPhase) {
   super(10);
   this.tf = tf;
   this.gainOrPhase = gainOrPhase; }

@Override
public double getY (double x) {
   if (x < 0 || x > 0.5) {
      return Double.NaN; }
   Complex w = Complex.expj(2 * Math.PI * x);
   Complex t = PolynomialUtils.evaluate(tf, w);
   return gainOrPhase ? t.abs() : t.arg(); }

} // end class TransferFunctionPlotFunction
} // end class TransferFunctionPlot
