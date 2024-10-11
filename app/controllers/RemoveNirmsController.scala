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
import forms.RemoveNirmsFormProvider
import models.NormalMode
import navigation.Navigator
import pages.{NirmsNumberUpdatePage, RemoveNirmsPage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.RemoveNirmsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class RemoveNirmsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RemoveNirmsFormProvider,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveNirmsView,
  traderProfileConnector: TraderProfileConnector
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(RemoveNirmsPage) match {
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
              request.userAnswers.set(RemoveNirmsPage, value) match {
                case Success(answers) =>
                  sessionRepository.set(answers).map { _ =>
                    Redirect(navigator.nextPage(RemoveNirmsPage, NormalMode, answers))
                  }
                case _                => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
              }
            } else {

              for {
                traderProfile     <- traderProfileConnector.getTraderProfile(request.eori)
                nirmsNumberOpt     = request.userAnswers.get(NirmsNumberUpdatePage).orElse(traderProfile.nirmsNumber)
                nirmsNumber       <- Future.fromTry(Try(nirmsNumberOpt.get))
                uaWithRemoveValue <- Future.fromTry(request.userAnswers.set(RemoveNirmsPage, value))
                uaWithNirmsNumber <-
                  Future.fromTry(uaWithRemoveValue.set(NirmsNumberUpdatePage, nirmsNumber))
                _                 <- sessionRepository.set(uaWithNirmsNumber)
              } yield Redirect(navigator.nextPage(RemoveNirmsPage, NormalMode, uaWithNirmsNumber))

            }
        )
  }

}
