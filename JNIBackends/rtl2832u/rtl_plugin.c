/*
 * Realtek RTL2832 userspace driver - Plugin to RDS Surveyor
 * Copyright (c) 2012 by Christophe Jacquet
 * Copyright (C) 2012 by Steve Markgraf <steve@steve-m.de>
 * Copyright (C) 2012 by Hoernchen <la@tfc-server.de>
 * Copyright (C) 2012 by Kyle Keen <keenerd@gmail.com>
 * Copyright (C) 2013 by Elias Oenal <EliasOenal@gmail.com>
 * Copyright (C) 2015 by Michael von Glasow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


/*
 * Most of this code is adapted from rtl_fm,
 * not all of the todo items below may apply here.
 *
 * lots of locks, but that is okay
 * (no many-to-many locks)
 *
 * todo:
 *       sanity checks
 *       scale squelch to other input parameters
 *       test all the demodulations
 *       pad output on hop
 *       frequency ranges could be stored better
 *       scaled AM demod amplification
 *       auto-hop after time limit
 *       peak detector to tune onto stronger signals
 *       fifo for active hop frequency
 *       clips
 *       noise squelch
 *       merge stereo patch
 *       merge soft agc patch
 *       merge udp patch
 *       testmode to detect overruns
 *       watchdog to reset bad dongle
 *       fix oversampling
 */

#include "eu_jacquet80_rds_input_NativeTunerGroupReader.h"
#include <errno.h>
#include <signal.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

#ifndef _WIN32
#include <unistd.h>
#else
#include <windows.h>
#include <fcntl.h>
#include <io.h>
#define usleep(x) Sleep(x/1000)
#ifdef _MSC_VER
#define round(x) (x > 0.0 ? floor(x + 0.5): ceil(x - 0.5))
#endif
#define _USE_MATH_DEFINES
#endif

#include <math.h>
#include <pthread.h>
#include <libusb.h>

#include <rtl-sdr.h>
#include "convenience/convenience.h"
#include "kissfft/kiss_fftr.h"

/* The default sample rate.
 * Samples will be collected at this rate and resampled if needed.
 * 200000 is the default for rtl_fm but rtl_redsea uses 250000. */
#define DEFAULT_SAMPLE_RATE		250000

#define DEFAULT_BUF_LENGTH		(1 * 16384)
#define MAXIMUM_OVERSAMPLE		16
#define MAXIMUM_BUF_LENGTH		(MAXIMUM_OVERSAMPLE * DEFAULT_BUF_LENGTH)
#define AUTO_GAIN			-100
#define BUFFER_DUMP			4096

#define FREQ_MIN            87.5e+6
#define FREQ_MAX            108.0e+6
#define INITIAL_FREQ		FREQ_MIN

/* Minimum RSSI to stop during seek */
#define RSSI_MIN            -7.5
#define RSSI_MIN_DX         -10
#define RSSI_INVALID        -100


static volatile int do_exit = 0;
static int lcm_post[17] = {1,1,1,3,1,5,3,7,1,9,5,11,3,13,7,15,1};
static int ACTUAL_BUF_LENGTH;

static int *atan_lut = NULL;
static int atan_lut_size = 131072; /* 512 KB */
static int atan_lut_coef = 8;

/** The resample rate used for output. */
static int sampleRateOut;

/** Commands to change frequency. */
enum tune {
	TUNE_NONE,      /**< do nothing, i.e. maintain current frequency */
	TUNE_FREQ,      /**< tune to given frequency */
	TUNE_SEEK_UP,   /**< seek up */
	TUNE_SEEK_DOWN  /**< seek down */
};

struct dongle_state
{
	int      exit_flag;
	pthread_t thread;
	rtlsdr_dev_t *dev;
	int      dev_index;
	uint32_t freq;       /**< The current frequency in Hz, corrected. */
	uint32_t rate;
	int      gain;       /**< Tuner gain, in multiples of 0.1 dB. If the tuner does not support
	                          this level, it will be changed to the nearest supported level. */
	int      *gains;     /**< Gain levels supported by the tuner. */
	uint16_t gains_len;  /**< Number of supported gain levels. */
	uint16_t buf16[MAXIMUM_BUF_LENGTH];
	uint32_t buf_len;
	uint8_t  minSample;  /**< Lowest-value sample encountered, used for gain control */
	uint8_t  maxSample;  /**< Highest-value sample encountered, used for gain control */
	int      ppm_error;
	int      offset_tuning;
	int      direct_sampling;
	int      mute;
	struct demod_state *demod_target;
};

struct demod_state
{
	int      exit_flag;
	pthread_t thread;
	int16_t  lowpassed[MAXIMUM_BUF_LENGTH];
	int      lp_len;
	int16_t  lp_i_hist[10][6];
	int16_t  lp_q_hist[10][6];
	int16_t  result[MAXIMUM_BUF_LENGTH];
	int16_t  droop_i_hist[9];
	int16_t  droop_q_hist[9];
	int      result_len;
	int      rate_in;
	int      rate_out;
	int      rate_out2;
	int      now_r, now_j;
	int      pre_r, pre_j;
	int      prev_index;
	int      downsample;    /* min 1, max 256 */
	int      post_downsample;
	int      downsample_passes;
	int      comp_fir_size;
	int      custom_atan;
	int      deemph, deemph_a;
	int      now_lpr;
	int      prev_lpr_index;
	int      dc_block, dc_avg;
	pthread_rwlock_t rw;
	pthread_cond_t ready;                /**< Signals that samples are available for demodulation */
	pthread_mutex_t ready_m;             /**< Mutex to control access to {@code ready} */
	struct output_state *output_target;
	double   rssi;  /**< signal strength in dBm */

	// to call event handler methods
	JavaVM   *jvm;
	jobject  self;
    JNIEnv   *env;
    jmethodID  onRssiChanged;
};

struct output_state
{
	int      exit_flag;
	pthread_t thread;

	// to access the output stream
	JavaVM   *jvm;
	jobject  self;
    JNIEnv   *env;
    jobject  tunerOut;
    jmethodID  write;
    jbyteArray jdata;

	int16_t  result[MAXIMUM_BUF_LENGTH];
	int      result_len;
	int      rate;
	pthread_rwlock_t rw;
	pthread_cond_t ready;
	pthread_mutex_t ready_m;
};

