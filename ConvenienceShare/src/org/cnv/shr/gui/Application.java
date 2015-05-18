/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cnv.shr.gui;

import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.TableListener.TableRowListener;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.stng.Setting;
import org.cnv.shr.sync.DebugListener;

/**
 * 
 * @author John
 */
public class Application extends javax.swing.JFrame
{
    public LinkedList<MachineView> remoteViewers = new LinkedList<>();
    LinkedList<String> logMessages = new LinkedList<>();
    
	/**
	 * Creates new form Application
	 */
	public Application()
	{
		initComponents();
		initializeSettings();
		initializeLocals();
		initializeMachines();
		
		machinesList.setAutoCreateRowSorter(true);
		localsView.setAutoCreateRowSorter(true);
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
	}
        
	public synchronized void refreshLocals()
	{
		DefaultTableModel model = (DefaultTableModel) localsView.getModel();
		while (model.getRowCount() > 0)
		{
			model.removeRow(0);
		}

		int ndx = 0;
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
	
	public synchronized void refreshLocal(LocalDirectory local)
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

	public void refreshRemote(Machine machine)
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
            });
	}

	public void refreshRemote(RemoteDirectory remote)
	{
		refreshRemote(remote.getMachine());
		for (MachineView view : remoteViewers)
		{
			if (view.getMachine().equals(remote.getMachine()))
			{
				view.refreshRoot(remote);
			}
		}
	}

	public synchronized void refreshRemotes()
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
        });

		DbIterator<Machine> listRemoteMachines = DbMachines.listRemoteMachines();
		while (listRemoteMachines.hasNext())
		{
			refreshRemote(listRemoteMachines.next());
		}
	}

	public void showRemote(final Machine machine)
	{
		final JFrame frame = new JFrame();
		frame.setBounds(getBounds());
		frame.setTitle("Machine " + machine.getName());
		final MachineView viewer = new MachineView();
		// Ugly, I couldn't get netbeans/MachineViewer to work...
		// Just want to make a frame and add a panel.
		frame.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent arg0)
			{
				viewer.setBounds(0, 0, 
						frame.getRootPane().getWidth(), 
						frame.getRootPane().getHeight());
			}
		});
		
		frame.getRootPane().setLayout(null);
		frame.getRootPane().add(viewer);
		remoteViewers.add(viewer);
		
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent evt)
			{
				remoteViewers.remove(viewer);
			}
		});
		frame.setVisible(true);
		Services.logger.println("Showing remote " + machine.getName());

		java.awt.EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				viewer.setBounds(0, 0, 
						frame.getRootPane().getWidth(), 
						frame.getRootPane().getHeight());
				viewer.setMachine(machine);
			}
		});
	}

	public void refreshDownloads()
	{

	}

	public void refreshSettings()
	{
            addressLabel.setText(Services.settings.getLocalIp() + ":" + Services.settings.servePortBegin);
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
        jPanel4 = new javax.swing.JPanel();
        Debug = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
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
        jTable3 = new javax.swing.JTable();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane8 = new javax.swing.JScrollPane();
        jPanel11 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
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
                "Name", "Current Address", "Id", "Sharing", "Number of files", "Total files size"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton2))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(addressLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 704, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(addressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton2)
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

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Incoming Files"));

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Machine", "Directory", "File", "Size", "Added on", "Status", "Priority", "Local path", "Number of Mirrors", "Speed", "Percent"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false
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
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 809, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 26, Short.MAX_VALUE)
                .addContainerGap())
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 809, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 285, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(jPanel6);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1)
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

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 64, Short.MAX_VALUE)
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

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Machine", "Type", "Date", "Message"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
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
        jScrollPane7.setViewportView(jTable3);

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
                        .addComponent(jButton5)))
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Messages", jPanel9);

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 842, Short.MAX_VALUE)
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 410, Short.MAX_VALUE)
        );

        jScrollPane8.setViewportView(jPanel11);

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane8)
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane8)
        );

        jTabbedPane2.addTab("Open connections", jPanel10);

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
        
        final String whatTheUserEntered = JOptionPane.showInputDialog(this, "Enter a remote address:");
        if (whatTheUserEntered == null)
        {
            return;
        }
        UserActions.addMachine(whatTheUserEntered);
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Debug;
    private javax.swing.JTextField addressLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
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
    private javax.swing.JTable jTable3;
    private javax.swing.JTable localsView;
    private javax.swing.JSpinner logLines;
    private javax.swing.JTextArea logTextArea;
    private javax.swing.JTable machinesList;
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
				final RootDirectory root = DbRoots.getLocal(mId);
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
						showRemote(DbMachines.getMachine(mId));
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
				UserActions.removeMachine(DbMachines.getMachine(mId));
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
}
