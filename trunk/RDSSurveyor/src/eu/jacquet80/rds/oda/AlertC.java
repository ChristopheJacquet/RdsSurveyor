/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009 Christophe Jacquet

 Permission is hereby granted, free of charge, to any person
 obtaining a copy of this software and associated documentation
 files (the "Software"), to deal in the Software without
 restriction, including without limitation the rights to use,
 copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the
 Software is furnished to do so, subject to the following
 conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 OTHER DEALINGS IN THE SOFTWARE.
*/

package eu.jacquet80.rds.oda;

public class AlertC extends ODA {
	public static AlertC INSTANCE = new AlertC();
	
	private AlertC() {
	}

	@Override
	public void receiveGroup(int type, int version, int[] blocks) {
		if(type == 3 && version == 0) {
			int var = (blocks[2]>>14) & 0x3;
			System.out.print("Sys.Info v=" + var+ ", ");
			
			if(var == 0) {
				int ltn = (blocks[2]>>6) & 0x3F;
				int afi = (blocks[2]>>5) & 1;
				int mode = (blocks[2]>>4) & 1;
				int scopeI = (blocks[2]>>3) & 1;
				int scopeN = (blocks[2]>>2) & 1;
				int scopeR = (blocks[2]>>1) & 1;
				int scopeU = (blocks[2]) & 1;
				
				System.out.printf("LTN=%d, AFI=%d, Mode=%d, MGS[scope]=%c%c%c%c ",
						ltn, afi, mode,
						scopeI==1 ? 'I' : ' ',
						scopeN==1 ? 'N' : ' ',
						scopeR==1 ? 'R' : ' ',
						scopeU==1 ? 'U' : ' ');
			} else if(var == 1) {
				int gap = (blocks[2]>>12) & 3;
				int sid = (blocks[2]>>6) & 0x3F;
				int ta = (blocks[2]>>4) & 3;
				int tw = (blocks[2]>>2) & 3;
				int td = blocks[2] & 3;
				
				System.out.printf("Gap=%d, SID=%d, for mode 1: Ta=%d, Tw=%d, Td=%d", gap, sid, ta, tw, td);
			}
		}
		
		else
		if(type == 8 && version == 0) {
			int x4 = (blocks[1] & 0x10)>>4;
			System.out.print("X4=" + x4 + " ");
			
			if(x4 == 0) {
				int single_group = (blocks[1] & 0x8)>>3;
				if(single_group == 1) {
					System.out.print("single-group: ");
					int dp = blocks[1] & 7;
					int div = (blocks[2]>>15) & 1;
					int dir = (blocks[2]>>14) & 1;
					int extent = (blocks[2]>>11) & 7;
					int event = blocks[2] & 0x7FF;
					int location = blocks[3];
					System.out.print("DP=" + dp + ", DIV=" + div + ", DIR=" + dir + ", ext=" + extent + ", evt=" + event + ", loc=" + location);
				}
				else {
					int idx = blocks[1] & 7;
					System.out.print("multi-group [" + idx + "]: ");
					int first = (blocks[2]>>15) & 1;
					if(first == 1) {
						System.out.print("1st, ");
						
						int dir = (blocks[2]>>14) & 1;
						int extent = (blocks[2]>>11) & 7;
						int event = blocks[2] & 0x7FF;
						int location = blocks[3];
						System.out.print("DIR=" + dir + ", ext=" + extent + ", evt=" + event + ", loc=" + location);

					} else {
						int second = (blocks[2]>>14) & 1;
						int remaining = (blocks[2]>>12) & 3;
						if(second == 1) {
							System.out.print("2nd");
						} else System.out.print("later");
						System.out.print("[rem=" + remaining + "]");
					}
				}
			} else {
				int addr = blocks[1] & 0xF;
				System.out.print("Tuning Info: ");
				switch(addr) {
				case 4: case 5: System.out.printf("Prov.name[%d]=\"%c%c%c%c\" ", 
						addr-4, (blocks[2]>>8) & 0xFF, blocks[2] & 0xFF, (blocks[3]>>8) & 0xFF, blocks[3] & 0xFF);
				break;
				default: System.out.print("addr=" + addr);
				}
			}
		}
	}

	@Override
	public String getName() {
		return "TMC/Alert-C";
	}
}
