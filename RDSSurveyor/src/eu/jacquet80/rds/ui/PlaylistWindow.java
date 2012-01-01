package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import eu.jacquet80.rds.core.DecoderShell;
import eu.jacquet80.rds.input.GroupReader;
import eu.jacquet80.rds.input.HexFileGroupReader;

public class PlaylistWindow extends JFrame {
	private static final long serialVersionUID = 1711324533473299689L;
	private final JList list = new JList();
	private final JLabel lblStatus = new JLabel();
	private static final Pattern linkPattern = 
			Pattern.compile("<a.*?href=['\"]([^'\"]*?\\.(?:rds|spy))['\"].*?>(.*?)</a>", 
					Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern CONTENT_TYPE = Pattern.compile(".*; charset=(.*)");
	private static final Pattern HTTP_EQUIV = 
			Pattern.compile("<\\s*?meta\\s+http-equiv=[\"']Content-Type[\"']\\s+content=[\"'](.*?)[\"']\\s*?/?>",
					Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern TAG = Pattern.compile("<.*?>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private final MainWindow main;
	private Item currentItem = null;

	public PlaylistWindow(final MainWindow main) {
		super("RDS Playlist");
		this.main = main;
		
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(250, 500));
		add(new JScrollPane(list), BorderLayout.CENTER);
		
		JPanel pnlControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
		JButton btnOpen = new JButton("Open...");
		pnlControls.add(btnOpen);
		pnlControls.add(lblStatus);
		
		add(pnlControls, BorderLayout.NORTH);
		
		pack();
		
		list.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				final Item item = (Item)list.getSelectedValue();
				
				if(item != currentItem) {
					currentItem = item;
					
					new Thread() {
						{
							setName("RDSSurveyor-PlaylistWindow-Launcher");
						}

						public void run() {
							final GroupReader reader;
							try {
								reader = new HexFileGroupReader(item.url);
							} catch (Exception e) {
								System.err.println(e);
								reportError("Could not open selected file");
								return;
							}

							main.setReader(DecoderShell.instance.getLog(), reader);
							DecoderShell.instance.process(reader);
						}
					}.start();
				}
			}
		});
		
		btnOpen.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String url = JOptionPane.showInputDialog(
						PlaylistWindow.this, 
						"Web page address (URL):", 
						"Open web page", 
						JOptionPane.QUESTION_MESSAGE);
				
				setURL(url);
			}
		});
	}
	
	private void reportError(String message) {
		lblStatus.setText(message);
		lblStatus.setForeground(Color.RED);
	}
	
	private void reportOK() {
		lblStatus.setText("OK");
		lblStatus.setForeground(Color.GREEN);
	}
	
	public void setURL(String url) {
		Vector<Item> items = new Vector<Item>();
		
		BufferedReader br;
		URL pageURL;
		try {
			pageURL = new URL(url);
			br = createReader(pageURL);
		} catch (MalformedURLException e) {
			reportError("Malformed URL");
			return;
		} catch (IOException e) {
			reportError("Could not open page");
			return;
		}
		reportOK();
		
		String line;
		StringBuilder contents = new StringBuilder();
		try {
			while((line = br.readLine()) != null) {
				contents.append(line).append(' ');
			}
		} catch (IOException e) {
			reportError("Error while reading page");
			return;
		}
		
		Matcher matcher = linkPattern.matcher(contents);
		while(matcher.find()) {
			try {
				//String caption = matcher.group(2).replace("&nbsp;", " ").trim();
				Matcher m = TAG.matcher(matcher.group(2));
				String caption = "<html>" + m.replaceAll("") + "</html>";
				items.add(new Item(caption, new URL(pageURL, matcher.group(1))));
			} catch (MalformedURLException e) {
				System.err.println("Warning: bad URL in web page: " + matcher.group(0) + " -> " + e);
			}
		}
		
		list.setListData(items);
		
		System.out.println(items);
	}
	
	private BufferedReader createReader(URL url) throws IOException {
		URLConnection conn = url.openConnection();
		String charset = null;
		
		// Was the charset sent in the Content-Type: header?
		if(conn.getContentType() != null) {
			Matcher m = CONTENT_TYPE.matcher(conn.getContentType());
			if(m.matches()) {
				charset = m.group(1);
			}
		}
		
		// Otherwise fall-back to parsing the HTML
		if(charset == null) {
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder contents = new StringBuilder();
			String line;
			while((line = br.readLine()) != null) {
				contents.append(line).append(' ');
			}

			Matcher mTag = HTTP_EQUIV.matcher(contents);
			if(mTag.find()) {
				String contentType = mTag.group(1);
				Matcher mCharset = CONTENT_TYPE.matcher(contentType);
				if(mCharset.matches()) {
					charset = mCharset.group(1);
				}
			}
			
			conn = url.openConnection();
		}
		
		// As a last resort suppose it's UTF-8
		if(charset == null) charset = "utf-8";
		
		System.out.println("$$ charset = " + charset);
		return new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
	}
	
	private static class Item {
		public final String title;
		public final URL url;
		
		public Item(String title, URL url) {
			this.title = title;
			this.url = url;
		}
		
		public String toString() {
			return title + " -> " + url;
		}
	}
}
