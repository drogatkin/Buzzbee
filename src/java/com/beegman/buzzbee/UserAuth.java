/*
 *  Copyright D. Rogatkin 2017-2021
 */
/**
 *
 */
package com.beegman.buzzbee;

/** this interfac is used to keep 
 * a user authentication information
 * 
 * @author Dmitriy
 *
 */
public interface UserAuth {
	
	/** returns user login name, usually DN
	 * 
	 * @return login name of a user
	 */
	String getLoginName();

	/** return user id
	 * 
	 * @return user id, positive value, or 0 or negative
	 * when not available
	 */
	long getId();
	
	/** return a named attribute which can be associated with
	 * a user, for example login country code, or org/role pair
	 * @param name
	 * @return
	 */
	Object getAttribute(String name);
}