struct controller_state
{
	int      exit_flag;
	pthread_t thread;
	uint32_t freq;       /**< If {@code retune == TUNE_FREQ}, this member contains the next
	                          frequency to tune to. Otherwise it indicates the current frequency.
	                          Values are in Hz and unoptimized, i.e. as set by user. */
	enum tune retune;    /**< Whether and how to change the frequency. */
	int      edge;
	int      wb_mode;
	pthread_rwlock_t rw;
	pthread_cond_t hop;
	pthread_mutex_t hop_m;

	// to call event handler methods
	JavaVM   *jvm;
	jobject  self;
    JNIEnv   *env;
    jmethodID  onFrequencyChanged;
};

// multiple of these, eventually
struct dongle_state dongle;
struct demod_state demod;
struct output_state output;
struct controller_state controller;

/* more cond dumbness */
#define safe_cond_signal(n, m) pthread_mutex_lock(m); pthread_cond_signal(n); pthread_mutex_unlock(m)
#define safe_cond_wait(n, m) pthread_mutex_lock(m); pthread_cond_wait(n, m); pthread_mutex_unlock(m)

/* {length, coef, coef, coef}  and scaled by 2^15
   for now, only length 9, optimal way to get +85% bandwidth */
#define CIC_TABLE_MAX 10
int cic_9_tables[][10] = {
	{0,},
	{9, -156,  -97, 2798, -15489, 61019, -15489, 2798,  -97, -156},
	{9, -128, -568, 5593, -24125, 74126, -24125, 5593, -568, -128},
	{9, -129, -639, 6187, -26281, 77511, -26281, 6187, -639, -129},
	{9, -122, -612, 6082, -26353, 77818, -26353, 6082, -612, -122},
	{9, -120, -602, 6015, -26269, 77757, -26269, 6015, -602, -120},
	{9, -120, -582, 5951, -26128, 77542, -26128, 5951, -582, -120},
	{9, -119, -580, 5931, -26094, 77505, -26094, 5931, -580, -119},
	{9, -119, -578, 5921, -26077, 77484, -26077, 5921, -578, -119},
	{9, -119, -577, 5917, -26067, 77473, -26067, 5917, -577, -119},
	{9, -199, -362, 5303, -25505, 77489, -25505, 5303, -362, -199},
};

#ifdef _MSC_VER
double log2(double n)
{
	return log(n) / log(2.0);
}
#endif

/**
 * @brief Calculates the base 2 logarithm, rounded to an adjacent integer.
 *
 * @param n The argument of the log function
 * @param ceiling If true, round up, else round down; meaningless if {@code n} is a power of 2.
 */
int int_log2(int n, short ceiling) {
	short rounded = 0;
	int result = 0;
	while (n > 1) {
		rounded |= (n & 0x1);
		n = n >> 1;
		result++;
	}
	if (ceiling)
		result += rounded;
	return result;
}

/** FFT input buffer ({@code nfft} scalar points) */
kiss_fft_scalar* fft_in;

/** FFT output buffer ({@code nfft/2 + 1} complex points) */
kiss_fft_cpx* fft_out;

/** Spectrum of demodulated FM baseband, in multiples of .01 dB ({@code nfft/2 + 1} integer items) */
int16_t* spectrum;

/** FFT size (number of input samples), must be a power of two */
int nfft;

/** FFT configuration */
kiss_fftr_cfg kiss_cfg;

void rotate_90(unsigned char *buf, uint32_t len)
/* 90 rotation is 1+0j, 0+1j, -1+0j, 0-1j
   or [0, 1, -3, 2, -4, -5, 7, -6] */
{
	uint32_t i;
	unsigned char tmp;
	for (i=0; i<len; i+=8) {
		/* uint8_t negation = 255 - x */
		tmp = 255 - buf[i+3];
		buf[i+3] = buf[i+2];
		buf[i+2] = tmp;

		buf[i+4] = 255 - buf[i+4];
		buf[i+5] = 255 - buf[i+5];

		tmp = 255 - buf[i+6];
		buf[i+6] = buf[i+7];
		buf[i+7] = tmp;
	}
}

void low_pass(struct demod_state *d)
/* simple square window FIR */
{
	int i=0, i2=0;
	while (i < d->lp_len) {
		d->now_r += d->lowpassed[i];
		d->now_j += d->lowpassed[i+1];
		i += 2;
		d->prev_index++;
		if (d->prev_index < d->downsample) {
			continue;
		}
		d->lowpassed[i2]   = d->now_r; // * d->output_scale;
		d->lowpassed[i2+1] = d->now_j; // * d->output_scale;
		d->prev_index = 0;
		d->now_r = 0;
		d->now_j = 0;
		i2 += 2;
	}
	d->lp_len = i2;
}

int low_pass_simple(int16_t *signal2, int len, int step)
// no wrap around, length must be multiple of step
{
	int i, i2, sum;
	for(i=0; i < len; i+=step) {
		sum = 0;
		for(i2=0; i2<step; i2++) {
			sum += (int)signal2[i + i2];
		}
		//signal2[i/step] = (int16_t)(sum / step);
		signal2[i/step] = (int16_t)(sum);
	}
	signal2[i/step + 1] = signal2[i/step];
	return len / step;
}

void low_pass_real(struct demod_state *s)
/* simple square window FIR */
// add support for upsampling?
{
	int i=0, i2=0;
	int fast = (int)s->rate_out;
	int slow = s->rate_out2;
	while (i < s->result_len) {
		s->now_lpr += s->result[i];
		i++;
		s->prev_lpr_index += slow;
		if (s->prev_lpr_index < fast) {
			continue;
		}
		s->result[i2] = (int16_t)(s->now_lpr / (fast/slow));
		s->prev_lpr_index -= fast;
		s->now_lpr = 0;
		i2 += 1;
	}
	s->result_len = i2;
}

void fifth_order(int16_t *data, int length, int16_t *hist)
/* for half of interleaved data */
{
	int i;
	int16_t a, b, c, d, e, f;
	a = hist[1];
	b = hist[2];
	c = hist[3];
	d = hist[4];
	e = hist[5];
	f = data[0];
	/* a downsample should improve resolution, so don't fully shift */
	data[0] = (a + (b+e)*5 + (c+d)*10 + f) >> 4;
	for (i=4; i<length; i+=4) {
		a = c;
		b = d;
		c = e;
		d = f;
		e = data[i-2];
		f = data[i];
		data[i/2] = (a + (b+e)*5 + (c+d)*10 + f) >> 4;
	}
	/* archive */
	hist[0] = a;
	hist[1] = b;
	hist[2] = c;
	hist[3] = d;
	hist[4] = e;
	hist[5] = f;
}

