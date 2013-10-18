package eu.jacquet80.rds.ui.app;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.app.oda.ITunesTagging;

@SuppressWarnings("serial")
public class ITunesTaggingPanel extends AppPanel {

	private Vector<?> tagList;
	private JList list;
	
	public ITunesTaggingPanel(Application app) {
		super(new BorderLayout());

		setApplication(app);
		
		tagList = ((ITunesTagging)app).getTagList();
		list = new JList( model );
		add(new JScrollPane(list), BorderLayout.CENTER);
	}
		
	
	@Override
	protected void doNotifyChange() {
		model.notifyChange();
	}

	private TagModel model = new TagModel();
	
	class TagModel extends AbstractListModel {

		@Override
		public int getSize() {
			return tagList.size();
		}

		@Override
		public Object getElementAt(int index) {
			return tagList.get(index);
		}
		
		private void notifyChange() {
			fireContentsChanged(this, 0, tagList.size()-1);
		}
	}
}
