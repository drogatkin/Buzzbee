/*
 *  Copyright D. Rogatkin 2017-2021
 */
package com.beegman.buzzbee.web;

import java.util.Arrays;
import java.util.LinkedList;

import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.PathParam;

import org.aldan3.annot.Inject;

import com.beegman.buzzbee.LogImpl;
import com.beegman.buzzbee.NotificationServiceImpl;
import com.beegman.buzzbee.NotificationServiceImpl.NotifException;
import com.beegman.buzzbee.Subscriber;
import com.beegman.buzzbee.UserAuth;
import com.beegman.buzzbee.WebEvent;

//@ServerEndpoint(value = "/notif/web/{servName}", encoders = NotifEndPoint.WebEventEncoder.class)
public class NotifEndPoint implements Subscriber {
	
	protected Session ses;
	protected LinkedList<String> notifIds = new LinkedList<>();

	protected UserAuth userAuth;
	
	@Inject
	protected NotificationServiceImpl ns;

	@OnMessage
	public void subscribe(String id, Session s, @PathParam("servName") String servName) {
		LogImpl.log.debug("got message %s for %s from %s and notifserv %s", id, servName, s.getPathParameters(), ns);
		ses = s;
		
		if (ns != null)
			try {
				if (!notifIds.contains(id)) {
					ns.subscribe(id, this);
					notifIds.add(id);
					//LogImpl.log.debug("Subscribed to %s", id);
				}
			} catch (NotifException e) {
				LogImpl.log.error(e, "");
			}
		else
			LogImpl.log.error(new NullPointerException("No notif server set"), "");
	}

	@OnClose
	public void unsubscribe(@PathParam("servName") String servName, CloseReason reason) {
		LogImpl.log.debug("Closed for reason %s", reason);
		
		if (ns != null)
			notifIds.forEach(id -> {
				try {
					ns.unsubscribe(id, this);
				} catch (NotifException e) {
					LogImpl.log.error(e, "");
				}
			});

		ses = null;
	}

	@Override
	public void notify(WebEvent event) {
		if (ses != null)
			try {
				//LogImpl.log.debug("Sending %s", event);
				ses.getBasicRemote().sendObject(event);
			} catch (Exception e) {				
				LogImpl.log.error(e, "");
				if (ses.isOpen() == false)
					unsubscribe(ses.getPathParameters().get("servName"), new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, ""));
			}

	}

	@Override
	public void unsubscribed(String resourceId) {
		// TODO close session if there is no more subscriptions

	}

	public static class WebEventEncoder implements Encoder.Text<WebEvent> {

		@Override
		public void destroy() {
			// some actions on destroying

		}

		@Override
		public void init(EndpointConfig arg0) {
			// some actions on initializing

		}

		@Override
		public String encode(WebEvent arg0) throws EncodeException {
			// TODO use JSON serialization
			String result =
			"{\"func\":\"" + arg0.action + "\", \"params\":" + Arrays.toString(arg0.attributes) + "}";
			//LogImpl.log.debug("JSON:%s", result);
			return result;
		}
	}

	@Override
	public boolean isAlive() {
		return ses != null && ses.isOpen();
	}

	@Override
	public UserAuth getUserAuth() {
		// 
		return userAuth;
	}
}
