/*
Si470x USBFMRADIO-RD WebHID driver.

Copyright (c) 2020  Christophe Jacquet (WebHID Javascript driver)
Copyright (c) 2012  Christophe Jacquet (HIDAPI userspace driver)
Copyright (c) 2009  Tobias Lorenz (original Linux driver)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

"use strict";

/**************************************************************************
 * Register Definitions
 **************************************************************************/
const RADIO_REGISTER_SIZE = 2;	/* 16 register bit width */
const RADIO_REGISTER_NUM = 16;	/* DEVICEID   ... RDSD */
const RDS_REGISTER_NUM = 6;	/* STATUSRSSI ... RDSD */

const DEVICEID = 0;	/* Device ID */
const DEVICEID_PN = 0xf000;	/* bits 15..12: Part Number */
const DEVICEID_MFGID = 0x0fff;	/* bits 11..00: Manufacturer ID */

const CHIPID = 1;	/* Chip ID. Note: this field changed with firmware r16 */
const CHIPID_REV = 0xfc00;	/* bits 15..10: Silicon revision */
const CHIPID_DEV = 0x03C0;	/* bits 09..06: Device */
const CHIPID_FIRMWARE = 0x003f;	/* bits 05..00: Firmware Version */

const POWERCFG = 2;	/* Power Configuration */
const POWERCFG_DSMUTE = 0x8000;	/* bits 15..15: Softmute Disable */
const POWERCFG_DMUTE = 0x4000;	/* bits 14..14: Mute Disable */
const POWERCFG_MONO = 0x2000;	/* bits 13..13: Mono Select */
const POWERCFG_RDSM = 0x0800;	/* bits 11..11: RDS Mode (Si4701 only) */
const POWERCFG_SKMODE = 0x0400;	/* bits 10..10: Seek Mode */
const POWERCFG_SEEKUP = 0x0200;	/* bits 09..09: Seek Direction */
const POWERCFG_SEEK = 0x0100;	/* bits 08..08: Seek */
const POWERCFG_DISABLE = 0x0040;	/* bits 06..06: Powerup Disable */
const POWERCFG_ENABLE = 0x0001;	/* bits 00..00: Powerup Enable */

const CHANNEL = 3;	/* Channel */
const CHANNEL_TUNE = 0x8000;	/* bits 15..15: Tune */
const CHANNEL_CHAN = 0x03ff;	/* bits 09..00: Channel Select */

const SYSCONFIG1 = 4;	/* System Configuration 1 */
const SYSCONFIG1_RDSIEN = 0x8000;	/* bits 15..15: RDS Interrupt Enable (Si4701 only) */
const SYSCONFIG1_STCIEN = 0x4000;	/* bits 14..14: Seek/Tune Complete Interrupt Enable */
const SYSCONFIG1_RDS = 0x1000;	/* bits 12..12: RDS Enable (Si4701 only) */
const SYSCONFIG1_DE = 0x0800;	/* bits 11..11: De-emphasis (0=75us 1=50us) */
const SYSCONFIG1_AGCD = 0x0400;	/* bits 10..10: AGC Disable */
const SYSCONFIG1_BLNDADJ = 0x00c0;	/* bits 07..06: Stereo/Mono Blend Level Adjustment */
const SYSCONFIG1_GPIO3 = 0x0030;	/* bits 05..04: General Purpose I/O 3 */
const SYSCONFIG1_GPIO2 = 0x000c;	/* bits 03..02: General Purpose I/O 2 */
const SYSCONFIG1_GPIO1 = 0x0003;	/* bits 01..00: General Purpose I/O 1 */

const SYSCONFIG2 = 5;	/* System Configuration 2 */
const SYSCONFIG2_SEEKTH = 0xff00;	/* bits 15..08: RSSI Seek Threshold */
const SYSCONFIG2_BAND = 0x00C0;	/* bits 07..06: Band Select */
const SYSCONFIG2_SPACE = 0x0030;	/* bits 05..04: Channel Spacing */
const SYSCONFIG2_VOLUME = 0x000f;	/* bits 03..00: Volume */

