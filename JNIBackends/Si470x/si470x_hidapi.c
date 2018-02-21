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

#include <stdlib.h>
#include <errno.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <wchar.h>

#include <hidapi.h>

#include "si470x_hidapi.h"


#define dev_warn printf
#define dev_info printf

static inline uint16_t get_unaligned_be16(uint8_t *buf) {
    return buf[0] << 8 | buf[1];
}

static inline void put_unaligned_be16(uint16_t val, uint8_t *buf) {
    *buf++ = val >> 8;
    *buf = val;
}

/**************************************************************************
 * USB HID Reports
 **************************************************************************/

#define HID_REQ_GET_REPORT              0x01
#define HID_REQ_SET_REPORT              0x09

// On Windows, one apparently need to use the report size associated with
// the endpoint. -- To be confirmed.
#define EP0_REPORT_SIZE 17

/* Reports 1-16 give direct read/write access to the 16 Si470x registers */
/* with the (REPORT_ID - 1) corresponding to the register address across USB */
/* endpoint 0 using GET_REPORT and SET_REPORT */
#define REGISTER_REPORT_SIZE	(RADIO_REGISTER_SIZE + 1)
#define REGISTER_REPORT(reg)	((reg) + 1)

/* Report 17 gives direct read/write access to the entire Si470x register */
/* map across endpoint 0 using GET_REPORT and SET_REPORT */
#define ENTIRE_REPORT_SIZE	(RADIO_REGISTER_NUM * RADIO_REGISTER_SIZE + 1)
#define ENTIRE_REPORT		17

/* Report 18 is used to send the lowest 6 Si470x registers up the HID */
/* interrupt endpoint 1 to Windows every 20 milliseconds for status */
#define RDS_REPORT_SIZE		(RDS_REGISTER_NUM * RADIO_REGISTER_SIZE + 1)
#define RDS_REPORT		18

/* Report 19: LED state */
#define LED_REPORT_SIZE		3
#define LED_REPORT		19

/* Report 19: stream */
#define STREAM_REPORT_SIZE	3
#define STREAM_REPORT		19

/* Report 20: scratch */
#define SCRATCH_PAGE_SIZE	63
#define SCRATCH_REPORT_SIZE	(SCRATCH_PAGE_SIZE + 1)
#define SCRATCH_REPORT		20



/**************************************************************************
 * Software/Hardware Versions from Scratch Page
 **************************************************************************/
#define RADIO_SW_VERSION_NOT_BOOTLOADABLE	6
#define RADIO_SW_VERSION			7
#define RADIO_HW_VERSION			1


/**************************************************************************
 * Register Definitions
 **************************************************************************/
#define RADIO_REGISTER_SIZE	2	/* 16 register bit width */
#define RADIO_REGISTER_NUM	16	/* DEVICEID   ... RDSD */
#define RDS_REGISTER_NUM	6	/* STATUSRSSI ... RDSD */

#define DEVICEID		0	/* Device ID */
#define DEVICEID_PN		0xf000	/* bits 15..12: Part Number */
#define DEVICEID_MFGID		0x0fff	/* bits 11..00: Manufacturer ID */

#define CHIPID			1	/* Chip ID. Note: this field changed with firmware r16 */
#define CHIPID_REV		0xfc00	/* bits 15..10: Silicon revision */
#define CHIPID_DEV		0x03C0	/* bits 09..06: Device */
#define CHIPID_FIRMWARE		0x003f	/* bits 05..00: Firmware Version */

#define POWERCFG		2	/* Power Configuration */
#define POWERCFG_DSMUTE		0x8000	/* bits 15..15: Softmute Disable */
#define POWERCFG_DMUTE		0x4000	/* bits 14..14: Mute Disable */
#define POWERCFG_MONO		0x2000	/* bits 13..13: Mono Select */
#define POWERCFG_RDSM		0x0800	/* bits 11..11: RDS Mode (Si4701 only) */
#define POWERCFG_SKMODE		0x0400	/* bits 10..10: Seek Mode */
#define POWERCFG_SEEKUP		0x0200	/* bits 09..09: Seek Direction */
#define POWERCFG_SEEK		0x0100	/* bits 08..08: Seek */
#define POWERCFG_DISABLE	0x0040	/* bits 06..06: Powerup Disable */
#define POWERCFG_ENABLE		0x0001	/* bits 00..00: Powerup Enable */

