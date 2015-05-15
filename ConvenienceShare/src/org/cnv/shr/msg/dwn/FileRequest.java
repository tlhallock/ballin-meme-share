package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionStatistics;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class FileRequest extends Message
{
	private String rootName;
	private String path;
	private String checksum;
	private int chunkSize;

	public static int TYPE = 13;

	public FileRequest(RemoteFile remoteFile, int chunkSize)
	{
		rootName = remoteFile.getRootDirectory().getName();
		path = remoteFile.getPath().getFullPath();
		checksum = remoteFile.getChecksum();
		if (checksum == null)
		{
			checksum = "";
		}
		this.chunkSize = chunkSize;
	}
	
	public FileRequest(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	protected void parse(InputStream bytes, ConnectionStatistics stats) throws IOException
	{
		rootName  = ByteReader.readString(bytes);
		path      = ByteReader.readString(bytes);
		checksum  = ByteReader.readString(bytes);
		chunkSize = ByteReader.readInt(bytes);
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(rootName );
		buffer.append(path     );
		buffer.append(checksum );
		buffer.append(chunkSize);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("I wanna download " + rootName + ":" + path);
		
		return builder.toString();
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		PathElement pathElement = DbPaths.getPathElement(path);
		LocalDirectory local = DbRoots.getLocalByName(rootName);
		LocalFile localFile = (LocalFile) DbFiles.getFile(local, pathElement);
		ServeInstance serve = Services.server.serve((LocalFile) localFile, connection, chunkSize);
		serve.sendChunks();
	}
}
