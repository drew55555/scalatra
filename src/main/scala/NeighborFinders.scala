package com.twitterCluster.app

import akka.actor._
import akka.routing.RoundRobinRouter
import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.pipe
//import scala.concurrent.impl.Future

object NeighborFinders {
  
  sealed trait TweetMessage
  case object Calculate extends TweetMessage
  case class Work(tweets: scala.collection.Iterable[Tweet], idfResult: Map[String, Double], sublist: List[Tweet])
  case class Result() extends TweetMessage
  case class Finished() extends TweetMessage
  
  class Worker extends Actor {
    
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
    
    def findNearestNeighbors(tweets: List[Tweet], idfResult: Map[String, Double], sublist: List[Tweet]) = {
      for (tweet <- sublist) {
        var dists = (for (
          tempTweet <- tweets if (tweet.ID != tempTweet.ID)
        ) yield new Tuple2(tempTweet.ID, tweetDotProd(tweet, tempTweet, idfResult))).toList
        dists = dists.sortBy(x => x._2)
        tweet.nearestNeighbors ++= dists.takeRight(10)
      }
    }
    
    def receive = {
      case Work(tweets: List[Tweet], idfResult: Map[String, Double], sublist: List[Tweet]) =>
        println("Worker: " + sublist.length)
        findNearestNeighbors(tweets, idfResult, sublist)
        sender ! Result
    }
  }
  
  class Master (tweets: List[Tweet], idfResult: Map[String, Double], tweetCount: Int, listener: ActorRef)
    extends Actor {
    import context.dispatcher
    var senderRef: ActorRef = _
    val numWorkers = 12
    var completedWorkers = 0
    val Tweets = tweets
    val idfTable = idfResult
    val TweetCount = tweetCount
    val numPerWorker = TweetCount / numWorkers
    val workRouter = context.actorOf(
        Props[Worker].withRouter(RoundRobinRouter(numWorkers)),
        name = "workRouter")
        
    def receive = {
      case Calculate => 
        senderRef = sender
        val lists = tweets.grouped(tweetCount/ numWorkers).toList
        println("Calculate Started")
        for(sublist <- lists) {
          workRouter ! Work(tweets, idfTable, sublist)
        }
      case Result =>
        completedWorkers += 1
        println("Worker Done")
        if(completedWorkers == numWorkers) {
          Future {"Finished"} pipeTo(senderRef)
          listener ! Finished
          println("Master Done")
          context.stop(self)
        }
    }
  }
  
  class Listener extends Actor {
    def receive = {
      case Finished =>
        println("Listener Done")
        context.system.shutdown()
    }
  }
  
  
  def calculate(tweets: List[Tweet], idfResult: Map[String, Double], tweetCount: Int) = {
    val system = ActorSystem("TweetSystem")
    val listener = system.actorOf(Props[Listener], name = "listener")
    val master = system.actorOf(Props(new Master(tweets, idfResult, tweetCount, listener)),
        name = "master")
        
    implicit val timeout = Timeout(10 hours)
    val future = master ? Calculate
    val result = Await.result(future, timeout.duration).asInstanceOf[String]
    
    //val future: Future[Finished] = ask(master, Calculate).mapTo[Finished]
    //val result = Await.result(future, timeout.duration).asInstanceOf[Finished]
    //val future = master ? Calculate
  }
}