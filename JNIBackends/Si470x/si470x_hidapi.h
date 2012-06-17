/*
Si470x userspace driver - Derived from the Linux kernel driver
Copyright (c) 2012  Christophe Jacquet
Copyright (c) 2009  Tobias Lorenz (original Linux driver)

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


#include <stdint.h>

/* Spacing (kHz) */
/* 0: 200 kHz (USA, Australia) */
/* 1: 100 kHz (Europe, Japan) */
/* 2:  50 kHz */
#define CHANNEL_SPACING_50_KHZ 2
#define CHANNEL_SPACING_100_KHZ 1
#define CHANNEL_SPACING_200_KHZ 0

/* Bottom of Band (MHz) */
/* 0: 87.5 - 108 MHz (USA, Europe)*/
/* 1: 76   - 108 MHz (Japan wide band) */
/* 2: 76   -  90 MHz (Japan) */
#define BAND_87_108 0
#define BAND_76_108 1
#define BAND_76_90 2

/* De-emphasis */
/* 0: 75 us (USA) */
/* 1: 50 us (Europe, Australia, Japan) */
#define DE_USA 0
#define DE_WORLD 1


/**************************************************************************
 * LED State Definitions
 **************************************************************************/
#define LED_COMMAND		0x35

#define NO_CHANGE_LED		0x00
#define ALL_COLOR_LED		0x01	/* streaming state */
#define BLINK_GREEN_LED		0x02	/* connect state */
#define BLINK_RED_LED		0x04
#define BLINK_ORANGE_LED	0x10	/* disconnect state */
#define SOLID_GREEN_LED		0x20	/* tuning/seeking state */
#define SOLID_RED_LED		0x40	/* bootload state */
#define SOLID_ORANGE_LED	0x80



typedef struct si470x_dev si470x_dev_t;

typedef struct {
    uint16_t block[4];
    uint16_t bler[4];
    unsigned char sync;
    unsigned char stereo;
    unsigned char rssi;
    int frequency;
} si470x_tunerdata_t;

int si470x_set_led_state(si470x_dev_t *radio, unsigned char led_state);
int si470x_get_scratch_page_versions(si470x_dev_t *radio);
int si470x_read_rds(si470x_dev_t *radio, si470x_tunerdata_t *data);
int si470x_open(si470x_dev_t **out_dev, uint32_t index);
int si470x_start(si470x_dev_t *dev, uint8_t space, uint8_t band, uint8_t de);
int si470x_get_freq(si470x_dev_t *radio, int *freq);
int si470x_set_freq(si470x_dev_t *radio, int freq);
int si470x_start_seek(si470x_dev_t *radio, unsigned int wrap_around, unsigned int seek_upward);