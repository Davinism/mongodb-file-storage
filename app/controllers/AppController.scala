package controllers

import javax.inject._

import akka.stream.Materializer
import org.joda.time.DateTime
import play.api.mvc._
import play.api.Logger
import java.io.{File, FileInputStream}

import scala.concurrent.{Await, Future, duration}
import duration.Duration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Request}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, JsString, Json}
import reactivemongo.api.gridfs.{DefaultFileToSave, GridFS, ReadFile}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONValue
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

  def getFile(id: String) = Action.async { request =>
    gridFS.flatMap { fs =>
      val file = fs.find[JsObject, JSONReadFile](Json.obj("_id" -> id))

      request.getQueryString("inline") match {
        case Some("true") =>
          serve[JsString, JSONReadFile](fs)(file, CONTENT_DISPOSITION_INLINE)

        case _ => serve[JsString, JSONReadFile](fs)(file)
      }
    }
  }

  def showAllFiles = Action.async { request =>
    gridFS.flatMap { fs =>
      fs.find[JsObject, JSONReadFile](Json.obj()).collect[List]().map { files =>
        @inline def filesWithId = files.map { file => file.id -> file }

        Ok(views.html.show("Files stored: ", Some(filesWithId)))
      }
    }
  }

  def deleteFile(id: String) = Action.async { request =>
    gridFS.flatMap(_.remove(Json toJson id).map(_ => Ok)).recover {
      case _ => InternalServerError
    }
  }

}
