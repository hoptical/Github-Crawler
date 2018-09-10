/*
FollowerExtractor actor: It's job is to extract specific parameters from provided json
and sends AddQueue msg to FetchManager

Input Messages:
  1) Extract(followers_list: List[JValue], parent:User)

Output Messages
  1) AddQueue: sends this message to FetchManager to add new user to queue and visited

Constructor parameters:
  manager: ActorRef => this is needed to send AddQueue message to FetchManager
 */

import akka.actor._
import org.json4s._
import org.json4s.native.JsonMethods._
import Misc._

// Class Constructor
class FollowerExtractor(manager: ActorRef) extends Actor with ActorLogging {
  implicit val formats = DefaultFormats // This is needed for implement extract method on JValue
  def receive ={
    case FollowerExtractor.Extract(followers_list: List[JValue], parent:User) => {
      // for every JValue in the list, extract User class from json and send it to manager actor
      for (item <- followers_list) {
        // extracting User class from json
        val follower: User = item.extract[User]
        //increment follower role with respect to its parent
        follower.role = parent.role + 1
        // Make a relation Map from follower and parent and add it to relation Set
        relation += Map("follower" -> follower.login, "parent" -> parent.login)
        // This Block is for Runtime debug
          if (relation.size == 791) {
            stop_tick()
            println(s"runtime(ms)= $runtime")
          }
        // Send AddtoQueue Message to Manager
        manager ! FetchManager.AddToQueue(follower)

      }
    }
  }
}

// Companion Object
object FollowerExtractor{
  case class Extract(followers_list: List[JValue], parent:User)
  def props(manager: ActorRef):Props = Props(classOf[FollowerExtractor], manager)
}
