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

const RadioApp = {
  data() {
    return {
      freq: 0,
      pi: 0,
      ps: "",
      rt: "",
      dongle: null,
      groupStats: []
    }
  },
  computed: {
    formattedPI() {
      return this.pi.toString(16).toUpperCase().padStart(4, 0);
    },
    dongleText() {
      return this.dongle == null ? 
        "No dongle connected. Click here to start." :        
        "Dongle connected: " + this.dongle;
    },
  }
}

let vm;
function start_radio_app() {
  vm = Vue.createApp(RadioApp).mount("#radio_app");
}

let dongle;
async function init() {
  if ("hid" in navigator) {
    console.log("WebHID API is supported.");
    const [device] = await navigator.hid.requestDevice({ filters: supportedDevices });
    console.log("Device", device);

    dongle = new Si470x(device, BAND_87_108, CHANNEL_SPACING_100_KHZ);
    const decoder = new RdsDecoder(vm);
    dongle.rdsEventListener = decoder;

    await dongle.init();
    await dongle.tune(99900);
  }
}
