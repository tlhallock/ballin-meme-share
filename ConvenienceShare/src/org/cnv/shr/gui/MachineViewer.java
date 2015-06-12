
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.logging.Level;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreePath;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.not.NotificationListenerAdapter;
import org.cnv.shr.gui.tbl.FilesTable;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.msg.UserMessageMessage;
import org.cnv.shr.msg.key.PermissionFailure.PermissionFailureEvent;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

/**
 *
 * @author thallock
 */
public class MachineViewer extends javax.swing.JFrame
{
    private String machineIdent;
    private String rootDirectoryName;
    private PathTreeModel model;
    private NotificationListenerAdapter listener;
    private FilesTable filesManager;
    /**
     * Creates new form RemoteViewer
     */
    public MachineViewer(Machine rMachine) {
    	machineIdent = rMachine.getIdentifier();
        initComponents();
        filesManager = new FilesTable(filesTable, this);

        pathsTable.setAutoCreateRowSorter(true);

        addPathsListener();
        addPopupMenu();
        
        filesTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                filesTree.setVisibleRowCount(filesTree.getRowCount());
                if (e.getClickCount() >= 2) {
                    final TreePath pathForLocation = filesTree.getClosestPathForLocation(e.getPoint().x, e.getPoint().y);
                    final PathTreeModelNode n = (PathTreeModelNode) pathForLocation.getPath()[pathForLocation.getPath().length - 1];
                    filesManager.setCurrentlyDisplaying(machineIdent, rootDirectoryName, n.getFileList());
                }
            }
        });
        filesTree.setScrollsOnExpand(true);
        filesTree.setLargeModel(true);
        setMachine(rMachine);
        viewNoDirectory();
        
        Services.notifications.add(createListener());
        pack();
		
		addPermissionListeners();
		
			addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent e)
				{
					model.closeConnections();
				}
			});
    }

    private void addPermissionListeners()
	{
    	// need to go to DB
		isMessaging.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent arg0)
			{
				Machine machine = getMachine();
				if (machine == null) return;
				if (machine.isLocal())
				{
					return;
				}
				if (isMessaging.isSelected() && !machine.getAllowsMessages())
				{
					machine.setAllowsMessages(true);
					machine.tryToSave();
					Services.notifications.remoteChanged(machine);
				}
				else if (!isMessaging.isSelected() && machine.getAllowsMessages())
				{
					machine.setAllowsMessages(false);
					machine.tryToSave();
					Services.notifications.remoteChanged(machine);
				}
			}
		});
        sharingWithRemoteMachine.setModel(new PermissionChanger(sharingWithRemoteMachine, getMachine().sharingWithOther())
		{
			@Override
			protected void setPermission(SharingState state)
			{
				Machine machine = getMachine();
				if (machine == null) return;
				machine.setWeShare(state);
				machine.tryToSave();
			}
		});
	}

    public RootDirectory getRootDirectory()
    {
    	if (rootDirectoryName == null)
    	{
    		return null;
    	}
    	
    	Machine machine = getMachine();
			if (machine == null) return null;
			return DbRoots.getRoot(machine, rootDirectoryName);
    }
    
	public Machine getMachine()
	{
        Machine machine = DbMachines.getMachine(machineIdent);
        if (machine == null)
        {
        	dispose();
        }
				return machine;
    }

    public void refreshRoot(final RemoteDirectory remote) {
    	Machine machine = getMachine();
        if (machine.getId() != remote.getMachine().getId()) {
            return;
        }

        setMachine(machine);

        if (rootDirectoryName == null) {
            viewNoDirectory();
            return;
        }
        if (model.getRootDirectory().getName().equals(rootDirectoryName)) {
            return;
        }
        model.setRoot(remote);
    }

    private void addPathsListener() {
        final TableListener tableListener = new TableListener(pathsTable);
        tableListener.addListener(new TableListener.TableRowListener() {
            @Override
            public void run(final int row) {
                try {
                    final String mId = tableListener.getTableValue("Name", row);
                    if (mId == null) {
                        LogWrapper.getLogger().info("Unable to find machine " + mId);
                        return;
                    }
                    Services.userThreads.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                            	Machine machine = getMachine();
                      				if (machine == null) return;
                                final RootDirectory root = DbRoots.getRoot(machine, mId);
                                if (root == null) {
                                    LogWrapper.getLogger().info("Unable to find root mid=" + machine + " name=" + mId);
                                    viewNoDirectory();
                                } else {
                                    view(root);
                                }
                            } catch (final Exception ex) {
                                LogWrapper.getLogger().log(Level.INFO, "Unable to show directory " + mId, ex);
                            }
                        }
                    });
                } catch (final Exception ex) {
                    LogWrapper.getLogger().log(Level.INFO, "Unable to show machine at index " + row, ex);
                }
            }

            @Override
            public String getString() {
                return "Show";
            }
        }, true);
    }
    private void addPopupMenu()
    {
        class LastPopupClick { int x; int y; }; final LastPopupClick lastPopupClick = new LastPopupClick();
        final JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem("Download all currently cached");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                final TreePath pathForLocation = filesTree.getClosestPathForLocation(lastPopupClick.x, lastPopupClick.y);
                final PathTreeModelNode n = (PathTreeModelNode) pathForLocation.getPath()[pathForLocation.getPath().length - 1];
                LinkedList<SharedFile> accumulator = new LinkedList<>();
                CollectingFiles display = new CollectingFiles();
                display.setLocation(getLocation());
                display.setVisible(true);
                n.getPathElement().collectAllCachedFiles(getRootDirectory(), accumulator, display);
                if (!display.confirm())
                {
                	return;
                }
                
                for (SharedFile file : accumulator)
                {
                	try
									{
										Services.downloads.download(file);
									}
									catch (IOException e)
									{
										LogWrapper.getLogger().log(Level.INFO, "Unable to download " + file, e);
									}
                }
            }
        });
        menu.add(item);
        item = new JMenuItem("Show");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                final TreePath pathForLocation = filesTree.getClosestPathForLocation(lastPopupClick.x, lastPopupClick.y);
                final PathTreeModelNode n = (PathTreeModelNode) pathForLocation.getPath()[pathForLocation.getPath().length - 1];
                n.ensureExpanded();
                LogWrapper.getLogger().info("Showing " + n);
                filesManager.setCurrentlyDisplaying(machineIdent, rootDirectoryName, n.getFileList());
            }
        });
        menu.add(item);
        item = new JMenuItem("Open");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                final TreePath pathForLocation = filesTree.getClosestPathForLocation(lastPopupClick.x, lastPopupClick.y);
                final PathTreeModelNode n = (PathTreeModelNode) pathForLocation.getPath()[pathForLocation.getPath().length - 1];
                n.ensureExpanded();
                LogWrapper.getLogger().info("Opening " + n);
                SharedFile file = n.getFile();
                if (!file.isLocal())
                {
                	// Should show message...
                	return;
                }
                
                Misc.nativeOpen(((LocalFile) file).getFsFile(), false);
            }
        });
        menu.add(item);
        item = new JMenuItem("Show in folder");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                final TreePath pathForLocation = filesTree.getClosestPathForLocation(lastPopupClick.x, lastPopupClick.y);
                final PathTreeModelNode n = (PathTreeModelNode) pathForLocation.getPath()[pathForLocation.getPath().length - 1];
                n.ensureExpanded();
                LogWrapper.getLogger().info("Opening " + n);
                SharedFile file = n.getFile();
                if (!file.isLocal())
                {
                	// Should show message...
                	return;
                }
                
                Misc.nativeOpen(((LocalFile) file).getFsFile(), true);
            }
        });
        menu.add(item);
        item = new JMenuItem("Recursively cache all subfolders");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                final TreePath pathForLocation = filesTree.getClosestPathForLocation(lastPopupClick.x, lastPopupClick.y);
                final PathTreeModelNode n = (PathTreeModelNode) pathForLocation.getPath()[pathForLocation.getPath().length - 1];
                n.syncFully();
            }
        });
        menu.add(item);
        filesTree.add(menu);

        filesTree.addMouseListener(new MouseAdapter() {
            public void doPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    lastPopupClick.x = e.getX(); lastPopupClick.y = e.getY();
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseClicked(final MouseEvent e) {
                doPopup(e);
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                doPopup(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                doPopup(e);
            }
        });
    }
    
    private PathTreeModel getRoot() {
        if (model == null) {
            model = new PathTreeModel(this);
        }
        return model;
    }

    public void setMachine(final Machine machine)
    {
        this.machineIdent = machine.getIdentifier();
        jLabel15.setText(machineIdent);
        sharingWithRemoteMachine.setSelectedItem(machine.sharingWithOther().humanReadable());
        remoteSharingWithUs.setText(machine.getSharesWithUs().humanReadable());
        machineLabel.setText((machine.isLocal() ? "Local machine: " : "") + machine.getName());
        final StringBuilder builder = new StringBuilder();
        PublicKey[] keys = DbKeys.getKeys(machine);
        for (PublicKey str : keys)
        {
        	builder.append(Misc.format(str.getEncoded())).append(", ");
        }
        String keysString = builder.toString();
        if (keysString.length() > 50)
        {
        	keysString = keysString.substring(0, 50) + "...";
        }
//        keysLabel.setText(keysString);

        final DefaultTableModel model = (DefaultTableModel) pathsTable.getModel();
        while (model.getRowCount() > 0)
        {
            model.removeRow(0);
        }

        final DbIterator<? extends RootDirectory> listRemoteDirectories = DbRoots.list(machine);
        while (listRemoteDirectories.hasNext())
        {
            model.addRow(new String[]{listRemoteDirectories.next().getName()});
        }
        
        if (machine.isLocal())
        {
            jButton1.setEnabled(false); // cannot ask to share with local
            sharingWithRemoteMachine.setEnabled(false);
            isMessaging.setSelected(true); isMessaging.setEnabled(false);
            jButton3.setEnabled(false); // cannot sync roots to local
            jButton6.setEnabled(false); jButton5.setEnabled(false);
        }
        else
        {
            jButton1.setEnabled(true); // cannot ask to share with local
            sharingWithRemoteMachine.setEnabled(true);
            isMessaging.setSelected(machine.getAllowsMessages()); isMessaging.setEnabled(true);
            jButton3.setEnabled(true);
            jButton6.setEnabled(true); jButton5.setEnabled(true);
        }
    }

    private void viewNoDirectory() {
        this.rootDirectoryName = null;
        this.descriptionLabel.setText("Select a directory.");
        this.tagsLabel.setText("Select a directory.");
        this.numFilesLabel.setText("Select a directory.");
        this.diskSpaceLabel.setText("Select a directory.");
        this.rootNameLabel.setText("None");
        this.pathField.setText("");
        filesManager.empty();
        updatePermissionBoxesLocal();

		rootIsVisibleCheckBox.setSelected(false);
		rootIsDownloadableCheckBox.setSelected(false);
    }
    private void view(final RootDirectory directory) {
    	if (directory == null) {
    		viewNoDirectory();
    		return;
    	}
        LogWrapper.getLogger().info("Showing directory " + directory.getPathElement());
        this.rootDirectoryName = directory.getName();
        this.rootNameLabel.setText(directory.getName());
        this.rootNameLabel.setText(directory.getPathElement().getFullPath());
        this.descriptionLabel.setText(directory.getDescription());
        this.tagsLabel.setText(directory.getTags());
        this.numFilesLabel.setText(directory.getTotalNumberOfFiles());
        this.pathField.setText(directory.getPathElement().getFullPath());
        this.diskSpaceLabel.setText(directory.getTotalFileSize());
        ((PathTreeModel) filesTree.getModel()).setRoot(directory);
        filesManager.refresh();
        if (directory.isLocal())
        {
        	updatePermissionBoxesLocal();
        }
        else
        {
        	updatePermissionBoxesRemote((RemoteDirectory) directory);
        }
    }

    private void updatePermissionBoxesLocal()
    {
        changePathButton.setEnabled(false);
        rootIsVisibleCheckBox.setEnabled(false); rootIsVisibleCheckBox.setSelected(true);
        rootIsDownloadableCheckBox.setEnabled(false); rootIsDownloadableCheckBox.setSelected(false);
        requestDownloadButton.setEnabled(false);
        requestShareButton.setEnabled(false);
        pin.setSelected(true); pin.setEnabled(false);
    }
    
    private void updatePermissionBoxesRemote(RemoteDirectory remote)
    {
    	SharingState state = DbPermissions.getCurrentPermissions(remote);
        changePathButton.setEnabled(true);
        rootIsVisibleCheckBox.setEnabled(false); rootIsVisibleCheckBox.setSelected(state.listable());
        rootIsDownloadableCheckBox.setEnabled(false); rootIsDownloadableCheckBox.setSelected(state.downloadable());
        requestDownloadButton.setEnabled(!rootIsDownloadableCheckBox.isSelected());
        requestShareButton.setEnabled(!rootIsVisibleCheckBox.isSelected());
        pin.setEnabled(true);
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
        jButton1 = new javax.swing.JButton();
        isMessaging = new javax.swing.JCheckBox();
        machineLabel = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        pathsTable = new javax.swing.JTable();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel3 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        filesTable = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        filesTree = new javax.swing.JTree();
        jLabel2 = new javax.swing.JLabel();
        syncStatus = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        rootIsVisibleCheckBox = new javax.swing.JCheckBox();
        requestShareButton = new javax.swing.JButton();
        rootIsDownloadableCheckBox = new javax.swing.JCheckBox();
        requestDownloadButton = new javax.swing.JButton();
        pathField = new javax.swing.JTextField();
        changePathButton = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        rootNameLabel = new javax.swing.JLabel();
        descriptionLabel = new javax.swing.JLabel();
        tagsLabel = new javax.swing.JLabel();
        numFilesLabel = new javax.swing.JLabel();
        diskSpaceLabel = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        sharingWithRemoteMachine = new javax.swing.JComboBox();
        pin = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        remoteSharingWithUs = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Machine:");

        jButton1.setText("Request");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        isMessaging.setText("Allow messages");

        machineLabel.setText("Loading...");

        jButton3.setText("Synchronize Roots");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jSplitPane1.setDividerLocation(100);

        pathsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(pathsTable);

        jSplitPane1.setLeftComponent(jScrollPane1);

        jSplitPane2.setDividerLocation(200);

        jLabel5.setText("Filter:");

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        filesTable.setModel(FilesTable.createTableModel());
        jScrollPane3.setViewportView(filesTable);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1)
                .addContainerGap())
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 660, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE))
        );

        jSplitPane2.setRightComponent(jPanel3);

        jLabel6.setText("Filter:");

        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        filesTree.setModel(getRoot());
        jScrollPane2.setViewportView(filesTree);

        jLabel2.setText("Status:");

        syncStatus.setText("not connected");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField2))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(syncStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(syncStatus))
                .addGap(6, 6, 6))
        );

        jSplitPane2.setLeftComponent(jPanel4);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2)
        );

        jTabbedPane1.addTab("Files", jPanel1);

        jLabel7.setText("Current Root:");

        jLabel8.setText("Description:");

        jLabel9.setText("Path:");

        jLabel10.setText("Tags:");

        jLabel11.setText("Number of files:");

        jLabel12.setText("Total disk space:");

        jLabel13.setText("Share status:");

        rootIsVisibleCheckBox.setText("Visible");
        rootIsVisibleCheckBox.setEnabled(false);

        requestShareButton.setText("Request");
        requestShareButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                requestShareButtonActionPerformed(evt);
            }
        });

        rootIsDownloadableCheckBox.setText("Downloadable");
        rootIsDownloadableCheckBox.setEnabled(false);

        requestDownloadButton.setText("Request");
        requestDownloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                requestDownloadButtonActionPerformed(evt);
            }
        });

        pathField.setText("None");
        pathField.setEnabled(false);

        changePathButton.setText("Change");
        changePathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changePathButtonActionPerformed(evt);
            }
        });

        jButton7.setText("Synchronize");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        rootNameLabel.setText("None");

        descriptionLabel.setText("None");

        tagsLabel.setText("None");

        numFilesLabel.setText("None");

        diskSpaceLabel.setText("None");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(0, 428, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(rootIsVisibleCheckBox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(requestShareButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(rootIsDownloadableCheckBox)
                                .addGap(18, 18, 18)
                                .addComponent(requestDownloadButton))
                            .addComponent(jButton7, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(rootNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(pathField)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(changePathButton))
                            .addComponent(descriptionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(tagsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(numFilesLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(diskSpaceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(rootNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(descriptionLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addComponent(tagsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(pathField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(changePathButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(numFilesLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(diskSpaceLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(rootIsVisibleCheckBox)
                    .addComponent(requestShareButton)
                    .addComponent(rootIsDownloadableCheckBox)
                    .addComponent(requestDownloadButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton7)
                .addContainerGap(158, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Settings", jPanel2);

        jSplitPane1.setRightComponent(jTabbedPane1);

        jButton2.setText("Send Message");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        sharingWithRemoteMachine.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        pin.setText("Pin");

        jLabel3.setText("Permissions to us:");

        jLabel4.setText("Permissions from us:");

        remoteSharingWithUs.setText("Loading");

        jLabel14.setText("Identifier:");

        jLabel15.setText("Loading...");

        jButton4.setText("Clear Local Cache");
        jButton4.setEnabled(false);

        jButton5.setText("Find Trackers");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton6.setText("Find Machines");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(24, 24, 24))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel14)
                                .addGap(18, 18, 18)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(machineLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(sharingWithRemoteMachine, 0, 163, Short.MAX_VALUE)
                            .addComponent(remoteSharingWithUs, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButton6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(isMessaging)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pin)
                        .addGap(0, 140, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jLabel3)
                    .addComponent(remoteSharingWithUs)
                    .addComponent(machineLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(sharingWithRemoteMachine, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel15)
                    .addComponent(jLabel14))
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(pin)
                    .addComponent(isMessaging)
                    .addComponent(jButton4)
                    .addComponent(jButton5)
                    .addComponent(jButton6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 391, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    	MachineViewer b = this;
    	Services.userThreads.execute(new Runnable() { @Override
			public void run() { try {
        	Machine machine = getMachine();
  				if (machine == null) return;
            Communication connection = Services.networkManager.openConnection(machine, false);
            if (connection != null) {
                connection.send(new UserMessageMessage(UserMessage.createShareRequest()));
                connection.finish();

        		JOptionPane.showMessageDialog(b, 
        				"A request to share with " + machine.getName() + " was sent.",
        				"Request sent",
        				JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            LogWrapper.getLogger().log(Level.INFO, "Unable to sent message:", ex);
        }}});
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // filter files tree
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // filter files table
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        UserActions.syncRemote(getRootDirectory());
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        Machine machine = getMachine();
				if (machine == null) return;
				UserActions.syncRoots(machine);
    }//GEN-LAST:event_jButton3ActionPerformed

    private void changePathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changePathButtonActionPerformed
        // change path
    }//GEN-LAST:event_changePathButtonActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    	Machine machine = getMachine();
			if (machine == null) return;
			CreateMessage createMessage = new CreateMessage(machine);
        Services.notifications.registerWindow(createMessage);
		createMessage.setVisible(true);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void requestDownloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_requestDownloadButtonActionPerformed
    	MachineViewer b = this;
    	Services.userThreads.execute(new Runnable() { @Override
			public void run() { try {
            Machine machine = getMachine();
    				if (machine == null) return;
						Communication connection = Services.networkManager.openConnection(machine, false);
            if (connection != null) {
            		RootDirectory directory = getRootDirectory();
                connection.send(new UserMessageMessage(UserMessage.createShareRootRequest(directory)));
                connection.finish();
                
        		JOptionPane.showMessageDialog(b, 
        				"A request for permissions to download from " + rootDirectoryName + " was sent.",
        				"Request sent",
        				JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            LogWrapper.getLogger().log(Level.INFO, "Unable to sent message:", ex);
        }}});
    }//GEN-LAST:event_requestDownloadButtonActionPerformed

    private void requestShareButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_requestShareButtonActionPerformed
    	MachineViewer b = this;
        Services.userThreads.execute(new Runnable() { @Override
				public void run() { try {
            Machine machine = getMachine();
    				if (machine == null) return;
						Communication connection = Services.networkManager.openConnection(machine, false);
            if (connection != null) {
				RootDirectory directory = getRootDirectory();
                connection.send(new UserMessageMessage(UserMessage.createListRequest(directory)));
                connection.finish();

        		JOptionPane.showMessageDialog(b, 
        				"A request for permissions to view " + rootDirectoryName + " was sent.",
        				"Request sent",
        				JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            LogWrapper.getLogger().log(Level.INFO, "Unable to sent message:", ex);
        }}});
    }//GEN-LAST:event_requestShareButtonActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        Machine machine = getMachine();
        if (!machine.isLocal())
        {
            UserActions.findMachines(machine);
        }
        else
        {
            LogWrapper.getLogger().info("Unable to find machines from local machine.");
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton changePathButton;
    private javax.swing.JLabel descriptionLabel;
    private javax.swing.JLabel diskSpaceLabel;
    private javax.swing.JTable filesTable;
    private javax.swing.JTree filesTree;
    private javax.swing.JCheckBox isMessaging;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JLabel machineLabel;
    private javax.swing.JLabel numFilesLabel;
    private javax.swing.JTextField pathField;
    private javax.swing.JTable pathsTable;
    private javax.swing.JCheckBox pin;
    private javax.swing.JLabel remoteSharingWithUs;
    private javax.swing.JButton requestDownloadButton;
    private javax.swing.JButton requestShareButton;
    private javax.swing.JCheckBox rootIsDownloadableCheckBox;
    private javax.swing.JCheckBox rootIsVisibleCheckBox;
    private javax.swing.JLabel rootNameLabel;
    private javax.swing.JComboBox sharingWithRemoteMachine;
    private javax.swing.JLabel syncStatus;
    private javax.swing.JLabel tagsLabel;
    // End of variables declaration//GEN-END:variables
    
    public void setSyncStatus(String status)
    {
        if (syncStatus != null)
            syncStatus.setText(status);
    }

	private NotificationListenerAdapter createListener()
	{
            MachineViewer viewer = this;
		return listener = new NotificationListenerAdapter()
		{
			@Override
			public void remoteChanged(Machine remote)
			{
				if (remote.getIdentifier().equals(machineIdent))
				{
					setMachine(remote);
				}
			}

			@Override
			public void remoteDirectoryChanged(RemoteDirectory remote)
			{
				RootDirectory directory = getRootDirectory();
		    	Machine machine = getMachine();
				if (directory == null || !directory.getMachine().getIdentifier().equals(machine.getIdentifier())
									  || !directory.getName().equals(remote.getName()))
				{
					return;
				}
				
				// Check if it was deleted...

				view(remote);
			}

			@Override
			public void permissionFailure(PermissionFailureEvent event)
			{
				String rootName = event.getRootName();
				if (rootName == null)
				{
					if (!event.getMachineIdent().equals(machineIdent))
					{
						return;
					}
			        remoteSharingWithUs.setText(event.getCurrentSharingState().humanReadable());
				}
				else if (rootName.equals(rootDirectoryName))
				{
					RootDirectory directory = getRootDirectory();
					if (directory.isLocal()) return;
					updatePermissionBoxesRemote((RemoteDirectory) directory);
				}
				event.show(viewer);
			}
		};
	}
}
