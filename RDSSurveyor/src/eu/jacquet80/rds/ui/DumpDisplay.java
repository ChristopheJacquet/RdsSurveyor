package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataListener;

import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.EndOfStream;
import eu.jacquet80.rds.log.GroupReceived;
import eu.jacquet80.rds.log.Log;

public class DumpDisplay extends JFrame {
	private static final long serialVersionUID = -5330439172208705930L;

	private final Log log;
	private final List<GroupReceived> groups = new ArrayList<GroupReceived>();
	private final DumpListModel model;
	
	public DumpDisplay(Log log) {
		super("Group analyzer");
		
		this.log = log;
			
		model = new DumpListModel(groups);
		
		final JList lst = new JList(model);
		lst.setFont(new Font("monospaced", Font.PLAIN, lst.getFont().getSize()));
		add(new JScrollPane(lst), BorderLayout.CENTER);
		pack();
		
		log.addNewMessageListener(new DefaultLogMessageVisitor() {
			@Override
			public void visit(GroupReceived groupReceived) {
				groups.add(groupReceived);
				
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						model.update();
						lst.ensureIndexIsVisible(groups.size());
					}
				});
			}
			
			@Override
			public void visit(EndOfStream endOfStream) {
				/*
				System.out.print("********* FIN *********");
				model.update();
				lst.repaint();
				*/
			}
		});

	}
	
	private static class DumpListModel extends AbstractListModel {
		private static final long serialVersionUID = -2320190288582420477L;
		
		private final List<GroupReceived> groups;
		
		public DumpListModel(List<GroupReceived> groups) {
			this.groups = groups;
		}

		@Override
		public Object getElementAt(int index) {
			return "<html>" + groups.get(index).getAnalysis().replaceAll("\n", "<br>").replaceAll("\\s", "&nbsp;") + "</html>";
		}

		@Override
		public int getSize() {
			return groups.size();
		}
		
		public void update() {
			fireIntervalAdded(this, groups.size()-1, groups.size()-1);
		}
		
	}
}
