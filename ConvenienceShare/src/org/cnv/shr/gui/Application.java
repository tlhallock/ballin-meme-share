
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

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMessages;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.db.h2.TrackerInfoExport;
import org.cnv.shr.db.h2.bak.DbBackupRestore;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.dmn.mn.strt.RunOnStartUp;
import org.cnv.shr.dmn.not.NotificationListener;
import org.cnv.shr.dmn.trk.TrackerFrame;
import org.cnv.shr.dmn.trk.Trackers;
import org.cnv.shr.gui.tbl.DownloadTable;
import org.cnv.shr.gui.tbl.LocalTable;
import org.cnv.shr.gui.tbl.MachineTable;
import org.cnv.shr.gui.tbl.MessageTable;
import org.cnv.shr.gui.tbl.ServeTable;
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
import org.cnv.shr.sync.RootSynchronizer.SynchronizationListener;
import org.cnv.shr.util.IpTester;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.TextAreaHandler;

public class Application extends javax.swing.JFrame implements NotificationListener
{
	public static final int GUI_REFRESH_RATE = 1000;
	
    private LinkedList<ConnectionStatus> connections = new LinkedList<>();
    private TextAreaHandler logHandler;
    
    private LocalTable locals;
    private MachineTable remotes;
    private MessageTable messages;
    private DownloadTable downloads;
    private ServeTable serves;
    
    private TimerTask refresh;
    
