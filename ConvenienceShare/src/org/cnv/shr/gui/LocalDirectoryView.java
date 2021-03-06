
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.gui;

import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbRoots.IgnorePatterns;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

/**
 *
 * @author thallock
 */
public class LocalDirectoryView extends javax.swing.JFrame
{
	private Path path;
	private boolean exitOnSave;
	LinkedList<LocalSharePermission> permissions = new LinkedList<>();

	public LocalDirectoryView(LocalDirectory root, boolean exitOnSave)
	{
		this.exitOnSave = exitOnSave;
		initComponents();
//		jTextField1.setEditable(!root.isMirror());

		if (root == null)
		{
			setTitle("Unable to open directory.");
			return;
		}
		this.path = root.getPath();
		setTitle("LocalDirectory: " + path);
		pathLabel.setText(root.getPath().toString());

		jComboBox1.setModel(new PermissionChanger(jComboBox1, root.getDefaultSharingState())
		{
			@Override
			protected void setPermission(SharingState state)
			{
				LocalDirectory local = getLocal();
				if (local == null) return;
				local.setDefaultSharingState(state);
				local.tryToSave();
				LogWrapper.getLogger().info("Set permission of " + local.getName() + " to " + state.humanReadable());
			}
		});
		
		refresh(root);
	}


	private void refresh(LocalDirectory root)
	{
		tagsString.setText(root.getTags());
		descriptionString.setText(root.getDescription());
		jTextField1.setText(root.getName());

		StringBuilder builder = new StringBuilder();
		for (String string : DbRoots.getIgnores(root).getPatterns())
		{
			builder.append(string).append('\n');
		}
		ignoreTextArea.setText(builder.toString());

		permissions.clear();
		sharePanel.removeAll();
		sharePanel.setLayout(new GridLayout(0, 1));
		try (DbIterator<Machine> listRemoteMachines = DbMachines.listRemoteMachines();)
		{
			while (listRemoteMachines.hasNext())
			{
				Machine machine = listRemoteMachines.next();
				LocalSharePermission permission = new LocalSharePermission(machine, root);
				sharePanel.add(permission);
				permissions.add(permission);
			}
		}
		
		jComboBox1.setSelectedItem(root.getDefaultSharingState().humanReadable());
		jCheckBox1.setSelected(root.getMinFileSize() >= 0);
		
		setMinSize(root.getMinFileSize());
		
		int flags = root.getPermissionFlags();
		jCheckBox4.setSelected(IgnorePatterns.isSkipHidden(flags));
		jCheckBox3.setSelected(IgnorePatterns.isSkipExecutable(flags));
		jCheckBox2.setSelected(IgnorePatterns.isSkipRO(flags));
	}

