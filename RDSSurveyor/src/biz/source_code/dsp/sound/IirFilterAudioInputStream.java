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

package biz.source_code.dsp.sound;

import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterCoefficients;
import java.io.InputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
* Wrapper for using the IIR filter class with the Java Sound API.
*
* <p>
* This class provides an {@link javax.sound.sampled.AudioInputStream} for
* filtering a sound stream.
*/
public class IirFilterAudioInputStream {

// Dummy constructor to suppress Javadoc.
private IirFilterAudioInputStream() {}

/**
* Returns an AudioInputStream that supplies the filtered audio signal.
*
* @param in
*    the input AudioInputStream.
* @param coeffs
*    The IIR filter coefficients.
*/
public static AudioInputStream getAudioInputStream (AudioInputStream in, IirFilterCoefficients coeffs) {
   FilterStream filterStream = new FilterStream(in, coeffs);
   return new AudioInputStream(filterStream, in.getFormat(), in.getFrameLength()); }

//------------------------------------------------------------------------------

private static class FilterStream extends InputStream {

private static final int     inBufFrames = 4096;

private AudioInputStream     in;
private AudioFormat          format;
private int                  channels;
private int                  frameSize;
private IirFilter[]          iirFilters;
private byte[]               inBuf;
private float[][]            floatBufs;

public FilterStream (AudioInputStream in, IirFilterCoefficients coeffs) {
   this.in = in;
   format = in.getFormat();
   channels = format.getChannels();
   frameSize = format.getFrameSize();
   inBuf = new byte[inBufFrames * frameSize];
   floatBufs = new float[channels][];
   iirFilters = new IirFilter[channels];
   for (int channel = 0; channel < channels; channel++) {
      floatBufs[channel] = new float[inBufFrames];
      iirFilters[channel] = new IirFilter(coeffs); }}

@Override
public int read (byte[] outBuf, int outOffs, int len1) throws IOException {
   int len2 = Math.min(len1, inBuf.length);
   int len3 = (len2 / frameSize) * frameSize;
   int len = in.read(inBuf, 0, len3);
   if (len <= 0) {
      return len; }
   if (len % frameSize != 0) {
      throw new AssertionError(); }
   int frames = len / frameSize;
   AudioIo.unpackAudioStreamBytes(format, inBuf, 0, floatBufs, 0, frames);
   for (int channel = 0; channel < channels; channel++) {
      IirFilter iirFilter = iirFilters[channel];
      float[] floatBuf = floatBufs[channel];
      for (int i = 0; i < frames; i++) {
         floatBuf[i] = (float)iirFilter.step(floatBuf[i]); }}
   AudioIo.packAudioStreamBytes(format, floatBufs, 0, outBuf, outOffs, frames);
   return len; }

@Override
public int read() throws IOException {
   throw new AssertionError(); }

}}
