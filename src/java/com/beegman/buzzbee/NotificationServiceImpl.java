/*
 *  Copyright D. Rogatkin 2017-2023
 */
/**
 * notification service
 */
package com.beegman.buzzbee;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.aldan3.annot.Inject;
import org.aldan3.app.Registry;

/**
 * Base notification service, the following line can be added to config <br>
 
 * 
 * @author Dmitriy
 *
 */
public class NotificationServiceImpl extends MicroService<NotificationServiceImpl> implements NotificationService {
	@Inject
	Registry registry;
	
	Properties properties;
	
	/**
	 * the service specific exception
	 * 
	 * @author Dmitriy
	 *
	 */
	public static class NotifException extends Exception {

	}

	private LinkedHashMap<String, StampedHashSet<Subscriber>> subscribers;
	private ThreadPoolExecutor executor;

	private LinkedHashMap<String, ConcurrentLinkedDeque<Note>> reQueue;

	protected HashSet<NotificationForwarder> forwarders;

	@Override
	public NotificationServiceImpl init(Properties props, Object arg1) {
		properties = props;
		int maxAge = 20;
		try {
			if (props != null)
				maxAge = Integer.parseInt(props.getProperty("EMPTY_TOPIC_MAX_AGE"));
		} catch(Exception e) {
			
		}
		new AbandonedTopicsCleaner(maxAge) {
			
		};
		if (arg1 instanceof Registry)
			registry = (Registry)arg1;
		String forstr = props == null ? null : props.getProperty("FORWARDERS");
		if (forstr == null || forstr.isEmpty())
			return this;
		String[] fornames = forstr.split(":");
		forwarders = new HashSet<>();
		for (String nam : fornames)
			forwarders.add((NotificationForwarder) registry.getService(nam)); // TODO check for null
		LogImpl.log.debug("Initialized %d forwarders", forwarders.size());
		return this;
	}

	/**
	 * subscribe an interest to certain topic (resourceId) notifications
	 * 
	 * @param resourceId
	 *            - topic or resource id
	 * @param subscriber
	 *            - Subscriber instance which will be notified about the topic
	 *            event
	 * @throws NotifException
	 *             if server unavailable
	 * 
	 *             When WebSocket is used, ServerEndpoint class can implement
	 *             Subscriber interface
	 */
	@Override
	public void subscribe(String resourceId, Subscriber subscriber) throws NotifException {
		// TODO execute all pending event for this id against the subscriber
		synchronized (subscribers) {
			StampedHashSet<Subscriber> ls = subscribers.get(resourceId);
			if (ls == null) {
				ls = new StampedHashSet<Subscriber>();
				subscribers.put(resourceId, ls);
			}
			ls.add(subscriber);
			//  publish all queue to the subscriber
			ConcurrentLinkedDeque<Note> retainQ = reQueue.get(resourceId);
			if (retainQ != null && retainQ.isEmpty() == false)
				executor.submit(new NotificationTask(retainQ, subscriber));
		}
		//LogImpl.log.debug("Added sub %s for %s", subscriber, resourceId);
	}

	/**
	 * Unsubscribe subscriber from specified topic
	 * 
	 * @param resourceId
	 *            topic or resource id
	 * @param subscriber
	 *            Subscriber who previously subscribed
	 * @throws NotifException
	 *             if server unavailable There is no exception if topic id or
	 *             subscriber are unknown to the system
	 * 
	 */
	public void unsubscribe(String resourceId, Subscriber subscriber) throws NotifException {
		getSubscribers(resourceId).remove(subscriber);
		subscriber.unsubscribed(resourceId);
		subscribersStatus(resourceId);
	}

	/**
	 * Unsubscribe all subscribers to certain topic. It is used in case if
	 * resource gets removed from system and no more event to it are planned.
	 * For example a user can log off.
	 * 
	 * @param resourceId
	 *            resourceId topic or resource id
	 * @throws NotifException
	 * 
	 *             TODO a cleaning thread for all dead subscribers
	 * 
	 */
	public void unsubscribeAll(String resourceId) throws NotifException {
		getSubscribers(resourceId).forEach(s -> s.unsubscribed(resourceId));
		getSubscribers(resourceId).clear();
	}

