package skuber.api.streaming

import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.json.{Format, JsError, JsSuccess, Json}
import skuber.ObjectResource
import skuber.api.client.{K8SException, Status}

import scala.concurrent.ExecutionContext

private[api] object BytesToStreamResource {
  def apply[S <: ObjectResource](bytes: Source[ByteString, _], bufferSize: Int = 10000)(implicit ec: ExecutionContext, format: Format[S]): Source[S, _] = {
    bytes
      .via(JsonReader.select("$.items[*]"))
      .map { resourceBytes =>
        Json.parse(resourceBytes.utf8String).validate[S] match {
          case JsSuccess(value, _) => value
          case JsError(e) => throw new K8SException(Status(message = Some("Error parsing object"), details = Some(e.toString)))
        }
      }
  }
}
