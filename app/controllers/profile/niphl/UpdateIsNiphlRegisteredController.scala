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

package controllers.profile.niphl

import connectors.TraderProfileConnector
import controllers.BaseController
import controllers.actions.*
import controllers.profile.niphl.routes.*
import forms.profile.niphl.HasNiphlFormProvider
import models.Mode
import navigation.ProfileNavigator
import pages.profile.niphl.HasNiphlUpdatePage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.TraderProfileQuery
import repositories.SessionRepository
import views.html.profile.HasNiphlView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateIsNiphlRegisteredController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: ProfileNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: HasNiphlFormProvider,
  traderProfileConnector: TraderProfileConnector,
  val controllerComponents: MessagesControllerComponents,
  view: HasNiphlView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(HasNiphlUpdatePage) match {
        case None        =>
          for {
            traderProfile  <- traderProfileConnector.getTraderProfile
            updatedAnswers <-
              Future.fromTry(request.userAnswers.set(HasNiphlUpdatePage, traderProfile.niphlNumber.isDefined))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Ok(
            view(
              form.fill(traderProfile.niphlNumber.isDefined),
              UpdateIsNiphlRegisteredController.onSubmit(mode),
              mode,
              isCreateJourney = false
            )
          )
        case Some(value) =>
          Future.successful(
            Ok(
              view(
                form.fill(value),
                UpdateIsNiphlRegisteredController.onSubmit(mode),
                mode,
                isCreateJourney = false
              )
            )
          )
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  UpdateIsNiphlRegisteredController.onSubmit(mode),
                  mode,
                  isCreateJourney = false
                )
              )
            ),
          value =>
            traderProfileConnector.getTraderProfile.flatMap { traderProfile =>
              for {
                updatedAnswers                  <- Future.fromTry(request.userAnswers.set(HasNiphlUpdatePage, value))
                updatedAnswersWithTraderProfile <-
                  Future.fromTry(updatedAnswers.set(TraderProfileQuery, traderProfile))
                _                               <- sessionRepository.set(updatedAnswersWithTraderProfile)
              } yield Redirect(navigator.nextPage(HasNiphlUpdatePage, mode, updatedAnswersWithTraderProfile))
            }
        )
    }
}
