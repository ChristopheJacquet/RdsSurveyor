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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JComponent;

/**
* A function plot Swing component.
* Allows zooming and moving the plot window with the mouse.
* Shift + mouse click or shift + mouse wheel zooms vertically.
*/
public class FunctionPlot extends JComponent {

private static final long    serialVersionUID = 1;

private static final int     curveLineWidth = 1;
private static final int     gridLineWidth = 1;
private static final double  magnificationPerStep = 1.5;
private static final int     signalBorderWidth = 1;
private static final Color   signalColor = new Color(0x6060C0);
// private static final Color   signalFillColor = new Color(0x404080);

private PlotFunction         plotFunction;
private double               xMin;
private double               xMax;
private double               yMin;
private double               yMax;
private boolean              zoomModeHorizontal;

/**
* Constructs a function plot component.
*
* @param plotFunction
*    Delivers the values fot the function to be plotted.
* @param xMin
*    The initial minimum x value of the view.
* @param xMax
*    The initial maximum x value of the view.
* @param yMin
*    The initial minimum y value of the view.
* @param yMax
*    The initial maximum y value of the view.
*/
public FunctionPlot (PlotFunction plotFunction, double xMin, double xMax, double yMin, double yMax) {
   this.plotFunction = plotFunction;
   this.xMin = xMin;
   this.xMax = xMax;
   this.yMin = yMin;
   this.yMax = yMax;
   setOpaque(true);
   setBackground(Color.WHITE);
   enableEvents(AWTEvent.MOUSE_EVENT_MASK);
   enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
   enableEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK); }

/**
* If the horizontal zoom mode flag is on, the primary zoom mode is horizontal.
* Otherwise the primary zoom mode is horizontal and vertical.
*/
public void setZoomModeHorizontal (boolean on) {
   zoomModeHorizontal = on; }

//--- Painting -----------------------------------------------------------------

protected void paintComponent (Graphics g) {
   Graphics g2 = g.create();
   try {
      paintComponent2(g2); }
    finally {
      g2.dispose(); }}

private void paintComponent2 (Graphics g) {
   Rectangle clip = g.getClipBounds();
   g.setColor(getBackground());
   g.fillRect(clip.x, clip.y, clip.width, clip.height);
   drawGrid(g);
   drawCurve(g, clip); }

private void drawCurve (Graphics g, Rectangle clip) {
   for (int gx = clip.x; gx < clip.x + clip.width; gx++) {
      int[] gy = getGraphicsY(gx);
      if (gy != null) {
         drawY(g, gx, gy[0], gy[1]); }}}

private void drawY (Graphics g, int gx, int gy1, int gy2) {
   int h = gy2 - gy1;
   g.setColor(signalColor);
   g.fillRect(gx, gy1, 1, h); }

// Returns an array with the top and bottom graphics y coordinate values for the pixel column at graphics position gx.
private int[] getGraphicsY (int gx) {
   double fx1 = mapGraphicsXToFunctionX(gx);
   double fx2 = mapGraphicsXToFunctionX(gx + 1);
   double[] minMaxY = plotFunction.getMinMaxY(fx1, fx2);
   if (minMaxY == null) {
      return null; }
   double topGY = mapFunctionYToGraphicsY(minMaxY[1]);     // top graphics y is upper function y
   double botGY = mapFunctionYToGraphicsY(minMaxY[0]);     // bottom graphics y is lower function y
   int topGYInt = roundGraphicsCoordinate(topGY - curveLineWidth / 2.0);
   int botGYInt = roundGraphicsCoordinate(botGY + curveLineWidth / 2.0);
// if (topGYInt >= botGYInt) {                             // should not occur if curveLineWidth >= 1
//    botGYInt = topGYInt + 1; }
   return new int[]{topGYInt, botGYInt}; }

private void drawGrid (Graphics g) {
   GridLine[] hGrid = plotFunction.getHorizontalGridLines(yMin, yMax);
   GridLine[] vGrid = plotFunction.getVerticalGridLines(xMin, xMax);
   if (hGrid != null) {
      for (GridLine gridLine : hGrid) {
         drawHorizontalGridLine(g, gridLine); }}
   if (vGrid != null) {
      for (GridLine gridLine : vGrid) {
         drawVerticalGridLine(g, gridLine); }}}

