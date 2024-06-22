import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Directives._
import spray.json._
import DefaultJsonProtocol._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import java.util.Properties
import scala.collection.mutable.HashMap
import scala.util.Random
import scala.collection.mutable.ArrayBuffer
import java.util.Date
import scala.concurrent.duration._

case class Metric(metricName: String, value: Int, timestamp: Date, host: String, region: String)

object JsonFormats {
  import DefaultJsonProtocol._
  implicit object DateJsonFormat extends JsonFormat[Date] {
    def write(date: Date): JsValue = JsString(date.getTime.toString)
    def read(json: JsValue): Date = json match {
      case JsString(str) => new Date(str.toLong)
      case _ => deserializationError("Expected date in String format")
    }
  }
  implicit val metricFormat: RootJsonFormat[Metric] = jsonFormat5(Metric.apply)
}

import JsonFormats._

object AkkaMessageProducer {
  implicit val system = ActorSystem(Behaviors.empty, "MyActorSystem")

  val producer = KafkaProducerFactory.createProducer()

  def generateMessage: String = {
        val hosts = ArrayBuffer[String]("server01","server02","server03","server04","server05")
        val regions = ArrayBuffer[String]("us-east-1","us-west-2","eu-west-1","eu-central-1","ap-south-1")
        val metricTypes = HashMap[Int, String](
            0 -> "cpu_usage_percentage",
            1 -> "memory_usage_gb",
            2 -> "disk_io_rate_mbps",
            3 -> "network_throughput_mbps",
            4 -> "response_time_ms",
            5 -> "error_rate_percentage"
        )
        val metricValues = HashMap[String, Int] (
            "cpu_usage_percentage" -> 100,
            "memory_usage_gb" -> 64,
            "disk_io_rate_mbps" -> 500,
            "network_throughput_mbps" -> 1000,
            "response_time_ms" -> 2000,
            "error_rate_percentage" -> 100
        )
        val metricName = metricTypes.getOrElse(Random.nextInt(metricTypes.size-1), "")
        val metricValue = Random.nextInt(metricValues.getOrElse(metricName, 0))
        val host = hosts(Random.nextInt(hosts.size-1))
        val region = regions(Random.nextInt(regions.size-1))
        val jsonobj = Metric(metricName,metricValue,new Date(),host,region)
        println(jsonobj)
        val jsonString = jsonobj.toJson.toString()
        jsonString
   }

    def main(args: Array[String]): Unit = {
        while(true) {
            val jsonString = generateMessage
            val record = new ProducerRecord[String, String]("metric-topic", jsonString)
            producer.send(record)
            println(s"Sent to Kafka: $jsonString") 
            Thread.sleep(5000)
        }
    }
}