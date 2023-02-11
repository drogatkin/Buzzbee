/*
 *  Copyright D. Rogatkin 2017-2023
 */
/**
 *
 */
package com.beegman.buzzbee;

import java.util.HashSet;
import java.util.Properties;

import org.aldan3.annot.Inject;
import org.aldan3.app.Registry;

import com.beegman.buzzbee.NotificationService.NotifException;


/** This is stub created for forwarding publish event to other nodes without using notification service
 * It is required for systems not running Java 7 or higher
 * @author Dmitriy
 *
 */
public class NotifServStub extends MicroService<NotifServStub> {
	@Inject
	Registry registry;
	
	protected HashSet<NotificationForwarder> forwarders;
	@Override
	public NotifServStub init(Properties props, Object arg1) {
		//super.init(props, arg1);
		String forstr = props==null?null:props.getProperty("FORWARDERS");
		if (forstr == null || forstr.isEmpty())
			return this;
		String[] fornames = forstr.split(":");
		forwarders = new HashSet<>();
		for(String nam: fornames)
			forwarders.add((NotificationForwarder) registry.getService(nam)); // TODO check for null
		LogImpl.log.debug("Initialized %d forwarders", forwarders.size());
		return this;
	}
	
	/** Propagates event to other nodes, no local processing.
	 * 
	 * @param event
	 * @throws NotifException
	 * 
	 * TODO release Java 8 dependency, it has to be compiled with Java 6
	 */
	public void publish(WebEvent event) throws NotifException {
		if (forwarders != null)
			forwarders.forEach(f -> f.forward(event));
	}
	
	public void notify(String id, String uid, String notifFunc) throws NotifException {
		if (notifFunc == null || notifFunc.isEmpty())
			notifFunc = "refreshWordPreview";
		String params[] = id.split("-");
		publish(new WebEvent().setAction(notifFunc).setId(id).setAttributes(params));
	}
	
	public void refresh(String id, String uid) throws NotifException {
		notify(id, uid, null);
	}

	@Override
	public String getPreferredServiceName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NotifServStub getServiceProvider() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NotifServStub destroy() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public NotifServStub start() {
		// TODO Auto-generated method stub
		return this;
	}
	
}
