/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cnv.shr.gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.LogWrapper;

/**
 *
 * @author thallock
 */
public class DuplicateFileFrame extends javax.swing.JFrame {

    private final HashSet<Integer> machines = new HashSet<>();
    private final HashMap<String, RootThingy> roots = new HashMap<>();
    private final LinkedList<DuplicateFileEntry> entries = new LinkedList<>();
    private boolean modified;
    
    private final Object searchThreadSync = new Object();
    private Thread searchThread;
    
    /**
     * Creates new form DuplicateFileFrame
     */
    public DuplicateFileFrame() {
        initComponents();
        listPanel.setLayout(new GridLayout(0, 1));
        machinesList.setLayout(new GridLayout(0, 1));
        refresh();
        
        addWindowListener(new WindowAdapter() {
        	@Override
        	public void windowClosed(WindowEvent e)
        	{
        		cancelSearch();
        	}
        });
//        timer.scheduleAtFixedRate(new TimerTask() {
//					@Override
//					public void run()
//					{
//						if (!modified)
//						{
//							return;
//						}
//						modified = false;
//						Collection<DuplicateFileEntry> entryCollection;
//						synchronized (entries)
//						{
//							entryCollection = (Collection<DuplicateFileEntry>) entries.clone();
//						}
//						for (DuplicateFileEntry entry : entryCollection)
//						{
//							entry.sizeText(listPanel);
//						}
//					}}, 1000, 1000);
        addComponentListener(new ComponentAdapter() {@Override
        public void componentResized(ComponentEvent e)
        {
        	modified = true;
        }});
    }
    
    private void refresh()
    {
    	setMachines();
    	setRoots();
    }

