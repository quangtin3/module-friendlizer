/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.viettel.vep.friendlizer;

import com.viettel.vep.friendlizer.ModuleConfiguration.Module;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/**
 * GuiFriendlizerApp
 */
public class GuiFriendlizerApp extends JPanel implements ActionListener {

    private JButton openButton, patchButton;
    private JTextArea logger;
    private JFileChooser fc;
    private String selectedNetBeansPath = null;
    private ModuleConfiguration moduleConfiguration;

    public GuiFriendlizerApp() {
        super(new BorderLayout());

        // Init jPanel controls
        initComponent();

        // Clear output text area (as a logger)
        logger.setText(null);

        try {
            // Loading configuration
            moduleConfiguration = new ModuleConfiguration();

            // Print guide
            FriendlizerUtilities.Log(logger, "Steps:");
            FriendlizerUtilities.Log(logger, "1.Chosing your NetBeans IDE folder");
            FriendlizerUtilities.Log(logger, "2.Patching it.");
            FriendlizerUtilities.Log(logger, "======================================");
        } catch (IOException e) {
            FriendlizerUtilities.Log(logger, "Loading configuration error: " + e.getMessage());
        }
    }

    private void initComponent() {
        //Create the logger first, because the action listeners
        //need to refer to it.
        logger = new JTextArea(8, 50);
        logger.setMargin(new Insets(5, 5, 5, 5));
        logger.setEditable(false);

        JScrollPane logScrollPane = new JScrollPane(logger);

        //Create a file chooser
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        //Create the open button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        openButton = new JButton("Chose NetBeans ...", createImageIcon("images/open.gif"));
        openButton.addActionListener(GuiFriendlizerApp.this);

        //Create the save button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        patchButton = new JButton("Do Patch", createImageIcon("images/patch.gif"));
        patchButton.addActionListener(GuiFriendlizerApp.this);
        patchButton.setEnabled(false);

        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout        
        buttonPanel.add(openButton);
        buttonPanel.add(patchButton);

        //Add the buttons and the logger to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        //Handle open button action.
        if (e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(GuiFriendlizerApp.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                selectedNetBeansPath = file.getAbsolutePath();
                //This is where a real application would open the file.
                FriendlizerUtilities.Log(logger, "Selected: " + selectedNetBeansPath + ".");
                patchButton.setEnabled(true);
            } else {
                patchButton.setEnabled(false);
            }

            //Handle save button action.
        } else if (e.getSource() == patchButton) {
            if (selectedNetBeansPath == null || selectedNetBeansPath.isEmpty()) {
                FriendlizerUtilities.Log(logger, "Not in valid state");
            } else {

                boolean patched = true;
                for (Module moduleCfg : moduleConfiguration.getModules()) {
                    FriendlizerUtilities.Log(logger, "Patching for:\n\t"
                            + moduleCfg.getKey() + "\n\t"
                            + moduleCfg.getDescription());
                    patched &= FriendlizerUtilities.patchingNetBeans(selectedNetBeansPath, moduleCfg, logger);
                }

                if (patched) {
                    FriendlizerUtilities.Log(logger, "All modules are patched successful!");
                    patchButton.setEnabled(false);
                } else {
                    patchButton.setEnabled(false);
                    FriendlizerUtilities.Log(logger, "Patching fail, please re-select netbeans IDE");
                }
            }
        }
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = GuiFriendlizerApp.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be
     * invoked from the event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Viettel Enterprise Platform - Module Friendlizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add content to the window.
        frame.add(new GuiFriendlizerApp());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }
}