	private LocalDirectory getLocal()
	{
		if (path == null) return null;
		try
		{
			return DbRoots.getLocal(path);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, null, e);
			return null;
		}
	}
	
	private void save()
	{
		LocalDirectory local = getLocal();
		if (local == null)
		{
			return;
		}
		
		local.setName(jTextField1.getText());
		local.setDescription(descriptionString.getText());
		local.setTags(tagsString.getText());
		if (jCheckBox1.isSelected())
		{
			local.setMinimumSize(getMinSize());
		}
		else
		{
			local.setMinimumSize(-1);
		}

		local.setPermissionFlags(
				IgnorePatterns.createFlags(
						jCheckBox4.isSelected(),
						jCheckBox3.isSelected(),
						jCheckBox2.isSelected()));

		local.tryToSave();
		DbRoots.setIgnores(local, ignoreTextArea.getText().split("\n"));
		
		if (exitOnSave)
		{
			dispose();
		}
		else
		{
			refresh(getLocal());
		}
		// TODO: right here could clean the ignores...
	}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        pathLabel = new javax.swing.JLabel();
        tags = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        tagsString = new javax.swing.JTextField();
        descriptionString = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        ignoreTextArea = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        sharePanel = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jCheckBox1 = new javax.swing.JCheckBox();
        jSpinner1 = new javax.swing.JSpinner();
        jComboBox2 = new javax.swing.JComboBox();
        jButton3 = new javax.swing.JButton();
        jCheckBox2 = new javax.swing.JCheckBox();
        jCheckBox3 = new javax.swing.JCheckBox();
        jCheckBox4 = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Path:");

        pathLabel.setText("Loading...");

        tags.setText("Tags:");

        jLabel3.setText("Description:");

        jButton1.setText("Save");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Ignore files containing:"));

        ignoreTextArea.setColumns(20);
        ignoreTextArea.setRows(5);
        jScrollPane1.setViewportView(ignoreTextArea);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1034, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setTopComponent(jPanel1);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Share with:"));

        javax.swing.GroupLayout sharePanelLayout = new javax.swing.GroupLayout(sharePanel);
        sharePanel.setLayout(sharePanelLayout);
        sharePanelLayout.setHorizontalGroup(
            sharePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1035, Short.MAX_VALUE)
        );
        sharePanelLayout.setVerticalGroup(
            sharePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 237, Short.MAX_VALUE)
        );

        jScrollPane2.setViewportView(sharePanel);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE)
        );

        jSplitPane1.setRightComponent(jPanel2);

        jButton2.setText("Synchronize");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel2.setText("Global permissions:");

        jLabel4.setText("Name:");

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Ingore files with size less than or equal to:");
        jCheckBox1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jCheckBox1StateChanged(evt);
            }
        });

        jSpinner1.setModel(new javax.swing.SpinnerNumberModel(Long.valueOf(-1L), Long.valueOf(-1L), null, Long.valueOf(1L)));
        jSpinner1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinner1StateChanged(evt);
            }
        });

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "bytes", "Kb", "Mb", "Gb", "Tb" }));
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

        jButton3.setText("Open");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jCheckBox2.setText("Skip read only");

        jCheckBox3.setText("Skip executables");

        jCheckBox4.setText("Skip hidden");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jCheckBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBox4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jCheckBox3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jCheckBox2))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(tags, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tagsString)
                            .addComponent(descriptionString)
                            .addComponent(pathLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextField1)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 189, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(pathLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tags)
                    .addComponent(tagsString, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(descriptionString, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2)
                    .addComponent(jButton1)
                    .addComponent(jButton3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBox1)
                    .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBox2)
                    .addComponent(jCheckBox3)
                    .addComponent(jCheckBox4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        save();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        UserActions.userSync(this, getLocal(), null);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jCheckBox1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jCheckBox1StateChanged
         jSpinner1.setEnabled(jCheckBox1.isSelected());
        jComboBox2.setEnabled(jCheckBox1.isSelected());
        updateMinimumSize();      
    }//GEN-LAST:event_jCheckBox1StateChanged

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
        updateMinimumSize();
    }//GEN-LAST:event_jComboBox2ActionPerformed

    private void jSpinner1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinner1StateChanged
        updateMinimumSize();
    }//GEN-LAST:event_jSpinner1StateChanged

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        Misc.ensureDirectory(path, false);
        Misc.nativeOpen(path, false);
    }//GEN-LAST:event_jButton3ActionPerformed

    private void updateMinimumSize() {}
    
    long getMinSize()
    {
    	if (!jCheckBox1.isSelected())
    	{
    		return -1;
    	}
    	
    	long value = ((Long) jSpinner1.getValue()).longValue();
    	switch ((String) jComboBox2.getSelectedItem())
    	{
    	case "Tb": value *= 1024L;
    	case "Gb": value *= 1024L;
    	case "Mb": value *= 1024L;
    	case "Kb": value *= 1024L;
    	case "bytes": return value;
    		default:
    			return -1;
    	}
    }
  	
  	private void setMinSize(long minFileSize)
  	{
  		if (minFileSize < 1024)
  		{
  			jSpinner1.setValue(minFileSize);
  			jComboBox2.setSelectedItem("bytes");
  			return;
  		}
  		minFileSize /= 1024;
  		if (minFileSize < 1024)
  		{
  			jSpinner1.setValue(minFileSize);
  			jComboBox2.setSelectedItem("Kb");
  			return;
  		}
  		minFileSize /= 1024;
  		if (minFileSize < 1024)
  		{
  			jSpinner1.setValue(minFileSize);
  			jComboBox2.setSelectedItem("Mb");
  			return;
  		}
  		minFileSize /= 1024;
  		if (minFileSize < 1024)
  		{
  			jSpinner1.setValue(minFileSize);
  			jComboBox2.setSelectedItem("Gb");
  			return;
  		}
  		
			minFileSize /= 1024;
			jSpinner1.setValue(minFileSize);
			jComboBox2.setSelectedItem("Tb");
		}
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField descriptionString;
    private javax.swing.JTextArea ignoreTextArea;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JLabel pathLabel;
    private javax.swing.JPanel sharePanel;
    private javax.swing.JLabel tags;
    private javax.swing.JTextField tagsString;
    // End of variables declaration//GEN-END:variables
}