void generic_fir(int16_t *data, int length, int *fir, int16_t *hist)
/* Okay, not at all generic.  Assumes length 9, fix that eventually. */
{
	int d, temp, sum;
	for (d=0; d<length; d+=2) {
		temp = data[d];
		sum = 0;
		sum += (hist[0] + hist[8]) * fir[1];
		sum += (hist[1] + hist[7]) * fir[2];
		sum += (hist[2] + hist[6]) * fir[3];
		sum += (hist[3] + hist[5]) * fir[4];
		sum +=            hist[4]  * fir[5];
		data[d] = sum >> 15 ;
		hist[0] = hist[1];
		hist[1] = hist[2];
		hist[2] = hist[3];
		hist[3] = hist[4];
		hist[4] = hist[5];
		hist[5] = hist[6];
		hist[6] = hist[7];
		hist[7] = hist[8];
		hist[8] = temp;
	}
}

/* define our own complex math ops
   because ARMv5 has no hardware float */

void multiply(int ar, int aj, int br, int bj, int *cr, int *cj)
{
	*cr = ar*br - aj*bj;
	*cj = aj*br + ar*bj;
}

int polar_discriminant(int ar, int aj, int br, int bj)
{
	int cr, cj;
	double angle;
	multiply(ar, aj, br, -bj, &cr, &cj);
	angle = atan2((double)cj, (double)cr);
	return (int)(angle / 3.14159 * (1<<14));
}

int fast_atan2(int y, int x)
/* pre scaled for int16 */
{
	int yabs, angle;
	int pi4=(1<<12), pi34=3*(1<<12);  // note pi = 1<<14
	if (x==0 && y==0) {
		return 0;
	}
	yabs = y;
	if (yabs < 0) {
		yabs = -yabs;
	}
	if (x >= 0) {
		angle = pi4  - pi4 * (x-yabs) / (x+yabs);
	} else {
		angle = pi34 - pi4 * (x+yabs) / (yabs-x);
	}
	if (y < 0) {
		return -angle;
	}
	return angle;
}

int polar_disc_fast(int ar, int aj, int br, int bj)
{
	int cr, cj;
	multiply(ar, aj, br, -bj, &cr, &cj);
	return fast_atan2(cj, cr);
}

int atan_lut_init(void)
{
	int i = 0;

	atan_lut = malloc(atan_lut_size * sizeof(int));

	for (i = 0; i < atan_lut_size; i++) {
		atan_lut[i] = (int) (atan((double) i / (1<<atan_lut_coef)) / 3.14159 * (1<<14));
	}

	return 0;
}

int polar_disc_lut(int ar, int aj, int br, int bj)
{
	int cr, cj, x, x_abs;

	multiply(ar, aj, br, -bj, &cr, &cj);

	/* special cases */
	if (cr == 0 || cj == 0) {
		if (cr == 0 && cj == 0)
			{return 0;}
		if (cr == 0 && cj > 0)
			{return 1 << 13;}
		if (cr == 0 && cj < 0)
			{return -(1 << 13);}
		if (cj == 0 && cr > 0)
			{return 0;}
		if (cj == 0 && cr < 0)
			{return 1 << 14;}
	}

	/* real range -32768 - 32768 use 64x range -> absolute maximum: 2097152 */
	x = (cj << atan_lut_coef) / cr;
	x_abs = abs(x);

	if (x_abs >= atan_lut_size) {
		/* we can use linear range, but it is not necessary */
		return (cj > 0) ? 1<<13 : -1<<13;
	}

	if (x > 0) {
		return (cj > 0) ? atan_lut[x] : atan_lut[x] - (1<<14);
	} else {
		return (cj > 0) ? (1<<14) - atan_lut[-x] : -atan_lut[-x];
	}

	return 0;
}

void fm_demod(struct demod_state *fm)
{
	int i, pcm;
	int16_t *lp = fm->lowpassed;
	pcm = polar_discriminant(lp[0], lp[1],
		fm->pre_r, fm->pre_j);
	fm->result[0] = (int16_t)pcm;
	for (i = 2; i < (fm->lp_len-1); i += 2) {
		switch (fm->custom_atan) {
		case 0:
			pcm = polar_discriminant(lp[i], lp[i+1],
				lp[i-2], lp[i-1]);
			break;
		case 1:
			pcm = polar_disc_fast(lp[i], lp[i+1],
				lp[i-2], lp[i-1]);
			break;
		case 2:
			pcm = polar_disc_lut(lp[i], lp[i+1],
				lp[i-2], lp[i-1]);
			break;
		}
		fm->result[i/2] = (int16_t)pcm;
	}
	fm->pre_r = lp[fm->lp_len - 2];
	fm->pre_j = lp[fm->lp_len - 1];
	fm->result_len = fm->lp_len/2;
}

void deemph_filter(struct demod_state *fm)
{
	static int avg;  // cheating...
	int i, d;
	// de-emph IIR
	// avg = avg * (1 - alpha) + sample * alpha;
	for (i = 0; i < fm->result_len; i++) {
		d = fm->result[i] - avg;
		if (d > 0) {
			avg += (d + fm->deemph_a/2) / fm->deemph_a;
		} else {
			avg += (d - fm->deemph_a/2) / fm->deemph_a;
		}
		fm->result[i] = (int16_t)avg;
	}
}

void dc_block_filter(struct demod_state *fm)
{
	int i, avg;
	int64_t sum = 0;
	for (i=0; i < fm->result_len; i++) {
		sum += fm->result[i];
	}
	avg = sum / fm->result_len;
	avg = (avg + fm->dc_avg * 9) / 10;
	for (i=0; i < fm->result_len; i++) {
		fm->result[i] -= avg;
	}
	fm->dc_avg = avg;
}

int mad(int16_t *samples, int len, int step)
/* mean average deviation */
{
	int i=0, sum=0, ave=0;
	if (len == 0)
		{return 0;}
	for (i=0; i<len; i+=step) {
		sum += samples[i];
	}
	ave = sum / (len * step);
	sum = 0;
	for (i=0; i<len; i+=step) {
		sum += abs(samples[i] - ave);
	}
	return sum / (len / step);
}

