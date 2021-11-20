/*
 *  Copyright D. Rogatkin 2017-2021
 */
/**
 *
 */
package com.beegman.buzzbee;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import org.aldan3.model.Log;
import org.aldan3.model.ServiceProvider;

/**
 * this class is used for forwarding notifications to other nodes
 * 
 * @author Dmitriy TODO can be considered as functional interface
 */
public class NotificationForwarder extends MicroService<NotificationForwarder> {
	
	URL nodeUrl;

	@Override
	public NotificationForwarder init(Properties props, Object arg1) {
		//super.init(props, arg1);
		try {
			nodeUrl = new URL(props.getProperty("URL"));
			LogImpl.log.debug("Initialized for %s", nodeUrl);
		} catch (Exception e) {
			LogImpl.log.error(e, "Can't initialize %s", nodeUrl);
		}
		return this;
	}

	public void forward(WebEvent event) {
		try {
			HttpURLConnection con = (HttpURLConnection) nodeUrl.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.setInstanceFollowRedirects(false);
			// TODO use constants for parameter names
			try (OutputStream os = con.getOutputStream()) {
				os.write("resourceId".getBytes());
				os.write("=".getBytes());
				os.write(URLEncoder.encode(event.resourceId, "UTF-8").getBytes());
				os.write("&action".getBytes());
				os.write("=".getBytes());
				os.write(URLEncoder.encode(event.action, "UTF-8").getBytes());
				if (event.attributes != null && event.attributes.length > 0) {
					os.write("&nParams".getBytes());
					os.write("=".getBytes());
					os.write(String.valueOf(event.attributes.length).getBytes());
					for (int i = 0; i < event.attributes.length; i++) {
						os.write("&parameter".getBytes());
						os.write(String.valueOf(i).getBytes());
						os.write("=".getBytes());
						os.write(URLEncoder.encode(event.attributes[i].toString(), "UTF-8").getBytes());
					}
				}
				os.write("\r\n".getBytes());
				os.flush();
				if (con.getResponseCode() != HttpURLConnection.HTTP_OK)
					throw new IOException("Result code " + con.getResponseMessage());
			} catch (IOException e) {
				LogImpl.log.error(e, "Can't write %s to %s", event, nodeUrl);
			}

		} catch (Exception e) {
			LogImpl.log.error(e, "Can't forward event %s", event);
		}
	}

	@Override
	public String getPreferredServiceName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public NotificationForwarder getServiceProvider() {
		return this;
	}

	@Override
	public NotificationForwarder destroy() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public NotificationForwarder start() {
		// TODO Auto-generated method stub
		return this;
	}
}
