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

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
* Audio i/o utilities.
*/
public class AudioIo {

//------------------------------------------------------------------------------

/**
* A class for storing an audio signal in memory.
*/
public static class AudioSignal {

   /**
   * The sampling rate in Hz
   */
   public int                samplingRate;

   /**
   * The audio signal sample values, per channel separately.
   * The normal value range is -1 .. 1.
   */
   public float[][]          data;

   /**
   * Returns the signal length in samples.
   */
   public int getLength() {
      return data[0].length; }

   /**
   * Returns the number of channels.
   */
   public int getChannels() {
      return data.length; }}

//------------------------------------------------------------------------------

// Dummy constructor to suppress Javadoc.
private AudioIo() {}

/**
* Writes an audio signal into a WAV file.
*
* @param fileName
*    The name of the WAV file.
* @param signal
*    The audio signal to be written into the WAV file.
* @param pos
*    Position of the first sample (frame) to be written.
* @param len
*    Number of samples (frames) to be written.
*/
public static void saveWavFile (String fileName, AudioSignal signal, int pos, int len) throws Exception {
   AudioFormat format = new AudioFormat(signal.samplingRate, 16, signal.getChannels(), true, false);
   AudioBytesPackerStream audioBytesPackerStream = new AudioBytesPackerStream(format, signal.data, pos, len);
   AudioInputStream audioInputStream = new AudioInputStream(audioBytesPackerStream, format, len);
   AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(fileName)); }

/**
* Writes an audio signal into a WAV file.
*/
public static void saveWavFile (String fileName, AudioSignal signal) throws Exception {
   saveWavFile(fileName, signal, 0, signal.getLength()); }

/**
* Writes an audio signal into a WAV file.
*/
public static void saveWavFile (String fileName, float[] buf, int samplingRate) throws Exception {
   AudioSignal signal = new AudioSignal();
   signal.samplingRate = samplingRate;
   signal.data = new float[][]{buf};
   saveWavFile(fileName, signal); }

private static class AudioBytesPackerStream extends InputStream {
   AudioFormat     format;
   float[][]       inBufs;
   int             inOffs;
   int             inLen;
   int             pos;
   public AudioBytesPackerStream (AudioFormat format, float[][] inBufs, int inOffs, int inLen) {
      this.format = format;
      this.inBufs = inBufs;
      this.inOffs = inOffs;
      this.inLen  = inLen; }
   @Override
   public int read() throws IOException {
      throw new AssertionError("Not implemented."); }
   @Override
   public int read (byte[] outBuf, int outOffs, int outLen) throws IOException {
      int remFrames = inLen - pos;
      if (remFrames <= 0) {
         return -1; }
      int reqFrames = outLen / format.getFrameSize();
      int trFrames = Math.min(remFrames, reqFrames);
      packAudioStreamBytes(format, inBufs, inOffs + pos, outBuf, outOffs, trFrames);
      pos += trFrames;
      return trFrames * format.getFrameSize(); }}

/**
* Loads an audio signal from a WAV file.
*/
public static AudioSignal loadWavFile (String fileName) throws Exception {
   AudioSignal signal = new AudioSignal();
   AudioInputStream stream = AudioSystem.getAudioInputStream(new File(fileName));
   AudioFormat format = stream.getFormat();
   signal.samplingRate = Math.round(format.getSampleRate());
   int frameSize = format.getFrameSize();
   int channels = format.getChannels();
   long totalFramesLong = stream.getFrameLength();
   if (totalFramesLong > Integer.MAX_VALUE) {
      throw new Exception("Sound file too long."); }
   int totalFrames = (int)totalFramesLong;
   signal.data = new float[channels][];
   for (int channel = 0; channel < channels; channel++) {
      signal.data[channel] = new float[totalFrames]; }
   final int blockFrames = 0x4000;
   byte[] blockBuf = new byte[frameSize * blockFrames];
   int pos = 0;
   while (pos < totalFrames) {
      int reqFrames = Math.min(totalFrames - pos, blockFrames);
      int trBytes = stream.read(blockBuf, 0, reqFrames * frameSize);
      if (trBytes <= 0) {
         throw new AssertionError("Unexpected EOF. totalFrames=" + totalFrames + " pos=" + pos); }
      if (trBytes % frameSize != 0) {
         throw new AssertionError("reqFrames=" + reqFrames + " trBytes=" + trBytes + " frameSize=" + frameSize); }
      int trFrames = trBytes / frameSize;
      unpackAudioStreamBytes(format, blockBuf, 0, signal.data, pos, trFrames);
      pos += trFrames; }
   return signal; }

