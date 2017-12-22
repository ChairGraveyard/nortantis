package nortantis.editor;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import nortantis.editor.TextTool.ToolType;
import util.JComboBoxFixed;
import util.JTextFieldFixed;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.Box;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;

public class TestDialog extends JDialog
{
	public TestDialog() {
		setBounds(100, 100, 935, 584);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		
		JButton btnButton = new JButton("Button 1");
		btnButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		getContentPane().add(btnButton);
		
		
		JButton btnButton_1 = new JButton("Button 2");
		getContentPane().add(btnButton_1);
		
		JPanel panel = new JPanel();
		panel.setBackground(Color.RED);

		getContentPane().add(panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		panel.add(new JTextFieldFixed());
		
		JPanel line1Panel = new JPanel();
		line1Panel.setLayout(new BoxLayout(line1Panel, BoxLayout.X_AXIS));
		panel.add(line1Panel);
		JLabel lblLine = new JLabel("line 1");
		line1Panel.add(lblLine);
		lblLine.setPreferredSize(new Dimension(200, 20));
		JButton btnButtonLine = new JButton("line 1");
		line1Panel.add(btnButtonLine);
		line1Panel.add(Box.createHorizontalGlue());
		
		JPanel line2Panel = new JPanel();
		line2Panel.setPreferredSize(new Dimension(80, 20));
		line2Panel.setLayout(new BoxLayout(line2Panel, BoxLayout.X_AXIS));
		panel.add(line2Panel);
		JLabel lblLine2 = new JLabel("line 2 label");
		line2Panel.add(lblLine2);
		lblLine2.setPreferredSize(new Dimension(200, 30));
		JComboBox comboBox = new JComboBoxFixed();
        comboBox.setMaximumSize(new Dimension(200, 20));
		comboBox.addItem("item 1");
		comboBox.addItem("item 2");

		line2Panel.add(comboBox);
		line2Panel.add(Box.createHorizontalGlue());

		

		Component verticalGlue = Box.createVerticalGlue();
		getContentPane().add(verticalGlue);
		
		JButton btnButton_2 = new JButton("Button 3");
		getContentPane().add(btnButton_2);

	}

}
