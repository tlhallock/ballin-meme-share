package org.cnv.shr.test;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Main;
import org.cnv.shr.gui.UserActions;

public class TestActions
{
	private static LinkedList<TestAction> actions = new LinkedList<>();
	
	public static abstract class TestAction implements Serializable
	{
		public abstract void perform() throws Exception;
	}

//	public static TestAction REMOVE_MACHINE = new TestAction("REMOVE_MACHINE")
//	{
//		public void perform()
//		{
//			GuiActions.removeMachine(remote);
//		}
//	};
	
	public static class AddMachine extends TestAction
	{
		private String url;
		
		public AddMachine(String url)
		{
			this.url = url;
		}
		
		public void perform()
		{
			UserActions.addMachine(url);
		}
	};
	
	public static class Die extends TestAction
	{
		@Override
		public void perform() throws Exception
		{
			Main.quit();
		}
	};
	public static class SYNC_ROOTS extends TestAction
	{
		private String ident;
		
		public SYNC_ROOTS(String ident)
		{
			this.ident = ident;
		}
		public void perform()
		{
			UserActions.syncRoots(DbMachines.getMachine(ident));
		}
	};
	public static class FIND_MACHINES extends TestAction
	{
		private String ident;
		
		public void perform()
		{
			UserActions.findMachines(DbMachines.getMachine(ident));
		}
	};
	public static class SYNC_ALL_LOCAL extends TestAction
	{
		public void perform()
		{
			UserActions.syncAllLocals();
		}
	};
	public static class SYNC_LOCAL extends TestAction
	{
		String local;
		
		SYNC_LOCAL(String local)
		{
			this.local = local;
		}
		
		public void perform()
		{
			UserActions.sync(DbRoots.getLocal(local));
		}
	};
	public static class ADD_LOCAL extends TestAction
	{
		String local;
		
		ADD_LOCAL(String local)
		{
			this.local = local;
		}
		
		public void perform()
		{
			UserActions.addLocal(new File(local), true);
		}
	};
	public static class SYNC_REMOTE extends TestAction
	{
		String ident;
		String name;
		
		SYNC_REMOTE(String ident, String name)
		{
			this.name = name;
			this.ident = ident;
		}
		
		public void perform()
		{
			DbRoots.getRoot(DbMachines.getMachine(ident), name).synchronize(null);
		}
	};
	public static class REMOVE_LOCAL extends TestAction
	{
		String local;
		
		public REMOVE_LOCAL(String local)
		{
			this.local = local;
		}
		
		public void perform()
		{
			UserActions.remove(DbRoots.getLocal(local));
		}
	};
	public static class SHARE_WITH extends TestAction
	{
		private String ident;
		private boolean share;
		
		public SHARE_WITH(String ident, boolean share)
		{
			this.ident = ident;
			this.share = share;
		}
		
		public void perform()
		{
			UserActions.shareWith(DbMachines.getMachine(ident), share);
		}
	};
	public static class DOWNLOAD extends TestAction
	{
		String ident;
		String name;
		String path;
		
		public void perform()
		{
			UserActions.download(
					DbFiles.getFile(
							DbRoots.getRoot(
									DbMachines.getMachine(ident), name), 
							DbPaths.getPathElement(path)));
		}
	};
	
	public static void run(ObjectInputStream reader) throws Exception
	{
		TestAction action;
		while ((action = (TestAction) reader.readObject()) != null)
		{
			action.perform();
		}
	}
}
