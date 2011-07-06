package eu.jacquet80.rds.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Arrays;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class BLERDisplay extends JPanel {
	private static final int GROUPS_PER_X_PIXEL = 5;
	private final int width;
	
	private final float[] history;
	private int historyNextEntry;
	private float lastValue = 0f;
	
	private final int[] currentNbOK = new int[GROUPS_PER_X_PIXEL];
	private int currentNbOKNextEntry;
	
	public BLERDisplay(int width) {
		this.width = width;
		this.history = new float[width];
		clear();
		setPreferredSize(new Dimension(width, 25));
		setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
		setBackground(Color.BLACK);
	}
	
	public synchronized void addGroup(int nbOK) {
		this.currentNbOK[this.currentNbOKNextEntry] = nbOK;
		this.currentNbOKNextEntry++;
		if(this.currentNbOKNextEntry == GROUPS_PER_X_PIXEL) {
			this.currentNbOKNextEntry = 0;
			int sum = 0;
			for(int nb : currentNbOK) {
				sum += nb;
			}
			
			this.lastValue = (2*this.lastValue + 1f - (float)(sum) / (4 * GROUPS_PER_X_PIXEL)) / 3;
			this.history[this.historyNextEntry] = this.lastValue;
			this.historyNextEntry = (this.historyNextEntry + 1) % width;
		}
	}
	
	@Override
	protected synchronized void paintComponent(Graphics g_) {
		super.paintComponent(g_);
		
		Graphics2D g = (Graphics2D) g_;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.GREEN);
		
		Insets insets = this.getBorder().getBorderInsets(this);
		int height = getHeight() - insets.bottom - insets.top;
		
		for(int x = 1; x < this.width; x++) {
			int idxPrev = (this.historyNextEntry + x - 1) % this.width;
			int idxCur = (this.historyNextEntry + x) % this.width;
			
			if(history[idxPrev] >= 0 && history[idxCur] >= 0) {
				g.drawLine(x-1, insets.top + height-1 - (int)(history[idxPrev]*height), x, insets.top + height-1 - (int)(history[idxCur]*height));
			}
		}
	}

	public synchronized void clear() {
		this.currentNbOKNextEntry = 0;
		this.historyNextEntry = 0;

		Arrays.fill(history, -1f);
	}
}
