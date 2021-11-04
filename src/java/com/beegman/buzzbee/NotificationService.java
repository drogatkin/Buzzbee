/*
 *  Copyright D. Rogatkin 2017-2021
 */
/**
 *
 */
package com.beegman.buzzbee;

import com.beegman.buzzbee.NotifServ.NotifException;

public interface NotificationService {
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
	public void publish(WebEvent event) throws NotifException;
	
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
	public void subscribe(String resourceId, Subscriber subscriber) throws NotifException;
	
	/**
	 * publishes an event to subscribers who previously subscribed to the topic.
	 * It also cleans all retaining events of the topic.
	 * 
	 * @param event
	 *            WebEvent which also contains resource/topic id
	 * @throws NotifException
	 *             if server is unavailable Here is no any indication if any
	 *             active subscribers exist or that this event was delivered to
	 *             them.
	 */
	public void publishAndForget(WebEvent event) throws NotifException ;
	
	/**
	 * publishes an event to subscribers who previously subscribed to the topic
	 * and all subscribers doing it in future
	 * 
	 * @param event
	 *            WebEvent which also contains resource/topic id
	 * @throws NotifException
	 *             if server is unavailable Here is no any indication if any
	 *             active subscribers exist or that this event was delivered to
	 *             them.
	 * 
	 *             Sequential calling of this method will make no effect. If you need
	 *             to stop accumulating retaining events, then call {@link #publishAndForget}.
	 * 
	 */
	public void publishRetain(WebEvent event) throws NotifException ;
}
