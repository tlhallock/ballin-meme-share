/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cnv.shr.gui;

import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbMessages;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Notifications.NotificationListener;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.gui.TableListener.TableRowListener;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.stng.Setting;
import org.cnv.shr.sync.DebugListener;
import org.cnv.shr.util.IpTester;

/**
 * 
 * @author John
 */
public class Application extends javax.swing.JFrame
{
    LinkedList<String> logMessages = new LinkedList<>();
    LinkedList<ConnectionStatus> connections = new LinkedList<>();
    NotificationListener listener;
    
	/**
	 * Creates new form Application
	 */
	public Application()
	{
		initComponents();
		initializeSettings();
		initializeLocals();
		initializeMachines();
		initializeMessages();
		initializeDownloads();
		
		connectionsPanel.setLayout(new GridLayout(0, 1));
		machinesList.setAutoCreateRowSorter(true);
		localsView.setAutoCreateRowSorter(true);
		
		this.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentMoved(ComponentEvent arg0)
			{
				Point locationOnScreen = getLocationOnScreen();
				Services.settings.appLocX.set(locationOnScreen.x);
				Services.settings.appLocY.set(locationOnScreen.y);
			}
		});
		listener = createNotificationListener();
		Services.notifications.add(listener);
	}

	private void initializeSettings()
	{
		GridLayout layout = new GridLayout(0, 4);
		settingsPanel.setLayout(layout);

		settingsPanel.add(new JLabel("Name"));
		settingsPanel.add(new JLabel("Description"));
		settingsPanel.add(new JLabel("Requirest restart"));
		settingsPanel.add(new JLabel("Modify"));

		Setting[] settings = Services.settings.getUserSettings();
		for (Setting setting : settings)
		{
			settingsPanel.add(new JLabel(setting.getName()));
			settingsPanel.add(new JLabel(setting.getDescription()));
			settingsPanel.add(new JLabel(String.valueOf(setting.requiresReboot())));
			settingsPanel.add(setting.createInput());
		}
	}

	public void log(String line)
	{
		logMessages.add(line);
		while (logMessages.size() > (Integer) logLines.getValue())
		{
			logMessages.removeFirst();
		}
		StringBuilder builder = new StringBuilder();
		for (String s : logMessages)
		{
			builder.append(s);
		}
		logTextArea.setText(builder.toString());
	}

	public void refreshAll()
	{
		refreshDownloads();
		refreshSettings();
		refreshLocals();
		refreshRemotes();
                refreshMessages();
                refreshConnections();
                refreshServes();
	}
    
	// should sync the locals table...
	private synchronized void refreshLocals()
	{
		DefaultTableModel model = (DefaultTableModel) localsView.getModel();
		while (model.getRowCount() > 0)
		{
			model.removeRow(0);
		}

		DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();
		while (listLocals.hasNext())
		{
            	LocalDirectory local = listLocals.next();
                model.addRow(new Object[] {
                    local.getPathElement().getFullPath(),
                    local.getDescription(),
                    local.getTags(),
                    new NumberOfFiles(local.numFiles()),
                    new DiskUsage(local.diskSpace()),
                });
		}
	}

	// should sync the locals table...
	private synchronized void refreshLocal(LocalDirectory local)
	{
		DefaultTableModel model = (DefaultTableModel) localsView.getModel();
		TableListener.removeIfExists(model, "Path", local.getPathElement().getFullPath());
		
        model.addRow(new String[] {
            local.getPathElement().getFullPath(),
            local.getDescription(),
            local.getTags(),
            local.getTotalNumberOfFiles(),
            local.getTotalFileSize(),
        });
	}

	// should sync the machines table...
	private synchronized void refreshRemote(Machine machine)
	{
		DefaultTableModel model = (DefaultTableModel) machinesList.getModel();
		TableListener.removeIfExists(model, "Id", machine.getIdentifier());
        model.addRow(new Object[] {
        		machine.getName(),
        		machine.getIp() + ":" + machine.getPort(),
        		machine.getIdentifier(),
                String.valueOf(machine.isSharing()),
                new NumberOfFiles(DbMachines.getTotalNumFiles(machine)),
                new DiskUsage(DbMachines.getTotalDiskspace(machine)),
                machine.getIp(),
                machine.getPort(),
                machine.getNumberOfPorts(),
            });
	}

	private void refreshRemote(RemoteDirectory remote)
	{
		refreshRemote(remote.getMachine());
	}

	// should sync the machines table...
	private synchronized void refreshRemotes()
	{
		DefaultTableModel model = (DefaultTableModel) machinesList.getModel();
		while (model.getRowCount() > 0)
		{
			model.removeRow(0);
		}

        model.addRow(new Object[] {
        	"Local machine: " + Services.localMachine.getName(),
            Services.localMachine.getIp() + ":" + Services.localMachine.getPort(),
            Services.localMachine.getIdentifier(),
            String.valueOf(Services.localMachine.isSharing()),
            new NumberOfFiles(DbMachines.getTotalNumFiles(Services.localMachine)),
            new DiskUsage(DbMachines.getTotalDiskspace(Services.localMachine)),
            Services.settings.getLocalIp(),
            Services.settings.servePortBeginE.get(),
            Services.settings.maxServes.get(),
        });

		DbIterator<Machine> listRemoteMachines = DbMachines.listRemoteMachines();
		while (listRemoteMachines.hasNext())
		{
			refreshRemote(listRemoteMachines.next());
		}
	}

	private void showRemote(final Machine machine)
	{
		final MachineViewer viewer = new MachineViewer(machine);
		Services.notifications.registerWindow(viewer);
		viewer.setBounds(getBounds());
		viewer.setTitle("Machine " + machine.getName());
		viewer.setVisible(true);
		Services.logger.println("Showing remote " + machine.getName());
	}
        
	private void refreshMessages()
	{
		synchronized (this.messageTable)
		{
			DefaultTableModel model = (DefaultTableModel) this.messageTable.getModel();
			while (model.getRowCount() > 0)
			{
				model.removeRow(0);
			}
			try (DbIterator<UserMessage> messages = DbMessages.listMessages())
			{
				while (messages.hasNext())
				{
					UserMessage message = messages.next();
					model.addRow(new Object[] { 
							message.getMachine().getName(),
							message.getMessageType().humanReadable(), 
							new Date(message.getSent()),
							message.getMessage(), 
							String.valueOf(message.getId()),
					});
				}
			}
			catch (SQLException e)
			{
				Services.logger.print(e);
			}
		}
	}
	
	private void refreshConnections()
	{
		for (ConnectionStatus status : connections)
		{
			status.refresh();
		}
	}
	
	private synchronized void refreshDownloads()
	{
		DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
		while (model.getRowCount() > 0)
		{
			model.removeRow(0);
		}
		this.maxPending.setText(String.valueOf(Services.settings.maxDownloads.get()));
		Connection connection = Services.h2DbCache.getConnection();
		try (DbIterator<Download> downloads = new DbIterator<>(connection, DbObjects.PENDING_DOWNLOAD))
		{
			while (downloads.hasNext())
			{
				Download download = downloads.next();
				SharedFile file = download.getFile();
				RootDirectory directory = file.getRootDirectory();
				Machine machine = directory.getMachine();
				DownloadInstance downloadInstance = Services.downloads.getDownloadInstanceForGui(new SharedFileId(file));
				
				model.addRow(new Object[] {
						machine.getName(),
						directory.getName(),
						file.getPath().getUnbrokenName(),
						new DiskUsage(file.getFileSize()),
						new Date(download.getAdded()),
						download.getState().humanReadable(),
						String.valueOf(download.getPriority()),
						downloadInstance == null ? download.getTargetFile() : downloadInstance.getDestinationFile().getAbsolutePath(),
						downloadInstance == null ? "N/A" : downloadInstance.getSpeed(),
						download.getState().equals(DownloadState.ALL_DONE) ?  "100%" : 
							(downloadInstance == null ? "0.0" : String.valueOf(downloadInstance.getCompletionPercentage())),
						String.valueOf(download.getId()),
				});
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
	}
	
	private synchronized void refreshServes()
	{
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
		}
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
        jScrollPane6 = new javax.swing.JScrollPane();
        settingsPanel = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
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

        setTitle("Convenience Share");

        jButton2.setText("Add...");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            @Override
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

            @Override
			public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            @Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        machinesList.setEnabled(false);
        jScrollPane4.setViewportView(machinesList);

        jButton10.setText("Refresh");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            @Override
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
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 333, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Machines", null, jPanel1, "");

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Currently Shared Directories/Files"));

        jButton1.setText("Add...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton4.setText("Synchronize All");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        localsView.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Path", "Description", "Tags", "Number of files", "Total file size"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, false, false
            };

            @Override
			public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            @Override
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
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
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

            @Override
			public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            @Override
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
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
        );

        jSplitPane1.setRightComponent(jPanel6);

        jLabel2.setText("Currently set max downloads:");

        maxPending.setText("0");

        jButton7.setText("Initiate pending");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jButton11.setText("Refresh");
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        jButton12.setText("Clear completed");
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            @Override
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
                .addComponent(jSplitPane1))
        );

        jTabbedPane2.addTab("Downloads", jPanel3);

        Debug.setText("Print Debug Info");
        Debug.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                DebugActionPerformed(evt);
            }
        });

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("My IP"));

        jButton8.setText("Test IP");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton8)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap(18, Short.MAX_VALUE)
                .addComponent(jButton8)
                .addContainerGap())
        );

        javax.swing.GroupLayout settingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
        settingsPanel.setLayout(settingsPanelLayout);
        settingsPanelLayout.setHorizontalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 859, Short.MAX_VALUE)
        );
        settingsPanelLayout.setVerticalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 333, Short.MAX_VALUE)
        );

        jScrollPane6.setViewportView(settingsPanel);

        jButton3.setText("Delete Database!");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton6.setText("Change Keys");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(Debug, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 821, Short.MAX_VALUE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE)
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
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 357, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Logs", jPanel8);

        jButton5.setText("Clear all");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            @Override
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

            @Override
			public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            @Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane7.setViewportView(messageTable);

        jButton9.setText("Refresh");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            @Override
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
                .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
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
            .addGap(0, 367, Short.MAX_VALUE)
        );

        jScrollPane8.setViewportView(connectionsPanel);

        refreshConnections.setText("Refresh");
        refreshConnections.addActionListener(new java.awt.event.ActionListener() {
            @Override
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
                .addComponent(jScrollPane8))
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
            .addGap(0, 413, Short.MAX_VALUE)
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
        	UserActions.addLocal(fc.getSelectedFile(), true, null);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        AddMachine addMachine = new AddMachine();
        Services.notifications.registerWindow(addMachine);
		addMachine.setVisible(true);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void DebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DebugActionPerformed
    	UserActions.debug();
    }//GEN-LAST:event_DebugActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
    	UserActions.syncAllLocals();
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
    	UserActions.deleteDb();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        Services.downloads.initiatePendingDownloads();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        Services.logger.println("IP test results = " + new IpTester().getIpFromCanYouSeeMeDotOrg());
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
       DbMessages.clearAll();
       refreshMessages();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        refreshMessages();
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
       refreshRemotes();
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        refreshDownloads();
        refreshServes();
    }//GEN-LAST:event_jButton11ActionPerformed

    private void refreshConnectionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshConnectionsActionPerformed
        refreshConnections();
    }//GEN-LAST:event_refreshConnectionsActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
       DbDownloads.clearCompleted(); // should be moved to user actions and be run on a different thread.
    }//GEN-LAST:event_jButton12ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Debug;
    private javax.swing.JTextField addressLabel;
    private javax.swing.JPanel connectionsPanel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
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
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
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
    private javax.swing.JPanel settingsPanel;
    // End of variables declaration//GEN-END:variables


	private void initializeLocals()
	{
		final TableListener tableListener = new TableListener(localsView);
		tableListener.addListener(new TableRowListener()
		{
			@Override
			public void run(final int row)
			{
				final String mId = tableListener.getTableValue("Path", row);
				if (mId == null)
				{
					return;
				}
				final LocalDirectory root = DbRoots.getLocal(mId);
				if (root == null)
				{
					Services.logger.println("Unable to find local directory " + mId);
					return;
				}
				Services.userThreads.execute(new Runnable()
				{
					@Override
					public void run()
					{
						LocalDirectoryView localDirectoryView = new LocalDirectoryView();
						Services.notifications.registerWindow(localDirectoryView);
						localDirectoryView.view(root);
						localDirectoryView.setVisible(true);
						Services.logger.println("Displaying " + mId);
					}
				});
			}

			@Override
			public String getString()
			{
				return "Show";
			}
		}, true).addListener(new TableRowListener()
		{
			@Override
			public void run(int row)
			{
				final String mId = tableListener.getTableValue("Path", row);
				if (mId == null)
				{
					return;
				}
				final RootDirectory root = DbRoots.getLocal(mId);
				if (root == null)
				{
					Services.logger.println("Unable to find local directory " + mId);
					return;
				}
				UserActions.remove(root);
			}

			@Override
			public String getString()
			{
				return "Delete";
			}
		}).addListener(new TableRowListener()
		{
			@Override
			public void run(final int row)
			{
				final String mId = tableListener.getTableValue("Path", row);
				if (mId == null)
				{
					return;
				}
				final LocalDirectory root = DbRoots.getLocal(mId);
				if (root == null)
				{
					Services.logger.println("Unable to find local directory " + mId);
					return;
				}
				Services.userThreads.execute(new Runnable()
				{
					@Override
					public void run()
					{
						UserActions.sync(root);
					}
				});
			}

			@Override
			public String getString()
			{
				return "Synchronize";
			}
		});
	}
	private void initializeMachines()
	{
		final TableListener tableListener = new TableListener(machinesList);
		tableListener.addListener(new TableRowListener()
		{
			@Override
			public void run(final int row)
			{
				final String mId = tableListener.getTableValue("Id", row);
				if (mId == null)
				{
					return;
				}
				Services.userThreads.execute(new Runnable()
				{
					@Override
					public void run()
					{
						Machine machine = DbMachines.getMachine(mId);
						if (machine == null)
						{
							Services.logger.println("Unable to find machine: " + mId);
							return;
						}
						showRemote(machine);
					}
				});
			}

			@Override
			public String getString()
			{
				return "Show";
			}
		}, true).addListener(new TableRowListener()
		{
			@Override
			public void run(int row)
			{
				final String mId = tableListener.getTableValue("Id", row);
				if (mId == null)
				{
					return;
				}
				Machine machine = DbMachines.getMachine(mId);
				UserActions.syncRoots(machine);
			}

			@Override
			public String getString()
			{
				return "Synchronize";
			}
		}).addListener(new TableRowListener()
		{
			@Override
			public void run(int row)
			{
				final String mId = tableListener.getTableValue("Id", row);
				if (mId == null)
				{
					return;
				}
				UserActions.removeMachine(DbMachines.getMachine(mId));
			}

			@Override
			public String getString()
			{
				return "Delete";
			}
		});
	}
	private void initializeMessages()
	{
		messageTable.setAutoCreateRowSorter(true);
		final TableListener tableListener = new TableListener(messageTable);
		tableListener.addListener(new TableRowListener()
		{
			@Override
			public void run(final int row)
			{
				UserMessage message = null;
				final String mId = tableListener.getTableValue("Id", row);
				if (mId == null)
				{
					return;
				}
				message = DbMessages.getMessage(Integer.parseInt(mId));
				if (message == null)
				{
					return;
				}
				message.open();
			}

			@Override
			public String getString()
			{
				return "Open";
			}
		}, true).addListener(new TableRowListener()
		{
			@Override
			public void run(int row)
			{
				String mId = null;
				mId = tableListener.getTableValue("Id", row);
				if (mId == null)
				{
					return;
				}
				DbMessages.deleteMessage(Integer.parseInt(mId));
				refreshMessages();
			}

			@Override
			public String getString()
			{
				return "Delete";
			}
		});
	}
	private void initializeDownloads()
	{
		messageTable.setAutoCreateRowSorter(true);
		final TableListener tableListener = new TableListener(messageTable);
		tableListener.addListener(new TableRowListener()
		{
			@Override
			public void run(int row)
			{
				String mId = null;
				mId = tableListener.getTableValue("Id", row);
				if (mId == null)
				{
					return;
				}
				Download download = DbDownloads.getDownload(Integer.parseInt(mId));
				DownloadInstance dInstance = Services.downloads.getDownloadInstanceForGui(new SharedFileId(download.getFile()));
				if (dInstance != null)
				{
					dInstance.fail("User quit.");
				}
				download.delete();
			}

			@Override
			public String getString()
			{
				return "Delete";
			}
		});
	}

	DebugListener createLocalListener(final LocalDirectory root)
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
//					return;
				}
				
				lastGuiUpdate = now;
				model.setValueAt(new DiskUsage(startSize + bytesAdded), dirRow, 4);
				model.setValueAt(new NumberOfFiles(startNumFiles + filesAdded - filesRemoved), dirRow, 3);
			}
		};
	}
	
	private NotificationListener createNotificationListener()
	{
		return new NotificationListener() {
			@Override
			public void localsChanged()
			{
				refreshLocals();
			}

			@Override
			public void localDirectoryChanged(LocalDirectory local)
			{
				refreshLocal(local);
			}

			@Override
			public void remotesChanged()
			{
				refreshRemotes();
			}
			
			@Override
			public void remoteChanged(Machine remote)
			{
				// Should check if it was deleted...
				refreshRemote(remote);
			}

			@Override
			public void remoteDirectoryChanged(RemoteDirectory remote)
			{
				refreshRemote(remote);
			}

			@Override
			public void downloadAdded(DownloadInstance d)
			{
				refreshDownloads();
			}

			@Override
			public void downloadRemoved(DownloadInstance d)
			{
				refreshDownloads();
			}

			@Override
			public void downloadDone(DownloadInstance d)
			{
				refreshDownloads();
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
				refreshMessages();
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

			@Override
			public void dbException(Exception ex)
			{
			}
		};
	}
}
