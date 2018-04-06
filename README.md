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

Dead Letter Office: Any time a message is destined for an actor that either doesn't exist or is not running, it goes to the dead letter office.
User Guardian Actor: The user guardian actor is the parent of all actors we create from the ActorSystem.
System guardian actor: It serves the same purpose as the user guardian actor, but for "system" actors.
Scheduler: The default one lives as a child of the ActorSystem or also can be instantiated.
Event Stream: We use it every time we write a log message, and for other uses.
Settings: Akka uses a new configuration system that's useful for configuring Akka and your application. You can access it from the ActorSystem

CONTEXT:
Every actor has a context member that helps it do a lot of its work. 
The context decouples your internal actor logic from the rest of Akka that's managing it

Functionalitites:
ActorCreation:  System.actorOf --> child of user Guardian actor
                context.actorOf --> child of Parent actor
SystemAccess:   Can access ActorSystem, scheduler and the settings
Relationship access:    Context knows parent is, who our children are, and gives us the ability to find other actors in the ActorSystem
State:      When accessing self or sender from the actor, we're actually getting that information from the ActorContext
            Also tells about Current behavioural state, its Dispatcher
            
            
Looking up actors:
The ActorContext provides us with a set of three actorFor functions:
actorFor(path: Iterable[String]): ActorRef Allows us to use an iterable collection to look up our actors. For example, List("/user", "/Plane", "/ControlSurfaces").

If you have an ActorPath, then you can get the ActorRef using the ActorSystem
if you have an ActorContext, you can get the children and parent ActorRefs directly
if you want the ActorPath of any of those, then you'll need to get that through the ActorRef

ACTOR LIFECYCLE:
We can explicitly create and start an actor by constructing it using the factory methods we've seen before; i.e., context.actorOf(...) and system.actorOf(...).
We can explicitly stop an actor in several different ways, the most explicit being a call to context.stop(...) or system.stop(...), where the "..." is a placeholder for a valid ActorRef.
If our code throws an exception, then that immediately kicks in the supervisory behaviour. Depending on what we decide to do, our actor could be restarted, resumed, or stopped.
In addition to influencing the states of the actor's life cycle on a macro scale (i.e., start, stop, and restart), we also get some hooks into that life cycle, which we can use to perform certain activities at the right time.

What happens to the current message?
The simple answer is that, by default, it disappears. Since the message was (most likely) the cause of the problem in the first place, and computers being the rather deterministic machines that they are,
then it's reasonable to assume that trying to process it again will cause the same problem to occur. Because of this, the message is removed from the mailbox and processing begins at the next message. 

SUPERVISION STRATEGY:
1. OneForOne: Decision regarding an actor's failure will apply only to that one failed actor
2. AllForOne: Decision regarding a single actor's failure to all children

If code throws exception, then that immediately kicks in the supervisory behaviour. Depending on what we decide to do, our actor could be restarted, resumed, or stopped.
It takes care of its children

DECIDER DERIVATIVES:
1. STOP
2. RESUME
3. RESTART
4. ESCALATE

Resume Actor: Restarting provides the hooks you might want in order to participate in the restart life cycle. 
Using preRestart and postRestart, you can gain access to the exception that caused the failure and to the message that was being processed during that failure. 
In preRestart we have access to the message and the exception, including the sender of the message.   
post Restart has only access to the exception and not the message

Default implementation of pre and post restart:

def preRestart(reason: Throwable, message: Option[Any]) {
    context.children foreach context.stop
    postStop()
  }
  
  def postRestart(reason: Throwable) {
    preStart()
  }        
  
 Creation of children can be done in 2 ways:
 1. Creation of children in the constructor
 2. Creation of children in the prestart method
 If actor restart happens, it calls post stop method after terminating all children
 After that, it constructs a new instance of actor 
    If you're creating children in your constructor, they will get created at this time
 Thereafter, it will call postRestart method
    If you're creating children in your preStart method, they will get created at this time   
