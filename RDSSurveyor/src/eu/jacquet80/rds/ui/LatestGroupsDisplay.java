package eu.jacquet80.rds.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import eu.jacquet80.rds.util.RingBuffer;

@SuppressWarnings("serial")
public class LatestGroupsDisplay extends JPanel {
	private final int width;

	private final RingBuffer<Integer> history;

	public LatestGroupsDisplay(int width) {
		this.width = width;
		this.history = new RingBuffer<Integer>(Integer.class, width, -1);
		clear();
		setPreferredSize(new Dimension(width, 25));
		setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
		setBackground(Color.BLACK);
	}

	public void addGroup(int mask) {
		history.addValue(mask);
	}

	@Override
	protected void paintComponent(Graphics g_) {
		super.paintComponent(g_);

		Graphics2D g = (Graphics2D) g_;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.GREEN);

		Insets insets = this.getBorder().getBorderInsets(this);
		int height = getHeight() - insets.bottom - insets.top;

		synchronized(history) {
			for(int x = 1; x <= this.width; x++) {
				int mask = history.getValue(x);
				if(mask != -1) {
					for(int i=0, m=1; i<=3; i++, m*=2) {
						g.setColor((mask & m) > 0 ? Color.GREEN : Color.RED);
						g.drawLine(this.width-x, insets.top + height-1 - 6*i, this.width-x, insets.top + height-1 - 6*i - 5);
					}
				}
			}
		}
	}

	public void clear() {
		history.clear();
	}
}