	/**
	 * publishes an event to subscribers who previously subscribed to the topic
	 * 
	 * @param event
	 *            WebEvent which also contains resource/topic id
	 * @throws NotifException
	 *             if server is unavailable Here is no any indication if any
	 *             active subscribers exist or that this event was delivered to
	 *             them
	 * 
	 */
	@Override
	public void publish(WebEvent event) throws NotifException {
		//LogImpl.log.debug("publishing: %s at "+event, event);
		if (forwarders != null)
			forwarders.forEach(f -> f.forward(event));
		publishNoForward(event);
	}

	public void publishNoForward(WebEvent event) throws NotifException {
		//LogImpl.log.debug("no forward publishing: %s", event);
		NotificationTask nt;
		Future  notifFuture = executor.submit(nt = new NotificationTask(event));
		nt.run();
		ConcurrentLinkedDeque<Note> retainQ = reQueue.get(event.resourceId);
		if (retainQ != null && retainQ.isEmpty() == false)
			retainQ.add(event);
	}

	/**
	 * Publishes an event no echo to the given subscriber
	 * 
	 * @param event
	 * @param subscriber
	 */
	public void publish(WebEvent event, Subscriber subscriber) {
		// TODO get org/role pairs from subscriber and forward
		if (forwarders != null)
			forwarders.forEach(f -> f.forward(event));
		Future  notifFuture = executor.submit(new NotificationTask(event, subscriber));
	}
	
	//public void publishTo(WebEvent event, Subscriber subscriber) {
		//executor.submit(new NotificationTask(event, subscriber));
	//}

	/**
	 * publishes an event to subscribers who previously subscribed to the topic.
	 * DeliveryListener gets called for every delivered event to a subscriber.
	 * If subscriber supports delivery notification mechanism, then delivery
	 * notification happens only after delivery confirmation, otherwise when
	 * send was happened
	 * 
	 * @param event
	 *            WebEvent which also contains resource/topic id
	 * @param listener
	 *            DeliveryListener
	 * @throws NotifException
	 *             if server is unavailable
	 * 
	 * 
	 */
	public void publish(WebEvent event, DeliveryListener listener) throws NotifException {
		publish(event);
		// fake notification
		listener.delivered(event.resourceId, event.action);
	}

	/**
	 * publishes an event to subscribers who previously subscribed to the topic
	 * and all subscriber doing it in future
	 * 
	 * @param event
	 *            WebEvent which also contains resource/topic id
	 * @throws NotifException
	 *             if server is unavailable Here is no any indication if any
	 *             active subscribers exist or that this event was delivered to
	 *             them.
	 * 
	 *             Sequential calling of this method will make it to forget
	 *             previously retained event. For example if an event user XYZ
	 *             user going online was sent, then all new subscribers will get
	 *             it however after sending another event like user XYZ going
	 *             offline, all current and new subscribers will get event user
	 *             XYZ offline only.
	 * 
	 */
	@Override
	public void publishRetain(WebEvent event) throws NotifException {
		publish(event);
		retain(event);
	}
	
	public void publishRetain(WebEvent event, Subscriber subscriber) throws NotifException {
		publish(event);
		retain(event);
	}
	
	void retain(WebEvent event) {
		ConcurrentLinkedDeque<Note> retainQ = null;
		synchronized(reQueue) {
			retainQ = reQueue.get(event.resourceId);
			if (retainQ != null)
				retainQ.clear();
			else {
				retainQ = new ConcurrentLinkedDeque<Note>();
				reQueue.put(event.resourceId, retainQ);
			}
		}
		retainQ.add(event);	
	}
	
	/**
	 * publishes an event to subscribers who previously subscribed to the topic.
	 * It also cleans all retaining event to the topic.
	 * 
	 * @param event
	 *            WebEvent which also contains resource/topic id
	 * @throws NotifException
	 *             if server is unavailable Here is no any indication if any
	 *             active subscribers exist or that this event was delivered to
	 *             them.
	 */
	@Override
	public void publishAndForget(WebEvent event) throws NotifException {
		publish(event);
		synchronized(reQueue) {
			ConcurrentLinkedDeque<Note>  retainQ = reQueue.get(event.resourceId);
			if (retainQ != null)
				retainQ.clear();
		}
	}
	
