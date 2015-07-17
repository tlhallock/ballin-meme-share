
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.logging.Level;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreePath;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionParams.AutoCloseConnectionParams;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbRootPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.db.h2.bak.CleanBrowsingHistory;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.not.NotificationListenerAdapter;
import org.cnv.shr.gui.tbl.FilesTable;
import org.cnv.shr.mdl.LocalDirectory;
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
    
    private PathTreeModelNode currentFilesNode;
    private FilesTable filesManager;
    /**
     * Creates new form RemoteViewer
     */
    public MachineViewer(Machine rMachine) {
    	machineIdent = rMachine.getIdentifier();
        initComponents();
        syncStatus.setOpaque(true);
        filesManager = new FilesTable(filesTable, this, numFilesShowingLabel);

        pathsTable.setAutoCreateRowSorter(true);

        addPathsListener();
        addPopupMenu();
        
      	filesTree.setToggleClickCount(99999999);
        filesTree.setScrollsOnExpand(true);
        filesTree.setLargeModel(true);
        setSyncStatus(Color.GRAY, Color.WHITE, "Not viewing any root.");
        setMachine(rMachine);
        viewNoDirectory();
        
        Services.notifications.add(createListener());
        pack();
		
        addPermissionListeners();
        
        pin.addActionListener((ActionEvent e) ->
				{
					Machine machine = getMachine();
					boolean selected = pin.isSelected();
					if (machine == null || machine.isLocal() || machine.isPinned() == selected)
					{
						return;
					}
					LogWrapper.getLogger().info("Setting pin of " + machine.getName() + " to " + selected);
					machine.setPinned(selected);
					machine.tryToSave();
				});

    Misc.timer.scheduleAtFixedRate(model, PathTreeModel.INACTIVITY_DELAY, PathTreeModel.INACTIVITY_DELAY);
		addWindowListener(model);
		jCheckBox1.addActionListener((ActionEvent e) -> {
			Services.userThreads.execute(() -> {
				if (currentFilesNode == null)
				{
					return;
				}
				filesManager.setCurrentlyDisplaying(machineIdent, rootDirectoryName, currentFilesNode.getFileList(jCheckBox1.isSelected()));
			});
		});
	}

    private void addPermissionListeners()
	{
    	// need to go to DB
		isMessaging.addActionListener((ActionEvent arg0) ->
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
                    Services.userThreads.execute(() -> {
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
	                    }});
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
    
    private int popUpX, popUpY;
    private void addPopupMenu()
    {
    		MachineViewer v = this;
        final JPopupMenu menu = new JPopupMenu()
        {
        	@Override
        	public void show(Component invoker, int x, int y)
        	{
        		popUpX = x; popUpY = y;
        		super.show(invoker, x, y);
        	}
        };
        JMenuItem item;
        item = new JMenuItem("Recursively cache all subfolders");
        item.addActionListener((ActionEvent ae) -> {
          final TreePath pathForLocation = filesTree.getClosestPathForLocation(popUpX, popUpY);
          final PathTreeModelNode n = (PathTreeModelNode) pathForLocation.getPath()[pathForLocation.getPath().length - 1];

    			try
    			{
    				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    				n.syncFully(getRootDirectory());
    			}
    			finally
    			{
    				setCursor(Cursor.getDefaultCursor());
    			}
        });
        menu.add(item);
        item = new JMenuItem("Download all currently cached");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                final TreePath pathForLocation = filesTree.getClosestPathForLocation(popUpX, popUpY);
                final PathTreeModelNode n = (PathTreeModelNode) pathForLocation.getPath()[pathForLocation.getPath().length - 1];
                Services.userThreads.execute(() ->
                {
            			try
            			{
            				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    CollectingFiles display = new CollectingFiles();
                    display.setLocation(getLocation());
                    display.setVisible(true);
                    display.setAlwaysOnTop(true);
                    n.getPathElement().downloadAllCurrentlyCached(getRootDirectory(), display);
                    display.done();
            			}
            			finally
            			{
            				setCursor(Cursor.getDefaultCursor());
            			}
                });
            }
        });
        menu.add(item);
        item = new JMenuItem("Show");
        item.addActionListener((ActionEvent ae) -> {
                final TreePath pathForLocation = filesTree.getClosestPathForLocation(popUpX, popUpY);
                currentFilesNode = (PathTreeModelNode) pathForLocation.getPath()[pathForLocation.getPath().length - 1];
                currentFilesNode.ensureExpanded();
                LogWrapper.getLogger().info("Showing " + currentFilesNode);
                    filesManager.setCurrentlyDisplaying(machineIdent, rootDirectoryName, currentFilesNode.getFileList(jCheckBox1.isSelected()));
            });
        menu.add(item);
        item = new JMenuItem("Open");
        item.addActionListener((ActionEvent ae) -> {
          final TreePath pathForLocation = filesTree.getClosestPathForLocation(popUpX, popUpY);
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
        });
        menu.add(item);
        item = new JMenuItem("Show in folder");
        item.addActionListener((ActionEvent ae) -> {
          final TreePath pathForLocation = filesTree.getClosestPathForLocation(popUpX, popUpY);
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
        });
        menu.add(item);
        item = new JMenuItem("Set tags...");
        item.addActionListener((ActionEvent ae) -> {
          RootDirectory root = getRootDirectory();
          if (!(root instanceof LocalDirectory))
          {
          	JOptionPane.showMessageDialog(
          			v,
          			"It is only possible to set tags of a local directory. You either have not selected a directory or are viewing a remote directory",
          			"Can only set local tags.",
          			JOptionPane.INFORMATION_MESSAGE);
          	return;
          }
          LocalDirectory local = (LocalDirectory) root;
          
          final TreePath pathForLocation = filesTree.getClosestPathForLocation(popUpX, popUpY);
          final PathTreeModelNode n = (PathTreeModelNode) pathForLocation.getPath()[pathForLocation.getPath().length - 1];
          Services.userThreads.execute(() ->
          {
          	LogWrapper.getLogger().info("Settings tags of files in " + local + "." + n.getPathElement().getFullPath());
          	SetTagsFrame setTagsFrame = new SetTagsFrame(local, n.getFileList(true));
          	Services.notifications.registerWindow(setTagsFrame);
          	setTagsFrame.setVisible(true);
          });
        });
        menu.add(item);
        filesTree.add(menu);
        filesTree.setComponentPopupMenu(menu);

        filesTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
	            filesTree.setVisibleRowCount(filesTree.getRowCount());
	            if (e.getClickCount() < 2) {
	            	return;
	            }
	            try
	            {
		    				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	              final TreePath pathForLocation = filesTree.getClosestPathForLocation(e.getPoint().x, e.getPoint().y);
	              currentFilesNode = (PathTreeModelNode) pathForLocation.getPath()[pathForLocation.getPath().length - 1];
	              // Try our best to toggle (doesn't actually work)...
	              if (filesTree.isExpanded(pathForLocation))
	              	filesTree.collapsePath(pathForLocation);
	              else
	              	filesTree.expandPath(pathForLocation);
	              filesTree.expandPath(pathForLocation);
	              filesManager.setCurrentlyDisplaying(machineIdent, rootDirectoryName, currentFilesNode.getFileList(jCheckBox1.isSelected()));
	            }
	            finally
	            {
		    				setCursor(Cursor.getDefaultCursor());
	            }
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

        try (final DbIterator<? extends RootDirectory> listRemoteDirectories = DbRoots.list(machine);)
        {
	        while (listRemoteDirectories.hasNext())
	        {
	            model.addRow(new String[]{listRemoteDirectories.next().getName()});
	        }
        }
        
        if (machine.isLocal())
        {
            jButton1.setEnabled(false); // cannot ask to share with local
            sharingWithRemoteMachine.setEnabled(false);
            isMessaging.setSelected(true); isMessaging.setEnabled(false);
            jButton3.setEnabled(false); // cannot sync roots to local
            jButton6.setEnabled(false); jButton5.setEnabled(false);
            pin.setSelected(true); pin.setEnabled(false);
          	jCheckBox2.setSelected(false); jCheckBox2.setEnabled(false);
          	jButton4.setEnabled(false);
        }
        else
        {
            jButton1.setEnabled(true); // cannot ask to share with local
            sharingWithRemoteMachine.setEnabled(true);
            isMessaging.setSelected(machine.getAllowsMessages()); isMessaging.setEnabled(true);
            jButton3.setEnabled(true);
            jButton6.setEnabled(true); jButton5.setEnabled(true);
            pin.setSelected(machine.isPinned()); pin.setEnabled(true);
          	jCheckBox2.setSelected(Services.blackList.contains(machine.getIdentifier())); jCheckBox2.setEnabled(true);
          	jButton4.setEnabled(true);
        }
    }

    private void viewNoDirectory() {
    		jButton9.setEnabled(false);
        this.rootDirectoryName = null;
        jCheckBox1.setEnabled(false);
        jTextField1.setEnabled(false);
        jTextField2.setEnabled(false);
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
        LogWrapper.getLogger().info("Showing directory " + directory.getPath());
        jCheckBox1.setEnabled(true); jCheckBox1.setSelected(false);
        jTextField1.setEnabled(true); jTextField1.setText("");
        jTextField2.setEnabled(true); jTextField2.setText("");
        this.rootDirectoryName = directory.getName();
        this.rootNameLabel.setText(rootDirectoryName); rootNameLabel.setMinimumSize(new Dimension(5, 5));
        this.descriptionLabel.setText(directory.getDescription());
        this.tagsLabel.setText(directory.getTags());
        this.numFilesLabel.setText(directory.getTotalNumberOfFiles());
        this.pathField.setText(directory.getPath()); pathField.setMinimumSize(new Dimension(5, 5));
        this.diskSpaceLabel.setText(directory.getTotalFileSize());
        ((PathTreeModel) filesTree.getModel()).setRoot(directory);
        filesManager.setFilters("", "");
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
      jButton4.setEnabled(false);
      jButton9.setEnabled(false);
    }
    
    private void updatePermissionBoxesRemote(RemoteDirectory remote)
    {
    	SharingState state = remote.getSharesWithUs();
      changePathButton.setEnabled(true);
      rootIsVisibleCheckBox.setEnabled(false); rootIsVisibleCheckBox.setSelected(state.listable());
      rootIsDownloadableCheckBox.setEnabled(false); rootIsDownloadableCheckBox.setSelected(state.downloadable());
      requestDownloadButton.setEnabled(!rootIsDownloadableCheckBox.isSelected());
      requestShareButton.setEnabled(!rootIsVisibleCheckBox.isSelected());
      jButton4.setEnabled(true);
      jButton9.setEnabled(true);
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
        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jButton8 = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        numFilesShowingLabel = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        filesTree = new javax.swing.JTree();
        jLabel2 = new javax.swing.JLabel();
        syncStatus = new JLabel() {};
        jButton9 = new javax.swing.JButton();
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
        jCheckBox2 = new javax.swing.JCheckBox();

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

        jSplitPane2.setDividerLocation(350);

        jLabel5.setText("Filter paths:");

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        filesTable.setModel(FilesTable.createTableModel());
        jScrollPane3.setViewportView(filesTable);

        jCheckBox1.setText("Recurse");

        jLabel6.setText("Filter tags:");

        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        jButton8.setText("Filter!");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jLabel16.setText("Showing");

        numFilesShowingLabel.setText("0");

        jLabel17.setText("cached file(s).");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 510, Short.MAX_VALUE)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jTextField1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jCheckBox1))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jTextField2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton8))))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(numFilesShowingLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel17)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(jCheckBox1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(numFilesShowingLabel)
                    .addComponent(jLabel17))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 271, Short.MAX_VALUE))
        );

        jSplitPane2.setRightComponent(jPanel3);

        filesTree.setModel(getRoot());
        jScrollPane2.setViewportView(filesTree);

        jLabel2.setText("Status:");

        syncStatus.setText("not connected");

        jButton9.setText("Reconnect");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(syncStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
                .addGap(1, 1, 1)
                .addComponent(jButton9))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(syncStatus)
                    .addComponent(jButton9))
                .addGap(1, 1, 1))
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
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

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

        jCheckBox2.setText("Blacklist");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jCheckBox2)
                        .addGap(0, 52, Short.MAX_VALUE)))
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
                    .addComponent(machineLabel)
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
                    .addComponent(jButton6)
                    .addComponent(jCheckBox2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 391, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        	Machine machine = getMachine();
  				if (machine == null) return;
          Services.networkManager.openConnection(new AutoCloseConnectionParams(this, machine, false, "Send share request") {
						@Override
						public void opened(Communication connection) throws Exception
						{
	          	connection.send(new UserMessageMessage(UserMessage.createShareRequest()));
						}
					});

        	JOptionPane.showMessageDialog(this, 
        			"A request to share with " + machine.getName() + " was sent.",
        			"Request sent",
        			JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        filterFilesTable();
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
    	RootDirectory rootDirectory = getRootDirectory();
    	if (rootDirectory == null)
    	{
    		JOptionPane.showMessageDialog(this, "Please select a directory first.", "No directory selected", JOptionPane.INFORMATION_MESSAGE);
    		return;
    	}
			UserActions.syncRemote(this, rootDirectory);
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        Machine machine = getMachine();
				if (machine == null) return;
				UserActions.syncRoots(this, machine);
    }//GEN-LAST:event_jButton3ActionPerformed

    private void changePathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changePathButtonActionPerformed
        // change path
	  	RootDirectory root = getRootDirectory();
			RemoteDirectory remoteDir = (RemoteDirectory) root;
	  	if (!(root instanceof RemoteDirectory))
	  	{
	  		LogWrapper.getLogger().info("Cannot change path of local directory");
	  		return;
	  	}
			final JFileChooser fc = new JFileChooser();
			fc.setCurrentDirectory(new File(pathField.getText()));
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			{
				return;
			}
			String newPath = fc.getSelectedFile().getAbsolutePath();
			String oldPath = remoteDir.getLocalRoot().toString();
			int showConfirmDialog = JOptionPane.showConfirmDialog(this, 
					"Changing location of local mirror for directory " + remoteDir.getName() + ".\n"
					+ "Please be sure to move any contents of \n" + oldPath + "\nto\n" + newPath + ".\n"
					+ "Would you like to open these directories now?",
					"Changing local mirror for " + remoteDir.getName(), 
					JOptionPane.YES_NO_CANCEL_OPTION);

			try
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				switch (showConfirmDialog)
				{
				case JOptionPane.YES_OPTION:
					Misc.ensureDirectory(Paths.get(oldPath), false);
					Misc.ensureDirectory(Paths.get(newPath), false);
					Misc.nativeOpen(Paths.get(oldPath),  false);
					Misc.nativeOpen(Paths.get(newPath),  false);
					break;
				case JOptionPane.NO_OPTION:
					break;
				case JOptionPane.CANCEL_OPTION:
					return;
					default:
						return;
				}
				
				int rootPath = DbRootPaths.getRootPath(remoteDir.getPath());
				remoteDir.setLocalMirror(Paths.get(newPath));
				DbRootPaths.removeRootPath(rootPath);
	      this.pathField.setText(remoteDir.getPath());
			}
			finally
			{
				setCursor(Cursor.getDefaultCursor());
			}
    }//GEN-LAST:event_changePathButtonActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    	Machine machine = getMachine();
			if (machine == null) return;
			CreateMessage createMessage = new CreateMessage(machine);
        Services.notifications.registerWindow(createMessage);
		createMessage.setVisible(true);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void requestDownloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_requestDownloadButtonActionPerformed
        
    	Machine machine = getMachine();
    	if (machine == null) return;
			Services.networkManager.openConnection(new AutoCloseConnectionParams(this, machine, false, "Send share root request") {
				@Override
				public void opened(Communication connection) throws Exception
				{
         		RootDirectory directory = getRootDirectory();
            connection.send(new UserMessageMessage(UserMessage.createShareRootRequest(directory)));
				}
			});
            
      JOptionPane.showMessageDialog(this, 
      		"A request for permissions to download from " + rootDirectoryName + " was sent.",
      		"Request sent",
      		JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_requestDownloadButtonActionPerformed

    private void requestShareButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_requestShareButtonActionPerformed
        Machine machine = getMachine();
    		if (machine == null) return;
				Services.networkManager.openConnection(new AutoCloseConnectionParams(this, machine, false, "List directories")
				{
					@Override
					public void opened(Communication connection) throws Exception
					{
        		RootDirectory directory = getRootDirectory();
            connection.send(new UserMessageMessage(UserMessage.createListRequest(directory)));
					}
				});

        JOptionPane.showMessageDialog(this, 
        		"A request for permissions to view " + rootDirectoryName + " was sent.",
        		"Request sent",
        		JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_requestShareButtonActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        Machine machine = getMachine();
        JOptionPane.showMessageDialog(
                this,
                "Sending a request to list trackers to " + machine.getName(),
                "Sending request...",
                JOptionPane.INFORMATION_MESSAGE);
        UserActions.findTrackers(this, machine);
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        Machine machine = getMachine();
        if (!machine.isLocal())
        {
            UserActions.findMachines(this, machine);
        }
        else
        {
            LogWrapper.getLogger().info("Unable to find machines from local machine.");
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
    	filterFilesTable();
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        filterFilesTable();
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        Machine machine = getMachine();
        String ident = machine.getIdentifier();
        if (machine.isLocal())
        {
        	return;
        }
        Services.blackList.setBlacklisted(ident, jCheckBox2.isSelected());
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
			try
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    		jButton4.setEnabled(false);
    		pathsTable.setEnabled(false);
    		viewNoDirectory();
        CleanBrowsingHistory.removeAllNonEssentialData(getMachine());
        jButton4.setEnabled(!getMachine().isLocal());
    		pathsTable.setEnabled(true);
			}
			finally
			{
				setCursor(Cursor.getDefaultCursor());
			}
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        Services.userThreads.execute(() -> { model.resetRoot(); });
    }//GEN-LAST:event_jButton9ActionPerformed

    private void filterFilesTable()
    {
    	filesManager.setFilters(jTextField1.getText(), jTextField2.getText());
    }
    
    
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
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
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
    private javax.swing.JLabel numFilesShowingLabel;
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
    
    public void setSyncStatus(Color back, Color fore, String status)
    {
        if (syncStatus != null)
        {
            syncStatus.setText(status);
            syncStatus.setBackground(back);
            syncStatus.setForeground(fore);
            syncStatus.repaint();
        }
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
					updateRootPermissions(directory);
				}
				event.show(viewer);
			}
			
			private void updateRootPermissions(RootDirectory directory)
			{
				if (directory.isLocal()) return;
				updatePermissionBoxesRemote((RemoteDirectory) directory);
			}

			@Override
			public void permissionsChanged(Machine remote)
			{
				if (machineIdent == null || !remote.getIdentifier().equals(machineIdent))
				{
					return;
				}
		    remoteSharingWithUs.setText(remote.getSharesWithUs().humanReadable());
			}
			
			@Override
			public void permissionsChanged(RemoteDirectory remote)
			{
				if (machineIdent == null || !remote.getMachine().getIdentifier().equals(machineIdent)
						|| rootDirectoryName == null || !remote.getName().equals(rootDirectoryName))
				{
					return;
				}
				updateRootPermissions(remote);
			}
		};
	}
}
