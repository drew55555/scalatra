package com.twitterCluster.app

import com.github.nscala_time.time.Imports._
import com.mongodb.casbah.Imports._
import java.util.Date
import scala.collection.immutable.Map
import scala.collection.mutable._
import org.bson._

class Tweet(id: String, text: String, location: MongoDBList, date: Date, hashtags: List[Any]) {

  val ID: String = id.toString()
  val Text: String = text
  val Location: MongoDBList = location
  val Date: Date = date
  val nearestNeighbors : ListBuffer[(String, Double)] = ListBuffer[(String, Double)]()
  var revNearCount = 0

  val termFreq: scala.collection.immutable.Map[String, Int] = {
    val words = ArrayBuffer[String]()
    Text.split("[^A-Za-z]+").copyToBuffer(words)
    for(x <- hashtags)
      words += x.toString
    words.filter(x => x.toString.length() > 2).groupBy(x => x.toUpperCase()).mapValues(_.size)
  }
}