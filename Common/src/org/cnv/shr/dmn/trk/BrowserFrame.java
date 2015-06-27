
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



package org.cnv.shr.dmn.trk;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.cnv.shr.dmn.trk.TrackerClient.CommentsListInterface;
import org.cnv.shr.trck.CommentEntry;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.CloseableIterator;
import org.cnv.shr.util.LogWrapper;


/**
 *
 * @author thallock
 */
public abstract class BrowserFrame extends javax.swing.JFrame implements ListSelectionListener {

    private Map<String, TrackerClient> clients = new Hashtable<>();
    private ArrayList<MachineEntry> machines = new ArrayList<>();
    private List<CommentEntry> comments = new LinkedList<>();
    
    protected TrackerClient currentClient;
    protected MachineEntry currentMachine;
    
    int listStart = 0;
    boolean hasMore;
    private final Object sync = new Object();
    
    /**
     * Creates new form BrowserFrame
     */
    public BrowserFrame() {
        initComponents();
        commentPanel.setLayout(new GridLayout(0, 1));
        jTable1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refreshTrackers();
        pack();
        jList1.getSelectionModel().addListSelectionListener(this);
        
        jButton5.setText(getMachineText1());
        jButton3.setText(getMachineText2());
        jButton2.setText(getTrackerText1());
        jList1.setComponentPopupMenu(jPopupMenu1);
    }

