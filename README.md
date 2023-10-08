# Buzzbee

## Purpose

Sometimes a web application needs notifications about some events. Such events have to be delivered in
a web page asynchronously to a user interaction. A perfect solution for that is using a websocket. However,
using a websocket requires some additional coding. The project takes care as websocket work as a distribution of events.
The solution is wrapped as a notification service utilizing a subscriber publisher model. A subscriber subscribes to a type of events of an interest and then
when a publisher publishes such events, it gets a notification. The service itself is defined on a level of an interface.
It allows to change a service implementation without touching a subsriber's code.

## Typical use

A notification service has to be added in an application model or other common place
for initializing services:

    import com.beegman.buzzbee.NotificationServiceImpl;
       ....
      @Override
	protected void initServices() {
		super.initServices();
		.....
		register(notifService = new NotificationServiceImpl().init(new Properties(), this).start());
        .....
	}

The service should be accessible from some  global variable, since it is required that the service to be passed to a websocket endpoint.
A websocket endpoint looks like:

    @ServerEndpoint(value = "/notif/web", encoders = NotifEndPoint.WebEventEncoder.class)
    public class NotifEndPoint implements Subscriber {
    
      @OnMessage
      	public void fromClient(ClientMessage mes, Session s) {
           ....
           appModel.notifService.subscribe(mes.id, this);

Pay an attention to the address **/notif/web**, since it is used for establishing websocket 
connection in js code. Obviously it can be changed, but in all places of a use. 

The on message handler receives messages from a client and allows, for example, to subscribe on a certain event.
A subscription can  also happen in OnOpen handler.


Now, a client should get notifications on events. It can be done providing the following code:

     @Override
	public void notify(WebEvent event) {
		if (isAlive())
			try {
				....
				ses.getBasicRemote().sendObject(event);
			} catch (Exception e) {
	......
	
The method gets calls from a notification service. A connection from the notification service to the endpoint happens
at time of a subscribing.

## Examples
A good illustration of all steps required for using a notification service and websocket can be found in
[ShareLenks](https://github.com/drogatkin/sharelinks) project.
Quick recap of the steps:
### Adding Buzzbee jar in a project
Add Buzzbee jar and js in env.xml

    <variable name="BUZZBEE_LIB" type="path">/home/dmitriy/projects/Buzzbee/build/buzzbee.jar</variable>
    <variable name="BUZZBEE_JS" type="path">/home/dmitriy/projects/Buzzbee/src/js/buzzbee.js</variable>
    ....
    <expression variable="CUSTOM CP">
    	<operator name="append">
		    <value variable="PATH SEPARATOR"/>
		    <value variable="BUZZBEE_LIB"/>
    .....
  
Unless the websocket library is already in the project, it needed to be added too:

    <variable name="WEBSOCKET API" type="repo_artifact">maven:javax.websocket:javax.websocket-api:1.1</variable>
    ....
                    <value variable="PATH SEPARATOR"/>
		    <value variable="WEBSOCKET API"/>


 
And then add js file in the task **warit** of bee.xml:

         <parameter>A js/</parameter>
         <parameter type="path">src/js/*.js</parameter>
         <parameter>A js/</parameter>
         <parameter variable="BUZZBEE_JS" type="path"/>
                

### Add the notification service to the app model
It is added in the initServices section of the app model when WebBee is used. Or should be added in the initialization services
section of another framework of your choice.

     @Override
	protected void initServices() {
		super.initServices();
		register(notifService = new NotificationServiceImpl().init(new Properties(), this).start());
	}
	
See the [source](https://github.com/drogatkin/sharelinks/blob/69637f8ce176b682841d2bb6c1410f0d48650ccc/src/java/com/walletwizz/sharelinks/model/SharelinksModel.java#L63)

### Create endpoint
[Endpoint](https://github.com/drogatkin/sharelinks/blob/master/src/java/com/walletwizz/sharelinks/ux/ws/UIRefresher.java) is used as for
subscribing to notification events from a client, and delivering notification events.

Make sure that a web socket session is propagated to the endpoint underneath class which is used 
as a subscriber. The session is used to figure out that the subscriber is alive, see

     @OnMessage
	 public void subscribe(String id, Session s, @PathParam("servName") String servName) {
		LogImpl.log.debug("got message %s for %s from %s and notifserv %s", id, servName, s.getPathParameters(), ns);
		ses = s;  // setting the session to the subscriber
		.....
		

### Add common js code to the js section of common a web page code

    @%'insert/headextra.htmt'@
    <script src="@contextpath@/js/buzzbee.js" language="Javascript"></script>
    <script src="@contextpath@/js/wsinit.js" language="Javascript"></script>
    
The example [source](https://github.com/drogatkin/sharelinks/blob/master/src/res/view/insert/headextra.html)

### Tell to the underneath framework to process the added js files
It's done  in the configuration properties file as

     app_name=Share Links

     headextra=insert/headextra.html
     .....
    
The example [source](https://github.com/drogatkin/sharelinks/blob/69637f8ce176b682841d2bb6c1410f0d48650ccc/src/res/cfg/sharelinks.properties#L37) 

### Add custom js code to the common initialization code of a web page

The code provided in [wsinit.js](https://github.com/drogatkin/sharelinks/blob/master/src/js/wsinit.js). The function
 **extra_actions** is defined here and gets called in the common initialization sequence. 
 
### Add a notification code and a corresponding web page action
 
When some event happens and a web page needs to be notified regarding it, the following code needs to be added
on the server side:

     WebEvent we;
	 ns.publish(we = new WebEvent().setAction("refreshList").setId(getProperties().getProperty(SharelinksModel.NOTIF_CHANNEL))); 
	 
See a complete source [here](https://github.com/drogatkin/sharelinks/blob/6fef116e4c67a55cb9baf06cb7f80548f72c47d7/src/java/com/walletwizz/sharelinks/ux/Sync.java).
The action **refreshList** has to be triggered on a web page side, therefore the function **refreshList** has to be added in 
[js](https://github.com/drogatkin/sharelinks/blob/6fef116e4c67a55cb9baf06cb7f80548f72c47d7/src/js/wsinit.js#L13).

If you follow the steps, the notification starts working instantly. 


Note that if you use another than WebBee framework, like React, Angular, Vue or other, then the steps can be slightly different, so check with the documentation of a particular framework.
