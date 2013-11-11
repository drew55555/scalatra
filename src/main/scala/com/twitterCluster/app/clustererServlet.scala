package com.twitterCluster.app

import org.scalatra._
import scalate.ScalateSupport
import com.mongodb._
import com.mongodb.casbah.Imports._
import scala.xml._
import org.joda.convert.ToString
import org.joda.convert.ToString
import org.joda.convert.ToString
import org.joda.convert.ToString

class clustererServlet(coll: MongoCollection) extends TwitterclustererwebappStack with ScalateSupport {

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
        <form method="POST" action="/maps">
          <div id="map-canvas"></div>
          <div id="coord-info"></div>
          <input type="text" id="northeast" name="northeast"/>
          <input type="text" id="southwest" name="southwest"/>
          <input type="submit" method="Post"/><br/>
          Similarity Type:
          <select id="similarType" name="similarType">
            <option value="DotProduct">Dot Product</option>
            <option value="Euclidean">Euclidean Distance</option>
          </select><br/>
          K value of nearest neighboors:
          <input type="text" id="k" name="k" value="10"/>
        </form>
      </body>
    </html>
  }

  get("/maps/:NELat/:NELon/:SWLat/:SWLon/:Dist/:K") {
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
        <form method="POST" action="/maps">
          <div id="map-canvas"></div>
          <div id="coord-info"></div>
          <input type="text" id="northeast" name="northeast"/>
          <input type="text" id="southwest" name="southwest"/>
          <input type="submit" method="Post"/><br/>
          Similarity:
          <select id="similarType" name="similarType">
            <option value="DotProduct">Dot Product</option>
            <option value="Euclidean">Euclidean Distance</option>
          </select><br/>
          K:
          <input type="text" id="k" name="k" value="10"/>
          <div>
            <ul>
              {
                val nelat = params.getOrElse("NELat", "0").toDouble
                val nelon = params.getOrElse("NELon", "0").toDouble
                val swlat = params.getOrElse("SWLat", "0").toDouble
                val swlon = params.getOrElse("SWLon", "0").toDouble
                val kValue = params.getOrElse("K", "10").toInt
                val similarity = params.getOrElse("Dist", "DotProduct")
                val nwlat = nelat
                val nwlon = swlon
                val selat = swlat
                val selon = nelon
                val geo = MongoDBObject("$geometry" ->
                  MongoDBObject("type" -> "Polygon",
                    "coordinates" -> List(((GeoCoords(nelon, nelat),
                      GeoCoords(selon, selat),
                      GeoCoords(swlon, swlat),
                      GeoCoords(nwlon, nwlat),
                      GeoCoords(nelon, nelat))))))

                val query = "Location" $geoWithin (geo)
                val result = Common.getTopicsFromQuery(query, kValue, similarity)
                for (x <- result) yield <li>
                                          Tweet:{ x }
                                        </li>
              }
            </ul>
          </div>
        </form>
      </body>
    </html>
  }

  post("/maps") {
    val kValue = params("k")
    val similarity = params("similarType")
    val ne = params("northeast").split(',')
    val sw = params("southwest").split(',')
    val nelat = ne(0)
    val nelon = ne(1)
    val swlat = sw(0)
    val swlon = sw(1)
    redirect("/maps/" + nelat + "/" + nelon + "/" + swlat + "/" + swlon + "/" + similarity + "/" + kValue)
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

  get("/msgs/:author/") {
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
