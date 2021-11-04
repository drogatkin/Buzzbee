/*
 *  Copyright D. Rogatkin 2017-2021
 */
/**
 *
 * this interface represent subscriber
 */
package com.beegman.buzzbee;

public interface DeliveryListener {
	/** notifies that event with topis/resource id was delivered and 
	 * provides receipt id.
	 * 
	 * @param resourceId topic / resource id
	 * @param recepientId receipt id
	 */
	void delivered(String resourceId, String recepientId);
}
