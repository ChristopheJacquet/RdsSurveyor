package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.BoxLayout;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JSeparator;

public class PrefsWindow extends JFrame {

	private JPanel contentPane;
	private final ButtonGroup gpFlavour = new ButtonGroup();
	private final ButtonGroup gpStartup = new ButtonGroup();
	private final ButtonGroup gpInvert = new ButtonGroup();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					PrefsWindow frame = new PrefsWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public PrefsWindow() {
		setResizable(false);
		setTitle("RDS Surveyor Preferences");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		
		JLabel lblRdsVariant = new JLabel("RDS variant:");
		contentPane.add(lblRdsVariant);
		
		JRadioButton radRDS = new JRadioButton("Standard RDS");
		gpFlavour.add(radRDS);
		contentPane.add(radRDS);
		
		JRadioButton radRBDS = new JRadioButton("RBDS (US, Canada, Mexico)");
		gpFlavour.add(radRBDS);
		contentPane.add(radRBDS);
		
		JSeparator separator = new JSeparator();
		contentPane.add(separator);
		
		JLabel lblStartupAction = new JLabel("Startup action:");
		contentPane.add(lblStartupAction);
		
		JRadioButton radDialog = new JRadioButton("Show the “Choose input method” dialog");
		gpStartup.add(radDialog);
		contentPane.add(radDialog);
		
		JRadioButton radOpen = new JRadioButton("Show the “Open file” dialog");
		gpStartup.add(radOpen);
		contentPane.add(radOpen);
		
		JRadioButton radUSB = new JRadioButton("Use Video4Linux / SiLabs USBFMRADIO");
		gpStartup.add(radUSB);
		contentPane.add(radUSB);
		
		JRadioButton radSnd = new JRadioButton("Use sound card input");
		gpStartup.add(radSnd);
		contentPane.add(radSnd);
		
		JSeparator separator_1 = new JSeparator();
		contentPane.add(separator_1);
		
		JLabel lblSoundCardInput = new JLabel("Sound card input:");
		contentPane.add(lblSoundCardInput);
		
		JRadioButton radNonInvert = new JRadioButton("Non-inverting");
		gpInvert.add(radNonInvert);
		contentPane.add(radNonInvert);
		
		JRadioButton radInvert = new JRadioButton("Inverting");
		gpInvert.add(radInvert);
		contentPane.add(radInvert);
		
		JRadioButton radAuto = new JRadioButton("Autodetect (for probing only)");
		gpInvert.add(radAuto);
		contentPane.add(radAuto);
	}

}