#define CHANNEL			3	/* Channel */
#define CHANNEL_TUNE		0x8000	/* bits 15..15: Tune */
#define CHANNEL_CHAN		0x03ff	/* bits 09..00: Channel Select */

#define SYSCONFIG1		4	/* System Configuration 1 */
#define SYSCONFIG1_RDSIEN	0x8000	/* bits 15..15: RDS Interrupt Enable (Si4701 only) */
#define SYSCONFIG1_STCIEN	0x4000	/* bits 14..14: Seek/Tune Complete Interrupt Enable */
#define SYSCONFIG1_RDS		0x1000	/* bits 12..12: RDS Enable (Si4701 only) */
#define SYSCONFIG1_DE		0x0800	/* bits 11..11: De-emphasis (0=75us 1=50us) */
#define SYSCONFIG1_AGCD		0x0400	/* bits 10..10: AGC Disable */
#define SYSCONFIG1_BLNDADJ	0x00c0	/* bits 07..06: Stereo/Mono Blend Level Adjustment */
#define SYSCONFIG1_GPIO3	0x0030	/* bits 05..04: General Purpose I/O 3 */
#define SYSCONFIG1_GPIO2	0x000c	/* bits 03..02: General Purpose I/O 2 */
#define SYSCONFIG1_GPIO1	0x0003	/* bits 01..00: General Purpose I/O 1 */

#define SYSCONFIG2		5	/* System Configuration 2 */
#define SYSCONFIG2_SEEKTH	0xff00	/* bits 15..08: RSSI Seek Threshold */
#define SYSCONFIG2_BAND		0x00C0	/* bits 07..06: Band Select */
#define SYSCONFIG2_SPACE	0x0030	/* bits 05..04: Channel Spacing */
#define SYSCONFIG2_VOLUME	0x000f	/* bits 03..00: Volume */

#define SYSCONFIG3		6	/* System Configuration 3 */
#define SYSCONFIG3_SMUTER	0xc000	/* bits 15..14: Softmute Attack/Recover Rate */
#define SYSCONFIG3_SMUTEA	0x3000	/* bits 13..12: Softmute Attenuation */
#define SYSCONFIG3_SKSNR	0x00f0	/* bits 07..04: Seek SNR Threshold */
#define SYSCONFIG3_SKCNT	0x000f	/* bits 03..00: Seek FM Impulse Detection Threshold */

#define TEST1			7	/* Test 1 */
#define TEST1_AHIZEN		0x4000	/* bits 14..14: Audio High-Z Enable */

#define TEST2			8	/* Test 2 */
/* TEST2 only contains reserved bits */

#define BOOTCONFIG		9	/* Boot Configuration */
/* BOOTCONFIG only contains reserved bits */

#define STATUSRSSI		10	/* Status RSSI */
#define STATUSRSSI_RDSR		0x8000	/* bits 15..15: RDS Ready (Si4701 only) */
#define STATUSRSSI_STC		0x4000	/* bits 14..14: Seek/Tune Complete */
#define STATUSRSSI_SF		0x2000	/* bits 13..13: Seek Fail/Band Limit */
#define STATUSRSSI_AFCRL	0x1000	/* bits 12..12: AFC Rail */
#define STATUSRSSI_RDSE     0x0E00  /* bits 11..09: RDS Errors (firmware rev <= 12 or "standard mode") */
/* The next two fields are supported only in "verbose mode", available with firmware revs >12 */
#define STATUSRSSI_RDSS		0x0800	/* bits 11..11: RDS Synchronized (Si4701 only) */
#define STATUSRSSI_BLERA	0x0600	/* bits 10..09: RDS Block A Errors (Si4701 only) */
#define STATUSRSSI_ST		0x0100	/* bits 08..08: Stereo Indicator */
#define STATUSRSSI_RSSI		0x00ff	/* bits 07..00: RSSI (Received Signal Strength Indicator) */

#define READCHAN		11	/* Read Channel */
#define READCHAN_BLERB		0xc000	/* bits 15..14: RDS Block D Errors (Si4701 only) */
#define READCHAN_BLERC		0x3000	/* bits 13..12: RDS Block C Errors (Si4701 only) */
#define READCHAN_BLERD		0x0c00	/* bits 11..10: RDS Block B Errors (Si4701 only) */
#define READCHAN_READCHAN	0x03ff	/* bits 09..00: Read Channel */

