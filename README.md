# AkkaConcurrencyApp: Basic Actor App with receive and scheduling functionality

When you're inside an actor, you have an implicit value in scope that can satisfy the second curried parameter of the ! method, and this is how the sender is populated when the message is sent.
When you send your message, Akka puts your current actor's ActorRef and the message itself inside an envelope, and that's what actually gets delivered to the target actor.

def ! (message: Any)(implicit sender: ActorRef = null): Unit 
def forward (message: Any)(implicit context: ActorContext): Unit

If A sends to B and B forwards that message to C, then C sees the sender of the message as A, not B.
If A sends to B and B tells that message to C, then C sees the sender of the message as B

Best way to use sender:
case SomeMessage =>
    val requestor = sender
    context.system.scheduleOnce(5 seconds) {
      requestor ! DelayedResponse
    }
    
    def forward(message: Any)(implicit context: ActorContext) =
        tell(message, context.sender)
        
System configuration. When you need access to the configuration of Akka or  your own application, you can get it from the ActorSystem.
The default Scheduler, which we've seen earlier, is also available via the  ActorSystem.
You also have access to a generally accessible EventStream. 
The logger  uses it to log messages, which are events to which you can subscribe.  
You can  use it as well, to publish messages across your entire ActorSystem.
The dead letter office is available, which means you can hook things up to  it directly if you'd like.
We can obtain references to all of the currently  running actors in an ActorSystem's hierarchy via a set of functions called  actorFor.
You can get the uptime of your app from here as well.There are functions that let you shut the system down, as well as stop  individual actors.