	public final void refreshTrackers()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				LogWrapper.getLogger().info("Refreshing trackers");
				synchronized (sync)
				{
					DefaultListModel model = (DefaultListModel) jList1.getModel();
					model.clear();

					clients.clear();
					listClients(new TrackerListener()
					{
						@Override
						public void receiveTracker(TrackerEntry client)
						{
							String key = client.getAddress();
							model.addElement(key);
							clients.put(key, createTrackerClient(client));
						}
					});
				}
			}
		});
	}

	private void show(TrackerClient client, boolean fromStart)
	{
		BrowserFrame b = this;
		runLater(new Runnable()
		{
			@Override
			public void run()
			{
				boolean success;
				synchronized (sync)
				{
					if (fromStart)
					{
						listStart = 0;
					}
					success = showClientInternal(client);
				}
				if (!success)
				{
					JOptionPane.showMessageDialog(b, "Unable to connect to tracker.", "Unable to connect to tracker.", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	private boolean showClientInternal(TrackerClient client)
	{
		jButton1.setEnabled(trackAction2Enabled());
		jButton2.setEnabled(true);
		jButton6.setEnabled(true);
		currentClient = client;

    jButton2.setText(getTrackerText1());
    jButton5.setText(getMachineText1());
    jButton3.setText(getMachineText2());

		LogWrapper.getLogger().info("Showing " + client);
		
		DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
		while (model.getRowCount() > 0) model.removeRow(0);
		machines.clear();
		try (CloseableIterator<MachineEntry> iterator = client.list(listStart);)
		{
			while (iterator.hasNext())
			{
				MachineEntry entry = iterator.next();
				LogWrapper.getLogger().info("Listed machine " + entry);
				if (entry == null)
					break;
				machines.add(entry);
				model.addRow(new Object[] {
								entry.getName(),
				        entry.getAddress(),
				        entry.getIdentifer(),
				});
			}
			hasMore = true;
      jButton9.setEnabled(listStart > 0);
			jButton8.setEnabled(hasMore);
			jLabel6.setText(String.valueOf(listStart));
			return true;
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list machines: ", e);
			return false;
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

        jPopupMenu1 = new javax.swing.JPopupMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        ratingLabel = new javax.swing.JLabel();
        filesLabl = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        commentPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jButton6 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jButton4 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();

        jMenuItem2.setText("Show");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jMenuItem2);

        jMenuItem1.setText("Delete highlighted item");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jMenuItem1);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Trackers");

        jSplitPane1.setDividerLocation(201);

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jSplitPane2.setDividerLocation(200);

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Address", "Identifier"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jTable1);

        jSplitPane2.setLeftComponent(jScrollPane2);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Machine Options:"));
        jPanel2.setPreferredSize(new java.awt.Dimension(200, 200));

        jLabel1.setText("Average rating:");

        jLabel2.setText("Total number of files:");

        ratingLabel.setText("No machine selected");

        filesLabl.setText("No machine selected");

        commentPanel.setPreferredSize(new java.awt.Dimension(200, 200));

        javax.swing.GroupLayout commentPanelLayout = new javax.swing.GroupLayout(commentPanel);
        commentPanel.setLayout(commentPanelLayout);
        commentPanelLayout.setHorizontalGroup(
            commentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 447, Short.MAX_VALUE)
        );
        commentPanelLayout.setVerticalGroup(
            commentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 263, Short.MAX_VALUE)
        );

        jScrollPane3.setViewportView(commentPanel);

        jLabel3.setText("Name:");

        jLabel4.setText("No machine selected");

        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jButton3.setText("Open");
        jButton3.setEnabled(false);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton5.setText("Add Comment");
        jButton5.setEnabled(false);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton3)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(jButton5))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(filesLabl, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)
                    .addComponent(ratingLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(ratingLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(filesLabl, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE))
        );

        jSplitPane2.setRightComponent(jPanel2);

        jButton8.setText("Next");
        jButton8.setEnabled(false);
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jButton9.setText("Prev");
        jButton9.setEnabled(false);
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        jLabel5.setText("Viewing from:");

        jLabel6.setText("0");

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Tracker actions"));

        jButton6.setText("Refresh");
        jButton6.setEnabled(false);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jButton1.setText("Add trackers");
        jButton1.setEnabled(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Upload file metadata");
        jButton2.setEnabled(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton6)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton6)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton8))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton9)
                            .addComponent(jButton8)))
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSplitPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE))
        );

        jSplitPane1.setRightComponent(jPanel1);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("All trackers:"));

        jList1.setModel(new DefaultListModel());
        jList1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jList1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jList1MouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jList1);

        jButton4.setText("Add...");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton7.setText("Remove");
        jButton7.setEnabled(false);
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton4)
                .addGap(6, 6, 6))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton4)
                    .addComponent(jButton7))
                .addContainerGap())
        );

        jSplitPane1.setLeftComponent(jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jList1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MouseClicked
        if (evt.getClickCount() >= 2)
        {
            int selectedIndex = jList1.getSelectedIndex();
            if (selectedIndex < 0) return;
						TrackerClient client = clients.get(jList1.getModel().getElementAt(selectedIndex));
            if (client == null)
            {
                return;
            }
            
            show(client, true);
        }
        else
        {
            doPopup(evt);
        }
    }//GEN-LAST:event_jList1MouseClicked

    private void doPopup(java.awt.event.MouseEvent evt)
    {
			if (!evt.isPopupTrigger())
			{
				return;
			}
			int row = jList1.getSelectedIndex();
			if (row >= 0)
			{
				jList1.setSelectedIndex(row);
			}
			jPopupMenu1.show(evt.getComponent(), evt.getX(), evt.getY());
    }
    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
    	AddTracker addTracker = createAddTracker();
    	addTracker.setAlwaysOnTop(true);
    	addTracker.setLocation(getLocation());
    	addTracker.setVisible(true);
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        deleteTheRow();
    }//GEN-LAST:event_jMenuItem1ActionPerformed
    private void deleteTheRow() {
        int selectedIndex = jList1.getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }
        TrackerClient client = clients.get(jList1.getModel().getElementAt(selectedIndex));
        if (client == null) {
            return;
        }
				LogWrapper.getLogger().info("Removing " + client);
        remoceClient(client);
        refreshTrackers();
    }
    
    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed

      int selectedIndex = jList1.getSelectedIndex();
      if (selectedIndex < 0) return;
    	TrackerClient client = clients.get(jList1.getModel().getElementAt(selectedIndex));
            if (client == null)
            {
                return;
            }
            
            show(client, true);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jList1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MousePressed
        doPopup(evt);
    }//GEN-LAST:event_jList1MousePressed

    private void jList1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MouseReleased
        doPopup(evt);
    }//GEN-LAST:event_jList1MouseReleased

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
      if (evt.getClickCount() < 2) return;  
    	runLater(new Runnable() {
					@Override
					public void run()
					{
						refreshComments();
					}});
    }//GEN-LAST:event_jTable1MouseClicked

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
      // tracker actions 3
       runLater(new Runnable() {
            @Override
            public void run() {
                refreshAll();
            }
        });
                
    }//GEN-LAST:event_jButton6ActionPerformed

	protected void refreshAll()
	{
		refreshTrackers();
		if (currentClient != null)
		{
			show(currentClient, false);
		}
		if (currentMachine == null)
		{
			showNoComments();
		}
		else
		{
			refreshComments();
		}
	}

	private void showNoComments()
	{
		if (!isVisible())
		{
			return;
		}
		comments.clear();
		currentMachine = null;
		LogWrapper.getLogger().info("Showing no machine");

		jLabel4.setText("No machine selected.");
		jButton5.setEnabled(false);
		jButton3.setEnabled(false);
		commentPanel.removeAll();

		ratingLabel.setText("No machine selected.");
		filesLabl.setText("No machine selected.");
		commentPanel.removeAll();
		repaint();
	}

		private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
			runLater(new Runnable()
			{
				@Override
				public void run()
				{
					if (currentClient == null)
					{
						return;
					}
					
					currentClient.addOthers();
	
					if (isVisible())
					{
						refreshAll();
					}
				}
			});
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // tracker actions 1
    	trackerAction1();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        deleteTheRow();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
			listStart += TrackerEntry.MACHINE_PAGE_SIZE;
			if (currentClient != null)
			{
				show(currentClient, false);
			}
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        listStart = Math.max(0, listStart - TrackerEntry.MACHINE_PAGE_SIZE);
			if (currentClient != null)
			{
				show(currentClient, false);
			}
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // machine actions 1
        machineAction1();
    }//GEN-LAST:event_jButton5ActionPerformed
		
    

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // machine actions 2
        machineAction2();
    }//GEN-LAST:event_jButton3ActionPerformed
    
    void refreshComments()
    {
    	if (!isVisible())
    	{
    		return;
    	}
			int index = jTable1.getSelectedRow();
			if (index < 0 || index >= machines.size())
			{
			    return;
			}
		comments.clear();
		currentMachine = machines.get(index);
    jButton5.setText(getMachineText1());
    jButton3.setText(getMachineText2());
    
		LogWrapper.getLogger().info("Showing " + currentMachine);
		
		jLabel4.setText(currentMachine.getName());
		jButton5.setEnabled(true);
		jButton3.setEnabled(machineAction2Enabled());
		commentPanel.removeAll();

		ratingLabel.setText("0");
		filesLabl.setText("loading...");
		commentPanel.removeAll();
		
		try (CommentsListInterface listComments = currentClient.listComments(currentMachine);)
		{
			filesLabl.setText(listComments.getNumFiles());
			
			int count = 0;
			double sum = 0;
			while (listComments.hasNext())
			{
				CommentEntry entry = listComments.next();

				LogWrapper.getLogger().info("Listed comment " + entry);
				
				if (entry == null)
				{
					break;
				}
				commentPanel.add(new CommentPanel(entry, count));
				LogWrapper.getLogger().info("Found " + entry);
				comments.add(entry);

				count++;
				sum += entry.getRating();

				ratingLabel.setText(String.valueOf(sum / count));
			}
			LogWrapper.getLogger().info("Found " + count + " comments.");
			commentPanel.repaint();
			repaint();
		}
		catch (Exception ex)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "Unable to list comments.", ex);
			JOptionPane.showMessageDialog(this, "Unable to list comments.", "Unable to list comments.", JOptionPane.ERROR_MESSAGE);
		}
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel commentPanel;
    private javax.swing.JLabel filesLabl;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JList jList1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel ratingLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void valueChanged(ListSelectionEvent e) {
        jButton7.setEnabled(jList1.getSelectedIndex() >= 0);
    }

    public interface TrackerListener { public void receiveTracker(TrackerEntry client); }
  	protected abstract void listClients(TrackerListener listener);
  	
    protected abstract void runLater(Runnable runnable);
    protected abstract AddTracker createAddTracker();
    
    protected abstract String getMachineText1();
    protected abstract String getMachineText2();
    protected abstract String getTrackerText1();

		protected abstract void machineAction2();
    protected abstract void machineAction1();
    protected abstract void trackerAction1();
    
		protected abstract void remoceClient(TrackerClient client);
		protected abstract TrackerClient createTrackerClient(TrackerEntry entry);
		
		protected abstract boolean trackAction2Enabled();
    protected abstract boolean machineAction2Enabled();
}
