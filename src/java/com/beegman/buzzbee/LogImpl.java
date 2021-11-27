package com.beegman.buzzbee;

import org.aldan3.model.Log;

public class LogImpl extends Log {
    public static final LogImpl log = new LogImpl();
	@Override
	public void log(String arg0, String arg1, String arg2, Throwable arg3, Object... arg4) {
		arg1 = "buzzbee";
		System.out.printf(arg0+":"+arg1+": "+arg2+"%n", arg4);
		if (arg3 != null)
            arg3.printStackTrace();
	}
	
	public void error(Throwable t, String message, Object...params) {
		error(String.format(message, params), t);
	}

}
