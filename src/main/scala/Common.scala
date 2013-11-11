package com.twitterCluster.app

import com.mongodb.casbah.Imports._
import java.util.Date
import scala.collection.immutable._
import scala.math._
import scala.annotation.tailrec
import org.bson._
import javax.xml.soap.Text
import sun.font.TrueTypeFont
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer

object Common {

  def getTopicsFromQuery(query: MongoDBObject): List[String] = {

    def buildMongoDbObject(tweet: Tweet): MongoDBObject = {
      val locBuilder = MongoDBObject.newBuilder
      locBuilder += "type" -> "Point"
      locBuilder += "location" -> tweet.Location
      val builder = MongoDBObject.newBuilder
      builder += "Text" -> tweet.Text
      builder += "Location" -> locBuilder.result
      builder += "TweetTime" -> tweet.Date.toString()
      builder.result
    }

    def countReverseNeighboors(tweets: Map[String, Tweet]): Unit = {
      for (
        tweet <- tweets.values;
        neighboor <- tweet.nearestNeighbors
      ) {
        tweets.get(neighboor._1).get.revNearCount += 1
      }
    }

    def findNeighboors(tweets: scala.collection.Iterable[Tweet], idfResult: Map[String, Double]): Unit = {
      for (tweet <- tweets) {
        var dists = (for (
          tempTweet <- tweets if (tweet.ID != tempTweet.ID)
        ) yield new Tuple2(tempTweet.ID, tweetDotProd(tweet, tempTweet, idfResult))).toList
        dists = dists.sortBy(x => x._2)
        tweet.nearestNeighbors ++= dists.takeRight(10)
      }
    }

    def getTweets(mongo: MongoCollection, query: MongoDBObject): Map[String, Tweet] = {
      val res = for (x <- mongo.find(query)) yield buildTweet(x)
      res.toMap
    }

    def createIDF(tweets: scala.collection.Iterable[Tweet], count: Int): Map[String, Double] = {
      val words = scala.collection.mutable.ArrayBuffer[String]()
      tweets.foreach(x => words ++= x.termFreq.keys)
      idfMap(words.groupBy(x => x), count) //.mapValues(x => idf(x.size, docCount))
    }

    def idfMap(elements: Map[String, scala.collection.mutable.ArrayBuffer[String]], docCount: Double): Map[String, Double] = {
      elements.flatMap(x => if (x._2.size > 1) Some(x._1 -> idf(x._2.size.toDouble, docCount)) else None)
    }

    def idf(numTerms: Double, docCount: Double): Double = {
      log(docCount / numTerms)
    }

    def tweetDotProd(tweet1: Tweet, tweet2: Tweet, idfResult: Map[String, Double]): Double = {
      val wordSet = (tweet1.termFreq.keys ++ tweet2.termFreq.keys).toList
      @tailrec
      def dotProd(words: List[String], accu: Double): Double = {
        words match {
          case word :: tail => dotProd(tail, accu + ((tweet1.termFreq.getOrElse[Int](word, 0) * idfResult.getOrElse[Double](word, 0)) * (tweet2.termFreq.getOrElse[Int](word, 0) * idfResult.getOrElse[Double](word, 0))))
          case _ => accu
        }
      }
      dotProd(wordSet, 0)
    }

    def tweetDist(tweet1: Tweet, tweet2: Tweet, idfResult: Map[String, Double]): Double = {
      val wordSet = (tweet1.termFreq.keys ++ tweet2.termFreq.keys).toList
      @tailrec
      def dist(words: List[String], accu: Double): Double = {
        words match {
          case word :: tail => dist(tail, accu + diffSqares(tweet1.termFreq.getOrElse[Int](word, 0) * idfResult.getOrElse[Double](word, 0), tweet2.termFreq.getOrElse[Int](word, 0) * idfResult.getOrElse[Double](word, 0)))
          case _ => accu
        }
      }
      dist(wordSet, 0)
    }

    def diffSqares(x: Double, y: Double): Double = {
      pow(x - y, 2)
    }

    def buildTweet(obj: MongoDBObject): (String, Tweet) = {
      val id = obj.getAs[types.ObjectId]("_id").get
      val text = obj.getAs[String]("Text").get;
      val location = obj.getAs[BasicDBObject]("Location").get
      val coordinates = location.getAs[MongoDBList]("coordinates").get
      val date = obj.getAs[Date]("TweetTime").get
      val hashtags = obj.getAs[MongoDBList]("HashTags").get.toList
      new Tuple2(id.toString(), new Tweet(id.toString(), text, coordinates, date, hashtags))
    }
    
    val factory = new MongoFactory("Drew", "Hello")
    val tweets = getTweets(factory.collection, query)
    val tweetList = tweets.values.toList
    val count = tweetList.length
    println(count + " Tweets Read")
    val idfTable = createIDF(tweets.values, count)
    println("IDF Created")
    //findNeighboors(tweets.values, idfTable)
    NeighborFinders.calculate(tweetList, idfTable, count)
    println("Neighboors found")
    countReverseNeighboors(tweets)
    println("Reverse Neighboors Counted")

    try {
      val popular = tweetList.maxBy(x => x.revNearCount)//tweets.maxBy(x => x._2.revNearCount)._2
      val output: scala.collection.mutable.ListBuffer[String] = ListBuffer[String](popular.Text + ", " + popular.revNearCount.toString)
      for (ID <- popular.nearestNeighbors.reverse) {
        output += (tweets.get(ID._1).get.Text + "\t" + ID._2)
      }
      output.toList
        
    } catch {
      case e: Exception =>
        List[String]("Exception was thrown")
      // TODO: handle exception
    }
    
  }
}