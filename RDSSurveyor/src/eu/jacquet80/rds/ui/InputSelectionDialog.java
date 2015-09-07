package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.Semaphore;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import eu.jacquet80.rds.RDSSurveyor;
import eu.jacquet80.rds.core.BitStreamSynchronizer;
import eu.jacquet80.rds.img.Image;
import eu.jacquet80.rds.input.BitReader;
import eu.jacquet80.rds.input.FileFormatGuesser;
import eu.jacquet80.rds.input.GroupReader;
import eu.jacquet80.rds.input.LiveAudioBitReader;
import eu.jacquet80.rds.input.NativeTunerGroupReader;
import eu.jacquet80.rds.input.UnavailableInputMethod;

public class InputSelectionDialog extends JFrame implements ActionListener {
	private static final long serialVersionUID = 2745048916894636582L;

	private final JButton
		btnAudio = new JButton("<html><b>External decoder</b><br>(through the sound card)</html>", Image.MICROPHONE),
		btnTuner = new JButton("<html><b>Internal tuner or USB stick</b></html>", Image.USBKEY),
		btnFile = new JButton("<html><b>File</b><br>(playback)</html>", Image.OPEN),
		btnTCP = new JButton("<html><b>Network</b><br>(Web site or TCP)</html>", Image.NETWORK);
	
	private final JButton[] buttons = {btnTuner, btnFile, btnAudio, btnTCP};
	
	private GroupReader choice;
	public boolean live = false;
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
				BitReader br =  new LiveAudioBitReader();
				choice = new BitStreamSynchronizer(System.out, br);
				live = true;
				choiceDone.release();
			} else if(source == btnTuner) {
				String libDir = "lib";
				boolean found = false;
				String attempts = "Attempts to connect to a device:\n";
				File[] paths = new File(libDir).listFiles();
				if (paths == null)
					paths = new File[]{};
				
				for(File path : paths) {
					try {
						if("msvcrt.dll".equalsIgnoreCase(path.getName())) continue;

						choice = new NativeTunerGroupReader(path.getAbsolutePath());
						live = true;
						found = true;
						break;
					} catch(UnavailableInputMethod exc) {
						// try the next one
						System.out.println(exc.getMessage());
						attempts += exc.getMessage() + "\n";
					}
				}
				
				if(! found) {
					throw new RuntimeException("No available device found\n" + attempts);
				}

				
				choiceDone.release();
			} else if(source == btnFile) {
				String defaultPath = RDSSurveyor.preferences.get(RDSSurveyor.PREF_LAST_DIR, null);
				JFileChooser fc = new JFileChooser(defaultPath);
				if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
					choice = FileFormatGuesser.createReader(fc.getSelectedFile());
					choiceDone.release();
					RDSSurveyor.preferences.put(RDSSurveyor.PREF_LAST_DIR, fc.getSelectedFile().getParent());
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
