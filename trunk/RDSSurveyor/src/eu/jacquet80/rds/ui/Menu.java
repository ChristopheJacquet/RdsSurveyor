package eu.jacquet80.rds.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import eu.jacquet80.rds.RDSSurveyor;
import eu.jacquet80.rds.core.DecoderShell;
import eu.jacquet80.rds.input.GroupReader;

public class Menu {
	private static enum Item {
		Open("Open stream..."), 
		QUIT("Quit");
		
		private final String label;
		private final JMenuItem menuItem;

		Item(String label) {
			this.label = label;
			this.menuItem = new JMenuItem(label);
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
		 bar.add(buildMenu("File", Item.Open, Item.QUIT));
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
						switch(Item.forMenuItem(item)) {
						case QUIT:
							System.exit(0);
							break;
						case Open: {
							InputSelectionDialog dialog = new InputSelectionDialog();
							GroupReader reader = dialog.makeChoice();
							DecoderShell ds = new DecoderShell(reader, RDSSurveyor.nullConsole);
							mainWindow.setReader(ds.getLog(), reader);
							try {
								ds.process();
							} catch (IOException exc) {
								JOptionPane.showMessageDialog(mainWindow, "Error while processing stream: " + exc, "Probable bug encountered", JOptionPane.ERROR_MESSAGE);
							}
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