const SYSCONFIG3 = 6;	/* System Configuration 3 */
const SYSCONFIG3_SMUTER = 0xc000;	/* bits 15..14: Softmute Attack/Recover Rate */
const SYSCONFIG3_SMUTEA = 0x3000;	/* bits 13..12: Softmute Attenuation */
const SYSCONFIG3_SKSNR = 0x00f0;	/* bits 07..04: Seek SNR Threshold */
const SYSCONFIG3_SKCNT = 0x000f;	/* bits 03..00: Seek FM Impulse Detection Threshold */

const TEST1 = 7;	/* Test 1 */
const TEST1_AHIZEN = 0x4000;	/* bits 14..14: Audio High-Z Enable */

const TEST2 = 8;	/* Test 2 */
/* TEST2 only contains reserved bits */

const BOOTCONFIG = 9;	/* Boot Configuration */
/* BOOTCONFIG only contains reserved bits */

const STATUSRSSI = 10;	/* Status RSSI */
const STATUSRSSI_RDSR = 0x8000;	/* bits 15..15: RDS Ready (Si4701 only) */
const STATUSRSSI_STC = 0x4000;	/* bits 14..14: Seek/Tune Complete */
const STATUSRSSI_SF = 0x2000;	/* bits 13..13: Seek Fail/Band Limit */
const STATUSRSSI_AFCRL = 0x1000;	/* bits 12..12: AFC Rail */
const STATUSRSSI_RDSE = 0x0E00;  /* bits 11..09: RDS Errors (firmware <= 12 or "standard mode") */
/* The next two fields are supported only in "verbose mode", available with firmware revs >12 */
const STATUSRSSI_RDSS = 0x0800;	/* bits 11..11: RDS Synchronized (Si4701 only) */
const STATUSRSSI_BLERA = 0x0600;	/* bits 10..09: RDS Block A Errors (Si4701 only) */
const STATUSRSSI_ST = 0x0100;	/* bits 08..08: Stereo Indicator */
const STATUSRSSI_RSSI = 0x00ff;	/* bits 07..00: RSSI (Received Signal Strength Indicator) */

const READCHAN = 11;	/* Read Channel */
const READCHAN_BLERB = 0xc000;	/* bits 15..14: RDS Block D Errors (Si4701 only) */
const READCHAN_BLERC = 0x3000;	/* bits 13..12: RDS Block C Errors (Si4701 only) */
const READCHAN_BLERD = 0x0c00;	/* bits 11..10: RDS Block B Errors (Si4701 only) */
const READCHAN_READCHAN = 0x03ff;	/* bits 09..00: Read Channel */

const RDSA = 12;	/* RDSA */
const RDSA_RDSA = 0xffff;	/* bits 15..00: RDS Block A Data (Si4701 only) */

const RDSB = 13;	/* RDSB */
const RDSB_RDSB = 0xffff;	/* bits 15..00: RDS Block B Data (Si4701 only) */

const RDSC = 14;	/* RDSC */
const RDSC_RDSC = 0xffff;	/* bits 15..00: RDS Block C Data (Si4701 only) */

const RDSD = 15;	/* RDSD */
const RDSD_RDSD = 0xffff;	/* bits 15..00: RDS Block D Data (Si4701 only) */

const MIN_CHIP_FIRMWARE_REV_FOR_VERBOSE_MODE = 13;

/**************************************************************************
 * Values
 **************************************************************************/
/* Spacing (kHz) */
/* 0: 200 kHz (USA, Australia) */
/* 1: 100 kHz (Europe, Japan) */
/* 2:  50 kHz */
const CHANNEL_SPACING_50_KHZ = 2;
const CHANNEL_SPACING_100_KHZ = 1;
const CHANNEL_SPACING_200_KHZ = 0;

/* Bottom of Band (MHz) */
/* 0: 87.5 - 108 MHz (USA, Europe)*/
/* 1: 76   - 108 MHz (Japan wide band) */
/* 2: 76   -  90 MHz (Japan) */
const BAND_87_108 = 0;
const BAND_76_108 = 1;
const BAND_76_90 = 2;


/**************************************************************************
 * USB HID Reports
 **************************************************************************/

/* Reports 1-16 give direct read/write access to the 16 Si470x registers */
/* with the (REPORT_ID - 1) corresponding to the register address across USB */
/* endpoint 0 using GET_REPORT and SET_REPORT */
const REGISTER_REPORT_SIZE = (RADIO_REGISTER_SIZE + 1);
const REGISTER_REPORT = (reg) => ((reg) + 1);

