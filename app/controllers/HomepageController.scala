/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import connectors.RouterConnector
import controllers.actions.AuthoriseAction
import models.router.responses.SetUpProfileResponse
import scala.util.{Failure, Success}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HomepageView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.impl.Promise

class HomepageController @Inject() (
                                     val controllerComponents: MessagesControllerComponents,
                                     authorise: AuthoriseAction,
                                     view: HomepageView,
                                     routerConnector: RouterConnector
                                   )(implicit ec: ExecutionContext) extends FrontendBaseController
  with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise { implicit request =>

    val testConnector = routerConnector.setUpProfile(request.eori)
    println("CCCCCCC" + testConnector)

    testConnector.onComplete {
      case Success(yay) => println(s"yay CCC $yay")
      case Failure(t) => println("An error has occurred: " + t.getMessage)
    }

    Ok(view())
  }

}
