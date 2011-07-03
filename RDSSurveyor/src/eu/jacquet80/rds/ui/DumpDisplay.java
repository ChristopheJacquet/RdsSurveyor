package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.BoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;

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
	private int numLines = 1;
	private final DumpPanel contents = new DumpPanel();
	private final JScrollBar scroll = new JScrollBar(JScrollBar.VERTICAL);
	private final JTextField search = new JTextField();
	private final BoundedRangeModel scrollModel;
	private final static Font font = new Font("monospaced", Font.PLAIN, 12);
	
	private final static Color DARK_CYAN = Color.CYAN.darker();
	private final static Color DARK_ORANGE = new Color(0xDD, 0x66, 0x00);
	private final static Color ODA_COLOR = new Color(0, 130, 0);
	private final static Color HIGHLIGHT_COLOR = new Color(255, 255, 220);
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
	
	private final static Pattern NEWLINE_PATTERN = Pattern.compile("\n");
	private final static String TAB_STRING = "                      ";
	
	private Set<Log> logsImRegisteredAt = new HashSet<Log>();
	
	public DumpDisplay(int scrollBackSize) {
		super("Group analyzer");
		
		this.groups = new GroupReceived[scrollBackSize];
		
		this.setLayout(new BorderLayout());
		this.add(contents, BorderLayout.CENTER);
		this.add(scroll, BorderLayout.EAST);
		this.add(search, BorderLayout.NORTH);
		
		scrollModel = scroll.getModel();
		
		search.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				contents.repaint();
			}
			
			@Override
			public void keyReleased(KeyEvent e) {}
			
			@Override
			public void keyPressed(KeyEvent e) {}
		});
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setPreferredSize(new Dimension((int)(2*dim.getWidth()/3), (int)(2*dim.getHeight()/3)));
		pack();

		new Thread() {
			{
				setName("RDSSurveyor-DumpDisplay-updater");
			}
			
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
		}

		boolean down = scrollModel.getValue() == scrollModel.getMaximum() - scrollModel.getExtent();
		scrollModel.setMaximum(size);
		// move to the new line only if the scroll bar was all the way down:
		if(down) scrollModel.setValue(size-numLines);
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
			
			addMouseWheelListener(new ScrollListener());
		}
		
		@Override
		protected void paintComponent(Graphics g_) {
			super.paintComponent(g_);
						
			String searchText = search.getText();
			if("".equals(searchText)) searchText = null;
			
			Graphics2D g = (Graphics2D) g_;
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics();
			int lineHeight = fm.getHeight();
			
			numLines = getHeight() / lineHeight;
			// correct the extent, because numLines cannot be set before the frame is painted once
			if(scrollModel.getExtent() != numLines) {
				scrollModel.setExtent(numLines);
				if(scrollModel.getValue() > size - numLines) scrollModel.setValue(size - numLines);
			}
			
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
				// lineIndex is the "intuitive" index between 0 and the max value
				// However the actual index of groups[] must take into account
				// the fact that groups[] is a circular buffer
				GroupReceived currentGroup = groups[(firstIndex + lineIndex) % groups.length];
				if(currentGroup == null) {
					System.err.println("DumpDisplay: null group at lineIndex="
							+ lineIndex + ", firstIndex=" + firstIndex + ", size=" + size);
					break;
				}

				String allLines = currentGroup.toString();
				String[] lines = NEWLINE_PATTERN.split(allLines);

				if(searchText != null && allLines.contains(searchText)) {
					g.setColor(HIGHLIGHT_COLOR);
					g.fillRect(0, y - fm.getMaxAscent(), getWidth(), lineHeight * lines.length);
				}
				
				int type = currentGroup.getGroupType();
				g.setColor(type == -1 ? Color.GRAY : groupColors[type]);
				
				for(String l : lines) {
					g.drawString(l.replace("\t", TAB_STRING), 0, y);
					
					countRemaining--;
					y += lineHeight;
				}
				lineIndex++;
			}
		}
	}
	
	private class ScrollListener implements MouseWheelListener {

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int lines = e.getWheelRotation() * 3;
			synchronized(DumpDisplay.this) {
				scrollModel.setValue(scrollModel.getValue() + lines);
			}
		}
		
	}

	public void resetForNewLog(Log log) {
		// register at the log, but avoid registering twice at the same log
		// (otherwise each line read would appear twice (or more) in the
		// window...)
		if(! logsImRegisteredAt.contains(log)) {
			log.addNewMessageListener(new DefaultLogMessageVisitor() {
				@Override
				public void visit(GroupReceived groupReceived) {
					addGroup(groupReceived);
				}
			});
			
			logsImRegisteredAt.add(log);
		}
		
		synchronized(this) {
			scrollModel.setExtent(0);
			scrollModel.setMinimum(0);
			scrollModel.setMaximum(0);
			scrollModel.setValue(0);
			
			firstIndex = 0;
			nextIndex = 0;
			size = 0;
		}
	}
}
