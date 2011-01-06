package eu.jacquet80.rds.ui.input;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import eu.jacquet80.rds.input.GroupReader;
import eu.jacquet80.rds.input.RDSReader;
import eu.jacquet80.rds.input.TunerGroupReader;
import eu.jacquet80.rds.log.Log;

public abstract class InputToolBar extends JToolBar {
	private static final long serialVersionUID = -1085007696842056447L;
	
	private final ActionListener buttonListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			handleButtonAction(e);
		}
	};

	public static InputToolBar forReader(RDSReader reader, Log log) {
		if(reader instanceof TunerGroupReader) 
			return new TunerToolBar((TunerGroupReader)reader);
		else if(reader instanceof GroupReader) return new PlaybackToolBar(log);
		else return null;
	}
	
	protected InputToolBar(String name1, String name2) {
		setFloatable(false);
		JLabel label = new JLabel("<html>" + name1 + "<br>" + name2 + "</html>");
		label.setPreferredSize(label.getMinimumSize());
		label.setMaximumSize(label.getMinimumSize());
		add(label);
		addSeparator();
	}

	
	protected void addButton(Icon icon, String command) {
		JButton button = new JButton(icon);
		button.setActionCommand(command);
		button.addActionListener(buttonListener);
		add(button);
	}
	
	protected abstract void handleButtonAction(ActionEvent e);
}