#define RDSA			12	/* RDSA */
#define RDSA_RDSA		0xffff	/* bits 15..00: RDS Block A Data (Si4701 only) */

#define RDSB			13	/* RDSB */
#define RDSB_RDSB		0xffff	/* bits 15..00: RDS Block B Data (Si4701 only) */

#define RDSC			14	/* RDSC */
#define RDSC_RDSC		0xffff	/* bits 15..00: RDS Block C Data (Si4701 only) */

#define RDSD			15	/* RDSD */
#define RDSD_RDSD		0xffff	/* bits 15..00: RDS Block D Data (Si4701 only) */

#define MIN_CHIP_FIRMWARE_REV_FOR_VERBOSE_MODE 13


typedef struct si470x_dongle {
	uint16_t vid;
	uint16_t pid;
	const char *name;
} si470x_dongle_t;



/* USB Device ID List */
static si470x_dongle_t known_devices[] = {
    { 0x10c4, 0x818a, "Silicon Labs USB FM Radio Reference Design" },
    { 0x10c4, 0x818a, "ADS/Tech FM Radio Receiver (formerly Instant FM Music)" },
    { 0x06e1, 0xa155, "KWorld USB FM Radio SnapMusic Mobile 700 (FM700)" },
    { 0x1b80, 0xd700, "Sanei Electric, Inc. FM USB Radio (DealExtreme.com PCear)" },
    { 0x10c5, 0x819a, "Sanei Electric, Inc. FM USB Radio (DealExtreme.com PCear)" },
};

struct si470x_dev {
	hid_device *devh;
	
	/* Silabs internal registers (0..15) */
	uint16_t registers[RADIO_REGISTER_NUM];
	
	/* scratch page */
	unsigned char software_version;
	unsigned char hardware_version;
	
	/* radio configuration (in kHz) */
	int channel_spacing;
	int band_bottom;
	int band_top;
	
	/* description */
	wchar_t product_name[80];
	wchar_t vendor_name[80];
	uint16_t pid, vid;
	
	int chip_firmware_rev;
};


static int channel_to_frequency(si470x_dev_t *radio, uint16_t channel) {
	return radio->band_bottom + radio->channel_spacing * channel;
}

static uint16_t frequency_to_channel(si470x_dev_t *radio, int frequency) {
	return (uint16_t) ( (frequency - radio->band_bottom) / radio->channel_spacing);
}


/*
 * find_known_device - scans the list of known devices for a VID/PID pair
 */
static si470x_dongle_t *find_known_device(uint16_t vid, uint16_t pid)
{
	unsigned int i;
	si470x_dongle_t *device = NULL;

	for (i = 0; i < sizeof(known_devices)/sizeof(si470x_dongle_t); i++ ) {
		if (known_devices[i].vid == vid && known_devices[i].pid == pid) {
			device = &known_devices[i];
			break;
		}
	}

	return device;
}




/**************************************************************************
 * General Driver Functions - REGISTER_REPORTs
 **************************************************************************/

/*
 * si470x_get_report - receive a HID report
 */
static int si470x_get_report(si470x_dev_t *radio, unsigned char *buf, int size)
{
	int retval;

	retval = hid_get_feature_report(radio->devh, buf, size);

	if (retval < 0)
		dev_warn(
			"si470x_get_report: hid_get_feature_report returned %d\n",
			retval);
	return retval;
}


/*
 * si470x_set_report - send a HID report
 */
static int si470x_set_report(si470x_dev_t *radio, unsigned char *buf, int size)
{
	int retval;

    retval = hid_send_feature_report(radio->devh, buf, size);

	if (retval < 0)
		dev_warn(
			"si470x_set_report: hid_send_feature_report returned %d\n",
			retval);
	return retval;
}


/*
 * si470x_get_register - read register
 */
static int si470x_get_register(si470x_dev_t *radio, int regnr)
{
	unsigned char buf[EP0_REPORT_SIZE]; //REGISTER_REPORT_SIZE];
	int retval;

	memset(buf, 0, EP0_REPORT_SIZE);

	buf[0] = REGISTER_REPORT(regnr);

	retval = si470x_get_report(radio, (void *) &buf, sizeof(buf));

	if (retval >= 0)
		radio->registers[regnr] = get_unaligned_be16(&buf[1]);

	return (retval < 0) ? -EINVAL : 0;
}


