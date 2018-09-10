/*
This is the Fetcher actor. It's job is to fetch urls by scalaj package

Input Messages:
  1) Fetch(user: User):
    FetchManager actor sends this to fire actor to fetch new user followers
  2) WorkAvailable:
    FetchManger sends this to inform actor that new user is ready to fetch

Output Messages:
  1) GiveMeWork:
    When a Fetcher actor is free, sends this message to FetchManager get a user to fetch
  2) Interpret: Fetcher after done the fetching work, sends this msg to ResponseInterpreter actor to parse it to json

Constructor Parameters:
  token: Option[String]
  future_mode: Boolean
  parser: ActorRef => ResponseInterpreter actor for parsing fetched user, this is needed for sending Interpret msg
  manager: ActorRef => FetchManager actor, this is needed for sending GiveMeWork msg
 */

import akka.actor._
import scalaj.http._
import scala.concurrent._
import Misc._

// Fetcher constructor
class Fetcher(val token:Option[String],
              val future_mode: Boolean,
              val parser: ActorRef,
              val manager: ActorRef) extends Actor with ActorLogging {
  // We will need an execution context for the future.
  // Recall that the dispatcher doubles up as execution
  // context.
  import context.dispatcher
  def receive = {
    case Fetcher.WorkAvailable => manager ! FetchManager.GiveMeWork
    case Fetcher.Fetch(user) => {
      val login = user.login
      val url = s"https://api.github.com/users/$login/followers"
      val unauth_request = Http(url)
      // converting unathorized request to authorized
      val auth_req = token.map(t => unauth_request.header("Authorization",
        s"token $t"))
      // if token is not available, do an unathorized request
      val request = auth_req.getOrElse(unauth_request)

      // fire the request in wrapping up a future
      if (future_mode) {
        // fire the request in wrapping up a future
        val response = Future {
          request.asString
        }
        response.onComplete(r => parser ! ResponseInterpreter.Interpret(r.get.body, user))
      }
      else {
        val response = request.asString
        parser ! ResponseInterpreter.Interpret(response.body, user)
        //manager ! FetchManager.GiveMeWork
      }
    }
  }
}

  object Fetcher {

    // message definition
    case class Fetch(user: User)
    case object WorkAvailable
    // Props factory definitions
    def props(token: Option[String],
              future_mode: Boolean,
              parser: ActorRef,
              manager: ActorRef): Props = Props(classOf[Fetcher], token, future_mode, parser, manager)

    def props(future_mode: Boolean,
              parser: ActorRef, manager: ActorRef): Props = Props(classOf[Fetcher], None, future_mode, parser, manager)
  }