private void drawHorizontalGridLine (Graphics g, GridLine gridLine) {
   double gy0 = mapFunctionYToGraphicsY(gridLine.pos);
   int gy = roundGraphicsCoordinate(gy0 - gridLineWidth / 2.0);
   g.setColor(gridLine.color);
   g.fillRect(0, gy, getWidth(), 1); }

private void drawVerticalGridLine (Graphics g, GridLine gridLine) {
   double gx0 = mapFunctionXToGraphicsX(gridLine.pos);
   int gx = roundGraphicsCoordinate(gx0 - gridLineWidth / 2.0);
   g.setColor(gridLine.color);
   g.fillRect(gx, 0, 1, getHeight()); }

//--- Coordinate mapping -------------------------------------------------------

// Note: Function y and graphics y are reversed (function y is bottom up, graphics y is top down).

private double mapGraphicsXToFunctionX (int gx) {
   return xMin + mapGraphicsXDeltaToFunctionXDelta(gx); }

private double mapGraphicsYToFunctionY (int gy) {
   return yMax + mapGraphicsYDeltaToFunctionYDelta(gy); }

private double mapGraphicsXDeltaToFunctionXDelta (int gxDelta) {
   int width = getWidth();
   if (width == 0) {
      return 0; }
   return (xMax - xMin) / width * gxDelta; }

private double mapGraphicsYDeltaToFunctionYDelta (int gyDelta) {
   int height = getHeight();
   if (height == 0) {
      return 0; }
   return - (yMax - yMin) / height * gyDelta; }

private double mapFunctionXToGraphicsX (double x) {
   return (x - xMin) / (xMax - xMin) * getWidth(); }

private double mapFunctionYToGraphicsY (double y) {
   return (1 - (y - yMin) / (yMax - yMin)) * getHeight(); }

private int roundGraphicsCoordinate (double v) {
   return (int)Math.round(Math.max(-100000, Math.min(100000, v))); }

//--- Mouse events -------------------------------------------------------------

private boolean              mouseDraggingMode;
private int                  mouseDraggingX;
private int                  mouseDraggingY;

protected void processMouseWheelEvent (MouseWheelEvent event) {
   switch (event.getID()) {
      case MouseEvent.MOUSE_WHEEL: {
         zoomPlot(event, event.getPreciseWheelRotation()); }}}

protected void processMouseEvent (MouseEvent event) {
   int button = event.getButton();
   switch (event.getID()) {
      case MouseEvent.MOUSE_CLICKED: {
         if (button == 1 || button == 3) {
            int step = (button == 1) ? -1 : 1;
            zoomPlot(event, step); }
         break; }
      case MouseEvent.MOUSE_PRESSED: {
         if (button == 1) {
            startMouseDragging(event.getX(), event.getY()); }
           else {
            stopMouseDragging(); }
         break; }
      case MouseEvent.MOUSE_RELEASED: {
         stopMouseDragging();
         break; }
      case MouseEvent.MOUSE_EXITED: {
         stopMouseDragging();
         break; }}}

protected void processMouseMotionEvent (MouseEvent event) {
   switch (event.getID()) {
      case MouseEvent.MOUSE_DRAGGED: {
         if (mouseDraggingMode) {
            movePlot(mouseDraggingX - event.getX(), mouseDraggingY - event.getY());
            startMouseDragging(event.getX(), event.getY()); }
         break; }
      default: {
         super.processMouseEvent(event); }}}

private void startMouseDragging (int gx, int gy) {
   mouseDraggingX = gx;
   mouseDraggingY = gy;
   mouseDraggingMode = true; }

private void stopMouseDragging() {
   mouseDraggingMode = false; }

//------------------------------------------------------------------------------

private void zoomPlot (MouseEvent event, double zoomStep) {
   boolean verticalOnly = event.isShiftDown();
   int gx = event.getX();
   int gy = verticalOnly ? getHeight() / 2 : event.getY();
   zoomPlot(gx, gy, zoomStep, !verticalOnly, verticalOnly || !zoomModeHorizontal); }

