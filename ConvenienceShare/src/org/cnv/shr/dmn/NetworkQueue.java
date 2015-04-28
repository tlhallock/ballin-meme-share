package org.cnv.shr.dmn;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.cnv.shr.msg.Message;

public class NetworkQueue
{
	ExecutorService service = Executors.newFixedThreadPool(Settings.getInstance().getNumThreads());
	HashSet<Message> pendingMessages;
}