int rms(int16_t *samples, int len, int step)
/* largely lifted from rtl_power */
{
	int i;
	long p, t, s;
	double dc, err;

	p = t = 0L;
	for (i=0; i<len; i+=step) {
		s = (long)samples[i];
		t += s;
		p += s * s;
	}
	/* correct for dc offset in squares */
	dc = (double)(t*step) / (double)len;
	err = t * 2 * dc - dc * dc * len;

	return (int)sqrt((p-err) / len);
}

/**
 * @brief Returns signal strength in dBm.
 *
 * @param samples Pointer to a buffer holding the samples (16-bit signed int)
 * @param len Number of samples in the buffer
 * @param step Process only one in {@code step} samples
 * @return Signal strength in dBm
 */
double dbm(int16_t *samples, int len, int step) {
/* largely lifted from rms in rtl_power */
	/*
	 * The threshold for receiving RDS data is around -10 dBm. If the signal is weaker than that,
	 * it is virtually impossible to extract RDS data from it, hence the DX threshold should be
	 * in that range. The threshold for getting good RDS data is somewhere between -7.5 dBm
	 * (PI but no PS name) and -5.5 dBm (PS name visible, around 50% block error rate).
	 */
	int i;
	long p, t, s;
	double dc, err;

	p = t = 0L;
	for (i=0; i<len; i+=step) {
		s = (long)samples[i];
		t += s;
		p += s * s;
	}
	/* correct for dc offset in squares */
	dc = (double)(t*step) / (double)len;
	err = t * 2 * dc - dc * dc * len;

	return 10 * log10((p-err) / len);
}

/**
 * @brief Determines signal quality to identify valid stations.
 *
 * Because RSSI is not sufficient to identify valid stations, as some frequencies have noise which
 * is stronger than some valid stations, this function provides an additional tool to determine if
 * the current station is valid.
 *
 * It works by doing an FFT across the demodulated FM baseband spectrum and analyzing both the
 * average signal power and the mean average deviation.
 *
 * Noise typically has consistently high signal power across most of the spectrum, while a valid
 * station presents more variation between frequencies and a lower average power level.
 *
 * This tool calculates a weighted difference between both, which can be used as a signal quality
 * indicator: greater values indicate a better signal, 0 is an approximate acceptance threshold.
 * Note that RDS reception may require the signal to be well above the acceptance threshold.
 *
 * Demodulated audio samples are taken from {@code demod}.
 *
 * @param out_avg Address which will receive the measured average signal power, can be NULL
 * @param out_mad Address which will receive the mean average deviation, can be NULL
 *
 * @return Signal quality, with greater values indicating a better signal.
 */
double qual(double *out_avg, int *out_mad) {
	double ret;         // return value
	int lbound, ubound; // lower and upper boundary for frequency bins to analyze
	int i;              // iterator
	double avg;         // average power level across all frequency bins
	int fft_mad;        // power level mean average deviation

	/* if we need to pad FFT input data, low frequencies are not reliable */
	lbound = 1 / (demod.result_len * nfft) + 1;

	/* Stop at upper boundary of RDS subcarrier spectrum (58.65 kHz) */
	ubound = nfft / 2 + 1;
	if (ubound > (58650 * nfft / sampleRateOut - 1))
		ubound = 58650 * nfft / sampleRateOut - 1;

	for (i = 0; i < nfft; i++) {
		fft_in[i] = demod.result[i % demod.result_len];
	}
	kiss_fftr(kiss_cfg, fft_in, fft_out);

	avg = 0;
	for (i = lbound; i < ubound; i++) {
		spectrum[i] = round(500 * log10(fft_out[i].r * fft_out[i].r + fft_out[i].i * fft_out[i].i));
		avg += spectrum[i];
	}
	avg /= ubound - lbound;
	if (out_avg)
		*out_avg = avg;
	fft_mad = mad(spectrum + lbound * sizeof(int16_t), ubound - lbound, 1);
	if (out_mad)
		*out_mad = fft_mad;

	ret = 21 * fft_mad - avg;

#if 0
	if (ret >= 0) {
		fprintf(stderr, "\n+++++ GOOD SIGNAL (avg %.0f, mad %d) +++++\n", avg, fft_mad);
	}
	else {
		fprintf(stderr, "\n----- bad signal (avg %.0f, mad %d) -----\n", avg, fft_mad);
	}
#endif

	return ret;
}

void arbitrary_upsample(int16_t *buf1, int16_t *buf2, int len1, int len2)
/* linear interpolation, len1 < len2 */
{
	int i = 1;
	int j = 0;
	int tick = 0;
	double frac;  // use integers...
	while (j < len2) {
		frac = (double)tick / (double)len2;
		buf2[j] = (int16_t)(buf1[i-1]*(1-frac) + buf1[i]*frac);
		j++;
		tick += len1;
		if (tick > len2) {
			tick -= len2;
			i++;
		}
		if (i >= len1) {
			i = len1 - 1;
			tick = len2;
		}
	}
}

void arbitrary_downsample(int16_t *buf1, int16_t *buf2, int len1, int len2)
/* fractional boxcar lowpass, len1 > len2 */
{
	int i = 1;
	int j = 0;
	int tick = 0;
	double remainder = 0;
	double frac;  // use integers...
	buf2[0] = 0;
	while (j < len2) {
		frac = 1.0;
		if ((tick + len2) > len1) {
			frac = (double)(len1 - tick) / (double)len2;}
		buf2[j] += (int16_t)((double)buf1[i] * frac + remainder);
		remainder = (double)buf1[i] * (1.0-frac);
		tick += len2;
		i++;
		if (tick > len1) {
			j++;
			buf2[j] = 0;
			tick -= len1;
		}
		if (i >= len1) {
			i = len1 - 1;
			tick = len1;
		}
	}
	for (j=0; j<len2; j++) {
		buf2[j] = buf2[j] * len2 / len1;}
}

void arbitrary_resample(int16_t *buf1, int16_t *buf2, int len1, int len2)
/* up to you to calculate lengths and make sure it does not go OOB
 * okay for buffers to overlap, if you are downsampling */
{
	if (len1 < len2) {
		arbitrary_upsample(buf1, buf2, len1, len2);
	} else {
		arbitrary_downsample(buf1, buf2, len1, len2);
	}
}

