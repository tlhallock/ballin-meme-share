/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cnv.shr.gui;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.swup.UpdateInfoRequestRequest;
import org.cnv.shr.updt.UpdateInfo;
import org.cnv.shr.updt.UpdateInfoImpl;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;

/**
 *
 * @author thallock
 */
public class UpdateServerFrame extends javax.swing.JFrame {

	private ArrayList<String> displayedMachineIdents = new ArrayList<>();
    /**
     * Creates new form UpdateServerFrame
     */
    public UpdateServerFrame() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Update Server Options");

        jTable1.setModel(getTableModel());
        jScrollPane1.setViewportView(jTable1);

        jButton1.setText("Request Logs");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Send update request");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("Use new key");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel1.setText("IP:");

        jLabel2.setText("Port:");

        jLabel3.setText("Last key generation:");

        jLabel4.setText("Last key:");

        jLabel5.setText("Unable to find local update server info!");

        jLabel6.setText("Unable to find local update server info!");

        jLabel7.setText("Unable to find local update server info!");

        jLabel8.setText("Unable to find local update server info!");

        jButton4.setText("Update cached version");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setText("Refresh");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jLabel9.setText("Serving version:");

        jLabel10.setText("Unable to find local update server info!");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 286, Short.MAX_VALUE)
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton1))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel4)
                            .addComponent(jLabel9))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButton3))
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel6))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton3)
                            .addComponent(jButton5)))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton4))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        refresh();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
      LogWrapper.getLogger().info("Updating cached versions");  
      UpdateServerFrame frame = this;
    	Services.userThreads.execute(new Runnable() {
					@Override
					public void run()
					{
						HashSet<String> selectedIdents = getSelectedIdents();
						if (selectedIdents.isEmpty())
						{
							JOptionPane.showMessageDialog(frame,
									"No machines selected.",
									"You have not selected any machines.",
									JOptionPane.INFORMATION_MESSAGE);
							return;
						}
						for (String identifier : selectedIdents)
		        {
		            Machine machine = DbMachines.getMachine(identifier);
		            if (machine == null)
		            {
		            	LogWrapper.getLogger().info("Unable to get machine " + identifier);
		            	continue;
		            }
		            
								try
								{
									Communication connection = Services.networkManager.openConnection(frame, machine, false, "Update cached version info.");
			            if (connection == null)
			            {
			            	continue;
			            }
			            connection.finish();
								}
								catch (IOException e)
								{
		            	LogWrapper.getLogger().info("Unable to connect to machine " + identifier);
								}
		  					
		  					try
		  					{
		  						LogWrapper.getLogger().info("Yielding for 10 sec.");
		  						Thread.sleep(10 * 1000);
		  					}
		  					catch (InterruptedException ex)
		  					{
		  						LogWrapper.getLogger().log(Level.INFO, "Interrupted", ex);
		  						return;
		  					}
		        }
					}});
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
		LogWrapper.getLogger().info("Sending update requests");
		UpdateServerFrame frame = this;
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				HashSet<String> selectedIdents = getSelectedIdents();
				if (selectedIdents.isEmpty())
				{
					JOptionPane.showMessageDialog(frame,
							"No machines selected.",
							"You have not selected any machines.",
							JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				for (String identifier : selectedIdents)
				{
					Machine machine = DbMachines.getMachine(identifier);
					if (machine == null)
					{
						LogWrapper.getLogger().info("Unable to get machine " + identifier);
						continue;
					}

					try
					{
						Communication connection = Services.networkManager.openConnection(frame, machine, false, "Update code info.");
						if (connection == null)
						{
							continue;
						}
						try
						{
							connection.send(new UpdateInfoRequestRequest("update info"));
						}
						catch (Exception ex)
						{
  						LogWrapper.getLogger().log(Level.INFO, "Unable to send update info request to " + identifier, ex);
							connection.finish();
						}
					}
					catch (IOException e)
					{
						LogWrapper.getLogger().info("Unable to connect to machine " + identifier);
					}
					
					try
					{
						LogWrapper.getLogger().info("Yielding for 10 sec.");
						Thread.sleep(10 * 1000);
					}
					catch (InterruptedException ex)
					{
						LogWrapper.getLogger().log(Level.INFO, "Interrupted", ex);
						return;
					}
				}
			}
		});
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    	LogWrapper.getLogger().info("Getting logs");
    	JFrame frame = this;
  		Services.userThreads.execute(new Runnable()
  		{
  			@Override
  			public void run()
  			{
  				HashSet<String> selectedIdents = getSelectedIdents();
  				if (selectedIdents.isEmpty())
  				{
  					JOptionPane.showMessageDialog(frame,
  							"No machines selected.",
  							"You have not selected any machines.",
  							JOptionPane.INFORMATION_MESSAGE);
  					return;
  				}
  				for (String identifier : selectedIdents)
  				{
  					Machine machine = DbMachines.getMachine(identifier);
  					if (machine == null)
  					{
  						LogWrapper.getLogger().info("Unable to get machine " + identifier);
  						continue;
  					}

  					try
  					{
  						Communication connection = Services.networkManager.openConnection(frame, machine, false, "Get logs");
  						if (connection == null)
  						{
  							continue;
  						}
  						try
  						{
  							connection.send(new UpdateInfoRequestRequest("getLogs"));
							}
							catch (Exception ex)
							{
	  						LogWrapper.getLogger().log(Level.INFO, "Unable to send get logs request to " + identifier, ex);
								connection.finish();
							}
  					}
  					catch (IOException e)
  					{
  						LogWrapper.getLogger().info("Unable to connect to machine " + identifier);
  					}
  					
  					try
  					{
  						LogWrapper.getLogger().info("Yielding for 10 sec.");
  						Thread.sleep(10 * 1000);
  					}
  					catch (InterruptedException ex)
  					{
  						LogWrapper.getLogger().log(Level.INFO, "Interrupted", ex);
  						return;
  					}
  				}
  			}
  		});
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        UpdateInfo info = Services.codeUpdateInfo;
        if (info == null)
        {
        	LogWrapper.getLogger().info("Unable to use new key: no code update info.");
        	return;
        }
        
        try
				{
					info.useNewKey();
				}
				catch (NoSuchAlgorithmException | NoSuchProviderException e)
				{
        	LogWrapper.getLogger().log(Level.SEVERE, "Unable to use new key: No such algorithm.", e);
        	System.exit(-1);
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to write new key.", e);
				}
    }//GEN-LAST:event_jButton3ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables

    void refresh()
    {
			UpdateInfo codeUpdateInfo = Services.codeUpdateInfo;
			if (codeUpdateInfo == null)
			{
				LogWrapper.getLogger().info("Unable to show machine versions because can't find update server.");
				String errorString = "Unable to find local update server info! (Are you running one?)";
				jLabel8.setText(errorString);
				jLabel7.setText(errorString);
				jLabel5.setText(errorString);
				jLabel6.setText(errorString);
				jLabel10.setText(errorString);
				
				jButton1.setEnabled(false);
				jButton2.setEnabled(false);
				jButton3.setEnabled(false);
				jButton4.setEnabled(false);
				jButton5.setEnabled(false);
				return;
			}
			
			LogWrapper.getLogger().info("Refreshing update server frame.");
			
			((UpdateInfoImpl) codeUpdateInfo).checkTime();
			
			jLabel8.setText(codeUpdateInfo.getIp());
			jLabel7.setText(String.valueOf(codeUpdateInfo.getPort()));
			jLabel5.setText(new Date(codeUpdateInfo.getLastKeyTimeStamp()).toString());
			String serialize = KeyPairObject.serialize(codeUpdateInfo.getLatestPublicKey());
			if (serialize.length() > 50)
			{
				serialize = serialize.substring(0, 50) + "...";
			}
			jLabel6.setText(serialize);
			
			String servingVersion = codeUpdateInfo.getVersionOfCodeServing();
			if (servingVersion == null)
			{
				jLabel10.setText("No code found.");
			}
			else
			{
				jLabel10.setText(servingVersion);
			}

			jButton1.setEnabled(true);
			jButton2.setEnabled(true);
			jButton3.setEnabled(true);
			jButton4.setEnabled(true);
			jButton5.setEnabled(true);
			
        SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run()
					{
						synchronized (displayedMachineIdents)
						{
							displayedMachineIdents.clear();
							DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
							while (model.getRowCount() > 0)
							{
								model.removeRow(0);
							}
							try (DbIterator<Machine> iterator = DbMachines.listRemoteMachines();)
							{
								while (iterator.hasNext())
								{
									Machine machine = iterator.next();
									if (machine == null)
									{
										LogWrapper.getLogger().info("Iterator returned null!");
										continue;
									}
									String identifier = machine.getIdentifier();
									model.addRow(new Object[]
									{
										identifier,
										machine.getUrl(),
										new java.util.Date(machine.getLastActive()),
										codeUpdateInfo.getLastKnownVersion(identifier),
									});
									displayedMachineIdents.add(identifier);
								}
							}
						}
					}});
    }
    
    private HashSet<String> getSelectedIdents()
    {
    	HashSet<String> returnValue = new HashSet<>();
    	
    	synchronized (displayedMachineIdents)
    	{
    		for (int row : jTable1.getSelectedRows())
    		{
    			if (row >= 0 && row < displayedMachineIdents.size())
    			{
    				returnValue.add(displayedMachineIdents.get(row));
    			}
    		}
    	}
    	
    	return returnValue;
    }
    
    private static final TableModel getTableModel()
    {
    	return new javax.swing.table.DefaultTableModel(
          new Object [][] {

          },
          new String [] {
              "Identifier", "Url", "Last connection", "Last Known Version"
          }
      ) {
          Class[] types = new Class [] {
              java.lang.String.class, java.lang.String.class, java.util.Date.class, java.lang.String.class
          };
          boolean[] canEdit = new boolean [] {
              false, false, false, false
          };

          public Class getColumnClass(int columnIndex) {
              return types [columnIndex];
          }

          public boolean isCellEditable(int rowIndex, int columnIndex) {
              return canEdit [columnIndex];
          }
      };
    }
}
