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

import connectors.TraderProfileConnector
import controllers.actions._
import forms.RemoveNiphlFormProvider
import models.NormalMode
import navigation.Navigator
import pages.{NiphlNumberUpdatePage, RemoveNiphlPage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.RemoveNiphlView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class RemoveNiphlController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: RemoveNiphlFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveNiphlView,
  traderProfileConnector: TraderProfileConnector
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(RemoveNiphlPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm))
  }

  def onSubmit: Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          value =>
            if (value) {
              request.userAnswers.set(RemoveNiphlPage, value) match {
                case Success(answers) =>
                  sessionRepository.set(answers).map { _ =>
                    Redirect(navigator.nextPage(RemoveNiphlPage, NormalMode, answers))
                  }
                // case _ => Future.successful(InternalServerError) // TODO: Do we want to log an error here if it fails to set the value?
              }
            } else {

              for {
                traderProfile     <- traderProfileConnector.getTraderProfile(request.eori)
                niphlNumberOpt     = request.userAnswers.get(NiphlNumberUpdatePage).orElse(traderProfile.niphlNumber)
                niphlNumber       <- Future.fromTry(Try(niphlNumberOpt.get))
                uaWithRemoveValue <- Future.fromTry(request.userAnswers.set(RemoveNiphlPage, value))
                uaWithNiphlNumber <-
                  Future.fromTry(uaWithRemoveValue.set(NiphlNumberUpdatePage, niphlNumber))
                _                 <- sessionRepository.set(uaWithNiphlNumber)
              } yield Redirect(navigator.nextPage(RemoveNiphlPage, NormalMode, uaWithNiphlNumber))

            }
        )
  }

}