/*
 * si470x_set_register - write register
 */
static int si470x_set_register(si470x_dev_t *radio, int regnr)
{
	unsigned char buf[EP0_REPORT_SIZE]; //REGISTER_REPORT_SIZE];
	int retval;

	memset(buf, 0, EP0_REPORT_SIZE);

	buf[0] = REGISTER_REPORT(regnr);
	put_unaligned_be16(radio->registers[regnr], &buf[1]);

	retval = si470x_set_report(radio, (void *) &buf, sizeof(buf));

	return (retval < 0) ? -EINVAL : 0;
}



/**************************************************************************
 * General Driver Functions - ENTIRE_REPORT
 **************************************************************************/

/*
 * si470x_get_all_registers - read entire registers
 */
/*
static int si470x_get_all_registers(si470x_dev_t *radio)
{
	unsigned char buf[ENTIRE_REPORT_SIZE];
	int retval;
	unsigned char regnr;

	memset(buf, 0, EP0_REPORT_SIZE);
	buf[0] = ENTIRE_REPORT;

	retval = si470x_get_report(radio, (void *) &buf, sizeof(buf));

	if (retval >= 0)
		for (regnr = 0; regnr < RADIO_REGISTER_NUM; regnr++)
			radio->registers[regnr] = get_unaligned_be16(
				&buf[regnr * RADIO_REGISTER_SIZE + 1]);

	return (retval < 0) ? -EINVAL : 0;
}
*/


// 2012-06-18 - for now use this dummy version, as I cannot
// figure out why it doesn't work on Windows. Maybe the
// report size is not correct?
static int si470x_get_all_registers(si470x_dev_t *radio)
{
    for(int i=0; i<RADIO_REGISTER_NUM; i++) {
        si470x_get_register(radio, i);
    }
}


/*
 * si470x_get_rds_registers - read RDS registers
 */
static int si470x_get_rds_registers(si470x_dev_t *radio)
{
	unsigned char buf[RDS_REPORT_SIZE];
	int retval;
	unsigned char regnr;

	buf[0] = RDS_REPORT;

	retval = hid_read(radio->devh, (void *) &buf, sizeof(buf));

	if (retval >= 0) {
        if(retval != 1 + RDS_REGISTER_NUM * RADIO_REGISTER_SIZE) {
            return -90 - retval;
        }

		for (regnr = 0; regnr < RDS_REGISTER_NUM; regnr++) {
			radio->registers[RADIO_REGISTER_NUM-RDS_REGISTER_NUM+regnr] = get_unaligned_be16(
				&buf[regnr * RADIO_REGISTER_SIZE + 1]);
		}
	}

	return (retval < 0) ? -EINVAL : 0;
}


/**************************************************************************
 * General Driver Functions - LED_REPORT
 **************************************************************************/

/*
 * si470x_set_led_state - sets the led state
 */
int si470x_set_led_state(si470x_dev_t *radio,
		unsigned char led_state)
{
	unsigned char buf[EP0_REPORT_SIZE]; //LED_REPORT_SIZE];
	int retval;
	
	memset(buf, 0, EP0_REPORT_SIZE);

	buf[0] = LED_REPORT;
	buf[1] = LED_COMMAND;
	buf[2] = led_state;

	retval = si470x_set_report(radio, (void *) &buf, sizeof(buf));

	return (retval < 0) ? -EINVAL : 0;
}



/**************************************************************************
 * General Driver Functions - SCRATCH_REPORT
 **************************************************************************/

/*
 * si470x_get_scratch_versions - gets the scratch page and version infos
 */
int si470x_get_scratch_page_versions(si470x_dev_t *radio)
{
	unsigned char buf[SCRATCH_REPORT_SIZE];
	int retval;

	buf[0] = SCRATCH_REPORT;

	retval = si470x_get_report(radio, (void *) &buf, sizeof(buf));

	if (retval < 0)
		dev_warn("si470x_get_scratch: "
			"si470x_get_report returned %d\n", retval);
	else {
		radio->software_version = buf[1];
		radio->hardware_version = buf[2];
		printf("Versions: sw=%d, hw=%d\n", buf[1], buf[2]);
	}

	return (retval < 0) ? -EINVAL : 0;
}