void full_demod(struct demod_state *d, JNIEnv *env)
{
	int i, ds_p;
	double rssi;
	ds_p = d->downsample_passes;
	if (ds_p) {
		for (i=0; i < ds_p; i++) {
			fifth_order(d->lowpassed,   (d->lp_len >> i), d->lp_i_hist[i]);
			fifth_order(d->lowpassed+1, (d->lp_len >> i) - 1, d->lp_q_hist[i]);
		}
		d->lp_len = d->lp_len >> ds_p;
		/* droop compensation */
		if (d->comp_fir_size == 9 && ds_p <= CIC_TABLE_MAX) {
			generic_fir(d->lowpassed, d->lp_len,
				cic_9_tables[ds_p], d->droop_i_hist);
			generic_fir(d->lowpassed+1, d->lp_len-1,
				cic_9_tables[ds_p], d->droop_q_hist);
		}
	} else {
		low_pass(d);
	}
	rssi = dbm(d->lowpassed, d->lp_len, 1);
	if (rssi != d->rssi) {
		d->rssi = rssi;
		(*(env))->CallVoidMethod(env, d->self, d->onRssiChanged, (jfloat)rssi);
	}
	fm_demod(d);  /* lowpassed -> result */
	/* todo, fm noise squelch */
	// use nicer filter here too?
	if (d->post_downsample > 1) {
		d->result_len = low_pass_simple(d->result, d->result_len, d->post_downsample);}
	if (d->deemph) {
		deemph_filter(d);}
	if (d->dc_block) {
		dc_block_filter(d);}
	if (d->rate_out2 > 0) {
		low_pass_real(d);
		//arbitrary_resample(d->result, d->result, d->result_len, d->result_len * d->rate_out2 / d->rate_out);
	}
}

/**
 * @brief Processes samples obtained from a previous call to {@code rtlsdr_read_async()}.
 *
 * This function is passed as a callback function to {@code rtlsdr_read_async()}, which will start
 * reading samples from the tuner device and call this function periodically until terminated by
 * calling {@code rtlsdr_cancel_async()}.
 *
 * @param buf Points to the buffer of samples received. Samples are stored as 16-bit integers.
 * @param len The number of samples received. The length of {@code buf} is {@code 2 * len} bytes.
 * @param ctx The context, a pointer to the corresponding {@code struct dongle_state}.
 */
static void rtlsdr_callback(unsigned char *buf, uint32_t len, void *ctx)
{
	int i;
	struct dongle_state *s = ctx;
	struct demod_state *d = s->demod_target;

	if (do_exit) {
		return;}
	if (!ctx) {
		return;}
	if (s->mute) {
		for (i=0; i<s->mute; i++) {
			buf[i] = 127;}
		s->mute = 0;
	}
	if (!s->offset_tuning) {
		rotate_90(buf, len);}
	s->minSample = 255;
	s->maxSample = 0;
	for (i=0; i<(int)len; i++) {
		if (buf[i] < s->minSample)
			s->minSample = buf[i];
		if (buf[i] > s->maxSample)
			s->maxSample = buf[i];
		s->buf16[i] = (int16_t)buf[i] - 127;}
	pthread_rwlock_wrlock(&d->rw);
	memcpy(d->lowpassed, s->buf16, 2*len);
	d->lp_len = len;
	pthread_rwlock_unlock(&d->rw);
	safe_cond_signal(&d->ready, &d->ready_m);
}

static void *dongle_thread_fn(void *arg)
{
	struct dongle_state *s = arg;
	rtlsdr_read_async(s->dev, rtlsdr_callback, s, 0, s->buf_len);
	return 0;
}

static void *demod_thread_fn(void *arg)
{
	struct demod_state *d = arg;
	struct output_state *o = d->output_target;

	(*(d->jvm))->AttachCurrentThread(d->jvm, (void **)&(d->env), NULL);
	jclass clsSelf = (*(d->env))->GetObjectClass(d->env, d->self);
	d->onRssiChanged = (*(d->env))->GetMethodID(d->env, clsSelf, "onRssiChanged", "(F)V");

	while (!do_exit) {
		safe_cond_wait(&d->ready, &d->ready_m);
		pthread_rwlock_wrlock(&d->rw);
		full_demod(d, d->env);
		pthread_rwlock_unlock(&d->rw);
		if (d->exit_flag) {
			do_exit = 1;
		}
		pthread_rwlock_wrlock(&o->rw);
		memcpy(o->result, d->result, 2*d->result_len);
		o->result_len = d->result_len;
		pthread_rwlock_unlock(&o->rw);
		safe_cond_signal(&o->ready, &o->ready_m);
		safe_cond_signal(&controller.hop, &controller.hop_m);
	}
	(*(d->jvm))->DetachCurrentThread(d->jvm);
	return 0;
}

static void write_output(struct output_state *s) {
	int len = 2*s->result_len;
	void *temp = (*(s->env))->GetPrimitiveArrayCritical(s->env, (jarray) s->jdata, 0);
	memcpy(temp, &(s->result), len);
	(*(s->env))->ReleasePrimitiveArrayCritical(s->env, s->jdata, temp, 0);
	(*(s->env))->CallVoidMethod(s->env, s->tunerOut, s->write, s->jdata, 0, len);
}

static void *output_thread_fn(void *arg)
{
	struct output_state *s = arg;

	(*(s->jvm))->AttachCurrentThread(s->jvm, (void **)&(s->env), NULL);
	jclass clsSelf = (*(s->env))->GetObjectClass(s->env, s->self);
	jfieldID fTunerOut = (*(s->env))->GetFieldID(s->env, clsSelf, "tunerOut", "Ljava/io/DataOutputStream;");
	s->tunerOut = (*(s->env))->GetObjectField(s->env, s->self, fTunerOut);
	jclass cls = (*(s->env))->GetObjectClass(s->env, s->tunerOut);
	s->write = (*(s->env))->GetMethodID(s->env, cls, "write", "([BII)V");
	s->jdata = (*(s->env))->NewByteArray(s->env, 2 * MAXIMUM_BUF_LENGTH);

	if (!s->write || !cls || !s->tunerOut || !fTunerOut || !clsSelf) {
		fprintf(stderr, "Could not get reference to output stream, exiting\n");
		return 0;
	}

	while (!do_exit) {
		// use timedwait and pad out under runs
		safe_cond_wait(&s->ready, &s->ready_m);
		pthread_rwlock_rdlock(&s->rw);
		write_output(s);
		pthread_rwlock_unlock(&s->rw);
	}
	(*(s->env))->DeleteLocalRef(s->env, s->jdata);
	(*(s->env))->DeleteGlobalRef(s->env, s->self);
	(*(s->jvm))->DetachCurrentThread(s->jvm);
	return 0;
}

