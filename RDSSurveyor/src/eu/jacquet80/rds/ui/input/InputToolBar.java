package eu.jacquet80.rds.ui.input;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import eu.jacquet80.rds.core.BitStreamSynchronizer;
import eu.jacquet80.rds.input.GroupReader;
import eu.jacquet80.rds.input.LiveAudioBitReader;
import eu.jacquet80.rds.input.RDSReader;
import eu.jacquet80.rds.input.TunerGroupReader;
import eu.jacquet80.rds.log.Log;

@SuppressWarnings("serial")
public abstract class InputToolBar extends JToolBar {
	
	private final ActionListener buttonListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			handleButtonAction(e);
		}
	};

	public static InputToolBar forReader(RDSReader reader, Log log) {
		//System.out.println("For reader: " + reader);
		if(reader instanceof TunerGroupReader) 
			return new TunerToolBar((TunerGroupReader)reader);
		else if(reader instanceof BitStreamSynchronizer) return new BitStreamSynchronizerToolBar((BitStreamSynchronizer)reader);
		else if(reader instanceof LiveAudioBitReader) return new LiveAudioToolBar((LiveAudioBitReader) reader);
		else if(reader instanceof GroupReader) return new PlaybackToolBar(log);
		else return null;
	}
	
	/**
	 * Constructs a new input tool bar with a two-line label.
	 * 
	 * @param name1 first line of the label
	 * @param name2 second line of the label
	 */
	protected InputToolBar(String name1, String name2) {
		this();
		JLabel label = new JLabel("<html>" + name1 + "<br>" + name2 + "</html>");
		label.setPreferredSize(label.getMinimumSize());
		label.setMaximumSize(label.getMinimumSize());
		add(label);
		addSeparator();
	}
	
	/**
	 * Constructs a new input tool bar without a label.
	 */
	protected InputToolBar() {
		setFloatable(false);
	}

	
	protected JButton addButton(Icon icon, String command) {
		JButton button = new JButton(icon);
		button.setActionCommand(command);
		button.addActionListener(buttonListener);
		add(button);
		
		return button;
	}

	protected JButton addButton(String caption, Icon icon, String command) {
		JButton button = new JButton(caption, icon);
		button.setActionCommand(command);
		button.addActionListener(buttonListener);
		add(button);
		
		return button;
	}

	protected JButton addButton(String caption, String command) {
		JButton button = new JButton(caption);
		button.setActionCommand(command);
		button.addActionListener(buttonListener);
		add(button);
		
		return button;
	}
	
	protected abstract void handleButtonAction(ActionEvent e);
	
	public abstract void unregister();
}