/**
* Plays an audio signal on the default system audio output device.
*/
public static void play (AudioSignal signal) throws Exception {
   int channels = signal.getChannels();
   AudioFormat format = new AudioFormat(signal.samplingRate, 16, channels, true, false);
   int frameSize = format.getFrameSize();
   SourceDataLine line = AudioSystem.getSourceDataLine(format);
   line.open(format, signal.samplingRate * frameSize);     // 1 second buffer
   line.start();
   final int blockFrames = 0x4000;
   byte[] blockBuf = new byte[frameSize * blockFrames];
   int pos = 0;
   while (pos < signal.getLength()) {
      int frames = Math.min(signal.getLength() - pos, blockFrames);
      packAudioStreamBytes(format, signal.data, pos, blockBuf, 0, frames);
      int bytes = frames * frameSize;
      int trBytes = line.write(blockBuf, 0, bytes);
      if (trBytes != bytes) {
         throw new AssertionError(); }
      pos += frames; }
   line.drain();
   line.stop();
   line.close(); }

/**
* Plays an audio signal on the default system audio output device.
*/
public static void play (float[] buf, int samplingRate) throws Exception {
   AudioSignal signal = new AudioSignal();
   signal.data = new float[][]{buf};
   signal.samplingRate = samplingRate;
   play(signal); }

/**
* A utility routine to unpack the data of a Java Sound audio stream.
*/
public static void unpackAudioStreamBytes (AudioFormat format, byte[] inBuf, int inPos, float[][] outBufs, int outPos, int frames) {
   Encoding encoding = format.getEncoding();
   if (encoding == Encoding.PCM_SIGNED) {
      unpackAudioStreamBytesPcmSigned(format, inBuf, inPos, outBufs, outPos, frames); }
    else if (encoding == Encoding.PCM_FLOAT) {
      unpackAudioStreamBytesPcmFloat(format, inBuf, inPos, outBufs, outPos, frames); }
    else {
      throw new UnsupportedOperationException("Audio stream format not supported (not signed PCM or Float)."); }}

private static void unpackAudioStreamBytesPcmSigned (AudioFormat format, byte[] inBuf, int inPos, float[][] outBufs, int outPos, int frames) {
   int channels = format.getChannels();
   boolean bigEndian = format.isBigEndian();
   int sampleBits = format.getSampleSizeInBits();
   int frameSize = format.getFrameSize();
   if (outBufs.length != channels) {
      throw new IllegalArgumentException("Number of channels not equal to number of buffers."); }
   if (sampleBits != 16 && sampleBits != 24 && sampleBits != 32) {
      throw new UnsupportedOperationException("Audio stream format not supported (" + sampleBits + " bits per sample for signed PCM)."); }
   int sampleSize = (sampleBits + 7) / 8;
   if (sampleSize * channels != frameSize) {
      throw new AssertionError(); }
   float maxValue = (float)((1 << (sampleBits - 1)) - 1);
   for (int channel = 0; channel < channels; channel++) {
      float[] outBuf = outBufs[channel];
      int p0 = inPos + channel * sampleSize;
      for (int i = 0; i < frames; i++) {
         int v = unpackSignedInt(inBuf, p0 + i * frameSize, sampleBits, bigEndian);
         outBuf[outPos + i] = v / maxValue; }}}