static void optimal_settings(int freq)
{
	// giant ball of hacks
	// seems unable to do a single pass, 2:1
	int capture_freq, capture_rate;
	struct dongle_state *d = &dongle;
	struct demod_state *dm = &demod;
	struct controller_state *cs = &controller;
	dm->downsample = (1000000 / dm->rate_in) + 1;
	if (dm->downsample_passes) {
		dm->downsample_passes = (int)log2(dm->downsample) + 1;
		dm->downsample = 1 << dm->downsample_passes;
	}
	capture_freq = freq;
	capture_rate = dm->downsample * dm->rate_in;
	if (!d->offset_tuning) {
		capture_freq = freq + capture_rate/4;}
	capture_freq += cs->edge * dm->rate_in / 2;
	d->freq = (uint32_t)capture_freq;
	d->rate = (uint32_t)capture_rate;
}

/**
 * @brief Waits for signal quality to stabilize, then returns the result.
 *
 * Signal quality is considered to have stabilized when the average power level reaches 2500
 * (25 dB).
 *
 * Other criteria (trend/stability of the quality indicator, monotonic sequence formed by quality
 * indicator and/or its constituents) were not considered as average power level was considered
 * sufficient.
 *
 * See {@code qual()} on how to interpret the result.
 */
double get_stabilized_qual() {
	/* Size of samples buffer */
	int samplesSize = MAXIMUM_BUF_LENGTH;

	/* Buffer to receive samples for quality measurement */
	int16_t samples[samplesSize];

	/* Samples actually read */
	int samplesRead = 0;

	/* Average and mean average deviation of FFT */
	double fft_avg;
	int fft_mad;

	double ret;

	while (1) {
		/* get a burst of samples to measure quality */
		if (rtlsdr_read_sync(dongle.dev, &samples, samplesSize, &samplesRead) < 0) {
			fprintf(stderr, "\nget_stabilized_qual: rtlsdr_read_sync failed\n");
			return 0;
		}

		rtlsdr_callback((unsigned char *)&samples, samplesRead, &dongle);
		pthread_rwlock_wrlock(&demod.rw);
		full_demod(&demod, controller.env);
		ret = qual(&fft_avg, &fft_mad);
		pthread_rwlock_unlock(&demod.rw);
		if (fft_avg > 2500)
			return ret;
	}
}

/**
 * @brief Waits for RSSI to stabilize, then returns the result.
 *
 * The RSSI is considered to have stabilized when three subsequent measurements fail to form a
 * monotonic sequence. In that case, the last RSSI measurement is returned.
 *
 * A previous stabilization criterion of two subsequent measurements differing by less than 1 dB
 * was dropped as it gave frequent false positives, resulting in good stations being skipped.
 */
double get_stabilized_rssi() {
	/* Size of samples buffer */
	int samplesSize = MAXIMUM_BUF_LENGTH / 16;
	/* Buffer to receive samples for RSSI measurement */
	int16_t samples[samplesSize];
	int samplesRead = 0;
	int rssiTrend = 0;
	double rssi = RSSI_INVALID;
	double lastRssi = RSSI_INVALID;

	while (1) {
		lastRssi = rssi;

		/* get a burst of samples to measure RSSI */
		if (rtlsdr_read_sync(dongle.dev, &samples, samplesSize, &samplesRead) < 0) {
			fprintf(stderr, "\nget_stabilized_rssi: rtlsdr_read_sync failed\n");
			return RSSI_INVALID;
		}

		rtlsdr_callback((unsigned char *)&samples, samplesRead, &dongle);
		pthread_rwlock_rdlock(&demod.rw);
		rssi = dongle.demod_target->rssi;
		pthread_rwlock_unlock(&demod.rw);

		//fprintf(stderr, "\nget_stabilized_rssi: rssi=%.2f", rssi);

		if (lastRssi == RSSI_INVALID) {
			/* nothing to compare to yet */
		} else if (!rssiTrend) {
			if (rssi < lastRssi)
				rssiTrend = -1;
			else if (rssi > lastRssi)
				rssiTrend = 1;
		} else {
			/* RSSI started oscillating, assume stable state */
			if (((rssi < lastRssi) && rssiTrend == 1)
					|| ((rssi > lastRssi) && rssiTrend == -1))
				return rssi;
		}
	}
}

