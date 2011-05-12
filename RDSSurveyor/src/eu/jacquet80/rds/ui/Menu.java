package eu.jacquet80.rds.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class Menu {
	private static enum Item {
		DECODE("Decode"), 
		QUIT("QUIT");
		
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
		 bar.add(buildMenu("RDS", Item.DECODE, Item.QUIT));
		 return bar;
	}

	private static ActionListener listener = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() instanceof JMenuItem) {
				JMenuItem item = (JMenuItem) e.getSource();
				
				switch(Item.forMenuItem(item)) {
				case QUIT:
					System.exit(0);
					break;
				default:
				}
			}
		}
	};
}