private static void unpackAudioStreamBytesPcmFloat (AudioFormat format, byte[] inBuf, int inPos, float[][] outBufs, int outPos, int frames) {
   int channels = format.getChannels();
   boolean bigEndian = format.isBigEndian();
   int sampleBits = format.getSampleSizeInBits();
   int frameSize = format.getFrameSize();
   if (outBufs.length != channels) {
      throw new IllegalArgumentException("Number of channels not equal to number of buffers."); }
   if (sampleBits != 32) {
      throw new UnsupportedOperationException("Audio stream format not supported (" + sampleBits + " bits per sample for floating-point PCM)."); }
   int sampleSize = (sampleBits + 7) / 8;
   if (sampleSize * channels != frameSize) {
      throw new AssertionError(); }
   for (int channel = 0; channel < channels; channel++) {
      float[] outBuf = outBufs[channel];
      int p0 = inPos + channel * sampleSize;
      for (int i = 0; i < frames; i++) {
         outBuf[outPos + i] = unpackFloat(inBuf, p0 + i * frameSize, bigEndian); }}}

/**
* A utility routine to pack the data for a Java Sound audio stream.
*/
public static void packAudioStreamBytes (AudioFormat format, float[][] inBufs, int inPos, byte[] outBuf, int outPos, int frames) {
   Encoding encoding = format.getEncoding();
   if (encoding == Encoding.PCM_SIGNED) {
      packAudioStreamBytesPcmSigned(format, inBufs, inPos, outBuf, outPos, frames); }
    else if (encoding == Encoding.PCM_FLOAT) {
      packAudioStreamBytesPcmFloat(format, inBufs, inPos, outBuf, outPos, frames); }
    else {
      throw new UnsupportedOperationException("Audio stream format not supported (not signed PCM or Float)."); }}

private static void packAudioStreamBytesPcmSigned (AudioFormat format, float[][] inBufs, int inPos, byte[] outBuf, int outPos, int frames) {
   int channels = format.getChannels();
   boolean bigEndian = format.isBigEndian();
   int sampleBits = format.getSampleSizeInBits();
   int frameSize = format.getFrameSize();
   if (inBufs.length != channels) {
      throw new IllegalArgumentException("Number of channels not equal to number of buffers."); }
   if (sampleBits != 16 && sampleBits != 24 && sampleBits != 32) {
      throw new UnsupportedOperationException("Audio stream format not supported (" + sampleBits + " bits per sample for signed PCM)."); }
   int sampleSize = (sampleBits + 7) / 8;
   if (sampleSize * channels != frameSize) {
      throw new AssertionError(); }
   int maxValue = (1 << (sampleBits - 1)) - 1;
   for (int channel = 0; channel < channels; channel++) {
      float[] inBuf = inBufs[channel];
      int p0 = outPos + channel * sampleSize;
      for (int i = 0; i < frames; i++) {
         float clipped = Math.max(-1, Math.min(1, inBuf[inPos + i]));
         int v = Math.round(clipped * maxValue);
         packSignedInt(v, outBuf, p0 + i * frameSize, sampleBits, bigEndian); }}}

private static void packAudioStreamBytesPcmFloat (AudioFormat format, float[][] inBufs, int inPos, byte[] outBuf, int outPos, int frames) {
   int channels = format.getChannels();
   boolean bigEndian = format.isBigEndian();
   int sampleBits = format.getSampleSizeInBits();
   int frameSize = format.getFrameSize();
   if (inBufs.length != channels) {
      throw new IllegalArgumentException("Number of channels not equal to number of buffers."); }
   if (sampleBits != 32) {
      throw new UnsupportedOperationException("Audio stream format not supported (" + sampleBits + " bits per sample for floating-point PCM)."); }
   int sampleSize = (sampleBits + 7) / 8;
   if (sampleSize * channels != frameSize) {
      throw new AssertionError(); }
   for (int channel = 0; channel < channels; channel++) {
      float[] inBuf = inBufs[channel];
      int p0 = outPos + channel * sampleSize;
      for (int i = 0; i < frames; i++) {
         float clipped = Math.max(-1, Math.min(1, inBuf[inPos + i]));
         packFloat(clipped, outBuf, p0 + i * frameSize, bigEndian); }}}

