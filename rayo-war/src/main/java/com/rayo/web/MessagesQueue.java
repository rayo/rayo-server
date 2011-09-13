package com.rayo.web;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MessagesQueue {

	private static ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue<Message>();
	
	public static void publish(Message message) {
		
		queue.add(message);
	}
	
	public static Message poll() {
		
		return queue.poll();
	}
}
