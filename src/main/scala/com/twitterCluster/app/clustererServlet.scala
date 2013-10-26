package com.twitterCluster.app

import org.scalatra._
import scalate.ScalateSupport
import com.mongodb._
import com.mongodb.casbah.Imports._
import scala.xml._

class clustererServlet extends TwitterclustererwebappStack with ScalateSupport {

  val mongo = MongoConnection()
  val coll = mongo("twitterDB")("TweetData")

  get("/") {
    <html>
      <body>
        <h1>Hello, world!</h1>
        <a href="hello/handsome">hello to me!!!</a>
        <a href="msgs">go here</a>
        <a href="maps">googleMaps</a>
      </body>
    </html>
  }
  get("/maps") {
    <html>
      <meta name="viewport" content="initial-scale=1.0, user-scalable=no"/>
      <meta charset="utf-8"/>
      <style> { "html, body, #map-canvas {height: 90%;width:  90%;margin: 0px;padding: 0px}" }</style>
      <head>
        <title> A Map </title>
        <script src={ "https://maps.googleapis.com/maps/api/js?v=3.exp&sensor=false" }></script>
        <script src="/assets/map.js"></script>
      </head>
      <body>
        <div id="map-canvas"></div>
        <div id="coord-info"></div>
      </body>
    </html>
  }

  get("/hello/:name") {
    <html><head><title>Hello!</title></head><body><h1>Hello { params("name") }!</h1></body></html>
  }

  get("/msgs") {
    <html>
      <head></head>
      <body>
        <form method="POST" action="/msgs">
          Author:<input type="text" name="author" style="margin-left:120px"/>
          <input type="submit"/>
          <br/>
        </form>
      </body>
    </html>
  }

  post("/msgs") {
    redirect("/msgs/" + params("author"))
  }

  get("/msgs/:author") {
    <body>
      <form method="POST" action="/msgs">
        Author:<input type="text" name="author"/>
        <input type="submit"/>
        <br/>
        <ul>
          {
            val query = MongoDBObject("CountryCode" -> params.getOrElse("author", None))
            for (l <- coll.find(query)) yield <li>
                                                Text:{ l.getOrElse("Text", "???") }
                                                Country:{ l.getOrElse("CountryCode", "???") }
                                              </li>
          }
        </ul>
      </form>
    </body>
  }
}
