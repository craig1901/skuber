package skuber.api

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import client._
import skuber.json.format._
import org.specs2.mutable.Specification
import play.api.libs.json.{JsValue, Json}
import skuber.{ListResource, Namespace}
import skuber.api.streaming.BytesToStreamResource

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class BytesToStreamResourceSpec extends Specification {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val loggingContext: LoggingContext = new LoggingContext { override def output:String="test" }

  "Can create a source from the JSON items list" >> {
    val listJson = Json.toJson(
      ListResource("v1", "NamespaceList", None, List(Namespace("Test"), Namespace("Random"), Namespace("NS")))
    )
    val lsStr = Json.stringify(listJson)


    val bytesSource: Source[ByteString, NotUsed] = Source.single(ByteString(lsStr))
    val nsSource: Source[Namespace, _] = BytesToStreamResource[Namespace](bytesSource, 10000)
    val nsSink: Sink[Namespace, Future[Namespace]] = Sink.head[Namespace]
    val run: Future[Namespace] = nsSource.runWith(nsSink)

    val result: Namespace = Await.result(run, Duration.Inf)
    result.name mustEqual "Test"

  }
}