/**************************************************************************
 * RDS Driver Functions
 **************************************************************************/

/*
 * si470x_read_rds - read RDS data, and other real-time RX information
 */
int si470x_read_rds(si470x_dev_t *radio, si470x_tunerdata_t *data) {
    int retval;
	static int cleared = 0;
	static int count = 0;
	static uint16_t old_rds[4] = {0, 0, 0, 0};
    
    retval = si470x_get_rds_registers(radio);
    if(retval < 0) return retval;

    /* RDS data */    
    data->block[0] = radio->registers[RDSA];
    data->block[1] = radio->registers[RDSB];
    data->block[2] = radio->registers[RDSC];
    data->block[3] = radio->registers[RDSD];

    /* Error info */
    if(radio->chip_firmware_rev >= MIN_CHIP_FIRMWARE_REV_FOR_VERBOSE_MODE) {
        /* Verbose mode */
        data->bler[0] = (radio->registers[STATUSRSSI] & STATUSRSSI_BLERA) >> 9;
        data->bler[1] = (radio->registers[READCHAN] & READCHAN_BLERB) >> 14;
        data->bler[2] = (radio->registers[READCHAN] & READCHAN_BLERC) >> 12;
        data->bler[3] = (radio->registers[READCHAN] & READCHAN_BLERD) >> 10;
    } else {
        /* Standard mode */
        /* If one or more errors, mark the BLER of any block as "errorful" (3) */
        int bler = ((radio->registers[STATUSRSSI] & STATUSRSSI_RDSE) >> 9) > 0 ? 3 : 0;
        
        data->bler[0] =
        data->bler[1] =
        data->bler[2] =
        data->bler[3] = bler;
    }

/*
    printf("%04X/%d %04X/%d %04X/%d %04X/%d     STATUS=%04X\n",
        data->block[0], data->bler[0],
        data->block[1], data->bler[1],
        data->block[2], data->bler[2],
        data->block[3], data->bler[3],
        radio->registers[SYSCONFIG1]);
*/

    data->sync = (radio->registers[STATUSRSSI] & STATUSRSSI_RDSS) != 0;
    data->stereo = (radio->registers[STATUSRSSI] & STATUSRSSI_ST) != 0;
    data->rssi = (radio->registers[STATUSRSSI] & STATUSRSSI_RSSI);
    data->frequency = channel_to_frequency(radio, radio->registers[READCHAN] & READCHAN_READCHAN);
    
    /* If STC (seek/tune complete) or SF (seek fail) then stop tuning or seeking */
    if(radio->registers[STATUSRSSI] & (STATUSRSSI_STC | STATUSRSSI_SF)) {
        radio->registers[POWERCFG] &= ~POWERCFG_SEEK;
    	si470x_set_register(radio, POWERCFG);
        radio->registers[CHANNEL] &= ~CHANNEL_TUNE;
        si470x_set_register(radio, POWERCFG);
    }

/* Should suppress this? TODO (this only detects if RDS is enabled)
    if ((radio->registers[SYSCONFIG1] & SYSCONFIG1_RDS) == 0) {
        cleared = 1;
        return -2; // TODO FIXME No RDS group available???
    }
*/

/*
    if (radio->chip_firmware_rev >= MIN_CHIP_FIRMWARE_REV_FOR_VERBOSE_MODE &&
    (radio->registers[STATUSRSSI] & STATUSRSSI_RDSS) == 0) {*/
    if((radio->registers[STATUSRSSI] & STATUSRSSI_RDSR) == 0) {
        cleared = 1;
        //printf("<!> ");
        return -3; // TODO FIXME Not synced
    }
    //printf("<Rdy> ");


    cleared = 0;

    for(int i=0; i<4; i++) {
        if(old_rds[i] != data->block[i]) {
            cleared = 1;
            old_rds[i] = data->block[i];
        }
    }
    
    if(cleared) return 0; else return -5;
/*    
    count++;
    
    if(cleared) {
        cleared = 0;
        count = 0;
        return -5;
    } else {
        if(count == 1) {
            return 0;
        }
        return -4;
    }
*/
}



/*
 * si470x_open - try to find an Si470x device and open it
 */
