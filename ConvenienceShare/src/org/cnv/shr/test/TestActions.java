package org.cnv.shr.test;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Scanner;

import org.cnv.shr.gui.UserActions;

public class TestActions
{
	private static LinkedList<TestAction> actions = new LinkedList<>();
	
	public static abstract class TestAction
	{
		protected String name;

		protected TestAction(String n)
		{
			this.name = n;
			actions.add(this);
		}
		public abstract void send(PrintStream stream, String... args);
		public abstract void perform(Scanner scanner) throws Exception;
		
		public boolean matches(String line)
		{
			return line.startsWith(name);
		}
	}

//	public static TestAction REMOVE_MACHINE = new TestAction("REMOVE_MACHINE")
//	{
//		public void perform()
//		{
//			GuiActions.removeMachine(remote);
//		}
//	};
	public static TestAction ADD_MACHINE = new TestAction("ADD_MACHINE")
	{
		public void perform(Scanner scanner)
		{
			scanner.next(); // skip name
			UserActions.addMachine(scanner.next());
		}
		
		@Override
		public void send(PrintStream stream, String... args)
		{
			stream.println(name + " " + args[0]);
		}
	};
//	public static TestAction SYNC_ROOTS = new TestAction("SYNC_ROOTS")
//	{
//		public void perform()
//		{
//			GuiActions.syncRoots(m);
//		}
//	};
//	public static TestAction FIND_MACHINES = new TestAction("FIND_MACHINES")
//	{
//		public void perform()
//		{
//			GuiActions.findMachines(m);
//		}
//	};
//	public static TestAction SYNC_ALL_LOCAL = new TestAction("SYNC_ALL_LOCAL")
//	{
//		public void perform()
//		{
//			GuiActions.syncAllLocals();
//		}
//	};
//	public static TestAction SYNC_LOCAL = new TestAction("SYNC_LOCAL")
//	{
//		public void perform()
//		{
//			GuiActions.sync(d);
//		}
//	};
//	public static TestAction ADD_LOCAL = new TestAction("ADD_LOCAL")
//	{
//		public void perform()
//		{
//			GuiActions.addLocal(localDirectory);
//		}
//	};
//	public static TestAction SYNC_REMOTE = new TestAction("SYNC_REMOTE")
//	{
//		public void perform()
//		{
//			GuiActions.sync(d);
//		}
//	};
//	public static TestAction REMOVE_LOCAL = new TestAction("REMOVE_LOCAL")
//	{
//		public void perform()
//		{
//			GuiActions.remove(l);
//		}
//	};
//	public static TestAction SHARE_WITH = new TestAction("SHARE_WITH")
//	{
//		public void perform()
//		{
//			GuiActions.share(m);
//		}
//	};
//	public static TestAction SHARE_LOCAL_WITH = new TestAction("SHARE_LOCAL_WITH")
//	{
//		public void perform()
//		{
//			GuiActions.share(m, local);
//		}
//	};
//	public static TestAction DOWNLOAD = new TestAction("DOWNLOAD")
//	{
//		public void perform()
//		{
//			GuiActions.download(remote);
//		}
//	};
	
	public static void run(BufferedReader reader) throws Exception
	{
		String line;
		while ((line = reader.readLine()) != null)
		{
			for (TestAction action : actions)
			{
				if (!action.matches(line))
				{
					continue;
				}
				action.perform(new Scanner(line));
				break;
			}
		}
	}
}
