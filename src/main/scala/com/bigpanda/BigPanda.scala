import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.io.StdIn
import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import scala.util.parsing.json.JSON

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.PathMatchers

object BigPanda extends App {

  implicit val actorSystem = ActorSystem("system")
  implicit val ec = actorSystem.dispatcher
  
  val etypeStatsActor = actorSystem.actorOf(Props[StatsActor], "etypeStatsActor")
  val dataStatsActor = actorSystem.actorOf(Props[StatsActor], "dataStatsActor")
  val parser = new Parser(etypeStatsActor, dataStatsActor)
  val server = new Server(etypeStatsActor, dataStatsActor)
  val p = Process("./generator-linux-amd64") run ProcessLogger({ line => parser.handleLine(line) }, { line => println("stderr: " + line) })
  
  println("running...")
  server.run()
  StdIn.readLine()
  p.destroy()
  actorSystem.terminate()
  println("goodbye")
}

class Parser(etypeStatsActor: ActorRef, dataStatsActor: ActorRef)(implicit ex: ExecutionContext) {
  
  def handleLine(line: String) = {
    val parsed = Future { parse(line) }
    parsed.onSuccess({ case event: Event => 
      etypeStatsActor ! Increment(event.etype) 
      dataStatsActor ! Increment(event.data)
        })
    parsed.onFailure({ case t => println(t) })
  }

  def parse(line: String): Event = {
    val json = JSON.parseFull(line)
    json match {
      case Some(m: Map[String, Any] @unchecked) => {
        val etype = m.getOrElse("event_type", { throw new ParseFailedExcpetion("event_type not found") }) match {
          case s: String => s
          case _         => throw new ParseFailedExcpetion("event_type is not a string")
                }
        val data = m.getOrElse("data", { throw new ParseFailedExcpetion("data not found") }) match {
          case s: String => s
          case _         => throw new ParseFailedExcpetion("data is not a string")
                }
        Event(etype, data)
            }
      case _ => throw new ParseFailedExcpetion("event is not a JSON object")
        }
    }
}

class StatsActor extends Actor {
  
  var stats: Map[String, Long] = Map[String, Long]().withDefaultValue(0)
  
  def receive = {
    case Increment(key) => updateStats(key)
    case EventQuery(None) => sender() ! stats 
    case EventQuery(Some(key)) => sender() ! stats(key) 
  }

  def updateStats(key: String) = {
    stats += (key -> (stats(key) + 1))
  }
}

class Server(etypeStatsActor: ActorRef, dataStatsActor: ActorRef)(implicit ec: ExecutionContext, actorSystem: ActorSystem) {
  implicit val timeout = Timeout(5 seconds)
  implicit val materializer = ActorMaterializer()

  def statTypeRoute(actor: ActorRef): Route = {
    pathPrefix(PathMatchers.Segment) { key =>
      pathEndOrSingleSlash {
        val statsFuture = (actor ? EventQuery(Some(key))).mapTo[Long]
        onSuccess(statsFuture) { stats => complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, stats.toString + "\n")) }
        }
      } ~
   pathEndOrSingleSlash {
        val statsFuture = (actor ? EventQuery(None)).mapTo[Map[String, Long]]
        onSuccess(statsFuture) { stats => complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, stats.mkString("\n") + "\n")) }
      }
  }
  
  val route =
    get {
      pathPrefix("event-type") {
        statTypeRoute(etypeStatsActor)
            } ~
      pathPrefix("data") {
        statTypeRoute(dataStatsActor)
            }
    }

  def run() = {
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  }
}

class ParseFailedExcpetion(message: String, cause: Throwable = null) extends Exception(message, cause)
case class Event(etype: String, data: String)
case class Increment(key: String)
case class EventQuery(etype: Option[String])

