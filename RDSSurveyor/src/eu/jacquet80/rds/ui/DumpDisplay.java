package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;

import javax.swing.BoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.GroupReceived;
import eu.jacquet80.rds.log.Log;

public class DumpDisplay extends JFrame {
	private static final long serialVersionUID = -5330439172208705930L;

	private final GroupReceived[] groups;
	private int firstIndex = 0;
	private int nextIndex = 0;
	private int size = 0;
	private boolean changed = true;
	private int numLines;
	private DumpPanel contents = new DumpPanel();
	private JScrollBar scroll = new JScrollBar(JScrollBar.VERTICAL);
	private final BoundedRangeModel scrollModel;
	private final static Font font = new Font("monospaced", Font.PLAIN, 12);
	
	private final static Color DARK_CYAN = Color.CYAN.darker();
	private final static Color DARK_ORANGE = new Color(0xDD, 0x66, 0x00);
	private final static Color ODA_COLOR = new Color(0, 130, 0);
	private final static Color[] groupColors = new Color[] {
		Color.BLACK, Color.BLACK,			// 0
		DARK_ORANGE, DARK_ORANGE,			// 1
		Color.BLUE, Color.BLUE,				// 2
		DARK_ORANGE, ODA_COLOR,				// 3
		Color.RED, ODA_COLOR,				// 4
		ODA_COLOR, ODA_COLOR,				// 5
		ODA_COLOR, ODA_COLOR,				// 6
		ODA_COLOR, ODA_COLOR,				// 7
		ODA_COLOR, ODA_COLOR,				// 8
		ODA_COLOR, ODA_COLOR,				// 9
		DARK_CYAN, ODA_COLOR,				// 10
		ODA_COLOR, ODA_COLOR,				// 11
		ODA_COLOR, ODA_COLOR,				// 12
		ODA_COLOR, ODA_COLOR,				// 13
		Color.MAGENTA, Color.MAGENTA,		// 14
		ODA_COLOR, Color.BLACK				// 15
	};
	
	public DumpDisplay(Log log, int scrollBackSize) {
		super("Group analyzer");
		
		this.groups = new GroupReceived[scrollBackSize];
		
		this.setLayout(new BorderLayout());
		this.add(contents, BorderLayout.CENTER);
		this.add(scroll, BorderLayout.EAST);
		
		scrollModel = scroll.getModel();
		scrollModel.setExtent(10);
		scrollModel.setMinimum(0);
		scrollModel.setMaximum(1);
		scrollModel.setValue(0);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setPreferredSize(new Dimension((int)(2*dim.getWidth()/3), (int)(2*dim.getHeight()/3)));
		pack();

		log.addNewMessageListener(new DefaultLogMessageVisitor() {
			@Override
			public void visit(GroupReceived groupReceived) {
				addGroup(groupReceived);
			}
		});

		new Thread() {
			public void run() {
				for(;;) {
					update();
					try {
						if(changed) {
							update();
							changed = false;
						}
						sleep(100);
					} catch (InterruptedException e) {}
				}
			};
		}.start();
	}
	
	private synchronized void addGroup(GroupReceived groupReceived) {
		groups[nextIndex] = groupReceived;
		nextIndex = (nextIndex + 1) % groups.length;
		if(size == groups.length) {
			firstIndex = (firstIndex + 1) % groups.length;
		} else {
			size++;
			scrollModel.setMaximum(size);
			scrollModel.setValue(size-numLines);
			scrollModel.setExtent(numLines);
		}
		changed = true;
	}
	
	private synchronized void update() {
		repaint();
	}
	

	private class DumpPanel extends JPanel {
		private static final long serialVersionUID = -7383531173801650813L;

		public DumpPanel() {
			super();
			
			setBackground(Color.WHITE);
		}
		
		@Override
		protected void paintComponent(Graphics g_) {
			super.paintComponent(g_);
			
			Graphics2D g = (Graphics2D) g_;
			g.setFont(font);
			int lineHeight = g.getFontMetrics().getHeight();
			numLines = getHeight() / lineHeight;
			
			int y = lineHeight;
			int lineIndex;
			int countRemaining;
			if(size < numLines) {
				//lineIndex = firstIndex;
				countRemaining = size;
			} else {
				//lineIndex = (nextIndex - numLines) % groups.length;
				countRemaining = numLines;
			}
			lineIndex = scrollModel.getValue();
			
			while(lineIndex < groups.length && countRemaining > 0) {
				int type = groups[lineIndex].getGroupType();
				g.setColor(type == -1 ? Color.GRAY : groupColors[type]);
				g.drawString(groups[lineIndex].toString(), 0, y);
				
				lineIndex = (lineIndex + 1) % groups.length;
				countRemaining--;
				y += lineHeight;
			}
		}
	}
}
