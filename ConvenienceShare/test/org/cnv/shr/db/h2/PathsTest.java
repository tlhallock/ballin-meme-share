package org.cnv.shr.db.h2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.mn.Arguments;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.Misc;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PathsTest
{
	private static final String LOCAL_DIR = "this_does_not_exist";
	private static LocalDirectory local;
	private static Machine machine;

	@BeforeClass
	public static void before()
	{
		Services.settings = new Settings(Paths.get("tests").resolve("settings.props"));
		Services.settings.dbFile.set(Paths.get("tests").resolve("dbfile"));
	}

	@Before
	public void setup() throws ClassNotFoundException, SQLException, IOException 
	{
		Arguments arguments = new Arguments();
		arguments.deleteDb = true;
		Services.h2DbCache = new DbConnectionCache(arguments);
		

		machine = new Machine(
				"127.0.0.1",
				8990,
				5,
				"machineName",
				Misc.getRandomString(50),
				true,
				SharingState.DO_NOT_SHARE,
				SharingState.DO_NOT_SHARE,
				true);
		
		machine.tryToSave();
		
		Misc.ensureDirectory(new File(LOCAL_DIR), false);
		local = new LocalDirectory("localTest", "test description", "test tags", -1, -1, LOCAL_DIR, SharingState.DO_NOT_SHARE, (long) 0, (long) 50);
		local.setMachine(machine);
		local.tryToSave();
	}
	
	@After
	public void takeDown() throws ClassNotFoundException, SQLException, IOException 
	{
		Services.h2DbCache.close();
	}

	@AfterClass
	public static void after() throws IOException 
	{
		Misc.rm(Paths.get(LOCAL_DIR).toAbsolutePath());
	}

	@Test
	public void simpleTest() throws SQLException
	{
		DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here", true);
		Assert.assertEquals(1 + 1, DbPaths2.getNumPaths());
		Assert.assertEquals(1 + 1, DbPaths2.getNumContains());
	}


	@Test
	public void simpleTestWithSlash() throws SQLException
	{
		DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here/", true);
		Assert.assertEquals(1 + 1, DbPaths2.getNumPaths());
		Assert.assertEquals(1 + 1, DbPaths2.getNumContains());
	}
	

	@Test
	public void simpleTestThree() throws SQLException
	{
		DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here/another/here/foo", true);
		Assert.assertEquals(1 + 3, DbPaths2.getNumPaths());
		Assert.assertEquals(1 + 4, DbPaths2.getNumContains());
	}

	@Test
	public void simpleList() throws SQLException
	{
		PathElement firstEndPoint = DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here/another/here/foo", true);
		LinkedList<PathElement> listPaths = DbPaths2.listPaths(firstEndPoint.getParent());
		Assert.assertEquals(1, listPaths.size());
	}
	@Test
	public void simpleListTwice() throws SQLException
	{
		PathElement firstEndPoint = DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here/another/here/foo", true);
		LinkedList<PathElement> listPaths = DbPaths2.listPaths(firstEndPoint.getParent());
		Assert.assertEquals(1, listPaths.size());firstEndPoint = DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here/another/here/foo", true);
		listPaths = DbPaths2.listPaths(firstEndPoint.getParent());
		Assert.assertEquals(1, listPaths.size());
	}

	@Test
	public void simpleListTwo() throws SQLException
	{
		PathElement firstEndPoint = DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here/another/here/foo", true);
		PathElement secondEndPoint = DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here/another/here/bar", true);
		LinkedList<PathElement> listPaths = DbPaths2.listPaths(firstEndPoint.getParent());
		Assert.assertEquals(1 + 4, DbPaths2.getNumPaths());
		Assert.assertEquals(1 + 5, DbPaths2.getNumContains());
		Assert.assertEquals(2, listPaths.size());
		contains(listPaths, "foo/");
		contains(listPaths, "bar/");
	}

	@Test
	public void simpleListRemoveOneOfTwo() throws SQLException
	{
		PathElement firstEndPoint = DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here/another/here/foo", true);
		PathElement secondEndPoint = DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here/another/here/bar", true);		
		Assert.assertEquals(1 + 4, DbPaths2.getNumPaths());
		Assert.assertEquals(1 + 5, DbPaths2.getNumContains());
		DbPaths2.removePathFromRoot(secondEndPoint);		
		Assert.assertEquals(1 + 3, DbPaths2.getNumPaths());
		Assert.assertEquals(1 + 4, DbPaths2.getNumContains());
		LinkedList<PathElement> listPaths = DbPaths2.listPaths(firstEndPoint.getParent());
		Assert.assertEquals(1, listPaths.size());
		contains(listPaths, "foo/");
	}
	

	@Test
	public void simpleListRemoveAll() throws SQLException
	{
		PathElement endPoint = DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here/another/here/foo", true);
		Assert.assertEquals(1 + 3, DbPaths2.getNumPaths());
		Assert.assertEquals(1 + 4, DbPaths2.getNumContains());
		DbPaths2.removePathFromRoot(endPoint);		
		Assert.assertEquals(1 + 2, DbPaths2.getNumPaths());
		Assert.assertEquals(1 + 3, DbPaths2.getNumContains());
		
		DbObjects.ROOT_CONTAINS.debug(Services.h2DbCache.getThreadConnection());

		LinkedList<PathElement> listPaths = DbPaths2.listPaths(endPoint.getParent());
		Assert.assertEquals(0, listPaths.size());
	}

	@Test
	public void simpleListRemoveAllParent() throws SQLException
	{
		PathElement endPoint = DbPaths2.addPathTo(local, DbPaths2.getRoot(local), "here/another/here/foo/bar", true).getParent();
		Assert.assertEquals(1 + 4, DbPaths2.getNumPaths());
		Assert.assertEquals(1 + 5, DbPaths2.getNumContains());
		DbPaths2.removePathFromRoot(endPoint);
		DbObjects.PELEM.debug(Services.h2DbCache.getThreadConnection());
		
		Assert.assertEquals(1 + 3, DbPaths2.getNumContains());
		// Not entirely cleaned...
		Assert.assertEquals(1 + 3, DbPaths2.getNumPaths());
		
		DbPaths2.cleanPelem();
		// Now it should be cleaned...
		Assert.assertEquals(1 + 2, DbPaths2.getNumPaths());
		
		LinkedList<PathElement> listPaths = DbPaths2.listPaths(endPoint.getParent());
		Assert.assertEquals(0, listPaths.size());
	}
	
	private void contains(LinkedList<PathElement> list, String aPathElement)
	{
		for (PathElement element : list)
		{
			if (element.getUnbrokenName().equals(aPathElement))
			{
				return;
			}
		}
		Assert.fail("The list should have contained " + aPathElement);
	}
}
