/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cnv.shr.gui;

import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbPermissions.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;

/**
 *
 * @author thallock
 */
public class LocalSharePermission extends javax.swing.JPanel {

    private final Machine machine;
    private final LocalDirectory local;
    /**
     * Creates new form SharePermission
     */
    public LocalSharePermission(Machine remote, LocalDirectory local) {
        this.machine = remote;
        this.local = local;
        initComponents();
        setLocation(Services.settings.appLocX.get(), Services.settings.appLocY.get());
        refresh();
    }

    public void save()
    {
        if (!listBox.isSelected()) {
            DbPermissions.share(machine, local, SharingState.INVISIBLE);
            return;
        }
        if (!downloadableBox.isSelected()) {
            DbPermissions.share(machine, local, SharingState.VISIBLE);
            return;
        }
        DbPermissions.share(machine, local, SharingState.DOWNLOADABLE);
    }
    
    public final void refresh()
    {
        this.machineLabel.setText(machine.getName());
        setSharing(DbPermissions.isSharing(machine, local));
    }
    
    public final void setSharing(SharingState state)
    {
        switch (state)
        {
        case INVISIBLE: // Fall through
        case NOT_SET:
            listBox.setSelected(false);
            downloadableBox.setSelected(false);
            break;
        case VISIBLE:
            listBox.setSelected(true);
            downloadableBox.setSelected(false);
            break;
        case DOWNLOADABLE:
            listBox.setSelected(true);
            downloadableBox.setSelected(true);
            break;
        }
        updateEditable();
    }
    
    public void updateEditable()
    {
        if (listBox.isSelected())
        {
            downloadableBox.setEnabled(true);
        }
        else
        {
            downloadableBox.setSelected(false);
            downloadableBox.setEnabled(false);
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

        jLabel1 = new javax.swing.JLabel();
        machineLabel = new javax.swing.JLabel();
        listBox = new javax.swing.JCheckBox();
        downloadableBox = new javax.swing.JCheckBox();
        saveButton = new javax.swing.JButton();

        jLabel1.setText("Machine:");

        machineLabel.setText("loading");

        listBox.setText("Can list");
        listBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                listBoxStateChanged(evt);
            }
        });

        downloadableBox.setText("Can download");
        downloadableBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                downloadableBoxStateChanged(evt);
            }
        });

        saveButton.setText("Save");
        saveButton.setToolTipText("");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(machineLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 498, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(listBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(downloadableBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(saveButton)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(machineLabel)
                    .addComponent(listBox)
                    .addComponent(downloadableBox)
                    .addComponent(saveButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        save();
    }//GEN-LAST:event_saveButtonActionPerformed

    private void downloadableBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_downloadableBoxStateChanged
        updateEditable();
    }//GEN-LAST:event_downloadableBoxStateChanged

    private void listBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_listBoxStateChanged
        updateEditable();
    }//GEN-LAST:event_listBoxStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox downloadableBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JCheckBox listBox;
    private javax.swing.JLabel machineLabel;
    private javax.swing.JButton saveButton;
    // End of variables declaration//GEN-END:variables
}