int si470x_open(si470x_dev_t **out_dev, uint32_t index) {
	si470x_dev_t *dev = NULL;
	uint32_t device_count = 0;

	dev = malloc(sizeof(si470x_dev_t));
	if (NULL == dev)
		return -ENOMEM;

	memset(dev, 0, sizeof(si470x_dev_t));

    struct hid_device_info *devs = hid_enumerate(0, 0);
    struct hid_device_info *cur_dev = devs;
	while(cur_dev) {
	    si470x_dongle_t *sidev = find_known_device(
	        cur_dev->vendor_id, 
	        cur_dev->product_id);
	    if(sidev) {
	        
		    printf("[%i] %04X:%04X %s V='%ls' P='%ls' SN='%ls'\n", 
		        device_count, 
    	        cur_dev->vendor_id, 
	            cur_dev->product_id,
                sidev->name,
                cur_dev->manufacturer_string,
                cur_dev->product_string,
                cur_dev->serial_number);
			device_count++;
			
            if (index == device_count - 1) {
            /*
                wcsncpy(dev->product_name, cur_dev->product_string, sizeof(dev->product_name)-1);
                dev->product_name[sizeof(dev->product_name)-1] = '\0';

                wcsncpy(dev->vendor_name, cur_dev->manufacturer_string, sizeof(dev->vendor_name)-1);
                dev->vendor_name[sizeof(dev->vendor_name)-1] = '\0';

                dev->pid = cur_dev->product_id;
                dev->vid = cur_dev->vendor_id;
            */

                break;
            }
		}
		
		cur_dev = cur_dev->next;
	}

    if(cur_dev) {
    	dev->devh = hid_open(cur_dev->vendor_id, cur_dev->product_id, NULL);
    	if (!dev->devh) {
    		/* If we get here, a device was listed but could not be opened. This indicates a permission problem. */
        	printf("Opening device %04X:%04X failed. Make sure you have the necessary permissions.\n", cur_dev->vendor_id, cur_dev->product_id);
    		return -1; // TODO should we use different return values for different errors here?
    	}
    } else {
    	printf("No Si470x device found!\n");
		return -1;
    }

	hid_free_enumeration(devs);
	
	*out_dev = dev;
	
	return 0;
}


/*
 * si470x_start - configure the device and start operating
 */
int si470x_start(si470x_dev_t *dev, uint8_t space, uint8_t band, uint8_t de) {
    if(si470x_get_all_registers(dev) < 0) return -1;

    /* Store chip/dongle version info */
    dev->chip_firmware_rev = dev->registers[CHIPID] & CHIPID_FIRMWARE;

	/* powercfg */
	dev->registers[POWERCFG] =
		POWERCFG_DMUTE | POWERCFG_ENABLE | POWERCFG_RDSM;
	if(si470x_set_register(dev, POWERCFG) < 0) return -2;

	/* sysconfig 1 */
	dev->registers[SYSCONFIG1] = 
		SYSCONFIG1_RDS;		/* RDS on */
	if(de) dev->registers[SYSCONFIG1] |= SYSCONFIG1_DE;
	if(si470x_set_register(dev, SYSCONFIG1) < 0) return -3;

	/* sysconfig 2 */
	dev->registers[SYSCONFIG2] =
		(16  << 8) |				/* SEEKTH */
		((band  << 6) & SYSCONFIG2_BAND)  |	/* BAND */
		((space << 4) & SYSCONFIG2_SPACE) |	/* SPACE */
		15;					/* VOLUME (max) */
	if(si470x_set_register(dev, SYSCONFIG2) < 0) return -4;

    /* sysconfig 3 */
    dev->registers[SYSCONFIG3] = 
        6 << 4 |   /* bits 07..04: Seek SNR Threshold */
        10;        /* bits 03..00: Seek FM Impulse Detection Threshold */
	if(si470x_set_register(dev, SYSCONFIG3) < 0) return -5;

	switch(space) {
		case CHANNEL_SPACING_50_KHZ: dev->channel_spacing = 50; break;
		case CHANNEL_SPACING_200_KHZ: dev->channel_spacing = 200; break;
		default:
		case CHANNEL_SPACING_100_KHZ: dev->channel_spacing = 100; break;
	}
	
	switch(band) {
		case BAND_76_108:
			dev->band_bottom = 76000;
			dev->band_top = 108000;
			break;

		case BAND_76_90:
			dev->band_bottom = 76000;
			dev->band_top = 90000;
			break;

		default:
		case BAND_87_108:
			dev->band_bottom = 87500;
			dev->band_top = 108000;
	}
	
	si470x_get_scratch_page_versions(dev);
	
	
	
	/* Part number */
	int pn = (dev->registers[DEVICEID] & DEVICEID_PN)>>12;
	
	printf("Part number:     0x%01X", pn);
	
	switch(pn) {
	    case 0x1: printf(" (Si4700/01/02/03)\n"); break;
	    default: printf(" (Unknown)\n"); break;
	}
	
	/* Manufacturer */
	int mfg = dev->registers[DEVICEID] & DEVICEID_MFGID;
	
	printf("Manufacturer ID: 0x%03X", mfg);
	
	switch(mfg) {
	    case 0x242: printf(" (Silicon Labs)\n"); break;
	    default: printf(" (Unknown)\n"); break;
	}
	
	/* Chip revisions */
	printf("Chip revision:   %c\n", 64 + ((dev->registers[CHIPID] & CHIPID_REV)>>10));

    /* Device type */
    int dev_type = (dev->registers[CHIPID] & CHIPID_DEV)>>6;

	printf("Device type:     0x%01X", dev_type);
	
	switch(dev_type) {
	    case 0x0: printf(" (Si4700)\n"); break;
	    case 0x1: printf(" (Si4702)\n"); break;
	    case 0x8: printf(" (Si4701)\n"); break;
	    case 0x9: printf(" (Si4703)\n"); break;
	}

	printf("Si470x firmware: %d\n", dev->registers[CHIPID] & CHIPID_FIRMWARE);
	
	return 0;
}