	public void publishAndForget(WebEvent event, Subscriber s) throws NotifException {
		publish(event, s);
		synchronized(reQueue) {
			ConcurrentLinkedDeque<Note>  retainQ = reQueue.get(event.resourceId);
			if (retainQ != null)
				retainQ.clear();
		}
	}

	/**
	 * Cleans all retaining event to the topic and unsubscribe all subscriber.
	 * For example if user XYZ is going offline then all subscribers interested
	 * in his/her activities can be removed from the system.
	 * 
	 * @param resourceId
	 *            topic/resource id
	 * @throws NotifException
	 * @see {@link #unsubscribeAll(String)}
	 */
	public void forgetAll(String resourceId) throws NotifException {
		// TODO clean pending related event in queue
		LinkedHashSet<Subscriber> ls = null;
		synchronized (subscribers) {
			ls = subscribers.remove(resourceId);
		}
		if (ls != null)
			ls.forEach(s -> s.unsubscribed(resourceId));
	}

	@Override
	public NotificationServiceImpl destroy() {
		executor.shutdownNow();
		//LogImpl.log.debug("executor stopped");
		return this;
	}

	@Override
	public synchronized NotificationServiceImpl start() {
		init();
		return this;
	}

	private void init() {
		subscribers = new LinkedHashMap<>();
		executor = new ThreadPoolExecutor(getConfigInt("pool_size", 1000),
				getConfigInt("max_pool_size", Integer.MAX_VALUE), 1l, TimeUnit.MINUTES,
				new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {

					@Override
					public Thread newThread(Runnable arg0) {
						Thread t = new Thread(arg0, "_t_" + getPreferredServiceName());
						t.setDaemon(true);
						t.setPriority(Thread.NORM_PRIORITY);
						return t;
					}
				});
		reQueue = new LinkedHashMap<>();
		//LogImpl.log.debug("executor started");
	}

	int getConfigInt(String name, int def) {
		try {
			return Integer.parseInt(properties.getProperty(name));
		} catch (Exception e) {

		}
		return def;
	}

	LinkedHashSet<Subscriber> getSubscribers(String resourceId) {
		synchronized (subscribers) {
			StampedHashSet<Subscriber> ls = subscribers.get(resourceId);
			//LogImpl.log.debug("Subscribers for %s -> %s", ls, resourceId);
			if (ls == null) {
				ls = new StampedHashSet<Subscriber>();
				 subscribers.put(resourceId, ls); // TODO check other possible adding 
			} else
				ls.stamp();
			return ls;
		}
	}
	
	void subscribersStatus(String resourceId) {
		if (resourceId != null) {
			LogImpl.log.debug("Subscribtions for %s - %d", resourceId, getSubscribers(resourceId).size() );
		}
		LogImpl.log.debug("Total topics: %d", subscribers.size());
	}

	class NotificationTask implements Runnable {
		WebEvent note;
		Subscriber noEcho;
		ConcurrentLinkedDeque<Note> notes;

		NotificationTask(WebEvent we) {
			note = we;
		}

		NotificationTask(ConcurrentLinkedDeque<Note> mesQ, Subscriber s) {
			noEcho = s;
			notes = mesQ;
		}
		
		NotificationTask(WebEvent we, Subscriber s) {
			this(we);
			noEcho = s;			
		}