private void zoomPlot (int gx, int gy, double zoomStep, boolean horizontal, boolean vertical) {
   double f = Math.exp(Math.log(magnificationPerStep) * zoomStep);
   if (horizontal) {
      double x0 = mapGraphicsXToFunctionX(gx);
      xMin = x0 + (xMin - x0) * f;
      xMax = x0 + (xMax - x0) * f; }
   if (vertical) {
      double y0 = mapGraphicsYToFunctionY(gy);
      yMin = y0 + (yMin - y0) * f;
      yMax = y0 + (yMax - y0) * f; }
   repaint(); }

private void movePlot (int gxDelta, int gyDelta) {
   double xDelta = mapGraphicsXDeltaToFunctionXDelta(gxDelta);
   double yDelta = mapGraphicsYDeltaToFunctionYDelta(gyDelta);
   xMin += xDelta;
   xMax += xDelta;
   yMin += yDelta;
   yMax += yDelta;
   repaint(); }

//--- Plot function ----------------------------------------------------------------

/**
* Represents the function to be plotted.
*/
public interface PlotFunction {

   /**
   * Returns a <code>double[2]</code> array with the minimum and maximum y values within the function range x1 to x2.
   * May return null if no values are available.
   */
   double[] getMinMaxY (double x1, double x2);

   /**
   * Returns the definition of the horizontal grid lines for the current view.
   * May return null for no horizontal grid lines.
   * @param yMin
   *    Minimum y value of the current view.
   * @param yMax
   *    Maximum y value of the current view.
   */
   GridLine[] getHorizontalGridLines (double yMin, double yMax);

   /**
   * Returns the definition of the vertical grid lines for the current view.
   * May return null for no vertical grid lines.
   * @param xMin
   *    Minimum x value of the current view.
   * @param xMax
   *    Maximum x value of the current view.
   */
   GridLine[] getVerticalGridLines (double xMin, double xMax); }

/**
* Defines a grid line for the plot area.
*/
public static class GridLine {
   /** x or y position of the grid line. */
   public double             pos;
   /** Color of the grid line. */
   public Color              color;
   public GridLine (double pos, Color color) {
      this.pos = pos;
      this.color = color; }}

/**
* A convenience class for implementing the <code>PlotFunction</code> interface.
* The only method that has to be implemented is <code>getY()</code>.
*/
public static abstract class SimplePlotFunction implements PlotFunction {

   private int               interPixelSamples;

   /**
   * Constructs a plot function without inter-pixel sampling.
   */
   public SimplePlotFunction() {
      this(0); }

   /**
   * Constructs a plot function.
   *
   * @param interPixelSamples
   *    Number of inter-pixel samples to be computed on each <code>genMinMayY()</code> call.
   *    0 for no inter-pixel sampling.
   */
   public SimplePlotFunction (int interPixelSamples) {
      this.interPixelSamples = interPixelSamples; }

   /**
   * Returns an y value for an x value.
   * This method is abstract and must be overridden by the derived class.
   */
   public abstract double getY (double x);

   @Override
   public double[] getMinMaxY (double x1, double x2) {
      double y1 = getY(x1);
      double y2 = getY(x2);
      if (Double.isNaN(y1) || Double.isNaN(y2)) {
         if (Double.isNaN(y1) && Double.isNaN(y2)) {
            return null; }
         return Double.isNaN(y1) ? new double[]{y2, y2} : new double[]{y1, y1}; }
      double min = Math.min(y1, y2);
      double max = Math.max(y1, y2);
      for (int p = 0; p < interPixelSamples; p++) {
         double x = x1 + (x2 - x1) / (interPixelSamples + 1) * (p + 1);
         double y = getY(x);
         if (!Double.isNaN(y)) {
            min = Math.min(min, y);
            max = Math.max(max, y); }}
      return new double[]{min, max}; }

   @Override
   public GridLine[] getHorizontalGridLines (double yMin, double yMax) {
      return new GridLine[]{new GridLine(0, Color.LIGHT_GRAY)}; }

   @Override
   public GridLine[] getVerticalGridLines (double xMin, double xMax) {
      return new GridLine[]{new GridLine(0, Color.LIGHT_GRAY)}; }}

}