/* Report 17 gives direct read/write access to the entire Si470x register */
/* map across endpoint 0 using GET_REPORT and SET_REPORT */
const ENTIRE_REPORT_SIZE = (RADIO_REGISTER_NUM * RADIO_REGISTER_SIZE + 1);
const ENTIRE_REPORT = 17;

/* Report 18 is used to send the lowest 6 Si470x registers up the HID */
/* interrupt endpoint 1 to Windows every 20 milliseconds for status */
const RDS_REPORT_SIZE = (RDS_REGISTER_NUM * RADIO_REGISTER_SIZE + 1);
const RDS_REPORT = 18;
const RDS_REPORT_BASE = STATUSRSSI; // First register in RDS_REPORT is RSSI.

/* Report 19: LED state */
const LED_REPORT_SIZE = 3;
const LED_REPORT = 19;

/* Report 19: stream */
const STREAM_REPORT_SIZE = 3;
const STREAM_REPORT = 19;

/* Report 20: scratch */
const SCRATCH_PAGE_SIZE = 63;
const SCRATCH_REPORT_SIZE = (SCRATCH_PAGE_SIZE + 1);
const SCRATCH_REPORT = 20;

const supportedDevices = [
  { vendorId: 0x10c4, productId: 0x818a },  // Silicon Labs USB FM Radio Reference Design.
  { vendorId: 0x06e1, productId: 0xa155 },  // KWorld USB FM Radio SnapMusic Mobile 700 (FM700).
  { vendorId: 0x1b80, productId: 0xd700 },  // Sanei Electric FM USB Radio (DealExtreme.com PCear).
  { vendorId: 0x10c5, productId: 0x819a },  // Sanei Electric FM USB Radio (DealExtreme.com PCear).
];

class Si470x {
  #oldRegs = new Uint16Array(6);

  constructor(device, band, channelSpacing) {
    this.device = device;
    this.band = band;
    this.channelSpacing = channelSpacing;
    this.rdsEventListener = null;

    switch(band) {
      case BAND_76_90:
      case BAND_76_108:
        this.bandBottom = 76000;
        break;
  
      default:
      case BAND_87_108:
        this.bandBottom = 87500;
    }

    switch(channelSpacing) {
      case CHANNEL_SPACING_50_KHZ: this.channelSpacingKhz = 50; break;
      case CHANNEL_SPACING_200_KHZ: this.channelSpacingKhz = 200; break;
      default:
      case CHANNEL_SPACING_100_KHZ: this.channelSpacingKhz = 100; break;
    }
  }

  channelToFrequency(channel) {
    return this.bandBottom + this.channelSpacingKhz * channel;
  }
  
  frequencyToChannel(frequency) {
    return Math.floor((frequency - this.bandBottom) / this.channelSpacingKhz);
  }

  async init() {
    await this.device.open();

    const deviceIdReg = await this.getRegister(DEVICEID);
    console.log("DEVICEID: ", deviceIdReg.toString(16));

    const chipIdReg = await this.getRegister(CHIPID);
    this.firmware = chipIdReg & CHIPID_FIRMWARE;
    console.log("CHIPID: chip=%d, firmware=%d", (chipIdReg & CHIPID_DEV)>>6, this.firmware);

    await this.setRegister(POWERCFG, POWERCFG_DMUTE | POWERCFG_ENABLE | POWERCFG_RDSM);
    await this.setRegister(SYSCONFIG1, SYSCONFIG1_RDS);
    await this.setRegister(SYSCONFIG2, 
      (16  << 8) |                                        // SEEKTH
      ((this.band  << 6) & SYSCONFIG2_BAND)  |            // BAND
      ((this.channelSpacing << 4) & SYSCONFIG2_SPACE) |   // SPACE
      15);                                                // VOLUME (max)
    await this.setRegister(SYSCONFIG3,
      6 << 4 |    // bits 07..04: Seek SNR Threshold
      10);        // bits 03..00: Seek FM Impulse Detection Threshold

    this.device.addEventListener("inputreport", event => this.processRdsReport(event));
  }

  async setRegister(regNum, regVal) {
    const dataView = new DataView(new ArrayBuffer(2));
    dataView.setUint16(0, regVal, /* Big Endian */ false);
    await this.device.sendFeatureReport(REGISTER_REPORT(regNum), dataView);
  }

  async getRegister(regNum) {
    const data = await this.device.receiveFeatureReport(REGISTER_REPORT(regNum));
    return data.getUint16(1, false);
  }

