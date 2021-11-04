/*
 *  Copyright D. Rogatkin 2017-2021
 */
/**
 * subscriber interface
 */
package com.beegman.buzzbee;

public interface Subscriber {
	/** this method gets called when an event of the subscriber interest happened
	 * 
	 * @param event WebEvent
	 */
	void notify(WebEvent event);

	/** this method is called when the subscriber forcefully was unsubscribed
	 * Because server doesn't manage of topic of interest anymore. The subscriber can still
	 * be involved in delivery of other subscribed topics
	 * 
	 * @param resourceId topic/resource id
	 */
	void unsubscribed(String resourceId);
	
	/** tells if a subscriber can still process notifications
	 * 
	 * @return true if can accept notification, otherwise false
	 * 
	 * If false returned then a subscriber can be removed from notification chain
	 */
	boolean isAlive();
	
	/** returns associated to a subscriber authenticated user
	 * 
	 * @return a user authentication info when available, otherwise null
	 */
	UserAuth getUserAuth();
}
