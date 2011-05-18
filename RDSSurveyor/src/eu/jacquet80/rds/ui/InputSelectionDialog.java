package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Semaphore;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import eu.jacquet80.rds.core.StreamLevelDecoder;
import eu.jacquet80.rds.img.Image;
import eu.jacquet80.rds.input.GroupReader;
import eu.jacquet80.rds.input.HexFileGroupReader;
import eu.jacquet80.rds.input.LiveAudioBitReader;
import eu.jacquet80.rds.input.USBFMRadioGroupReader;

public class InputSelectionDialog extends JFrame implements ActionListener {
	private static final long serialVersionUID = 2745048916894636582L;

	private final JButton
		btnAudio = new JButton("<html><b>External decoder</b><br>(through the sound card)</html>", Image.MICROPHONE),
		btnTuner = new JButton("<html><b>Internal tuner or USB key</b></html>", Image.USBKEY),
		btnFile = new JButton("<html><b>File</b><br>(playback)</html>", Image.OPEN),
		btnTCP = new JButton("<html><b>Network</b><br>(Web site or TCP)</html>", Image.NETWORK);
	
	private final JButton[] buttons = {btnAudio, btnTuner, btnFile, btnTCP};
	
	private GroupReader choice;
	private final Semaphore choiceDone = new Semaphore(0);
	
	public InputSelectionDialog() {
		super("Choose an input method:");
		
		JPanel main = new JPanel();
		
		main.setLayout(new GridLayout(4, 1, 10, 10));
		add(main, BorderLayout.CENTER);
		main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		for(JButton b : buttons) {
			main.add(b);
			b.addActionListener(this);
			b.setMargin(new Insets(10, 30, 10, 30));
			b.setIconTextGap(30);
			b.setAlignmentX(JButton.LEFT_ALIGNMENT);
			b.setHorizontalAlignment(JButton.LEFT);
		}
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				choice = null;
				choiceDone.release();
			}
		});
		
		pack();
		
		// center the frame
		setLocationRelativeTo(null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		try {
			if(source == btnAudio) {
				choice = new StreamLevelDecoder(System.out, new LiveAudioBitReader());
				choiceDone.release();
			} else if(source == btnTuner) {
				choice = new USBFMRadioGroupReader();
				((USBFMRadioGroupReader)choice).init();
				choiceDone.release();
			} else if(source == btnFile) {
				JFileChooser fc = new JFileChooser();
				if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
					choice = new HexFileGroupReader(fc.getSelectedFile());
					choiceDone.release();
				}
			} else if(source == btnTCP) {
				Thread t = new Thread() {
					public void run() {
						choice = NetworkOpenDialog.dialog();
						choiceDone.release();
					}
				};
				t.start();
			}
		} catch(Throwable exc) {
			JOptionPane.showMessageDialog(this, exc.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public GroupReader makeChoice() {
		setVisible(true);
		do {
			choiceDone.acquireUninterruptibly();
		} while(choice == null);
		setVisible(false);
		
		return choice;
	}
}
