/*
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

class RdsDecoder {
  pi
  ps
  rt
  freq
  rssi
  dongle
  groupStats

  constructor(vm) {
    this.vm = vm;
    this.init();
  }

  init() {
    this.pi = 0
    this.ps = Array(8).fill(" ");
    this.rt = Array(64).fill(" ");
    this.freq = 0;
    this.rssi = 0;
    this.dongle = null;
    this.groupStats = new Map();
  }

  fillTextFromBlock(text, position, block) {
    const charFromByte = byte => byte>=32 && byte<127 ? String.fromCharCode(byte) : ".";
    text[position] = charFromByte((block >> 8) & 0xFF);
    text[position+1] = charFromByte(block & 0xFF);
  }

  processRdsEvent(event) {
    let piChanged = false;
    let psChanged = false;
    let rtChanged = false;

    if (this.dongle != event.dongle) {
      this.dongle = event.dongle;
      this.vm.dongle = this.dongle;
    }

    if (this.freq != event.freq) {
      this.freq = event.freq;
      this.vm.freq = this.freq;
    }

    /*
    if (this.rssi != event.rssi) {
      this.rssi = event.rssi;
      this.vm.rssi = this.rssi;
    }
    */
    
    if (event.ok[0]) {
      const pi = event.block[0];
      if (this.pi != pi) {
        this.init();
        this.pi = pi;
        piChanged = true;
      }
    }

    if (!event.ok[1]) {
      return;
    }

    const group = (event.block[1] >> 12) & 0xF;
    const version = (event.block[1] >> 11) & 1;
    const groupName = group + (version==0 ? "A" : "B");
    if (this.groupStats.has(groupName)) {
      this.groupStats.set(groupName, this.groupStats.get(groupName) + 1);
    } else {
      this.groupStats.set(groupName, 1);
    }
    this.vm.groupStats = [...this.groupStats.entries()]
              .sort((a, b) => b[1] - a[1]).map(e => e[0] + ":\u00A0" + e[1]);
    
    if (group == 0) {   // 0A and 0B (basic tuning).
      const address = event.block[1] & 0b11;

      if (event.ok[3]) {
        this.fillTextFromBlock(this.ps, address*2, event.block[3]);
        psChanged = true;
      }
    }

    if (group == 2) {   // 2A and 2B (radiotext).
      const address = event.block[1] & 0xF;

      if (version == 0) {   // 2A.
        if (event.ok[2]) this.fillTextFromBlock(this.rt, address*4, event.block[2]);
        if (event.ok[3]) this.fillTextFromBlock(this.rt, address*4+2, event.block[3]);
      } else {   // 2B.
        if (event.ok[3]) this.fillTextFromBlock(this.rt, address*2, event.block[3]);
      }

      rtChanged = true;
    }

    let str="";
    if (piChanged) {
      str += "PI=" + this.pi.toString(16).toUpperCase().padStart(4, 0) + "  ";
      this.vm.pi = this.pi;
    }
    if (psChanged) {
      const ps = this.ps.join("");
      str += "PS=\"" + ps + "\"  ";
      this.vm.ps = ps;
    }
    if (rtChanged) {
      const rt = this.rt.join("");
      str += "RT=\"" + rt + "\"  ";
      this.vm.rt = rt;
    }
    if (str != "") {
      console.log("Decoder: " + str);
    }
  }
}