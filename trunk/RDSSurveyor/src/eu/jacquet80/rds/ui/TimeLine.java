/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009, 2010 Christophe Jacquet

 This file is part of RDS Surveyor.

 RDS Surveyor is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 RDS Surveyor is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser Public License for more details.

 You should have received a copy of the GNU Lesser Public License
 along with RDS Surveyor.  If not, see <http://www.gnu.org/licenses/>.

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
import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.EONReturn;
import eu.jacquet80.rds.log.EONSwitch;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.LogMessage;
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
		for(int i = 0; i < log.messageCount(); i++) {
			LogMessage m = log.getMessage(i);
			m.accept(painter);
		}
	}
	
	private int toScale(int bitTime) {
		return (int)(bitTime * scale);
	}
	
	private final static DateFormat timeFormat = new SimpleDateFormat("HH:mm");

	class LogMessagePainter extends DefaultLogMessageVisitor {
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
			
			if(eonSwitch.getON() != null) g.drawString(eonSwitch.getON().getPS().toString(), x+1, 49);
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