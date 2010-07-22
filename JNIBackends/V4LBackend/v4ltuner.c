/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/

 This file is based on code originally written by Danilo F. S. Santos,
 and released under the LGPL.

 Copyright 2010 Christophe Jacquet
 Copyright 2010 Dominique Matz
 Copyright 2009 Danilo F. S. Santos <danilo.santos@signove.com>

 This file is part of RDS Surveyor.

 RDS Surveyor is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 RDS Surveyor is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser Public License for more details.

 You should have received a copy of the GNU Lesser Public License
 along with RDS Surveyor.  If not, see <http://www.gnu.org/licenses/>.

*/

#include <string.h>
#include <math.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <assert.h>
#include <unistd.h>

#include <linux/videodev.h>

#include "v4ltuner.h"

struct v4l2_capability v_capability;
struct v4l2_tuner v_tuner;
struct v4l2_control v_control;
struct v4l2_frequency v_frequency;
struct v4l2_hw_freq_seek v_seek;

#define BUF_LEN 3

int radio_fd;
int silent;
char data_buf[BUF_LEN];

/*******************************************************************************/
#define 	_(String) 	gettext(String)
#define	FREQUENCY_MIN	87500
#define	FREQUENCY_MAX	108000
#define	FREQUENCY_STEP	5
#define 	FORMAT		"%06.2f"
#define 	FREQ_FRAC	16     /* For Nokia N800 only */
#define 	STATION_NAME_MAX_LENGTH	15
#define 	STATION_FREQ_MAX_LENGTH	6
#define 	SAMPLEDELAY     	15000
#define 	THRESHOLD 	65535.0   /* maximum acceptable signal %    */
#define 	ACCEPTLEVEL 	0.5
/*******************************************************************************/

int radio_open(char *device)
{
	// Open the Radio device.
	if ((radio_fd = open(device, O_RDONLY))< 0)
	{
		goto error;
	}

	// Query Radio device capabilities.
	if (ioctl(radio_fd, VIDIOC_QUERYCAP, &v_capability)<0)
	{
		goto error;
	}

	// Set tuner index. Must be 0.
	v_tuner.index = 0;
	ioctl(radio_fd, VIDIOC_S_TUNER, &v_tuner);

	if (!(v_capability.capabilities & V4L2_CAP_TUNER))
	{
		goto error;
	}

	printf("V4L device, driver='%s', card='%s', seek=%d\n",
			v_capability.driver,
			v_capability.card,
			v_capability.capabilities & V4L2_CAP_HW_FREQ_SEEK);

	return 1;

	error:
	if (radio_fd >= 0)
	{
		close(radio_fd);
		radio_fd = -1;
	}
	return 0;
}


void radio_mute()
{
	int res;
	v_control.id = V4L2_CID_AUDIO_MUTE;
	v_control.value = 1;
	res = ioctl(radio_fd, VIDIOC_S_CTRL, &v_control);
	if( res > 0 )
	{
		silent = 1;
	}
}

void radio_unmute()
{
	int res;
	v_control.id = V4L2_CID_AUDIO_MUTE;
	v_control.value = 0;
	res = ioctl(radio_fd, VIDIOC_S_CTRL, &v_control);
	if( res > 0 )
	{
		silent = 0;
	}
}


void radio_close()
{

	radio_mute();
	if (radio_fd >= 0)
	{
		close(radio_fd);
		radio_fd = -1;
	}
}

int radio_set_frequency(unsigned int frequency)
{
	int res;

	if(FREQUENCY_MIN > frequency)
		return -2;
	if(FREQUENCY_MAX < frequency)
		return -3;
	if(frequency % FREQUENCY_STEP != 0)
		return -4;

	if (radio_fd < 0)
		return -1;

	if (radio_fd < 0)
		return -1;
	v_frequency.tuner = 0;
	v_frequency.frequency = (frequency*FREQ_FRAC);

	res = ioctl(radio_fd, VIDIOC_S_FREQUENCY, &v_frequency);

	return res;
}

unsigned int radio_get_frequency()
{
	int res;
	unsigned long freq;

	if (radio_fd < 0)
		return -1;

	res = ioctl(radio_fd, VIDIOC_G_FREQUENCY, &v_frequency);

	if(res < 0)
		return -1;

	freq = v_frequency.frequency;
	freq /= FREQ_FRAC;

	return freq;
}

