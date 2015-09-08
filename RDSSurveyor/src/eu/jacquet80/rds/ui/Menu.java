package eu.jacquet80.rds.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import eu.jacquet80.rds.core.DecoderShell;
import eu.jacquet80.rds.input.GroupReader;

public class Menu {
	private static enum Item {
		OPEN("Open stream..."), 
		QUIT("Quit"),
		WINDOW_PLAYLIST("Playlist"),
		WINDOW_GROUP("Group analyzer");
		
		
		private final String label;
		private final JMenuItem menuItem;

		private Item(String label) {
			this.label = label;
			this.menuItem = new JMenuItem(label);
		}
		
		private Item(String label, boolean checked) {
			this.label = label;
			this.menuItem = new JCheckBoxMenuItem(label, checked);
		}
		
		public JMenuItem getMenuItem() {
			return menuItem;
		}
		
		public static Item forMenuItem(JMenuItem mi) {
			for(Item i : values()) {
				if(i.menuItem == mi)
					return i;
			}
			return null;
		}
	}
	
	private static final JMenu buildMenu(String label, Item ... items) {
		JMenu menu = new JMenu(label);
		for(Item item : items) {
			JMenuItem i = item.getMenuItem();
			i.addActionListener(listener);
			menu.add(i);
		}
		return menu;
	}
	
	public static final JMenuBar buildMenuBar() {
		 JMenuBar bar = new JMenuBar();
		 bar.add(buildMenu("File", Item.OPEN, Item.QUIT));
		 bar.add(buildMenu("Window", Item.WINDOW_GROUP, Item.WINDOW_PLAYLIST));
		 return bar;
	}
	
	private static MainWindow mainWindow;
	
	public static void setWindow(MainWindow mainWindow) {
		Menu.mainWindow = mainWindow;
	}

	private static ActionListener listener = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() instanceof JMenuItem) {
				final JMenuItem item = (JMenuItem) e.getSource();
				
				// the actions cannot be performed in AWT's dispatch thread
				new Thread() {
					public void run() {
						JFrame window = null;
						
						switch(Item.forMenuItem(item)) {
						case QUIT:
							System.exit(0);
							break;
						case OPEN: {
							InputSelectionDialog dialog = new InputSelectionDialog();
							GroupReader reader = dialog.makeChoice();
							if (reader != null) {
								mainWindow.setReader(DecoderShell.instance.getLog(), reader);
								DecoderShell.instance.process(reader, dialog.live);
							}
							break;
						}
						case WINDOW_GROUP: {
							window = mainWindow.getDumpDisplay();
							window.setVisible(true);
							break;
						}
						case WINDOW_PLAYLIST: {
							window = mainWindow.getPlaylistWindow();
							window.setVisible(true);
							break;
						}

						default:
						}
					}
				}.start();
			}
		}
	};
}
