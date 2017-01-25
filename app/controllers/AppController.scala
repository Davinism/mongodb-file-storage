package controllers

import javax.inject._

import akka.stream.Materializer
import org.joda.time.DateTime
import play.api.mvc._
import play.api.Logger

import scala.concurrent.{Await, Future, duration}
import duration.Duration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Request}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, JsString, Json}
import reactivemongo.api.gridfs.{GridFS, ReadFile}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.play.json._
import reactivemongo.play.json.collection._

@Singleton
class AppController @Inject() (
  val messagesApi: MessagesApi,
  val reactiveMongoApi: ReactiveMongoApi,
  implicit val materializer: Materializer)
    extends Controller with MongoController with ReactiveMongoComponents {

  import MongoController.readFileReads

  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]

  private val gridFS = for {
    fs <- reactiveMongoApi.database.map(db =>
      GridFS[JSONSerializationPack.type](db))
    _<- fs.ensureIndex().map { index =>
      Logger.info(s"Checked index, result is $index")
    }
  } yield fs

  def index = Action {
    Ok(views.html.index("Your new application is ready..."))
  }

  def save = {
    def fs = Await.result(gridFS, Duration("5s"))
    Action.async(gridFSBodyParser(fs)) { request =>
      val futureFile = request.body.files.head.ref

      futureFile.onFailure {
        case err => err.printStackTrace()
      }

      val futureUpdate = for {
        file <- futureFile
      } yield Redirect(routes.AppController.index())

      futureUpdate.recover {
        case e => InternalServerError(e.getMessage())
      }
    }
  }

}
