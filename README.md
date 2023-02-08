# Buzzbee

## Purpose

Sometimes a web application needs notifications about some events. Such events have to be delivered in
a web page asynchronously to a user interaction. A perfect solution for that is using a websocket. However,
using a websocket requires some additional coding. The project takes care as websocket work as a distribution of events.
The solution is wrapped as a notification service utilizing a subscriber publisher model. A subscriber subscribes to certain type of events and then
when a publisher publish such events, get a notification. The service itself defined on a level of an interface.
It allows to change a service implementation without touching subscribers code.

## Typical use

A notification service has to be added as

       import com.beegman.buzzbee.NotifServ;
       ....
      @Override
	protected void initServices() {
		super.initServices();
		register(notifService = new NotifServ().init(new Properties(), this).start());

	}

Since it is required to be passed in a websocket endpoint, the service should be accessible from some  global variable.
A websocket endpoint looks like:

      @OnMessage
	public void fromClient(ClientMessage mes, Session s) {
           ,,,,
           Model.notifService.subscribe(mes, this);

On message handler receives messages from client and allows, fer example, to subscribe on a certain event.