/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cnv.shr.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;

/**
 *
 * @author thallock
 */
public class KeyPanel extends javax.swing.JPanel {

    private static final int LENGTH_TO_SHOW = 30;
    
    KeyPairObject key;
    Application app;
    /**
     * Creates new form KeyPanel
     */
    public KeyPanel(Application app, KeyPairObject key) {
        initComponents();
        jLabel2.setText(key.getTime());
        String text = KeyPairObject.serialize(key.getPublicKey());
        if (text.length() > LENGTH_TO_SHOW)
        {
            text= text.substring(0, LENGTH_TO_SHOW) + "...";
        }
        jLabel4.setText(text);
        this.key = key;
        this.app = app;
        
        Services.userThreads.execute(new Runnable() {
					@Override
					public void run()
					{
		        if (KeyPairObject.serialize(Services.keyManager.getPublicKey()).equals(KeyPairObject.serialize(key.getPublicKey())))
		        {
		        	setBackground(Color.lightGray);
		        }
					}});
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
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();

        jLabel1.setText("Created:");

        jLabel2.setText("loading...");

        jButton1.setText("Revoke");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            @Override
						public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel3.setText("Text:");

        jLabel4.setText("loading...");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jButton1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        KeyPanel b = this;
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				if (Services.keyManager.getPairs().size() <= 1)
				{
					JOptionPane.showMessageDialog(b, "Please leave one key!");
					return;
				}
				Services.keyManager.revoke(key, Services.settings.keysFile.getPath(), Services.settings.keySize.get());
				try
				{
					Services.keyManager.writeKeys(Services.settings.keysFile.getPath());
				}
				catch (IOException ex)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to save keys.", ex);
				}
				app.refreshKeys();
			}
		});
    }//GEN-LAST:event_jButton1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    // End of variables declaration//GEN-END:variables
}