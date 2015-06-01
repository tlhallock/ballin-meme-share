/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cnv.shr.gui;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMessages;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Notifications.NotificationListenerImpl;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.gui.tbl.DownloadTable;
import org.cnv.shr.gui.tbl.LocalTable;
import org.cnv.shr.gui.tbl.MachineTable;
import org.cnv.shr.gui.tbl.MessageTable;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.msg.key.PermissionFailure.PermissionFailureEvent;
import org.cnv.shr.stng.Setting;
import org.cnv.shr.stng.Setting.SettingsEditor;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.sync.DebugListener;
import org.cnv.shr.util.IpTester;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.TextAreaHandler;

/**
 * 
 * @author John
 */
public class Application extends javax.swing.JFrame implements NotificationListenerImpl
{
    LinkedList<ConnectionStatus> connections = new LinkedList<>();
    TextAreaHandler logHandler;
    
    
    LocalTable locals;
    MachineTable remotes;
    MessageTable messages;
    DownloadTable downloads;
    
	/**
	 * Creates new form Application
	 */
	public Application()
	{
		initComponents();
		initializeSettings();
		
		setTitle("Convenience Share " + Settings.getVersion());

		locals = new LocalTable(this, localsView);
		remotes = new MachineTable(this, machinesList);
		downloads = new DownloadTable(this, jTable2);
		messages = new MessageTable(this, messageTable);
		
		connectionsPanel.setLayout(new GridLayout(0, 1));
		
		this.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentMoved(ComponentEvent arg0)
			{
				if (!isVisible())
				{
					return;
				}
				Point locationOnScreen = getLocationOnScreen();
				Services.settings.appLocX.set(locationOnScreen.x);
				Services.settings.appLocY.set(locationOnScreen.y);
			}
		});
		
		logHandler = new TextAreaHandler(logTextArea, (Integer) logLines.getValue());
		logLines.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0)
			{
				logHandler.setLogLines((Integer) logLines.getValue());
			}});
		LogWrapper.getLogger().addHandler(logHandler);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e)
			{
				LogWrapper.getLogger().removeHandler(logHandler);
			}
		});
		Services.notifications.add(this);
	}

	private void initializeSettings()
	{
		Setting[] settings = Services.settings.getUserSettings();
		DefaultTableModel model = (DefaultTableModel) settingsTable.getModel();
		settingsTable.getColumn("Modify").setCellRenderer(new TableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable arg0, Object arg1, boolean arg2, boolean arg3, int arg4, int arg5)
			{
				return ((SettingsEditor) arg1).get();
			}
		});
		settingsTable.getColumn("Modify").setCellEditor(createSettingColumnEditor());
		
		int maxHeight = Integer.MIN_VALUE;
		for (Setting setting : settings)
		{
			SettingsEditor createInput = setting.getEditor();
			maxHeight = Math.max(maxHeight, createInput.get().getPreferredSize().height);
			model.addRow(new Object[] {
					setting.getName(),
					setting.getDescription(),
					new Boolean(setting.requiresReboot()),
					createInput,
			});
		}
		settingsTable.setRowHeight(maxHeight);
	}

	private TableCellEditor createSettingColumnEditor()
	{
		return new TableCellEditor()
		{
			SettingsEditor c;
			
			@Override
			public boolean stopCellEditing()
			{
				return true;
			}
			
			@Override
			public boolean shouldSelectCell(EventObject arg0)
			{
				return true;
			}
			
			@Override
			public void removeCellEditorListener(CellEditorListener arg0) {}
			
			@Override
			public boolean isCellEditable(EventObject arg0)
			{
				return true; // c.isEditable();
			}
			
			@Override
			public Object getCellEditorValue()
			{
				return c;
			}
			
			@Override
			public void cancelCellEditing() {}
			
			@Override
			public void addCellEditorListener(CellEditorListener arg0) {}
			
			@Override
			public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int arg3, int arg4)
			{
				return (c = (SettingsEditor) arg1).get();
			}
		};
	}

	public void refreshAll()
	{
		downloads.refresh();
		refreshSettings();
		locals.refresh();
		remotes.refresh();
		messages.refresh();
		refreshConnections();
		refreshServes();
	}
    
	private synchronized void refreshLocal(LocalDirectory local)
	{
		locals.refresh(local);
	}

	private void refreshConnections()
	{
		for (ConnectionStatus status : connections)
		{
			status.refresh();
		}
	}
	
	private synchronized void refreshServes()
	{
		SwingUtilities.invokeLater(new Runnable() { public void run() {
		DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
		while (model.getRowCount() > 0)
		{
			model.removeRow(0);
		}
		
		for (ServeInstance instance : Services.server.getServeInstances())
		{
			model.addRow(new Object[] {
					instance.getMachine().getName(),
					instance.getFile().getPath().getFullPath(),
					String.valueOf(instance.getCompletionPercentage()),
					"Speed goes here...",
			});
		}}});
	}

	private void refreshSettings()
	{
		addressLabel.setText(Services.settings.getLocalIp() + ":" + Services.settings.servePortBeginE);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed"
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        addressLabel = new javax.swing.JTextField();
        jScrollPane4 = new javax.swing.JScrollPane();
        machinesList = new javax.swing.JTable();
        jButton10 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        localsView = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        maxPending = new javax.swing.JLabel();
        jButton7 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        Debug = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jButton8 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jScrollPane9 = new javax.swing.JScrollPane();
        settingsTable = new javax.swing.JTable();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        logTextArea = new javax.swing.JTextArea();
        logLines = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jButton5 = new javax.swing.JButton();
        jScrollPane7 = new javax.swing.JScrollPane();
        messageTable = new javax.swing.JTable();
        jButton9 = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane8 = new javax.swing.JScrollPane();
        connectionsPanel = new javax.swing.JPanel();
        refreshConnections = new javax.swing.JButton();
        jPanel12 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Convenience Share");

        jButton2.setText("Add...");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel1.setText("Your address:");

        addressLabel.setEditable(false);
        addressLabel.setText("Unkown");

        machinesList.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Current Address", "Id", "Sharing", "Number of files", "Total files size", "Last Ip", "Port", "Number of ports"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        machinesList.setEnabled(false);
        jScrollPane4.setViewportView(machinesList);

        jButton10.setText("Refresh");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane4)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(addressLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 704, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton2)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(addressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(jButton10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Machines", null, jPanel1, "");

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Currently Shared Directories/Files"));

        jButton1.setText("Add...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton4.setText("Synchronize All");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        localsView.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Path", "Name", "Description", "Tags", "Number of files", "Total file size"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        localsView.setToolTipText("");
        localsView.setEnabled(false);
        jScrollPane5.setViewportView(localsView);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(0, 581, Short.MAX_VALUE)
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton1))
                    .addComponent(jScrollPane5))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 339, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Local Directories", jPanel2);

        jSplitPane1.setDividerLocation(150);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Incoming Files"));

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Machine", "Directory", "File", "Size", "Added on", "Status", "Priority", "Local path", "Number of Mirrors", "Speed", "Percent", "Id"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Object.class, java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable2.setEnabled(false);
        jScrollPane2.setViewportView(jTable2);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 821, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE))
        );

        jSplitPane1.setTopComponent(jPanel5);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Outgoing Files"));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Machine", "File", "Percent", "Speed"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 833, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
        );

        jSplitPane1.setRightComponent(jPanel6);

        jLabel2.setText("Currently set max downloads:");

        maxPending.setText("0");

        jButton7.setText("Initiate pending");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jButton11.setText("Refresh");
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        jButton12.setText("Clear completed");
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(maxPending, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton7)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(maxPending)
                    .addComponent(jButton7)
                    .addComponent(jButton11)
                    .addComponent(jButton12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Downloads", jPanel3);

        Debug.setText("Print Debug Info");
        Debug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DebugActionPerformed(evt);
            }
        });

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("My IP"));

        jButton8.setText("Test IP");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap(603, Short.MAX_VALUE)
                .addComponent(jButton8)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap(13, Short.MAX_VALUE)
                .addComponent(jButton8)
                .addContainerGap())
        );

        jButton3.setText("Delete Database!");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton6.setText("Change Keys");

        settingsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Description", "Requires Restart", "Modify"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane9.setViewportView(settingsTable);

        jButton13.setText("Update Code");
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });

        jButton14.setText("Map Ports");
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane9, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton14, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(Debug, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(Debug)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton6))
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Settings", jPanel4);

        logTextArea.setEditable(false);
        logTextArea.setColumns(20);
        logTextArea.setRows(5);
        jScrollPane3.setViewportView(logTextArea);

        logLines.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(500), Integer.valueOf(0), null, Integer.valueOf(1)));

        jLabel3.setText("Number of Lines:");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 821, Short.MAX_VALUE)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(logLines, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(logLines, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Logs", jPanel8);

        jButton5.setText("Clear all");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        messageTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Machine", "Type", "Date", "Message", "Id"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane7.setViewportView(messageTable);

        jButton9.setText("Refresh");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 821, Short.MAX_VALUE)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton5)))
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton5)
                    .addComponent(jButton9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Messages", jPanel9);

        javax.swing.GroupLayout connectionsPanelLayout = new javax.swing.GroupLayout(connectionsPanel);
        connectionsPanel.setLayout(connectionsPanelLayout);
        connectionsPanelLayout.setHorizontalGroup(
            connectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 842, Short.MAX_VALUE)
        );
        connectionsPanelLayout.setVerticalGroup(
            connectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 511, Short.MAX_VALUE)
        );

        jScrollPane8.setViewportView(connectionsPanel);

        refreshConnections.setText("Refresh");
        refreshConnections.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshConnectionsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane8)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(refreshConnections)
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(refreshConnections)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Open connections", jPanel10);

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 845, Short.MAX_VALUE)
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 421, Short.MAX_VALUE)
        );

        jTabbedPane2.addTab("Keys", jPanel12);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2)
        );

        jTabbedPane2.getAccessibleContext().setAccessibleName("Remotes");
        jTabbedPane2.getAccessibleContext().setAccessibleDescription("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
        	UserActions.queueLocal(
        			Paths.get(fc.getSelectedFile().getAbsolutePath()), null);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        AddMachine addMachine = new AddMachine();
        addMachine.setAlwaysOnTop(true);
        Services.notifications.registerWindow(addMachine);
		addMachine.setVisible(true);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void DebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DebugActionPerformed
    	UserActions.debug();
    }//GEN-LAST:event_DebugActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
		final DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();
		while (listLocals.hasNext())
		{
			LocalDirectory next = listLocals.next();
			final List<DebugListener> singletonList = Collections.singletonList(createLocalListener(next));
			UserActions.userSync(next, singletonList);
		}
		Services.notifications.localsChanged();
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
    	UserActions.deleteDb();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        Services.downloads.initiatePendingDownloads();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        LogWrapper.getLogger().info("IP test results = " + new IpTester().getIpFromCanYouSeeMeDotOrg());
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
       DbMessages.clearAll();
   		messages.refresh();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
  		messages.refresh();
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
  		remotes.refresh();
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
    	// Should not be here...
  		this.maxPending.setText(String.valueOf(Services.settings.maxDownloads.get()));
  		downloads.refresh();
        refreshServes();
    }//GEN-LAST:event_jButton11ActionPerformed

    private void refreshConnectionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshConnectionsActionPerformed
        refreshConnections();
    }//GEN-LAST:event_refreshConnectionsActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
       DbDownloads.clearCompleted(); // should be moved to user actions and be run on a different thread.
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
       UpdateFrame u = new UpdateFrame();
       Services.notifications.registerWindow(u);
       u.setAlwaysOnTop(true);
       u.setVisible(true);
    }//GEN-LAST:event_jButton13ActionPerformed

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
        new PortMapperFrame().setVisible(true);
    }//GEN-LAST:event_jButton14ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Debug;
    private javax.swing.JTextField addressLabel;
    private javax.swing.JPanel connectionsPanel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable localsView;
    private javax.swing.JSpinner logLines;
    private javax.swing.JTextArea logTextArea;
    private javax.swing.JTable machinesList;
    private javax.swing.JLabel maxPending;
    private javax.swing.JTable messageTable;
    private javax.swing.JButton refreshConnections;
    private javax.swing.JTable settingsTable;
    // End of variables declaration//GEN-END:variables


	public DebugListener createLocalListener(final LocalDirectory root)
	{
		final DefaultTableModel model = (DefaultTableModel) localsView.getModel();
		int row = -1;
		String path = root.getPathElement().getFullPath();
		for (int i = 0; i < localsView.getRowCount(); i++)
		{
			if (model.getValueAt(i, 0).equals(path))
			{
				row = i;
				break;
			}
		}
		if (row < 0)
		{
			return null;
		}

		final HashMap<String, Object> map = new HashMap<>();
		
		final int dirRow = row;
		return new DebugListener(root)
		{
			long startSize = root.diskSpace();
			long startNumFiles = root.numFiles();
			long lastGuiUpdate = 0;
			
			@Override
			protected void changed()
			{
				long now = System.currentTimeMillis();
				if (now - lastGuiUpdate < 1000)
				{
					// return;
				}

				map.put("Total file size", new DiskUsage(startSize + bytesAdded));
				map.put("Number of files", new NumberOfFiles(startNumFiles + filesAdded - filesRemoved));
				locals.setValues(root, map, dirRow);
				lastGuiUpdate = now;
			}
		};
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
			@Override
			public void localsChanged()
			{
				locals.refresh();
			}

			@Override
			public void localDirectoryChanged(LocalDirectory local)
			{
				refreshLocal(local);
			}

			@Override
			public void remotesChanged()
			{
				remotes.refresh();
			}
			
			@Override
			public void remoteChanged(Machine remote)
			{
				remotes.refresh();
			}

			@Override
			public void remoteDirectoryChanged(RemoteDirectory remote)
			{
				remotes.refresh();
			}

			@Override
			public void downloadAdded(DownloadInstance d)
			{
				downloads.refresh();
			}

			@Override
			public void downloadRemoved(DownloadInstance d)
			{
				downloads.refresh();
			}

			@Override
			public void downloadDone(DownloadInstance d)
			{
				downloads.refresh();
			}

			@Override
			public void serveAdded(ServeInstance s)
			{
				refreshServes();
			}

			@Override
			public void serveRemoved(ServeInstance s)
			{
				refreshServes();
			}

			@Override
			public void connectionOpened(Communication c)
			{
				ConnectionStatus connectionStatus = new ConnectionStatus(c);
				connectionStatus.setVisible(true);
				connectionsPanel.add(connectionStatus);
				connections.add(connectionStatus);
				refreshConnections();
			}

			@Override
			public void messageReceived(UserMessage message)
			{
				messages.refresh();
			}

			@Override
			public void connectionClosed(Communication c)
			{
				for (ConnectionStatus s : connections)
				{
					if (s.shows(c))
					{
						connections.remove(s);
						connectionsPanel.remove(s);
						break;
					}
				}
				refreshConnections();
			}

			public void messagesChanged()
			{
				messages.refresh();
			}

			@Override
			public void dbException(Exception ex)
			{
			}

			@Override
			public void permissionFailure(PermissionFailureEvent event)
			{
				// TODO Auto-generated method stub
				
			}

			@Override
			public void fileAdded(SharedFile file)
			{
				// TODO Auto-generated method stub
				
			}

			@Override
			public void fileChanged(SharedFile file)
			{
				// TODO Auto-generated method stub
				
			}

			@Override
			public void fileDeleted(SharedFile file)
			{
				// TODO Auto-generated method stub
				
			}
}
