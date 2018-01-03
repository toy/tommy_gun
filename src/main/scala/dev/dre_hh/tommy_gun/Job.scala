package dev.dre_hh.tommy_gun

import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StringType, StructType}
import breeze.stats.distributions.Beta

import scala.math

case class Message(action: String, issue: String, arm: String)
case class Arm(issue:String, name: String, alpha: Int, beta: Int, score: Double)

object Job {
  def main(args: Array[String]): Unit = {
    val kafkaBrokers = sys.env.get("KAFKA_BROKERS")
    val kafkaGroupId = sys.env.get("KAFKA_GROUP_ID")
    val kafkaTopic = sys.env.get("KAFKA_TOPIC")

    require(kafkaBrokers.isDefined, "KAFKA_BROKERS has not been set")
    require(kafkaGroupId.isDefined, "KAFKA_GROUP_ID has not been set")
    require(kafkaTopic.isDefined, "KAFKA_TOPIC has not been set")

    val spark = SparkSession
      .builder()
      .appName("TommyGun")
      .getOrCreate()

    import spark.implicits._

    val kafkaFrame = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBrokers.get)
      .option("subscribe", kafkaTopic.get)
      .option("group.id", kafkaGroupId.get)
      .option("auto.offset.reset", "latest")
      .load()

    val jsonSchema = new StructType()
      .add("action", StringType)
      .add("issue", StringType)
      .add("arm", StringType)

    val messages = kafkaFrame.
      select(from_json(col("value").cast("string"), jsonSchema) as "message").
      select("message.*").as[Message]

    val arms = messages.flatMap(message => message.action match {
        //increment beta if arm drawn
      case "draw" => Some(Arm(message.issue, message.arm, 0, 1, 0))
        //increment alpha (and decrement beta) if arm rewarded
      case "reward" => Some(Arm(message.issue, message.arm, 1, -1, 0))
      case _ => None
    })

    val result = arms.groupByKey(arm => s"${arm.issue}_${arm.name}")
      .reduceGroups((acc, arm) => {
        val alpha = math.max(1, acc.alpha + arm.alpha)
        val beta =  math.max(1, acc.beta + arm.beta)
        val score = new Beta(alpha, beta).draw

        arm.copy(alpha=alpha, beta=beta, score=score)
      })

    val query = result.writeStream
      .outputMode("complete")
      .format("console")
      .option("truncate", false)
      .start()
    query.awaitTermination()
  }
}