/*
 * si470x_get_freq - get the frequency
 */
int si470x_get_freq(si470x_dev_t *radio, int *freq)
{
	unsigned short chan;
	int retval;

	/* read channel */
	retval = si470x_get_register(radio, READCHAN);
	chan = radio->registers[READCHAN] & READCHAN_READCHAN;

	/* Frequency (MHz) = Spacing (kHz) x Channel + Bottom of Band (MHz) */
	*freq = channel_to_frequency(radio, chan);

	return retval;
}



/*
 * si470x_set_chan - set the channel
 */
int si470x_set_freq(si470x_dev_t *radio, int freq)
{
	int retval;
	unsigned short chan = frequency_to_channel(radio, freq);

	/* start tuning */
	radio->registers[CHANNEL] &= ~CHANNEL_CHAN;
	radio->registers[CHANNEL] |= CHANNEL_TUNE | chan;
	retval = si470x_set_register(radio, CHANNEL);
	if (retval < 0)
		goto done;

    do {
        retval = si470x_get_register(radio, STATUSRSSI);
        if (retval < 0)
            goto stop;
    } while ((radio->registers[STATUSRSSI] & STATUSRSSI_STC) == 0);

	if ((radio->registers[STATUSRSSI] & STATUSRSSI_STC) == 0)
		dev_warn("tune does not complete\n");

stop:
	/* stop tuning */
	radio->registers[CHANNEL] &= ~CHANNEL_TUNE;
	retval = si470x_set_register(radio, CHANNEL);

done:
	return retval;
}


/*
 * si470x_start_seek - start seek
 */
int si470x_start_seek(si470x_dev_t *radio,
		unsigned int wrap_around, unsigned int seek_upward)
{
	int retval = 0;

	/* start seeking */
	radio->registers[POWERCFG] |= POWERCFG_SEEK;
	if (wrap_around == 1)
		radio->registers[POWERCFG] &= ~POWERCFG_SKMODE;
	else
		radio->registers[POWERCFG] |= POWERCFG_SKMODE;
	if (seek_upward == 1)
		radio->registers[POWERCFG] |= POWERCFG_SEEKUP;
	else
		radio->registers[POWERCFG] &= ~POWERCFG_SEEKUP;
	retval = si470x_set_register(radio, POWERCFG);
	
	return retval;
}


/*
 * si470x_device_info - get vendor and product name / ID
 */
/*
void si470x_device_info(si470x_dev_t *radio, wchar_t **vname, wchar_t **pname, uint16_t *vid, uint16_t *pid) {
    *vname = radio->vendor_name;
    *pname = radio->product_name;
    *vid = radio->vid;
    *pid = radio->pid;
}
*/
