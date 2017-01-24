package controllers

import javax.inject._
import play.api._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class AppController @Inject() extends Controller {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    val oneTwoThree = List(1, 2, 3)
    val fourFiveSix = List(4, 5, 6)
    println(oneTwoThree.:::(fourFiveSix))
    Ok("Your new application is ready.")
  }

}