	private void setMachines()
	{
		LogWrapper.getLogger().info("Setting machines.");

		Set<Integer> clone = (Set<Integer>) machines.clone();
		machines.clear();
		machinesList.removeAll();
		try (DbIterator<Machine> machineIterator = DbMachines.listMachines())
		{
			while (machineIterator.hasNext())
			{
				Machine machine = machineIterator.next();
				boolean contains = clone.contains(machine.getId());
				JRadioButton button = new JRadioButton(machine.getName(), contains);
				int id = machine.getId();
				button.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						if (button.isSelected())
						{
							machines.add(id);
						}
						else
						{
							machines.remove(id);
						}
						setRoots();
					}
				});
				button.setVisible(true);
				machinesList.add(button);
				if (contains)
				{
					machines.add(machine.getId());
				}
			}
		}
		Services.colors.childrenChanged(this, machinesList);
		repaint();
	}

	private void setRoots()
	{
		LogWrapper.getLogger().info("Setting roots.");
		roots.clear();
		DefaultListModel model = (DefaultListModel) rootsList.getModel();
		model.clear();

		if (machines.isEmpty())
		{
			return;
		}

		DbLocals locals = new DbLocals();
		try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection(); 
				 StatementWrapper statement = connection.prepareNewStatement("select MNAME, RNAME, R_ID, ROOT.IS_LOCAL from ROOT join MACHINE on ROOT.MID=MACHINE.M_ID where MID in " 
		+ createParamList(machines) + ";"); 
				 ResultSet results = statement.executeQuery();)
		{
			while (results.next())
			{
				int ndx = 1;
				String machineName = results.getString(ndx++);
				String name = results.getString(ndx++);
				int id = results.getInt(ndx++);
				boolean local = results.getBoolean(ndx++);
				String key = machineName + " : " + name;
				roots.put(key, new RootThingy(id, local));
				model.addElement(key);
			}
		}
		catch (SQLException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list all roots", ex);
		}
	}

	private void search()
	{
		listPanel.removeAll();
		synchronized (entries)
		{
			entries.clear();
		}
		Services.colors.childrenChanged(this, listPanel);
		
		HashMap<Integer, RootThingy> selectedThingies = new HashMap<>();
		// TODO: Same hash, different file size?
		HashMap<String, DuplicateFilePanel> panels = new HashMap<>();
		
		for (Object value : rootsList.getSelectedValuesList())
		{
			RootThingy e = roots.get(value);
			selectedThingies.put(e.id, e);
		}

		if (selectedThingies.isEmpty())
		{
			if (machines.isEmpty())
			{
				JOptionPane.showMessageDialog(this, "Please select a machine.", "No machines selected", JOptionPane.INFORMATION_MESSAGE);
			}
			else
			{
				JOptionPane.showMessageDialog(this, "Please select at least one root.", "No roots selected", JOptionPane.INFORMATION_MESSAGE);
			}
			return;
		}
		
		DbLocals locals = new DbLocals();
		
		String rootList = createParamList(selectedThingies.keySet());
		LogWrapper.getLogger().info("Searching with roots: " + rootList);

		
		boolean includeZeroSizeFiles = jCheckBox1.isSelected();

		DuplicateFileCancelFrame cancel = null;
		try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();
				 StatementWrapper statement = connection.prepareNewStatement(
						"select distinct * from SFILE as t1 "
								+ " where t1.ROOT in " + rootList 
								+ (includeZeroSizeFiles ? "" : " and t1.FSIZE > 0 ")
								+ " and exists(select F_ID from SFILE as t2 " 
								+ "     where  t2.CHKSUM = t1.CHKSUM " 
								+ " 		and    t2.FSIZE  = t1.FSIZE "
								+ " 		and    t2.F_ID  <> t1.F_ID "
								+ " 		and    t2.ROOT in " + rootList
								+ "     limit 1 "
								+ ") order by FSIZE desc, CHKSUM;");
				ResultSet results = statement.executeQuery();)
		{
			cancel = startSearch();
			
			while (results.next())
			{
//				int id = results.getInt("ROOT");
				SharedFile allocate = (SharedFile) DbObjects.SFILE.create(connection, results);
				LogWrapper.getLogger().fine("Found file " + allocate);
				
				String checksum = allocate.getChecksum();
				long fileSize = allocate.getFileSize();
				
				DuplicateFilePanel currentPanel = panels.get(checksum);
				if (currentPanel == null)
				{
					currentPanel = new DuplicateFilePanel(checksum, fileSize);
					currentPanel.setVisible(true);
					listPanel.add(currentPanel);
					panels.put(checksum, currentPanel);
					Services.colors.componentAdded(this, currentPanel, listPanel);
				}
				DuplicateFileEntry add = currentPanel.add(allocate);
				if (add != null)
				{
					synchronized (entries)
					{
						entries.add(add);
						modified = true;
					}
				}
				Services.colors.childrenChanged(this, currentPanel);
				repaint();
				Thread.sleep(50);
			}
		}
		catch (InterruptedException ex)
		{
			LogWrapper.getLogger().info("Search interrupted: " + ex.getMessage());
		}
		catch (Exception ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to search", ex);
		}
		finally
		{
			searchDone(cancel);
		}
		LogWrapper.getLogger().info("Done searching.");
	}

	private void searchDone(DuplicateFileCancelFrame cancel)
	{
		synchronized (searchThreadSync)
		{
			if (cancel != null)
			{
				cancel.dispose();
			}
			searchThread = null;
			jButton1.setEnabled(true);
			jCheckBox1.setEnabled(true);
		}
	}
	
	void cancelSearch()
	{
		synchronized (searchThreadSync)
		{
			if (searchThread == null)
			{
				return;
			}
			searchThread.interrupt();
		}
	}

	private synchronized DuplicateFileCancelFrame startSearch()
	{
		synchronized (searchThreadSync)
		{
			cancelSearch();
			searchThread = Thread.currentThread();
			DuplicateFileCancelFrame cancel = new DuplicateFileCancelFrame(this);
			Services.notifications.registerWindow(cancel);
			cancel.setAlwaysOnTop(true);
			cancel.setVisible(true);
			jButton1.setEnabled(false);
			jCheckBox1.setEnabled(false);
			return cancel;
		}
	}

    private static String createParamList(Collection<Integer> ids)
    {
        if (ids.isEmpty())
        {
            throw new RuntimeException("Cannot create param list of an empty list.");
        }
        
        Iterator<Integer> it = ids.iterator();        
        
        StringBuilder builder = new StringBuilder(4 + 3 * (ids.size() - 1));
        builder.append("( ").append(it.next());
        while (it.hasNext())
        {
            builder.append(", ").append(it.next());
        }
        return builder.append(")").toString();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        machinesList = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        rootsList = new javax.swing.JList();
        jPanel3 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        listPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Duplicate Files");

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jSplitPane2.setDividerLocation(200);

        jLabel1.setText("On machines:");

        javax.swing.GroupLayout machinesListLayout = new javax.swing.GroupLayout(machinesList);
        machinesList.setLayout(machinesListLayout);
        machinesListLayout.setHorizontalGroup(
            machinesListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 157, Short.MAX_VALUE)
        );
        machinesListLayout.setVerticalGroup(
            machinesListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(machinesList);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane2.setLeftComponent(jPanel1);

        jLabel2.setText("On roots:");

        rootsList.setModel(new DefaultListModel());
        jScrollPane2.setViewportView(rootsList);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(0, 404, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane2.setRightComponent(jPanel2);

        jSplitPane1.setTopComponent(jSplitPane2);

        jButton1.setText("Search!");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout listPanelLayout = new javax.swing.GroupLayout(listPanel);
        listPanel.setLayout(listPanelLayout);
        listPanelLayout.setHorizontalGroup(
            listPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 703, Short.MAX_VALUE)
        );
        listPanelLayout.setVerticalGroup(
            listPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 203, Short.MAX_VALUE)
        );

        jScrollPane3.setViewportView(listPanel);

        jLabel3.setText("Filters could go here if useful...");

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Include size 0 files");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 417, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jCheckBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jLabel3)
                    .addComponent(jCheckBox1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(jPanel3);

        jMenu1.setText("Actions");

        jMenuItem2.setText("Refresh Machines");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuItem3.setText("Hide");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    	new SwingWorker<Void, Void>()
			{
				@Override
				protected Void doInBackground() throws Exception
				{
          search();
					return null;
				}
			}.execute();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
    	new SwingWorker<Void, Void>()
			{
				@Override
				protected Void doInBackground() throws Exception
				{
					refresh();
					return null;
				}
			}.execute();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        dispose();
    }//GEN-LAST:event_jMenuItem3ActionPerformed
    
    static class RootThingy
    {
    	int id;
    	boolean local;

    	RootThingy(int id, boolean local)
    	{
    		this.id = id;
    		this.local = local;
    	}
    }
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JPanel listPanel;
    private javax.swing.JPanel machinesList;
    private javax.swing.JList rootsList;
    // End of variables declaration//GEN-END:variables
}
