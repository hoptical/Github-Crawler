/*
Misc Object: This object includes different fields and methods for storing results
and debugging runtime
 */

import scala.collection.mutable

object Misc {
  // Define the class User for extracting data from json
  case class User(login: String) {
    var role: Int = _
  }
  // visited: Set containing visited nodes
  val visited = mutable.Set[String]()
  // queue: Queue containing users ready to fetch on
  val queue = mutable.Queue[User]()
  // relation: Set containing relation result between a follower and a parent
  val relation = mutable.Set[Map[String,String]]()

    // This is for runtime debug
  var start: Long = _
  var stop: Long = _
  def start_tick() :Unit = {start = System.nanoTime()}
  def stop_tick() :Unit = {stop = System.nanoTime()}
  def runtime:Long = (stop - start)/1000000
}