/*
Si470x userspace driver - Plugin to RDS Surveyor
Copyright (c) 2012  Christophe Jacquet

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/


#include "eu_jacquet80_rds_input_NativeTunerGroupReader.h"
#include "si470x_hidapi.h"
#include <stdio.h>

static si470x_dev_t *dev;

JNIEXPORT jint JNICALL Java_eu_jacquet80_rds_input_NativeTunerGroupReader_setFrequency
  (JNIEnv *env, jobject self, jint freq) {
    int res = si470x_set_freq(dev, freq);
    if(res >= 0) {
        int freq;
        si470x_get_freq(dev, &freq);
        return freq;
    }
    
    return res;
}

JNIEXPORT jint JNICALL Java_eu_jacquet80_rds_input_NativeTunerGroupReader_readTuner
  (JNIEnv *env, jobject self) {
    si470x_tunerdata_t tunerdata;
    int res = si470x_read_rds(dev, &tunerdata);

    jclass clsSelf = (*env)->GetObjectClass(env, self);
    jfieldID fData = (*env)->GetFieldID(env, clsSelf, "data", "Leu/jacquet80/rds/input/TunerData;");

    jobject data = (*env)->GetObjectField(env, self, fData);

    jclass cls = (*env)->GetObjectClass(env, data);
    
    jfieldID fBlock = (*env)->GetFieldID(env, cls, "block", "[S");
    jfieldID fErr = (*env)->GetFieldID(env, cls, "err", "[S");
    jfieldID fGroupReady = (*env)->GetFieldID(env, cls, "groupReady", "Z");
    jfieldID fRdsSynchronized = (*env)->GetFieldID(env, cls, "rdsSynchronized", "Z");
    jfieldID fStereo = (*env)->GetFieldID(env, cls, "stereo", "Z");
    jfieldID fRssi = (*env)->GetFieldID(env, cls, "rssi", "I");
    jfieldID fFrequency = (*env)->GetFieldID(env, cls, "frequency", "I");

    jshortArray blockA = (*env)->GetObjectField(env, data, fBlock);
    
    (*env)->SetShortArrayRegion(env, blockA, 0, 4, tunerdata.block);

    jshortArray errA = (*env)->GetObjectField(env, data, fErr);
    (*env)->SetShortArrayRegion(env, errA, 0, 4, tunerdata.bler);

    (*env)->SetBooleanField(env, data, fGroupReady, res == 0);
    (*env)->SetBooleanField(env, data, fRdsSynchronized, tunerdata.sync);
    (*env)->SetBooleanField(env, data, fStereo, tunerdata.stereo);
    (*env)->SetIntField(env, data, fRssi, tunerdata.rssi * 873);
    (*env)->SetIntField(env, data, fFrequency, tunerdata.frequency);

    return res;
}

JNIEXPORT jboolean JNICALL Java_eu_jacquet80_rds_input_NativeTunerGroupReader_open
  (JNIEnv *env, jobject self) {
    
    if(si470x_open(&dev, 0) != 0) {
        return 0;
    }
    
    if(si470x_start(dev, CHANNEL_SPACING_100_KHZ, BAND_87_108, DE_WORLD) !=0) {
        return 0;
    }

    return 1;
}


JNIEXPORT jboolean JNICALL Java_eu_jacquet80_rds_input_NativeTunerGroupReader_seek
  (JNIEnv *env, jobject self, jboolean up) {
    return si470x_start_seek(dev, 1, up) == 0;
}


JNIEXPORT jstring JNICALL Java_eu_jacquet80_rds_input_NativeTunerGroupReader_getDeviceName
  (JNIEnv *env, jobject self) {
    return (*env)->NewStringUTF(env, "Si470x");
}