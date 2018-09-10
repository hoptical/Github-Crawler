/*
This is The FetchManager actor. It has two states:
  1) receive While Empty: receives messages from FollowerExtractor and Fetcher while Queue is empty
  2) receive While Empty: receives messages from FollowerExtractor and Fetcher while Queue is not empty

Input Messages:
  1) GiveMeWork:
     When a Fetcher actor is free, sends this message to get a user to fetch
  2) AddQueue:
     FollowerExtractor actor sends this message to add a user to the queue and visited list
Output Messages:
  1) WorkAvailable:
     Sends this message to Fetcher actor to inform the router that new users are ready to fetch

Constructor Parameters:
  token:Option[String] => token needed to login to access github api
  depth:Int
  future_mode:Int
  n_fetchers:Int => number of fetcher actors needed for roundrobin router
  n_parsers:Int => number of parser actors needed for roundrobin router
  n_extractors:Int => number of extractors actors needed for roundrobin router
 */

import akka.actor._
import akka.routing._
import Misc._

// FetchManager constructor
class FetchManager(val token: Option[String],
                   val depth: Int,
                   val future_mode: Boolean,
                   val n_fetchers:Int,
                   val n_parsers:Int,
                   val n_extractors:Int) extends Actor with ActorLogging {

  // handle states
  def receiveWhileEmpty: Receive = {
    // if queue is empty:
    case FetchManager.AddToQueue(user:User) =>
        queue_adding(user)
    // if GiveMeWork message received in empty state mode, do nothing
    case FetchManager.GiveMeWork =>

  }
  def receiveWhileNoneEmpty: Receive = {
    // if queue is not empty
    case FetchManager.AddToQueue(user:User) =>
        queue_adding(user)

    case FetchManager.GiveMeWork => {
      // dequeue a user and send it to Fetcher actor which wants the work(sender)
      val candidate = queue.dequeue()
      sender ! Fetcher.Fetch(candidate)

      if (queue.isEmpty)
        // if queue is empty, change state to Empty mode
        context.become(receiveWhileEmpty)
  }
  }
  // Initialize receive in Empty mode
  def receive = receiveWhileEmpty

  //// Instantiate actors
  // instantiate FollowerExtractor Actor
  val extractorActor:ActorRef = context.actorOf(
    RoundRobinPool(n_extractors).props(FollowerExtractor.props(self)))
  // instantiate ResponseInterpreter(Parser) Actor
  val parserActor:ActorRef = context.actorOf(
    RoundRobinPool(n_parsers).props(ResponseInterpreter.props(extractorActor)))
  // define the router: Instantiate Fetcher actors
  val FetcherRouter:ActorRef = context.actorOf(
    RoundRobinPool(n_fetchers).props(Fetcher.props(token,
      future_mode,
      parserActor,
      self)))

  // Helper methods:
  def check_fetched(login:String):Boolean ={
    visited(login)
  }
  def queue_adding(user:User): Unit = {
    // if the user is not visited previously and it's role is in depth range, you may fetch it
    if (!check_fetched(user.login)) {
      if (user.role < depth) {
        visited += user.login
        queue += user
        context.become(receiveWhileNoneEmpty)
        FetcherRouter ! Fetcher.WorkAvailable
      }
    }
  }

  // life cycle handling of postStop to log relation and visited Sets after actor shutting down.
  override def postStop(): Unit = {
    log.info(s"Size of relations: ${relation.size}")
    log.info(s"Size of visited: ${visited.size}")
    super.postStop()
  }
}

// Companion Object
object FetchManager{
  case object GiveMeWork
  case class AddToQueue(user:User)
  def props(token: Option[String],
            depth: Int,
            future_mode:Boolean,
            n_fetchers: Int,
            n_parsers: Int,
            n_extractors: Int):Props = Props(classOf[FetchManager], token, depth, future_mode,
                                            n_fetchers, n_parsers, n_extractors)
}