private static int unpackSignedInt (byte[] buf, int pos, int bits, boolean bigEndian) {
   switch (bits) {
      case 16:
         if (bigEndian) {
            return (buf[pos] << 8) | (buf[pos + 1] & 0xFF); }
          else {
            return (buf[pos + 1] << 8) | (buf[pos] & 0xFF); }
      case 24:
         if (bigEndian) {
            return (buf[pos] << 16) | ((buf[pos + 1] & 0xFF) << 8) | (buf[pos + 2] & 0xFF); }
          else {
            return (buf[pos + 2] << 16) | ((buf[pos + 1] & 0xFF) << 8) | (buf[pos] & 0xFF); }
      case 32:
         return unpackInt(buf, pos, bigEndian);
      default:
         throw new AssertionError(); }}

private static void packSignedInt (int i, byte[] buf, int pos, int bits, boolean bigEndian) {
   switch (bits) {
      case 16:
         if (bigEndian) {
            buf[pos]     = (byte)((i >>> 8) & 0xFF);
            buf[pos + 1] = (byte)(i & 0xFF); }
          else {
            buf[pos]     = (byte)(i & 0xFF);
            buf[pos + 1] = (byte)((i >>> 8) & 0xFF); }
         break;
      case 24:
         if (bigEndian) {
            buf[pos]     = (byte)((i >>> 16) & 0xFF);
            buf[pos + 1] = (byte)((i >>>  8) & 0xFF);
            buf[pos + 2] = (byte)(i & 0xFF); }
          else {
            buf[pos]     = (byte)(i & 0xFF);
            buf[pos + 1] = (byte)((i >>>  8) & 0xFF);
            buf[pos + 2] = (byte)((i >>> 16) & 0xFF); }
         break;
      case 32:
         packInt(i, buf, pos, bigEndian);
         break;
      default:
         throw new AssertionError(); }}

private static int unpackInt (byte[] buf, int pos, boolean bigEndian) {
   if (bigEndian) {
      return (buf[pos] << 24) | ((buf[pos + 1] & 0xFF) << 16) | ((buf[pos + 2] & 0xFF) << 8) | (buf[pos + 3] & 0xFF); }
    else {
      return (buf[pos + 3] << 24) | ((buf[pos + 2] & 0xFF) << 16) | ((buf[pos + 1] & 0xFF) << 8) | (buf[pos] & 0xFF); }}

private static void packInt (int i, byte[] buf, int pos, boolean bigEndian) {
   if (bigEndian) {
      buf[pos]     = (byte)((i >>> 24) & 0xFF);
      buf[pos + 1] = (byte)((i >>> 16) & 0xFF);
      buf[pos + 2] = (byte)((i >>>  8) & 0xFF);
      buf[pos + 3] = (byte)(i & 0xFF); }
    else {
      buf[pos]     = (byte)(i & 0xFF);
      buf[pos + 1] = (byte)((i >>>  8) & 0xFF);
      buf[pos + 2] = (byte)((i >>> 16) & 0xFF);
      buf[pos + 3] = (byte)((i >>> 24) & 0xFF); }}

private static float unpackFloat (byte[] buf, int pos, boolean bigEndian) {
   int i = unpackInt(buf, pos, bigEndian);
   return Float.intBitsToFloat(i); }

private static void packFloat (float f, byte[] buf, int pos, boolean bigEndian) {
   int i = Float.floatToIntBits(f);
   packInt(i, buf, pos, bigEndian); }

}