  async tune(freq) {
    // This starts tuning, the main event loop will ack the tune complete event.
    const chan = this.frequencyToChannel(freq);
    await this.setRegister(CHANNEL, CHANNEL_TUNE | (CHANNEL_CHAN & chan));
  }

  async seek(wrapAround, seekUpward) {
    // This starts seeking, the main event loop will ack the seek complete/fail event.
    let powerCfg = await this.getRegister(POWERCFG);

    powerCfg |= POWERCFG_SEEK;

    if (wrapAround)
      powerCfg &= ~POWERCFG_SKMODE;
    else
      powerCfg |= POWERCFG_SKMODE;

    if (seekUpward)
      powerCfg |= POWERCFG_SEEKUP;
    else
      powerCfg &= ~POWERCFG_SEEKUP;

    await this.setRegister(POWERCFG, powerCfg);
  }

  sendRdsEvent(event) {
    if (this.rdsEventListener == null) return;
    this.rdsEventListener.processRdsEvent(event);
  }

  async processRdsReport(event) {
    const { data, device, reportId } = event;

    if (reportId != RDS_REPORT) {
      return;
    }

    let regs = new Uint16Array(6);
    let regsChanged = false;
    for (let i=0; i<6; i++) {
      regs[i] = data.getUint16(i*2, /* Big Endian */ false);
      if (this.#oldRegs[i] != regs[i]) {
        regsChanged = true;
      }
    }
    this.#oldRegs = regs;

    if (!regsChanged) return;

    const freq = this.channelToFrequency(regs[READCHAN - RDS_REPORT_BASE] & READCHAN_READCHAN);

    // If STC (seek/tune complete) or SF (seek fail) then stop tuning or seeking.
    if (regs[STATUSRSSI - RDS_REPORT_BASE] & (STATUSRSSI_STC | STATUSRSSI_SF)) {
      if (regs[STATUSRSSI - RDS_REPORT_BASE] & STATUSRSSI_STC) {
        console.log("Seek/tune complete.");
      }
      
      if (regs[STATUSRSSI - RDS_REPORT_BASE] & STATUSRSSI_SF) {
        console.log("Seek failed.");
      }

      let powerCfg = await this.getRegister(POWERCFG);
      powerCfg &= ~POWERCFG_SEEK;
      this.setRegister(POWERCFG, powerCfg);
      let channel = await this.getRegister(CHANNEL);
      channel &= ~CHANNEL_TUNE;
      await this.setRegister(CHANNEL, channel);
    }

    const dongleInfo = this.device.productName;

    if ((regs[STATUSRSSI - RDS_REPORT_BASE] & STATUSRSSI_RDSR) == 0) {
      // Do nothing if no new RDS data is ready.
      console.log(freq/1000);
      this.sendRdsEvent({
        dongle: dongleInfo,
        freq: freq,
        ok: [false, false, false, false],
      });
      return;
    }

    const block = Uint16Array.from([
      regs[RDSA - RDS_REPORT_BASE],
      regs[RDSB - RDS_REPORT_BASE],
      regs[RDSC - RDS_REPORT_BASE],
      regs[RDSD - RDS_REPORT_BASE]]);
    
    const ok = this.firmware >= MIN_CHIP_FIRMWARE_REV_FOR_VERBOSE_MODE ?
    [
      (regs[STATUSRSSI - RDS_REPORT_BASE] & STATUSRSSI_BLERA) == 0,
      (regs[READCHAN - RDS_REPORT_BASE] & READCHAN_BLERB) == 0,
      (regs[READCHAN - RDS_REPORT_BASE] & READCHAN_BLERC) == 0,
      (regs[READCHAN - RDS_REPORT_BASE] & READCHAN_BLERD) == 0,
    ] : ((regs[STATUSRSSI - RDS_REPORT_BASE] & STATUSRSSI_RDSE) == 0 ?
    [true, true, true, true] : [false, false, false, false]);

    let str = "";
    for (let i=0; i<4; i++) {
      str += ok[i] ? block[i].toString(16).toUpperCase().padStart(4, 0) + " " : "---- ";
    }

    console.log(freq/1000, ": ", str);
    this.sendRdsEvent({
      dongle: dongleInfo,
      freq: freq,
      ok: ok,
      block: block,
    })
  }
}