static void *controller_thread_fn(void *arg)
{
	// thoughts for multiple dongles
	// might be no good using a controller thread if retune/rate blocks
	struct controller_state *s = arg;
	uint32_t freq;
	double rssi, lastRssi;
	/* reference RSSI: when nearStart is true, the lowest one observed; else the highest one */
	double refRssi;
	int maxFreq;
	/* signal quality at maxFreq */
	double maxQual;
	/* Whether we are still near the frequency where seek started (or near an invalid station) */
	int nearStart;

	(*(s->jvm))->AttachCurrentThread(s->jvm, (void **)&(s->env), NULL);
	jclass clsSelf = (*(s->env))->GetObjectClass(s->env, s->self);
	s->onFrequencyChanged = (*(s->env))->GetMethodID(s->env, clsSelf, "onFrequencyChanged", "(I)V");

	if (s->wb_mode) {
		pthread_rwlock_wrlock(&s->rw);
		s->freq += 16000;
		pthread_rwlock_unlock(&s->rw);
	}

	/* set up primary channel */
	pthread_rwlock_wrlock(&s->rw);
	optimal_settings(s->freq);
	s->retune = TUNE_NONE;
	freq = s->freq;
	pthread_rwlock_unlock(&s->rw);
	if (dongle.direct_sampling) {
		verbose_direct_sampling(dongle.dev, 1);}
	if (dongle.offset_tuning) {
		verbose_offset_tuning(dongle.dev);}

	/* Set the frequency */
	verbose_set_frequency(dongle.dev, dongle.freq);
	(*(s->env))->CallVoidMethod(s->env, s->self, s->onFrequencyChanged, (jint)(freq / 1.0e+3));

	/* Set the sample rate */
	verbose_set_sample_rate(dongle.dev, dongle.rate);

	/* Start the dongle thread */
	usleep(100000);
	pthread_create(&dongle.thread, NULL, dongle_thread_fn, (void *)(&dongle));

	while (!do_exit) {
		safe_cond_wait(&s->hop, &s->hop_m); // FIXME: do we need that here?
		pthread_rwlock_wrlock(&s->rw);
		switch (s->retune) {
		case TUNE_NONE:
			if (dongle.gain != AUTO_GAIN) {
				if (dongle.minSample == 0 || dongle.maxSample == 255) {
					/* clipping detected, decrease gain */
					int i = 0;
					while ((i < dongle.gains_len - 1) && (dongle.gains[i+1] < dongle.gain))
						i++;
					if (dongle.gain != dongle.gains[i]) {
						dongle.gain = dongle.gains[i];
						verbose_gain_set(dongle.dev, dongle.gain);
					}
				} else if (dongle.gain < dongle.gains[dongle.gains_len-1]) {
					/* check if we can increase gain without causing clipping */
					int i = 0;
					do {
						i++;
					} while ((i < dongle.gains_len - 1) && (dongle.gains[i] <= dongle.gain));
					float linGain = pow(10, (float)(dongle.gains[i] - dongle.gain) / 450);
					int newMax = (float) dongle.maxSample * linGain;
					int newMin = (float) dongle.minSample / linGain;
					if (newMin >= 0 && newMax <= 255) {
						dongle.gain = dongle.gains[i];
						verbose_gain_set(dongle.dev, dongle.gain);
					}
				}
			}
			pthread_rwlock_unlock(&s->rw);
			continue;
		case TUNE_FREQ:
			/* hacky hopping */
			freq = s->freq;
			optimal_settings(freq);
			rtlsdr_cancel_async(dongle.dev);
			rtlsdr_set_center_freq(dongle.dev, dongle.freq);
			/* TODO should we allow the tuner to settle and flush the buffer before continuing? */
			/* Start a new dongle thread */
			pthread_create(&dongle.thread, NULL, dongle_thread_fn, (void *)(&dongle));
			break;
		case TUNE_SEEK_UP:
		case TUNE_SEEK_DOWN:
			freq = s->freq;
			refRssi = -RSSI_INVALID;
			maxFreq = 0;
			nearStart = 1;
			while (s->retune != TUNE_NONE) {
				if (s->retune == TUNE_SEEK_UP)
					freq += 0.1e+6;
				else
					freq -= 0.1e+6;
				pthread_rwlock_unlock(&s->rw);
				if (freq > FREQ_MAX) {
					freq = FREQ_MIN;
					nearStart = 0;
				} else if (freq < FREQ_MIN) {
					freq = FREQ_MAX;
					nearStart = 0;
				}
				rtlsdr_cancel_async(dongle.dev);
				optimal_settings(freq);
				rtlsdr_set_center_freq(dongle.dev, dongle.freq);

				/* wait for tuner to settle */
				usleep(5000);

				lastRssi = rssi;

				/* measure RSSI */
				rssi = get_stabilized_rssi();

				pthread_rwlock_wrlock(&s->rw);
				if (nearStart && (rssi < refRssi)) {
					/* while we're near the start frequency, store lowest frequency observed */
					refRssi = rssi;
				} else if (rssi >= refRssi) {
					if (rssi >= lastRssi + 1)
						/* RSSI is increasing significantly (above 1 dB), we're no longer near the start */
						nearStart = 0;
					if (!nearStart) {
						/* store frequency and RSSI and see if the next frequency has a stronger signal */
						maxFreq = freq;
						maxQual = get_stabilized_qual();
						refRssi = rssi;
					}
				}
				if (maxFreq && (rssi < refRssi)) {
					/* we're past the peak, examine if the peak is a good station */
					if (maxQual >= 0) {
						/* we've found a good station, tune into it */
						freq = maxFreq;
						s->freq = freq;
						rtlsdr_cancel_async(dongle.dev);
						optimal_settings(freq);
						rtlsdr_set_center_freq(dongle.dev, dongle.freq);
						s->retune = TUNE_NONE;
					} else {
						/* we've found a peak but it is just noise, continue searching */
						nearStart = 1;
						refRssi = rssi;
						maxFreq = 0;
					}
				} else if (freq == s->freq) {
					/* We're back at the original frequency and didn't find any stations */
					rtlsdr_cancel_async(dongle.dev);
					optimal_settings(freq);
					rtlsdr_set_center_freq(dongle.dev, dongle.freq);
					s->retune = TUNE_NONE;
				}
			} // while (s->retune != TUNE_NONE)
			/* Start a new dongle thread */
			pthread_create(&dongle.thread, NULL, dongle_thread_fn, (void *)(&dongle));
		} // switch (s->retune)
		s->retune = TUNE_NONE;
		pthread_rwlock_unlock(&s->rw);
		(*(s->env))->CallVoidMethod(s->env, s->self, s->onFrequencyChanged, (jint)(freq / 1.0e+3));
		dongle.mute = BUFFER_DUMP;
	}
	(*(s->jvm))->DetachCurrentThread(s->jvm);
	return 0;
}

void dongle_init(struct dongle_state *s)
{
	s->rate = sampleRateOut;
	s->gain = 0; // tenths of a dB
	s->gains = NULL;
	s->gains_len = 0;
	s->mute = 0;
	s->direct_sampling = 0;
	s->offset_tuning = 0;
	s->demod_target = &demod;
}

void demod_init(struct demod_state *s)
{
	s->rate_in = sampleRateOut;
	s->rate_out = sampleRateOut;
	s->downsample_passes = 1;  /* truthy placeholder */
	s->comp_fir_size = 9;
	s->prev_index = 0;
	s->post_downsample = 1;  // once this works, default = 4
	s->custom_atan = 0;
	s->deemph = 0;
	s->rate_out2 = sampleRateOut;
	s->pre_j = s->pre_r = s->now_r = s->now_j = 0;
	s->prev_lpr_index = 0;
	s->deemph_a = 0;
	s->now_lpr = 0;
	s->dc_block = 0;
	s->dc_avg = 0;
	pthread_rwlock_init(&s->rw, NULL);
	pthread_cond_init(&s->ready, NULL);
	pthread_mutex_init(&s->ready_m, NULL);
	s->output_target = &output;
}

