/*
This is the main file. In this file, configuration parameters including token number
is defined. Then FetchManager actor is fired which that fires other actors to
fetch github followers of a base user
 */

import Misc._
import akka.actor._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main extends App{
    // Define configuration parameters
    // define the token
    val token: String = ""
    // depth: Integer number that shows how many depth do we want to fetch followers from base user
    val depth:Int = 2
    // Future mode: decides whether to use or not use future in the Fetcher actors
    val future_mode = true
    // number of actors for every actor layer
    val n_fetchers = 16
    val n_parsers = 1
    val n_extractors = 1
    // defining the system
    val system = ActorSystem("GithubFetcher")

    //make the FetchManager actor
    val manager = system.actorOf(FetchManager.props(Some(token),
      depth,
      future_mode,
      n_fetchers,
      n_parsers,
      n_extractors), name="Manager")

    // define base user
    val base_user = User("Odersky")
    base_user.role = 0   // base user has the role number of zero

    start_tick()

   // Start sending the message for our FetchManager
    manager ! FetchManager.AddToQueue(base_user)

    // terminate the system after specified time
    system.scheduler.scheduleOnce(5.seconds) {system.terminate()}
}
