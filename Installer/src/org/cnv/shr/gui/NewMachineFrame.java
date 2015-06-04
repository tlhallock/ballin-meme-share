/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cnv.shr.gui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.cnv.shr.inst.MonitorThread;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

/**
 *
 * @author thallock
 */
public class NewMachineFrame extends javax.swing.JFrame {

    /**
     * Creates new form NewMachineFrame
     */
    public NewMachineFrame() throws IOException {
        initComponents();
//        setIconImage(Misc.getIcon());
        name.setText(System.getProperty("user.name") +"'s Machine");
        application.setText(System.getProperty("user.home") + File.separator + "Applications" + File.separator + "ConvenienceShare");
        downloads.setText(System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "ConvenienceShare");
    }
    
    private static void error(String message)
    {
		JOptionPane.showMessageDialog(null, 
				message,
				"Unable to install.",
				JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
    }
    
    private static File getFile(String path)
    {
    	File f = new File(path);
    	Misc.ensureDirectory(path, true);
    	if (!f.getParentFile().exists())
    	{
    		error("Unable to create required file: " + path);
    	}
    	return f;
    }
    
    public void installSneaky(String append, int port)
	{
		application.setText(application.getText() + append);
		downloads.setText(downloads.getText() + append);
		beginSpinner.setValue(new Integer(port));
		endSpinner.setValue(new Integer(port + 10));
		name.setText(Misc.getRandomName());
		install(false);
    }
    
    private void install()
    {
    	install(true);
    }
    
    private void install(boolean die)
    {
    	disableInput();
    	
		Path root = Paths.get(application.getText());
		Settings stgs = new Settings(root.resolve(Settings.DEFAULT_SETTINGS_FILE));
		stgs.applicationDirectory.set(root.resolve("app"));
		stgs.downloadsDirectory.set(Paths.get(downloads.getText()));
		stgs.setDefaultApplicationDirectoryStructure();
		
		int beginPort = ((Number) this.beginSpinner.getValue()).intValue();
		int endPort =   ((Number) this.endSpinner.  getValue()).intValue();

		if (endPort <= beginPort)
		{
			error("End port must be larger than begin port!");
		}
		stgs.machineName.set(name.getText());

		stgs.servePortBeginI.set(beginPort);
		stgs.servePortBeginE.set(beginPort);
		stgs.maxServes.set(endPort - beginPort);
		stgs.machineIdentifier.set(Misc.getRandomString(50));
		stgs.shareWithEveryone.set(share.isSelected());
		try
		{
			stgs.write();
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to write settings", e);
			error("Unable to write settings file.");
		}

		try (ZipInputStream zipInputStream = new ZipInputStream(ClassLoader.getSystemResourceAsStream("dist/install_data.zip"));)
		{
			extract(zipInputStream, root);
		}
		catch (IOException e1)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to extract install data", e1);
			error("Unable to extract install data to " + root);
		}
		
		try (InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream("dist/updateKey");)
		{
			Files.copy(systemResourceAsStream, root.resolve("app" + File.separator + "updateKey"), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e1)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to extract update public key", e1);
		}

		try (InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream("dist/trackers");)
		{
			Files.copy(systemResourceAsStream, root.resolve("app" + File.separator + "trackers"), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e1)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to extract update public key", e1);
		}
		
		LinkedList<String> args = new LinkedList<>();
		args.add("java");
		args.add("-jar");
		args.add("ConvenienceShare.jar");
		args.add("-f");
		args.add(stgs.getSettingsFile());
		args.add("-d");
		args.add("-g");

		System.out.println("Restarting from:");
		System.out.println(root);
		System.out.println("with:");
		for (String str : args)
		{
			System.out.println(str);
		}

		ProcessBuilder builder = new ProcessBuilder();
		builder.command(args);
		builder.directory(root.toFile());
		try
		{
			Process start = builder.start();
			if (die)
			{
				new MonitorThread(start, getBounds()).start();
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to start new process", e);
			error("Unable to start new process.");
		}
	}

	private void disableInput()
	{
		application.setEnabled(false);
    	downloads.setEnabled(false);
    	beginSpinner.setEnabled(false);
    	endSpinner.setEnabled(false);
    	jButton1.setEnabled(false);
    	jButton2.setEnabled(false);
    	jButton3.setEnabled(false);
    	jButton4.setEnabled(false);
    	share.setEnabled(false);
    	jTextArea1.setEditable(false);
    	jTextArea1.setEnabled(false);
    	jTextArea2.setEditable(false);
    	jTextArea2.setEnabled(false);
    	name.setEnabled(false);
    	name.setEditable(false);
	}
	
	private static void extract(ZipInputStream zipInputStream, Path destPath) throws IOException
	{
		ZipEntry entry;
		while ((entry = zipInputStream.getNextEntry()) != null)
		{
			if (entry.isDirectory())
			{
				continue;
			}
			Path entryDest = destPath.resolve(entry.getName());
			Misc.ensureDirectory(entryDest, true);
			try
			{
				Files.copy(zipInputStream, entryDest, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException ex)
			{
				LogWrapper.getLogger().log(Level.WARNING, "Unable to extract " + entry.getName() + " to " + destPath, ex);
			}
		}
	}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        application = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        downloads = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        name = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        beginSpinner = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        endSpinner = new javax.swing.JSpinner();
        jButton4 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        share = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Application Directory"));

        application.setEditable(false);
        application.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                applicationActionPerformed(evt);
            }
        });

        jButton2.setText("Browse...");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(application)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton2)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(application, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2)
                .addContainerGap(33, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Downloads Directory:"));

        jTextArea1.setEditable(false);
        jTextArea1.setBackground(new java.awt.Color(238, 238, 238));
        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("This is where all the files you download will be placed.\n(You will select which files to share later.)");
        jScrollPane1.setViewportView(jTextArea1);

        downloads.setEditable(false);

        jButton3.setText("Browse...");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(downloads)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton3))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(downloads, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1)
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Machine Name"));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(name, javax.swing.GroupLayout.DEFAULT_SIZE, 660, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Port Range"));

        jTextArea2.setEditable(false);
        jTextArea2.setBackground(new java.awt.Color(238, 238, 238));
        jTextArea2.setColumns(20);
        jTextArea2.setLineWrap(true);
        jTextArea2.setRows(1);
        jTextArea2.setText("This ports must be open and not blocked by a firewall in order for this application to work.");
        jScrollPane2.setViewportView(jTextArea2);

        jLabel1.setText("From:");

        beginSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(7990), Integer.valueOf(0), null, Integer.valueOf(1)));

        jLabel2.setText("To:");

        endSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(8000), Integer.valueOf(0), null, Integer.valueOf(1)));

        jButton4.setText("Test ports");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jLabel3.setText("Url:");

        jLabel4.setText("127.0.0.1");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(beginSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(endSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 356, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(beginSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(endSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jButton4))
                .addGap(31, 31, 31))
        );

        jButton1.setText("Install");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        share.setText("Share with anybody (Can be changed later.)");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton1, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(share, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(share)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 114, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void applicationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applicationActionPerformed
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setSelectedFile(new File(application.getText()));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
        	fc.getSelectedFile(); // What goes here?
        }
    }//GEN-LAST:event_applicationActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
       // Run ip tester...
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        install();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setSelectedFile(new File(downloads.getText()));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
        	downloads.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setSelectedFile(new File(application.getText()));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
        	application.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField application;
    private javax.swing.JSpinner beginSpinner;
    private javax.swing.JTextField downloads;
    private javax.swing.JSpinner endSpinner;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextField name;
    private javax.swing.JCheckBox share;
    // End of variables declaration//GEN-END:variables

}
