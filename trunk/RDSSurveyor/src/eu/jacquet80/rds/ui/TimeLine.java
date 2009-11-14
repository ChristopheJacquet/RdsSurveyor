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

package eu.jacquet80.rds.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.JPanel;

import eu.jacquet80.rds.log.ClockTime;
import eu.jacquet80.rds.log.EONReturn;
import eu.jacquet80.rds.log.EONSwitch;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.LogMessage;
import eu.jacquet80.rds.log.LogMessageVisitor;
import eu.jacquet80.rds.log.StationLost;
import eu.jacquet80.rds.log.StationTuned;

public class TimeLine extends JPanel {
	private static final long serialVersionUID = 8291282167930195278L;

	private final Log log;
	private float scale = .003f;

	public TimeLine(Log log) {
		this.log = log;
	}

	public void update() {
		if(log.empty()) setPreferredSize(new Dimension(100, 100));
		else setPreferredSize(new Dimension((int)(scale * log.getLastTime()), 100));
		revalidate();
		repaint();
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		LogMessagePainter painter = new LogMessagePainter(g);
		for(LogMessage m : log.messages()) {
			m.accept(painter);
		}
	}
	
	private int toScale(int bitTime) {
		return (int)(bitTime * scale);
	}
	
	private final static DateFormat timeFormat = new SimpleDateFormat("HH:mm");

	class LogMessagePainter implements LogMessageVisitor {
		private final Graphics2D g;

		public LogMessagePainter(Graphics g) {
			this.g = (Graphics2D)g;
		}

		public void visit(ClockTime clockTime) {
			int x = toScale(clockTime.getBitTime());
			
			g.setColor(Color.BLACK);
			g.drawLine(x, 0, x, 15);
			g.drawString(timeFormat.format(clockTime.getTime()), x, 15);
		}

		public void visit(EONReturn eonReturn) {
			int x = toScale(eonReturn.getBitTime());
			g.setColor(Color.BLACK);
			g.drawLine(x, 45, x, 50);
			g.drawLine(x-2, 45, x, 48);
			g.drawLine(x+2, 45, x, 48);
			
			if(eonReturn.getON() != null) g.drawString(Integer.toHexString(eonReturn.getON().getPI()).toUpperCase(), x+1, 49);
		}

		public void visit(EONSwitch eonSwitch) {
			int x = toScale(eonSwitch.getBitTime());
			g.setColor(Color.BLACK);
			g.drawLine(x, 45, x, 49);
			g.drawLine(x-2, 46, x, 49);
			g.drawLine(x+2, 46, x, 49);
			
			if(eonSwitch.getON() != null) g.drawString(eonSwitch.getON().getPS(), x+1, 49);
		}

		public void visit(StationLost stationLost) {
			// TODO Auto-generated method stub

		}

		public void visit(StationTuned stationTuned) {
			int x1 = toScale(stationTuned.getBitTime());
			int x2 = toScale(stationTuned.getTimeLost());

			g.setColor(Color.GREEN);
			g.fillRect(x1, 40, x2-x1, 5);

			g.setColor(Color.BLACK);
			g.drawRect(x1, 40, x2-x1, 5);

			String ps = stationTuned.getStation().getPS() + " <" + Integer.toHexString(stationTuned.getStation().getPI()).toUpperCase() + ">"; //+ stationTuned.getBitTime() + ", " + stationTuned.getTimeLost() + " * " + x1 + ", " + x2;
			g.drawString(ps, (x1 + x2 - g.getFontMetrics().stringWidth(ps))/2, 39);

		}

	}
}