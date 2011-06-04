#!/usr/bin/python

# RDS V4L Server
# Copyright Christophe Jacquet, 2011
# See rds-surveyor.sourceforge.net
# This file is released under the GPL v2

# For documentation see http://linux.bytesex.org/v4l2/API.html

import SocketServer
import select
from fcntl import ioctl
import v4l2

# use v4l2 bindings from http://pypi.python.org/pypi/v4l2/

dev = "/dev/radio0"

FREQUENCY_MIN =  87500
FREQUENCY_MAX = 108000
FREQUENCY_STEP = 100
FREQUENCY_FRAC = 16

def readfreq(dev, outfile):
	freq = v4l2.v4l2_frequency(0)
	ioctl(dev, v4l2.VIDIOC_G_FREQUENCY, freq)
	outfile.write("% Freq: " + str(freq.frequency / FREQUENCY_FRAC) + "\n")

class RDSHandler(SocketServer.StreamRequestHandler):
	def setup(self):
		SocketServer.StreamRequestHandler.setup(self)
		print self.client_address, " connected."

	def handle(self):
		radio = open(dev, "r")
		expectedOffset = 0
		out = ""
		while 1:
			s = select.select([radio, self.rfile], [], [], 1)
			if len(s[0]) > 0:
				if s[0][0] == radio:
					data = radio.read(3)
					blockOffset = ord(data[2]) & 0x7
					if blockOffset == 4:
						blockOffset = 2	# C' offset
					if blockOffset != expectedOffset:
						print "Skip offset ", blockOffset, ", expecting ", expectedOffset
					else:
						# reject corrected blocks
						if ord(data[2]) & 0xC0 == 0:
							out = out + ("%02X" % ord(data[1])) + ("%02X" % ord(data[0])) + " "
						else:
							out = out + "---- "
						# if at the end of a group, print it out
						if blockOffset == 3:
							self.wfile.write(out + "\n")
							expectedOffset = 0
							out = ""
						else:
							expectedOffset = expectedOffset + 1
						#print data, " read."
				else:
					#print "reading from socket: "
					data = self.rfile.readline().strip().upper()
					parts = data.split()
					cmd = parts[0]
					print cmd + " / " + data
					if cmd == 'QUIT':
						return
					if cmd == "SET_FREQ":
						if len(parts) == 2:
							frequency = int(parts[1])
							if frequency >= FREQUENCY_MIN and frequency <= FREQUENCY_MAX and frequency % FREQUENCY_STEP == 0:
								freq = v4l2.v4l2_frequency(0)
								freq.frequency = frequency * FREQUENCY_FRAC
								ioctl(radio, v4l2.VIDIOC_S_FREQUENCY, freq)
								readfreq(radio, self.wfile)
							else:
								self.wfile.write("% Invalid frequency: " + str(frequency) + "\n")
						else:
							self.wfile.write("% Command FREQ requires one agument.\n")
					if cmd == "GET_FREQ":
						readfreq(radio, self.wfile)
					if cmd == "GET_SIGNAL":
						tuner = v4l2.v4l2_tuner(0)
						ioctl(radio, v4l2.VIDIOC_G_TUNER, tuner)
						self.wfile.write("% Signal: " + str(tuner.signal) + "\n")
					if cmd == "SEEK":
						seek = v4l2.v4l2_hw_freq_seek(0)
						seek.wrap_around = 1
						if len(parts) == 2 and parts[1] == "DOWN":
							seek.seek_upward = 0
						else:
							seek.seek_upward = 1
						ioctl(radio, v4l2.VIDIOC_S_HW_FREQ_SEEK, seek)
						readfreq(radio, self.wfile)
					if cmd == "UP" or cmd == "DOWN":
						freq = v4l2.v4l2_frequency(0)
						ioctl(radio, v4l2.VIDIOC_G_FREQUENCY, freq)
						f = freq.frequency / FREQUENCY_FRAC
						if cmd == "UP":
							f = f + FREQUENCY_STEP
						else:
							f = f - FREQUENCY_STEP
						if f > FREQUENCY_MAX:
							f = FREQUENCY_MIN
						if f < FREQUENCY_MIN:
							f = FREQUENCY_MAX
						freq.frequency = f * FREQUENCY_FRAC
						ioctl(radio, v4l2.VIDIOC_S_FREQUENCY, freq)
						readfreq(radio, self.wfile)
					if cmd == "ID":
						cp = v4l2.v4l2_capability()
						ioctl(radio, v4l2.VIDIOC_QUERYCAP, cp)
						self.wfile.write("% Id: " + cp.card + "\n")


	def finish(self):
		print self.client_address, " disconnected."


SocketServer.ThreadingTCPServer.allow_reuse_address = True
server = SocketServer.ThreadingTCPServer(('', 8750), RDSHandler)
server.serve_forever()
