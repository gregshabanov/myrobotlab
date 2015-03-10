/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import org.myrobotlab.codec.Codec;
import org.myrobotlab.codec.CodecException;
import org.myrobotlab.codec.DecimalCodec;
import org.myrobotlab.codec.CodecFactory;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Serial;
import org.slf4j.Logger;

public class SerialGUI extends ServiceGUI implements ActionListener, ItemListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(SerialGUI.class);

	// menu
	JComboBox<String> reqFormat = new JComboBox<String>(new String[] { "decimal", "hex", "ascii", "arduino" });
	JComboBox<String> ports = new JComboBox<String>();
	JButton refresh = new JButton("refresh");

	JButton createVirtualUART = new JButton("create virtual uart");
	JButton record = new JButton();
	// JButton sendTx = new JButton("send tx from file");

	JLabel connectLight = new JLabel();

	JTextArea rx = new JTextArea(20, 40);
	JLabel rxTotal = new JLabel("0");
	JLabel txTotal = new JLabel("0");
	String delimiter = " ";
	Integer width = 16;
	JTextField widthMenu = new JTextField("16");

	int rxCount = 0;
	int txCount = 0;

	// JTextField sendData = new JTextField(40);
	JTextArea tx = new JTextArea(3, 40);
	JButton send = new JButton("send");
	JButton sendFile = new JButton("send file");

	Serial mySerial = null;
	final SerialGUI myself;

	// gui's formatters
	Codec rxFormatter = new DecimalCodec();
	Codec txFormatter = new DecimalCodec();

	// Codec rxFormatter = null;
	// Codec txFormatter = null;

	// TODO
	// save data to file button
	// send file
	// create virtual port
	// create null modem cable

	public SerialGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		myself = this;
		mySerial = (Serial) Runtime.getService(boundServiceName);
	}

	public void init() {
		display.setLayout(new BorderLayout());

		JPanel north = new JPanel();
		north.add(new JLabel("port "));
		north.add(ports);
		north.add(refresh);
		north.add(connectLight);
		north.add(new JLabel(" "));
		north.add(reqFormat);
		north.add(new JLabel("width "));
		north.add(widthMenu);
		north.add(createVirtualUART);
		north.add(record);
		// north.add(sendTx);

		display.add(north, BorderLayout.NORTH);

		rx.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(rx);

		autoScroll(true);

		display.add(scrollPane, BorderLayout.CENTER);

		JPanel south = new JPanel();

		south.add(tx);
		south.add(send);
		south.add(sendFile);
		south.add(new JLabel("rx"));
		south.add(rxTotal);
		south.add(new JLabel("tx"));
		south.add(txTotal);
		display.add(south, BorderLayout.SOUTH);

		createVirtualUART.addActionListener(this);
		send.addActionListener(this);
		sendFile.addActionListener(this);
		record.addActionListener(this);
		reqFormat.addItemListener(this);
		ports.addItemListener(this);
		refresh.addActionListener(this);

	}

	public void autoScroll(boolean b) {
		DefaultCaret caret = (DefaultCaret) rx.getCaret();
		if (b) {
			caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		} else {
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		}
	}

	public void getState(final Serial serial) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				mySerial = serial;
				setPortStatus();

				try {
					
					// prevent re-firing the event :P
					reqFormat.removeItemListener(myself);
					
					// WARNING
					// don't use the same output formatters as the serial service
					// they might have a different state when they are writing
					// out to a file...
					String key = mySerial.getRXCodecKey();
					if (key != null && !key.equals(rxFormatter.getKey())) {
						// create new formatter from type key
						rxFormatter = CodecFactory.getDecoder(key);
						// TODO - set the reqTXFormat box .. too lazy :P - hopefully
						reqFormat.setSelectedItem(key);
					}
					key = mySerial.getTXCodecKey();
					if (key != null && !key.equals(txFormatter.getKey())) {
						// create new formatter from type key
						txFormatter = CodecFactory.getDecoder(key);
					}
					
					reqFormat.addItemListener(myself);

				} catch (Exception e) {
					Logging.logException(e);
				}

				if (!serial.isRecording()) {
					// captureRX.setText(serial.getRXFileName()); } else {
					record.setText("record");
				} else {
					record.setText("stop recording");
				}

				// if (mySerial.getRXFormatter())
			}
		});
	}

	public void setPortStatus() {
		ports.removeItemListener((ItemListener) self);
		if (mySerial.isConnected()) {
			connectLight.setIcon(Util.getImageIcon("green.png"));
			log.info(String.format("displaying %s", mySerial.getPortName()));
			ports.setSelectedItem(mySerial.getPortName());
		} else {
			connectLight.setIcon(Util.getImageIcon("red.png"));
			ports.setSelectedItem("");
		}
		ports.addItemListener((ItemListener) self);
	}

	public void getPortNames(final ArrayList<String> inPorts) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				ports.removeItemListener((ItemListener) self);
				ports.removeAllItems();
				ports.addItem("");
				for (int i = 0; i < inPorts.size(); ++i) {
					ports.addItem(inPorts.get(i));
				}
				ports.addItemListener((ItemListener) self);
				setPortStatus();
			}
		});
	}

	/**
	 * publishRX displays the "interpreted" byte it is interpreted by the
	 * Serial's service selected "format"
	 * 
	 * FORMAT_DECIMEL is a 3 digit decimal in ascii FORMAT_RAW is interpreted as
	 * 1 byte = 1 ascii char FORMAT_HEX is 2 digit asci hex
	 * 
	 * @param data
	 * @throws CodecException
	 */
	public final void publishRX(final Integer data) throws CodecException {
		++rxCount;
		rx.append(rxFormatter.decode(data));
		//rx.append(String.format("%s ", data));
		/*
		 * if (!mySerial.getDisplayFormat().equals(Serial.DISPLAY_RAW) && width
		 * != null && rxCount % width == 0) { rx.append("\n"); }
		 */
		/* FIXME FIXME FIXME THE CODEC SHOULD FORMAT !!!!! 
		if (width != null && rxCount % width == 0) {
			rx.append("\n");
		}
		*/
		rxTotal.setText(String.format("%d", rxCount));
	}

	public final void publishTX(final Integer data) throws CodecException {
		++txCount;
		tx.append(txFormatter.decode(data));
		/*
		 * if (!mySerial.getDisplayFormat().equals(Serial.DISPLAY_RAW) && width
		 * != null && txCount % width == 0) { tx.append("\n"); }
		 */
		/* FIXME FIXME FIXME THE CODEC SHOULD FORMAT !!!!! 
		if (width != null && txCount % width == 0) {
			tx.append("\n");
		}
		*/
		txTotal.setText(String.format("%d", txCount));
	}

	@Override
	public void attachGUI() {
		subscribe("publishRX", "publishRX", Integer.class);
		subscribe("publishTX", "publishTX", Integer.class);
		subscribe("publishState", "getState", Serial.class);
		subscribe("getPortNames", "getPortNames", ArrayList.class);

		send("publishState");
		send("getPortNames");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishRX", "publishRX", String.class);
		unsubscribe("publishTX", "publishTX", String.class);
		unsubscribe("publishState", "getState", Serial.class);
		unsubscribe("getPortNames", "getPortNames", ArrayList.class);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == record) {
			if (record.getText().startsWith("record")) {
				send("record");
				send("broadcastState");
			} else {
				send("stopRecording");
				send("broadcastState");
			}
		}

		if (o == createVirtualUART) {
			send("createVirtualUART");
		}

		if (o == refresh) {
			send("refresh");
		}

		if (o == sendFile) {
			JFileChooser fileChooser = new JFileChooser();
			// set current directory
			fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
			int result = fileChooser.showOpenDialog(this.getDisplay());
			if (result == JFileChooser.APPROVE_OPTION) {
				// user selects a file
				File selectedFile = fileChooser.getSelectedFile();
				send("writeFile", selectedFile.getAbsolutePath());
			}
		}

		if (o == ports) {
			String selected = (String) ports.getSelectedItem();
			if (selected == null || "".equals(selected)) {
				send("stopPolling");
			} else {
				log.info(String.format("changed to %s ", selected));
				send("setController", selected);
				send("startPolling");
			}
		}

		if (o == send) {
			String data = tx.getText();
			send("write", data.getBytes());
			myService.info("sent [%s]", data);
		}

	}

	// onChange of ports
	@Override
	public void itemStateChanged(ItemEvent event) {
		Object o = event.getSource();
		if (o == ports) {
			String port = (String) ports.getSelectedItem();
			if (port != null && !port.equals(mySerial.getPortName()) && port.length() > 0) {
				send("disconnect");
				send("connect", port);
			} else if (port.length() == 0) {
				send("disconnect");
			}
		}

		if (o == reqFormat) {
			String newFormat = (String) reqFormat.getSelectedItem();
			// changing our display and the Service's format
			try {
				rxFormatter = CodecFactory.getDecoder(newFormat);
				txFormatter = CodecFactory.getDecoder(newFormat);
				send("setFormat", newFormat);
			} catch (Exception e) {
				Logging.logException(e);
			}
		}
	}

}
