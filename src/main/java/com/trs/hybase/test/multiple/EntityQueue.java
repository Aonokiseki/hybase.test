package com.trs.hybase.test.multiple;

import java.util.LinkedList;

import org.springframework.context.annotation.Scope;

@Scope("prototype")
public class EntityQueue<T> {
	private LinkedList<T> queue;
	private int capacity;
	
	public EntityQueue(int capacity) {
		this.capacity = capacity;
		this.queue = new LinkedList<T>();
	}
	
	public synchronized void appendTail(T entity) throws InterruptedException {
		if(queue.size() > capacity)
			this.wait();
		this.notify();
		this.queue.add(entity);
	}
	
	public synchronized T removeHead() throws InterruptedException {
		if(queue.isEmpty())
			this.wait();
		this.notify();
		return this.queue.remove(0);
	}
}