void demod_cleanup(struct demod_state *s)
{
	pthread_rwlock_destroy(&s->rw);
	pthread_cond_destroy(&s->ready);
	pthread_mutex_destroy(&s->ready_m);
}

void output_init(struct output_state *s)
{
	s->rate = sampleRateOut;
	pthread_rwlock_init(&s->rw, NULL);
	pthread_cond_init(&s->ready, NULL);
	pthread_mutex_init(&s->ready_m, NULL);
}

void output_cleanup(struct output_state *s)
{
	pthread_rwlock_destroy(&s->rw);
	pthread_cond_destroy(&s->ready);
	pthread_mutex_destroy(&s->ready_m);
}

void controller_init(struct controller_state *s)
{
	s->freq = INITIAL_FREQ;
	s->edge = 0;
	s->wb_mode = 0;
	pthread_rwlock_init(&s->rw, NULL);
	pthread_cond_init(&s->hop, NULL);
	pthread_mutex_init(&s->hop_m, NULL);
}

void controller_cleanup(struct controller_state *s)
{
	pthread_rwlock_destroy(&s->rw);
	pthread_cond_destroy(&s->hop);
	pthread_mutex_destroy(&s->hop_m);
}


/*
 * @return true for success, false for failure
 */
JNIEXPORT jboolean JNICALL Java_eu_jacquet80_rds_input_SdrGroupReader_open
  (JNIEnv *env, jobject self) {
    jclass clsSelf = (*env)->GetObjectClass(env, self);

    jfieldID fSampleRate = (*env)->GetStaticFieldID(env, clsSelf, "sampleRate", "I");
    sampleRateOut = (*env)->GetStaticIntField(env, clsSelf, fSampleRate);

	int r;
	dongle_init(&dongle);
	demod_init(&demod);
	output_init(&output);
	controller_init(&controller);

	/*
	 * To get a maximum bin width of 20 Hz, nfft must be at least 1/20 of the sample rate.
	 * The log and shifting operation ensure nfft is a power of 2.
	 */
	nfft = 1 << int_log2(sampleRateOut / 20, 1);
	kiss_cfg = kiss_fftr_alloc(nfft, 0, 0, 0); // not inverse, no memory allocation
	fft_in = malloc(sizeof(kiss_fft_scalar) * nfft);
	fft_out = malloc(sizeof(kiss_fft_cpx) * (nfft / 2 + 1));
	spectrum = malloc(sizeof(int16_t) * (nfft / 2 + 1));
	fprintf(stderr, "Initialized FFT with size %d (%d bins, %.3f Hz bin width).\n", nfft, nfft/2 + 1, (float)sampleRateOut / (float)nfft);

	controller.self = (*env)->NewGlobalRef(env, self);
	(*env)->GetJavaVM(env, &(controller.jvm));

	demod.self = (*env)->NewGlobalRef(env, self);
	(*env)->GetJavaVM(env, &(demod.jvm));

	output.self = (*env)->NewGlobalRef(env, self);
	(*env)->GetJavaVM(env, &(output.jvm));

	/* quadruple sample_rate to limit to Δθ to ±π/2 */
	demod.rate_in *= demod.post_downsample;

	ACTUAL_BUF_LENGTH = lcm_post[demod.post_downsample] * DEFAULT_BUF_LENGTH;

	dongle.dev_index = verbose_device_search("0");

	if (dongle.dev_index < 0) {
		return 0;
	}

	r = rtlsdr_open(&dongle.dev, (uint32_t)dongle.dev_index);
	if (r < 0) {
		fprintf(stderr, "Failed to open rtlsdr device #%d.\n", dongle.dev_index);
		return 0;
	}

	if (demod.deemph) {
		demod.deemph_a = (int)round(1.0/((1.0-exp(-1.0/(demod.rate_out * 75e-6)))));
	}

	dongle.gains_len = rtlsdr_get_tuner_gains(dongle.dev, NULL);
	if (dongle.gains_len) {
		dongle.gains = malloc(sizeof(int) * dongle.gains_len);
		dongle.gains_len = rtlsdr_get_tuner_gains(dongle.dev, dongle.gains);
	}

	/* Fall back to auto gain if we can't get supported gain levels */
	if (!dongle.gains_len)
		dongle.gain = AUTO_GAIN;

	/* Set the tuner gain */
	if (dongle.gain == AUTO_GAIN) {
		verbose_auto_gain(dongle.dev);
	} else {
		dongle.gain = nearest_gain(dongle.dev, dongle.gain);
		verbose_gain_set(dongle.dev, dongle.gain);
	}

	verbose_ppm_set(dongle.dev, dongle.ppm_error);

	/* Reset endpoint before we start reading from it (mandatory) */
	verbose_reset_buffer(dongle.dev);

	pthread_create(&controller.thread, NULL, controller_thread_fn, (void *)(&controller));
	usleep(100000);
	pthread_create(&output.thread, NULL, output_thread_fn, (void *)(&output));
	pthread_create(&demod.thread, NULL, demod_thread_fn, (void *)(&demod));

    return 1;
}


JNIEXPORT jstring JNICALL Java_eu_jacquet80_rds_input_SdrGroupReader_getDeviceName
  (JNIEnv *env, jobject self) {
	/* Suppress compiler warnings */
	(void) self;

    return (*env)->NewStringUTF(env, "rtl2832u");
}

JNIEXPORT jint JNICALL Java_eu_jacquet80_rds_input_SdrGroupReader_setFrequency
  (JNIEnv *env, jobject self, jint freq) {
	/* Suppress compiler warnings */
	(void) env;
	(void) self;

	pthread_rwlock_wrlock(&controller.rw);
	controller.freq = freq * 1.0e+3;
	controller.retune = TUNE_FREQ;
	pthread_rwlock_unlock(&controller.rw);
    return freq;
}


JNIEXPORT jboolean JNICALL Java_eu_jacquet80_rds_input_SdrGroupReader_nativeSeek
  (JNIEnv *env, jobject self, jboolean up) {
	/* Suppress compiler warnings */
	(void) env;
	(void) self;

	pthread_rwlock_wrlock(&controller.rw);
	controller.retune = up ? TUNE_SEEK_UP : TUNE_SEEK_DOWN;
	pthread_rwlock_unlock(&controller.rw);
	return 0; // for now - result is never evaluated anyway
}