int radio_get_signal_strength()
{
	if (ioctl(radio_fd, VIDIOC_G_TUNER, &v_tuner) < 0)
	{
		return -1;
	}
	usleep(SAMPLEDELAY);
	return v_tuner.signal;
}

int radio_get_rds_data()
{
	fd_set all_fds;
	FD_ZERO(&all_fds);
	FD_SET(radio_fd,&all_fds);
	struct timeval timeout;
	timeout.tv_sec = 2;
	timeout.tv_usec = 0;
	//printf("<S:"); fflush(stdout);
	int n = select(radio_fd+1,&all_fds,(fd_set *)0,(fd_set *)0,&timeout);
	//printf("%d> ", n);
	//fflush(stdin);
	if(n <= 0) return 0;


	int n_read = read(radio_fd, data_buf, BUF_LEN);

	return n_read;
}

int radio_has_rds()
{
	return (v_capability.capabilities & V4L2_CAP_RDS_CAPTURE) != 0;
}


int radio_is_stereo()
{
	    struct video_audio va;
	    va.mode=-1;

	    if (radio_fd < 0)
	    	return -1;

	    if (ioctl (radio_fd, VIDIOCGAUDIO, &va) < 0)
			return -1;

	    if (va.mode == VIDEO_SOUND_STEREO)
			return 1;
		else
			return 0;
}


JNIEXPORT jboolean JNICALL Java_eu_jacquet80_rds_input_V4LTunerGroupReader_isStereo
  (JNIEnv *env, jobject obj) {
	return radio_is_stereo();
}


JNIEXPORT jint JNICALL Java_eu_jacquet80_rds_input_V4LTunerGroupReader_setFrequency
  (JNIEnv *env, jobject obj, jint frequency) {
	return radio_set_frequency(frequency);
}


JNIEXPORT jint JNICALL Java_eu_jacquet80_rds_input_V4LTunerGroupReader_getFrequency
  (JNIEnv *env, jobject obj) {
	return radio_get_frequency();
}


JNIEXPORT jint JNICALL Java_eu_jacquet80_rds_input_V4LTunerGroupReader_mute
  (JNIEnv *env, jobject obj) {
	radio_mute();
	return 0;
}


JNIEXPORT jint JNICALL Java_eu_jacquet80_rds_input_V4LTunerGroupReader_unmute
  (JNIEnv *env, jobject obj) {
	radio_unmute();
	return 0;
}


JNIEXPORT jint JNICALL Java_eu_jacquet80_rds_input_V4LTunerGroupReader_getSignalStrength
  (JNIEnv *env, jobject obj) {
	return radio_get_signal_strength();
}


JNIEXPORT jint JNICALL Java_eu_jacquet80_rds_input_V4LTunerGroupReader_open
  (JNIEnv *env, jobject obj, jstring device_name) {
	char *dev = (char *) (*env)->GetStringUTFChars(env, device_name, 0);
	printf("Opening %s...\n", dev);
	fflush(stdin);
	int res = radio_open(dev);
	(*env)->ReleaseStringUTFChars(env, device_name, dev);
	return res;
}


JNIEXPORT jint JNICALL Java_eu_jacquet80_rds_input_V4LTunerGroupReader_close
  (JNIEnv *env, jobject obj) {
	radio_close();
	return 0;
}


JNIEXPORT jboolean JNICALL Java_eu_jacquet80_rds_input_V4LTunerGroupReader_hasRDS
  (JNIEnv *env, jobject obj) {
	return radio_has_rds();
}


JNIEXPORT jbyteArray JNICALL Java_eu_jacquet80_rds_input_V4LTunerGroupReader_getRDSData
(JNIEnv *env, jobject obj) {
	int num_read = radio_get_rds_data();
	jbyteArray jb = (*env)->NewByteArray(env, num_read);
	(*env)->SetByteArrayRegion(env,jb,0,num_read,(jbyte*)data_buf);
	return jb;
}

JNIEXPORT void JNICALL Java_eu_jacquet80_rds_input_V4LTunerGroupReader_hwSeek
  (JNIEnv *env, jobject obj, jboolean up) {

	v_seek.tuner = 0;
	v_seek.type = V4L2_TUNER_RADIO;
	v_seek.seek_upward = up;
	v_seek.wrap_around = 1;

	printf("up=%d,  wrap=%d\n", v_seek.seek_upward, v_seek.wrap_around);
	fflush(stdout);

	ioctl(radio_fd, VIDIOC_S_HW_FREQ_SEEK, &v_seek);

}
