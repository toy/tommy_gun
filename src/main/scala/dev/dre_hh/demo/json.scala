package dev.dre_hh.demo

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StringType, StructType}

case class Message(action: String, issue: String, arm: String)
case class Arm(issue:String, name: String, alpha: Int, beta: Int, score: Double)

//spark-shell demo code
object Json  {
  def apply: Unit = {

    val spark = SparkSession
      .builder()
      .appName("KafkaSparkDemo")
      .getOrCreate()

    import spark.implicits._

    var messages = Seq(
        (0, """{"action":"draw","issue":"colors", "arm":"green"}"""),
        (1, """{"action":"reward","issue":"colors", "arm":"green"}"""),
        (2, """{"action":"draw","issue":"colors", "arm":"green"}"""),
        (3, "")
    ).toDF("id", "value")

    var jsonSchema = new StructType().
      add("action", StringType).
      add("issue", StringType).
      add("arm", StringType)

    var ds = messages.
      select(from_json(col("value").cast("string"), jsonSchema) as "message").
      select("message.*").as[Message]

    var res = ds.flatMap(message => message.action match {
      case "draw" => Option(Arm(message.issue, message.arm, 0, 1, 0))
      case "reward" => Option(Arm(message.issue, message.arm, 1, -1, 0))
      case _ => None
    }).groupByKey(message => s"${message.issue}_${message.name}").
      reduceGroups((acc, arm) => {
        Arm(arm.issue, arm.name, acc.alpha + arm.alpha, acc.beta + arm.beta, 0)
      })
  }
}


