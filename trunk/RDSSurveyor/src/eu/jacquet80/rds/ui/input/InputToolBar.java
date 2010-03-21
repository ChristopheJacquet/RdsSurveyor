package eu.jacquet80.rds.ui.input;

import javax.swing.JLabel;
import javax.swing.JToolBar;

import eu.jacquet80.rds.input.RDSReader;
import eu.jacquet80.rds.input.USBFMRadioGroupReader;

public class InputToolBar extends JToolBar {
	private static final long serialVersionUID = -1085007696842056447L;

	public static InputToolBar forReader(RDSReader reader) {
		if(reader instanceof USBFMRadioGroupReader) return new USBFMRadioToolBar((USBFMRadioGroupReader)reader);
		else return null;
	}
	
	protected InputToolBar(String name1, String name2) {
		JLabel label = new JLabel("<html>" + name1 + "<br>" + name2 + "</html>");
		label.setPreferredSize(label.getMinimumSize());
		label.setMaximumSize(label.getMinimumSize());
		add(label);
		addSeparator();
	}

}
