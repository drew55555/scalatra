import com.twitterCluster.app._
import org.scalatra._
import javax.servlet.ServletContext
import com.mongodb.casbah.Imports._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    val mongo =  MongoClient()
    mongo("twitterDB").authenticate("Drew", "Hello")
    val coll = mongo("twitterDB")("TweetData")
    //context.mount(new clustererServlet, "/*")
    context.mount(new clustererServlet(coll), "/*")
  }
}
