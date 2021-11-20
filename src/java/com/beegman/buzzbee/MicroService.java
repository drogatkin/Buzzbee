package com.beegman.buzzbee;

import java.util.Properties;

import org.aldan3.model.ServiceProvider;

public abstract class MicroService<T> implements ServiceProvider<T> {
    abstract T init(Properties props, Object free);
    
    public abstract T destroy();

	
	public  abstract T start();
	
	//default String getName() {
		//return this.getClass().getSimpleName();
	//}
}