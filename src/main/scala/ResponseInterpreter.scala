/*
ResponseInterpreter actor. It's job is to parse fetched messages to json

Input Messages:
  1) Interpret(body, parent:User)
    parse the body to json
Output Messages:
  1) Extract:
    sends this message to FollowerExtractor actor to extract specific fields from provided json

Constructor parameters:
  extractor: ActorRef => This is needed to send the Interpret message
 */

import Misc.User
import akka.actor._
import org.json4s._
import org.json4s.native.JsonMethods._

class ResponseInterpreter(extractor: ActorRef) extends Actor with ActorLogging {
  private def items_extract(body: String) = {
    val jsonResponse = parse(body)
    val JArray(items) = jsonResponse
    items
  }
  def receive = {
    case ResponseInterpreter.Interpret(body:String, parent) => {
      val followers_list = items_extract(body)
      extractor ! FollowerExtractor.Extract(followers_list, parent)
  }
  }

}

object ResponseInterpreter{
  case class Interpret(body: String, parent:User)
  def props(extractor: ActorRef):Props = Props(classOf[ResponseInterpreter], extractor)
}