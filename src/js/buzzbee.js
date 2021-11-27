var WSAPI = (function() {
      var wskt
      var handlers = {}
      var notifRecon = 0
      var maxReconn = 16 * 1000
      var ws_url
      var serv
      function setup() {
         console.log('connecting to '+ws_url+'/notif/web/'+serv)
         wskt = new WebSocket(ws_url+'/notif/web/'+serv)
         wskt.onopen = function(d) {
             notifRecon = 500
         }
         wskt.onmessage = function(e) {
            var note = JSON.parse(e.data)
             var hs = handlers[note.func]
             if (!hs) {
               if(window[note.func] && window[note.func] instanceof Function)
                 window[note.func].apply(this, note.params || [])
               else
                  console.log('function '+note.func+" doesn't exist or not function'")
             } else if (hs){
            for(var h in hs) {
				if(hs.hasOwnProperty(h)){
					try {
						hs[h].apply(this, note.params || [])
						} catch(e) {
							console.log("Error in calling event handler "+e)
							}
				}	
			}
            } 
         }
         wskt.onclose = function(e) {
            if (notifRecon == 0)
              notifRecon = 500
            notifRecon *= 2
            if (notifRecon > maxReconn)
              notifRecon= maxReconn;
            if (console && console.log)
	            console.log('Oops '+e + ' reconnecting in '+notifRecon+'ms')
            setTimeout(setup, notifRecon)
         }
      }   

 
      return {
       /** this call is used to subscribe on certain events by id
        * @param {string} id - id of event, id can include distribution channel name
        */
        subscribe: function(id) {
          if (wskt && wskt.readyState===WebSocket.OPEN)
            wskt.send(JSON.stringify({op:'subscribe', id:id}))
        }   ,
     /** this call is used to unsubscribe on certain events by id
      * @param {string} id - id of event
      */
        unsubscribe: function(id) {
          if (wskt && wskt.readyState===WebSocket.OPEN)
            wskt.send(JSON.stringify({op:'unsubscribe', id:id}))
        }   ,
      /** this call is used for sending notifications
       * @param {string} id - id of event
       * @param {string} en - notification event name. It can be either name of vent listener
       * or name of JS function to call
       * @param {array} ps - event parameters
       */
        notify: function(id,en,ps) {
           if (wskt && wskt.readyState===WebSocket.OPEN)
             wskt.send(JSON.stringify({op:'notify', id:id, data:{name:en, params:ps}}))
           // else throw new Exception()
        }   ,
        /** this call is used for sending notifications and persist them to be re-send to new subscribers
         * @param {string} id - id of event
         * @param {string} en - notification event name. It can be either name of vent listener
         * or name of JS function to call
         * @param {array} ps - event parameters
         */
          notifyAndKeep: function(id,en,ps) {
             if (wskt && wskt.readyState===WebSocket.OPEN)
               wskt.send(JSON.stringify({op:'retain', id:id, data:{name:en, params:ps}}))
             // else throw new Exception()
          }   ,
          /** this call is used for sending notifications and clear a possible re-send queue
           * @param {string} id - id of event
           * @param {string} en - notification event name. It can be either name of vent listener
           * or name of JS function to call
           * @param {array} ps - event parameters
           */
            notifyAndClear: function(id,en,ps) {
               if (wskt && wskt.readyState===WebSocket.OPEN)
                 wskt.send(JSON.stringify({op:'clear', id:id, data:{name:en, params:ps}}))
               // else throw new Exception()
            }   ,
        /** this call is used to add a listener for a specified event
         * @param {string} en - notification event name
         * @param {function} h - a handler function for this event name
         */
        addListener:function(en,h) {
           var hs = handlers[en];
           if (!hs) {
             hs = []
             handlers[en] = hs
          }
          hs.push(h)            
        }    ,
        init: function(host,srv) {
        	if (!host || typeof(WebSocket) !== "function")
        		return;
            ws_url=host
            serv=srv || ""
           //console.log('got: '+host+' and set '+ws_url);
            setup()   
        }     ,
        isReady: function() {
            return wskt && wskt.readyState===WebSocket.OPEN
        }     ,
        escape : function (str) {
          return str
            .replace(/[\"]/g, '\\"')
            .replace(/[\\]/g, '\\\\')
            .replace(/[\/]/g, '\\/')
            .replace(/[\b]/g, '\\b')
            .replace(/[\f]/g, '\\f')
            .replace(/[\n]/g, '\\n')
            .replace(/[\r]/g, '\\r')
            .replace(/[\t]/g, '\\t')
          }   
      }
})();
