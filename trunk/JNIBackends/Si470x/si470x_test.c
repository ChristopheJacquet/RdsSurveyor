/*
Si470x userspace driver - Test console program
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


#include <stdio.h>
#include <string.h>
#include <stdlib.h>


#include "si470x_hidapi.h"

#define DEFAULT_FREQ 95400 // default frequency

int main(int argc, char **argv) {
    si470x_dev_t *dev;
    char ps[] = "        ";
    char rt[] = "                                                                ";
    
    if(si470x_open(&dev, 0) != 0) {
        printf("Device not found.\n");
        return 1;
    }

    printf("**0\n");
    
    //si470x_get_scratch_page_versions(dev);
    
    //printf("Versions: HW=%i, SW=%i\n", dev->software_version, dev->hardware_version);
    
    si470x_set_led_state(dev, BLINK_RED_LED);

    printf("**1\n");

    /*
    si470x_get_all_registers(dev);

    for(int i=0; i<RADIO_REGISTER_NUM; i++) {
        printf("Reg#%02i = %04X\n", i, dev->registers[i]);
    }
    */
    
    int ret = si470x_start(dev, CHANNEL_SPACING_100_KHZ, BAND_87_108, 1);
    //printf("Start, ret = %d\n", ret);
    
    int freq;
    if(argc < 2) {
        freq = DEFAULT_FREQ;
        printf("No frequency given, using default frequency.\n");
    } else {
        freq = strtol(argv[1], NULL, 10);
        if(freq == 0) {
            freq = DEFAULT_FREQ;
            printf("Bad frequency given, using default frequency.\n");
        }
    }
    
    si470x_set_freq(dev, freq);
    
    ret = si470x_get_freq(dev, &freq);
    printf("Freq = %f\n", freq/1000.);
    //printf("spacing=%f bottom=%f top=%f\n", dev->channel_spacing, dev->band_bottom, dev->band_top);

    si470x_tunerdata_t data;
    uint16_t *rds = data.block, *bler = data.bler;

    while(1) {
        //printf("%.1f\n", data.frequency/1000.);
        int retval = si470x_read_rds(dev, &data);
        if(retval < 0) {
            //printf("%i ", retval);
            //fflush(stdout);
        } else {
            for(int i=0; i<4; i++) {
                if(bler[i] == 0) {
                    printf("%04X ", rds[i]);
                } else {
                    printf("---- ");
                    //printf("--%i- ", bler[i]);
                }
            }
            
            if(bler[1] == 0) {
                int type = rds[1] >> 12;
                int ver = (rds[1] >> 11) & 1;
                printf("   Type %d%c", type, 'A' + ver);
            
                if(type == 0 && bler[3] == 0) {
                    int pos = rds[1] & 3;
                    ps[2*pos] = rds[3] >> 8;
                    ps[2*pos+1] = rds[3] & 0xFF;
                    printf("    PS@%d, PS='%s'", pos, ps);
                } else if(type == 2 && ver == 0) {
                    int pos = rds[1] & 0xF;
                    
                    if(bler[2] == 0) {
                        rt[4*pos] = rds[2] >> 8;
                        rt[4*pos+1] = rds[2] & 0xFF;
                    }
                    if(bler[3] == 0) {
                        rt[4*pos+2] = rds[3] >> 8;
                        rt[4*pos+3] = rds[3] & 0xFF;
                    }
                    printf("    RT@%d, RT='%s'", pos, rt);
                }
            }
            
            
            printf("\n");
            fflush(stdout);
        }
    }        
}