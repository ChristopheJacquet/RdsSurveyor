package eu.jacquet80.rds.ui.app;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.app.oda.CatalunyaRadioTDC;

@SuppressWarnings("serial")
public class CatalunyaRadioTDCPanel extends AppPanel {
	private final JList<Object> list;
	private final Vector<String> messageList;
	private final NewsMessageListModel listModel;
	
	public CatalunyaRadioTDCPanel(Application app) {
		super(new BorderLayout());

		setApplication(app);
		
		messageList = ((CatalunyaRadioTDC)app).getMessageList();
		listModel = new NewsMessageListModel();
		list = new JList<Object>(listModel);
		add(new JScrollPane(list), BorderLayout.CENTER);
		
		new SwingWorker<Object, Object>() {

			@Override
			protected Object doInBackground() throws Exception {
				while(true) {
					Thread.sleep(1000);
					listModel.fireNewItems();
				}
			}
			
		}.execute();
	}
	
	@Override
	protected void doNotifyChange() {
	}

	private class NewsMessageListModel extends DefaultListModel<Object> {
		@Override
		public int getSize() {
			return messageList.size();
		}
		

		@Override
		public Object getElementAt(int index) {
			return messageList.get(index);
		}
		
		private int oldSize;
		
		public void fireNewItems() {
			final int newSize = getSize();
			if(newSize > oldSize) {
				fireIntervalAdded(this, oldSize, newSize-1);
				oldSize = newSize;
			}
		}

	}
}