	/**
	 * Creates new form Application
	 */
	public Application()
	{
		initComponents();
		initializeSettings();
		
		setTitle("Convenience Share " + Settings.getVersion(Main.class) + " Machine " + Services.localMachine.getName());

		locals = new LocalTable(this, localsView);
		remotes = new MachineTable(this, machinesList);
		downloads = new DownloadTable(this, jTable2);
		messages = new MessageTable(this, messageTable);
		serves = new ServeTable(jTable1);
		
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

    
    refresh = new TimerTask() {
			@Override
			public void run()
			{
				refreshChangingInfo();
			}
    };
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e)
			{
				LogWrapper.getLogger().removeHandler(logHandler);
				refresh.cancel();
			}
		});
		Services.notifications.add(this);

		refreshKeys();
                
		jPanel14.setLayout(new FlowLayout());
        jPanel14.setComponentOrientation(
                ComponentOrientation.LEFT_TO_RIGHT);
        
        

    for (String ip : Misc.collectIps())
    {
        JRadioButton button = new JRadioButton(ip);
        buttonGroup1.add(button);
        button.setVisible(true);
        jPanel14.add(button);
        button.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!button.isSelected() || Services.settings.getLocalIp().equals(ip))
                {
                	return;
                }
                Services.settings.setLocalAddress(ip);
                refreshSettings();
            }
        });
        boolean isCurrentLocal = ip.equals(Services.settings.getLocalIp());
        if (isCurrentLocal)
        {
        	button.setSelected(true);
        }
    }
    
    jComboBox1.setModel(new PermissionChanger(jComboBox1, 
    		SharingState.valueOf(Services.settings.defaultPermission.get())) {
        @Override
        protected void setPermission(SharingState state) {
            Services.settings.defaultPermission.set(state.name());
        }
    });
    makeDebugItems();
    downloads.refresh();
    Services.timer.scheduleAtFixedRate(refresh, GUI_REFRESH_RATE, GUI_REFRESH_RATE);
	}

	private void makeDebugItems()
	{
		for (DbObjects table : DbTables.ALL_TABLES)
		{
			JMenuItem item = new JMenuItem(table.getTableName());
			item.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					Services.userThreads.execute(new Runnable() {
						@Override
						public void run()
						{
							try (ConnectionWrapper threadConnection = Services.h2DbCache.getThreadConnection();)
							{
								table.debug(threadConnection);
							}
							catch (SQLException e1)
							{
		            LogWrapper.getLogger().log(Level.INFO, "Unable to close thread connection.", e);
							}
						}});
				}
			});
			jMenu8.add(item);
		}
		JMenuItem item = new JMenuItem("All");
		item.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Services.userThreads.execute(new Runnable() {
					@Override
					public void run()
					{
						try (ConnectionWrapper threadConnection = Services.h2DbCache.getThreadConnection();)
						{
							DbTables.debugDb();
						}
						catch (SQLException e1)
						{
		          LogWrapper.getLogger().log(Level.INFO, "Unable to close thread connection.", e);
						}
					}});
			}
		});
		jMenu8.add(item);
                
                
                
                
                logHandler.setScrollOnUpdate(jCheckBox1.isSelected());
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
	
	private void refreshChangingInfo()
	{
		downloads.refreshInPlace();
		serves.refreshInPlace();
		refreshConnections();
	}

	public void refreshAll()
	{
		downloads.refresh();
		refreshSettings();
		locals.refresh();
		remotes.refresh();
		messages.refresh();
    serves.refresh();
		refreshConnections();
    refreshKeys();
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
		connectionsPanel.repaint();
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        jMenuItem1 = new javax.swing.JMenuItem();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel11 = new javax.swing.JPanel();
        jComboBox1 = new javax.swing.JComboBox();
        jPanel7 = new javax.swing.JPanel();
        jButton8 = new javax.swing.JButton();
        addressLabel = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        machinesList = new javax.swing.JTable();
        jButton10 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
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
        jPanel9 = new javax.swing.JPanel();
        jButton5 = new javax.swing.JButton();
        jScrollPane7 = new javax.swing.JScrollPane();
        messageTable = new javax.swing.JTable();
        jButton9 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane9 = new javax.swing.JScrollPane();
        settingsTable = new javax.swing.JTable();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane8 = new javax.swing.JScrollPane();
        connectionsPanel = new javax.swing.JPanel();
        refreshConnections = new javax.swing.JButton();
        jPanel12 = new javax.swing.JPanel();
        jButton6 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        jPanel13 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        logTextArea = new javax.swing.JTextArea();
        logLines = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox();
        jButton3 = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem14 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem13 = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem21 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem11 = new javax.swing.JMenuItem();
        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jCheckBoxMenuItem2 = new javax.swing.JCheckBoxMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        jMenuItem22 = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jMenuItem17 = new javax.swing.JMenuItem();
        jMenuItem16 = new javax.swing.JMenuItem();
        jMenuItem20 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem15 = new javax.swing.JMenuItem();
        jMenu8 = new javax.swing.JMenu();
        jMenuItem18 = new javax.swing.JMenuItem();
        jMenuItem19 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();

        jMenuItem1.setText("jMenuItem1");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Convenience Share");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("My IP"));

        jButton8.setText("Test IP");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        addressLabel.setEditable(false);
        addressLabel.setText("Unkown");

        jLabel1.setText("Your address:");

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addressLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 682, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton8)))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(addressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        jLabel4.setText("Default permissions for new Machines/Directories:");

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 364, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBox1, 0, 539, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(329, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Home", jPanel11);

        jButton2.setText("Add...");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        machinesList.setModel(MachineTable.createTableModel());
        jScrollPane4.setViewportView(machinesList);

        jButton10.setText("Refresh");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        jButton17.setText("Find Trackers");
        jButton17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });

        jButton13.setText("Find machines");
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 921, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton10)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(jButton10)
                    .addComponent(jButton17)
                    .addComponent(jButton13))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Machines", null, jPanel1, "");

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

        localsView.setModel(LocalTable.createTableModel());
        localsView.setToolTipText("");
        jScrollPane5.setViewportView(localsView);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(0, 691, Short.MAX_VALUE)
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
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 394, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Local Shared Directories", jPanel2);

        jSplitPane1.setDividerLocation(150);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Incoming Files"));

        jTable2.setModel(DownloadTable.createTableModel());
        jTable2.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jScrollPane2.setViewportView(jTable2);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 921, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE))
        );

        jSplitPane1.setTopComponent(jPanel5);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Outgoing Files"));

        jTable1.setModel(ServeTable.createTableModel());
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 933, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE)
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
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(maxPending, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(160, 160, 160)
                .addComponent(jButton12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton11)
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
                .addComponent(jSplitPane1))
        );

        jTabbedPane2.addTab("Downloads", jPanel3);

        jButton5.setText("Clear all");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        messageTable.setModel(MessageTable.createTableModel());
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
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 921, Short.MAX_VALUE)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton9)))
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
                .addComponent(jScrollPane7)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Messages", jPanel9);

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

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 921, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Settings", jPanel4);

        javax.swing.GroupLayout connectionsPanelLayout = new javax.swing.GroupLayout(connectionsPanel);
        connectionsPanel.setLayout(connectionsPanelLayout);
        connectionsPanelLayout.setHorizontalGroup(
            connectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 941, Short.MAX_VALUE)
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
            .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 945, Short.MAX_VALUE)
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
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Open connections", jPanel10);

        jButton6.setText("Use a new key");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jButton18.setText("Refresh");
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 934, Short.MAX_VALUE)
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 471, Short.MAX_VALUE)
        );

        jScrollPane6.setViewportView(jPanel13);

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel12Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton18)
                .addContainerGap())
            .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 945, Short.MAX_VALUE)
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton6)
                    .addComponent(jButton18))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Keys", jPanel12);

        logTextArea.setEditable(false);
        logTextArea.setColumns(20);
        logTextArea.setRows(5);
        jScrollPane3.setViewportView(logTextArea);

        logLines.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(200), Integer.valueOf(0), null, Integer.valueOf(1)));

        jLabel3.setText("Number of Lines:");

        jLabel5.setText("Log Level:");

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All", "Fine", "Info", "Warning", "Severe", "None" }));
        jComboBox2.setSelectedIndex(2);
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

        jButton3.setText("Clear");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Scroll on update");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 921, Short.MAX_VALUE)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jCheckBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(logLines, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton3)))
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(logLines, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel5)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton3)
                    .addComponent(jCheckBox1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 394, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Logs", jPanel8);

        jMenu6.setText("App");

        jMenuItem14.setText("Hide");
        jMenuItem14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem14ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem14);

        jMenuItem2.setText("Restart");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem2);

        jMenuItem13.setText("Quit");
        jMenuItem13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem13ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem13);

        jMenuBar1.add(jMenu6);

        jMenu1.setText("Util");

        jMenuItem21.setText("Check for Updates");
        jMenuItem21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem21ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem21);

        jMenuItem3.setText("Show Trackers");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuItem7.setText("Configure Run on Startup...");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem7);

        jMenu4.setText("Port Mappings");

        jMenuItem11.setText("Launch port mapper");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem11ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem11);

        jCheckBoxMenuItem1.setText("Map ports on start up");
        jCheckBoxMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItem1ActionPerformed(evt);
            }
        });
        jMenu4.add(jCheckBoxMenuItem1);

        jMenu1.add(jMenu4);

        jMenu5.setText("Personal Tracker");

        jMenuItem4.setText("Launch now");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem4);

        jCheckBoxMenuItem2.setText("Launch on start up");
        jCheckBoxMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItem2ActionPerformed(evt);
            }
        });
        jMenu5.add(jCheckBoxMenuItem2);

        jMenu1.add(jMenu5);

        jMenuItem12.setText("Run Update Console (Must be running updater)");
        jMenuItem12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem12ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem12);

        jMenuItem22.setText("Show duplicate files...");
        jMenuItem22.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem22ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem22);

        jMenuBar1.add(jMenu1);

        jMenu7.setText("Actions");

        jMenuItem17.setText("Add Remote Machine");
        jMenuItem17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem17ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem17);

        jMenuItem16.setText("Add Local Directory");
        jMenuItem16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem16ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem16);

        jMenuItem20.setText("Find Machines");
        jMenuItem20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem20ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem20);

        jMenuBar1.add(jMenu7);

        jMenu2.setText("Database");

        jMenuItem5.setText("Backup");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem5);

        jMenuItem6.setText("Restore");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem6);

        jMenuItem8.setText("Delete");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem8);

        jMenuItem15.setText("Export Tracker Info");
        jMenuItem15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem15ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem15);

        jMenu8.setText("Debug to Logs");
        jMenu2.add(jMenu8);

        jMenuItem18.setText("Debug Connections");
        jMenuItem18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem18ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem18);

        jMenuItem19.setText("Flush Connections");
        jMenuItem19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem19ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem19);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("Info");

        jMenuItem9.setText("About");
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem9ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem9);

        jMenuItem10.setText("Show help");
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem10ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem10);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane2)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane2)
                .addGap(0, 0, 0))
        );

        jTabbedPane2.getAccessibleContext().setAccessibleName("Remotes");
        jTabbedPane2.getAccessibleContext().setAccessibleDescription("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        addLocal();
    }//GEN-LAST:event_jButton1ActionPerformed

		private void addLocal()
		{
			Application app = this;
			final JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			{
				return;
			}
				Services.userThreads.execute(new Runnable()
				{
					@Override
					public void run()
					{
						LocalDirectory local = UserActions.addLocalImmediately(Paths.get(fc.getSelectedFile().getAbsolutePath()), null);
						if (local == null)
						{
							return;
						}
							locals.refresh();

							Services.timer.schedule(new TimerTask() {
								@Override
								public void run()
								{
									List<SynchronizationListener> singletonList = Collections.singletonList(createLocalListener(local));
									UserActions.userSync(app, local, singletonList);
								}}, 1 * 1000);
					}
				});
		}

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        addRemoteMachine();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
		LogWrapper.getLogger().info("Synchronizing all local directories.");
                try (final DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();)
		{
			while (listLocals.hasNext())
			{
				LocalDirectory next = listLocals.next();
                                LogWrapper.getLogger().info("Synrchronizing local " + next);
				DebugListener createLocalListener = createLocalListener(next);
				List<DebugListener> singletonList = Collections.singletonList(createLocalListener);
				if (createLocalListener == null)
				{
					singletonList = null;
				}
				else
				{
					singletonList = Collections.singletonList(createLocalListener);
				}
				UserActions.userSync(this, next, singletonList);
			}
		}
		Services.notifications.localsChanged();
    }//GEN-LAST:event_jButton4ActionPerformed

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
      serves.refresh();
    }//GEN-LAST:event_jButton11ActionPerformed

    private void refreshConnectionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshConnectionsActionPerformed
        refreshConnections();
    }//GEN-LAST:event_refreshConnectionsActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
       DbDownloads.clearCompleted(); // should be moved to user actions and be run on a different thread.
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        Services.userThreads.execute(new Runnable() { @Override
				public void run()
        {
        try {
            LogWrapper.getLogger().info("Creating another key.");
            Services.keyManager.createAnotherKey(
                    Services.settings.keysFile.getPath(), 
                    Services.settings.keySize.get());
            LogWrapper.getLogger().info("Done.");
            refreshKeys();
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            LogWrapper.getLogger().log(Level.SEVERE, "No key algorithm.", ex);
            Services.quiter.quit();
        } catch (IOException ex) {
            LogWrapper.getLogger().log(Level.WARNING, "Unable to save keys.", ex);
        }
        }});
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
       Services.userThreads.execute(new Runnable() { public void run() { refreshKeys(); } });
    }//GEN-LAST:event_jButton18ActionPerformed

    private void jMenuItem13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem13ActionPerformed
    	Services.quiter.quit();
    }//GEN-LAST:event_jMenuItem13ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
    	TrackerFrame trackerFrame = new TrackerFrame();
    	Services.notifications.registerWindow(trackerFrame);
			trackerFrame.setVisible(true);
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem11ActionPerformed
    	PortMapperFrame frame = new PortMapperFrame();
    	Services.notifications.registerWindow(frame);
    	frame.setVisible(true);
    }//GEN-LAST:event_jMenuItem11ActionPerformed

    private void jCheckBoxMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItem1ActionPerformed
        Services.settings.autoMapPorts.set(jCheckBoxMenuItem1.isSelected());
    }//GEN-LAST:event_jCheckBoxMenuItem1ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        Trackers.launchTracker(true);
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jCheckBoxMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItem2ActionPerformed
        Services.settings.runTrackerOnStart.set(jCheckBoxMenuItem2.isSelected());
        
    }//GEN-LAST:event_jCheckBoxMenuItem2ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
      final JFileChooser fc = new JFileChooser();
      fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
      {
      	try
				{
					DbBackupRestore.backupDatabase(Paths.get(fc.getSelectedFile().getAbsolutePath()));
				}
				catch (IOException e)
				{
          LogWrapper.getLogger().log(Level.WARNING, "Unable to backup database.", e);
				}
      }
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			DbBackupRestore.restoreDatabase(this, Paths.get(fc.getSelectedFile().getAbsolutePath()));
		}
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
       // delete database
      if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the database? This cannot be undone.", "Are you sure?", JOptionPane.YES_NO_OPTION))
      {
      	UserActions.deleteDb();
      }
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
       // show about
    	JOptionPane.showMessageDialog(this, "About goes here...", "Convenience Share version " + Services.settings.getVersion(Main.class), JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_jMenuItem9ActionPerformed

    private void jMenuItem10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem10ActionPerformed
        // show help
        Path userManual = Services.settings.applicationDirectory.getPath().resolve(Paths.get("UserManual.pdf"));
        if (!Files.exists(userManual))
        {
            JOptionPane.showMessageDialog(this, "Unable to find user manual at " + userManual, "User Manual not found.", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Misc.nativeOpen(userManual, false);
    }//GEN-LAST:event_jMenuItem10ActionPerformed

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton17ActionPerformed
      UserActions.findTrackers();
    }//GEN-LAST:event_jButton17ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        Main.restart();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem14ActionPerformed
        dispose();
    }//GEN-LAST:event_jMenuItem14ActionPerformed

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
        switch ((String) jComboBox2.getSelectedItem())
        {
            case "All":
                LogWrapper.getLogger().setLevel(Level.ALL);
                break;
            case "Fine":
                LogWrapper.getLogger().setLevel(Level.FINE);
                break;
            case "Info":
                LogWrapper.getLogger().setLevel(Level.INFO);
                break;
            case "Warning":
                LogWrapper.getLogger().setLevel(Level.WARNING);
                break;
            case "Severe":
                LogWrapper.getLogger().setLevel(Level.SEVERE);
                break;
            case "None":
                LogWrapper.getLogger().setLevel(Level.OFF);
                break;
            default:
                LogWrapper.getLogger().info("Unknown log level: " + jComboBox2.getSelectedItem());
        }
    }//GEN-LAST:event_jComboBox2ActionPerformed

    private void jMenuItem15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem15ActionPerformed
        // export tracker info...
    	final JFileChooser fc = new JFileChooser();
      fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
      if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
      {
      	return;
      }
      
      LogWrapper.getLogger().info("Export tracker info to file " + fc.getSelectedFile());
      try
			{
				TrackerInfoExport.exportTrackerInfo(Paths.get(fc.getSelectedFile().getAbsolutePath()));
			}
			catch (IOException e)
			{
	      LogWrapper.getLogger().log(Level.INFO, "Unable to export tracker info to file " + fc.getSelectedFile(), e);
			}
    }//GEN-LAST:event_jMenuItem15ActionPerformed

    private void jMenuItem17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem17ActionPerformed
        // find remote machine
      addRemoteMachine();
    }//GEN-LAST:event_jMenuItem17ActionPerformed

		private static void addRemoteMachine()
		{
			AddMachine addMachine = new AddMachine();
      addMachine.setAlwaysOnTop(true);
      Services.notifications.registerWindow(addMachine);
      Services.colors.setColors(addMachine);
      addMachine.setVisible(true);
		}

    private void jMenuItem16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem16ActionPerformed
        // add local directory
    	addLocal();
    }//GEN-LAST:event_jMenuItem16ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        logHandler.clear();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jMenuItem18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem18ActionPerformed
       Services.h2DbCache.debug();
    }//GEN-LAST:event_jMenuItem18ActionPerformed

    private void jMenuItem19ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem19ActionPerformed
        Services.h2DbCache.flush();
    }//GEN-LAST:event_jMenuItem19ActionPerformed

    private void jMenuItem20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem20ActionPerformed
        UserActions.findMachines(this);
    }//GEN-LAST:event_jMenuItem20ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        UserActions.findMachines(this);
    }//GEN-LAST:event_jButton13ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        Boolean isEnabled = RunOnStartUp.isEnabled();
		if (isEnabled == null)
		{
			int response = JOptionPane.showConfirmDialog(
					this,
					"ConvenienceShare was not able to see if run on startup is already enabled.\n" 
							+ "More information can be found in the logs.\n" 
							+ "Would you like it to be enabled?\n", 
							"Unable to determine if ConvenienceShare is enabled.", 
					JOptionPane.YES_NO_CANCEL_OPTION);
			switch (response)
			{
			case JOptionPane.YES_OPTION:
				RunOnStartUp.runOnStartup();
				break;
			case JOptionPane.NO_OPTION:
				RunOnStartUp.doNotRunOnStartup();
				break;
			case JOptionPane.CANCEL_OPTION:
			case JOptionPane.CLOSED_OPTION:
				LogWrapper.getLogger().info("Cancelled");
				break;
			default:
				LogWrapper.getLogger().info("Unkown option chosen " + response);
			}
			return;
		}
		if (isEnabled)
		{
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
					this,
					"ConvenienceShare is currently set to run on startup.\n"
							+ "Would you like it to be disabled?\n", 
							"Disable run on startup", 
					JOptionPane.YES_NO_OPTION))
			{
				RunOnStartUp.doNotRunOnStartup();
			}
			return;
		}
		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				this,
				"ConvenienceShare is currently not set to run on startup.\n"
						+ "Would you like it to be enabled?\n", 
						"Enable run on startup", 
				JOptionPane.YES_NO_OPTION))
		{
			RunOnStartUp.runOnStartup();
		}
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jMenuItem21ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem21ActionPerformed
              UpdateFrame u = new UpdateFrame(this);
      Services.notifications.registerWindow(u);
      u.setAlwaysOnTop(true);
      u.setVisible(true);
    }//GEN-LAST:event_jMenuItem21ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        logHandler.setScrollOnUpdate(jCheckBox1.isSelected());
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jMenuItem12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem12ActionPerformed
        UpdateServerFrame frame = new UpdateServerFrame();
        Services.notifications.registerWindow(frame);
        frame.refresh();
        frame.setVisible(true);
    }//GEN-LAST:event_jMenuItem12ActionPerformed

    private void jMenuItem22ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem22ActionPerformed
        DuplicateFileFrame frame = new DuplicateFileFrame();
        Services.notifications.registerWindow(frame);
        frame.setVisible(true);
    }//GEN-LAST:event_jMenuItem22ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField addressLabel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JPanel connectionsPanel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem2;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenu jMenu8;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem12;
    private javax.swing.JMenuItem jMenuItem13;
    private javax.swing.JMenuItem jMenuItem14;
    private javax.swing.JMenuItem jMenuItem15;
    private javax.swing.JMenuItem jMenuItem16;
    private javax.swing.JMenuItem jMenuItem17;
    private javax.swing.JMenuItem jMenuItem18;
    private javax.swing.JMenuItem jMenuItem19;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem20;
    private javax.swing.JMenuItem jMenuItem21;
    private javax.swing.JMenuItem jMenuItem22;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
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
    private javax.swing.JScrollPane jScrollPane6;
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
				serves.refresh();
			}

			@Override
			public void serveRemoved(ServeInstance s)
			{
				serves.refresh();
			}

			@Override
			public void connectionOpened(Communication c)
			{
				ConnectionStatus connectionStatus = new ConnectionStatus(c);
				connectionsPanel.add(connectionStatus);
				connections.add(connectionStatus);
				Services.colors.childrenChanged(this, connectionsPanel);
				connectionStatus.setVisible(true);
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
				Services.colors.childrenChanged(this, connectionsPanel);
				refreshConnections();
			}

			@Override
			public void messagesChanged()
			{
				messages.refresh();
			}

			@Override
			public void dbException(Exception ex) {}
			@Override
			public void permissionFailure(PermissionFailureEvent event) {}
			@Override
			public void fileAdded(SharedFile file) {}
			@Override
			public void fileChanged(SharedFile file) {}
			@Override
			public void fileDeleted(SharedFile file) {}
			@Override
			public void permissionsChanged(Machine remote) {}
			@Override
			public void permissionsChanged(RemoteDirectory remote) {}

	void refreshKeys()
	{
		LinkedList<KeyPairObject> allKeys = new LinkedList<>();
		synchronized (Services.keyManager)
		{
			for (KeyPairObject pair : Services.keyManager.getPairs())
			{
				allKeys.add(pair);
			}
		}
		
		Collections.sort(allKeys, new Comparator<KeyPairObject> () {
			@Override
			public int compare(KeyPairObject o1, KeyPairObject o2)
			{
				return -Long.compare(o1.getTimeStamp(), o2.getTimeStamp());
			}});

		jPanel13.setLayout(new GridLayout(0, 1));
		
		jPanel13.removeAll();
		for (KeyPairObject pair : allKeys)
		{
			KeyPanel panel = new KeyPanel(this, pair);
			
			jPanel13.add(panel);
			panel.setVisible(true);
		}

		Services.colors.childrenChanged(this, jPanel13);
	}
}