		@Override
		public void run() {
			if (notes != null && noEcho != null) {
				// TODO if too many notes, client can be overloaded, better to chunk
				notes.forEach(n -> {if (isEligable(noEcho, (WebEvent)n)) noEcho.notify((WebEvent)n);});
				return;
			}
			// TODO synchronize all work around subscriber list
			LinkedHashSet<Subscriber> subscribers = (LinkedHashSet<Subscriber>) getSubscribers(note.getResourceId())
					.clone();
			//LogImpl.log.debug("Executing notif for %s to %d", note.getResourceId(),
				//	subscribers.size());
			ArrayList<Subscriber> deads = new ArrayList<>();
			subscribers.forEach(s -> {
				//LogImpl.log.debug("subscribg %s at %s", s, noEcho);
				if (s == noEcho)
					return;
				if (s.isAlive())
					try {
						if (isEligable(s, note)) { // TODO think if the check can be performed in notify()
							//LogImpl.log.debug("Notifying %s at %s", s, note);
							s.notify(note); 
						}
					} catch (Exception e) {
						LogImpl.log.error(e, "notifying %s for %s", note, note.getResourceId());
					}
				else {
					LogImpl.log.debug("Dead %s", s);
					deads.add(s);
				}
			});
			final LinkedHashSet<Subscriber> subscribers2 = getSubscribers(note.getResourceId());

			synchronized (subscribers2) {
				deads.forEach(s -> subscribers.remove(s));
				// no unsubscribed notification since dead
			}
			/*for (Subscriber s:subscribers.get(note.getResourceId())) {
				s.notify(note);
			}*/
		}
	}
	
	/** checks if subscriber is eligible to receive the notification in scope
	 * of distribution
	 * @param s - subscriber
	 * @param note2 - notification message
	 * @return true if subscriber user info is covered by scope of message
	 */
	protected boolean isEligable(Subscriber s, WebEvent note2) {
		//s.getUserAuth().
		return true;
	}
	
	static class StampedHashSet<T> extends LinkedHashSet<T> {
		long accessStamp;

		@Override
		public boolean add(T arg0) {
			accessStamp = System.currentTimeMillis();
			return super.add(arg0);
		}

		@Override
		public void clear() {
			accessStamp = System.currentTimeMillis();
			super.clear();
		}

		@Override
		public boolean remove(Object arg0) {
			accessStamp = System.currentTimeMillis();
			return super.remove(arg0);
		}
		
		public long getStamp() {
			return accessStamp;
		}
		
		public void stamp() {
			accessStamp = System.currentTimeMillis();
		}
		
	}
	
	class AbandonedTopicsCleaner extends Thread {
		long age;
		boolean running;
		AbandonedTopicsCleaner(int ma) {
			if (ma < 2)
				throw new IllegalArgumentException("Invalid age parameter: "+ma+", it has to be a value in minites more than 1");
			age = ma*60*1000l;
			setName("Abandoned Topics Cleaner");
			setDaemon(true);
			setPriority(MIN_PRIORITY);
			running = true;
			//setUncaughtExceptionHandler(eh);
			start();
		}
		
		
		@Override
		public void run() {
			do {
				try {
					compactSubscribers();
				} catch (Throwable t) {
					if (t instanceof ThreadDeath)
						throw (ThreadDeath) t;
					LogImpl.log.error(t, "an exception in compacting");
				}
				try {
					Thread.sleep(age);
				} catch (Exception e) {
					running = false;
				}
			} while (running);
			LogImpl.log.debug("Abandoned topics cleaning thread has terminated.");
		}
		
		private void compactSubscribers() {
			if (subscribers == null)
				return;  // nothing to do yet
			HashSet<String> topics = null;
			
			synchronized(subscribers) {
				topics = new HashSet<>(subscribers.keySet());
			}
			// how race condition is solved
			// a new topic get removed added in sync bloc
			// if topic is added it gets immediately stamped and doesn't fall in deletion
			// if almost expired topic is obtained first it is also gets stamped
			for (String topic:topics) {
				StampedHashSet<Subscriber> subs = null;
				synchronized(subscribers) {
					subs = subscribers.get(topic);
				//}
				long stamp = 0;
				if (subs != null && subs.isEmpty()) {
					synchronized(subs) {
						stamp = subs.getStamp();
						if (stamp == 0) {
							subs.stamp();
							continue;
						}
					}
					if (subs.isEmpty() && System.currentTimeMillis() - stamp > age)
					//	synchronized(subscribers) {
							subscribers.remove(topic);
						}		
				}
			}
			LogImpl.log.debug("Completed compacting the topics queue, size after: %d / %d", subscribers.size(), topics.size());
		}
	}

	@Override
	public String getPreferredServiceName() {
		
		return "NotifServer";
	}

	@Override
	public NotificationServiceImpl getServiceProvider() {
		
		return this;
	}
}
