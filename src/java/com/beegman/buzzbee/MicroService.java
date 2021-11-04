package com.beegman.buzzbee;

import java.util.Properties;

import org.aldan3.model.ServiceProvider;

public abstract class MicroService<T> implements ServiceProvider<T> {
    abstract T init(Properties props, Object free);
    
    public abstract void destroy();

	
	public  abstract void start();
	
	//default String getName() {
		//return this.getClass().getSimpleName();
	//}
}