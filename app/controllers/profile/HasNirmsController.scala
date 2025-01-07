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

package controllers.profile

import connectors.TraderProfileConnector
import controllers.BaseController
import controllers.actions._
import forms.profile.HasNirmsFormProvider
import models.Mode
import navigation.ProfileNavigator
import pages.profile.{HasNirmsPage, HasNirmsUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.TraderProfileQuery
import repositories.SessionRepository
import views.html.profile.HasNirmsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasNirmsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: ProfileNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  profileAuth: ProfileAuthenticateAction,
  traderProfileConnector: TraderProfileConnector,
  formProvider: HasNirmsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: HasNirmsView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(HasNirmsPage, form)
      Ok(
        view(
          preparedForm,
          controllers.profile.routes.HasNirmsController.onSubmitCreate(mode),
          mode,
          isCreateJourney = true
        )
      )
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  controllers.profile.routes.HasNirmsController.onSubmitCreate(mode),
                  mode,
                  isCreateJourney = true
                )
              )
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasNirmsPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasNirmsPage, mode, updatedAnswers))
        )
    }

  def onPageLoadUpdate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(HasNirmsUpdatePage) match {
        case None        =>
          for {
            traderProfile  <- traderProfileConnector.getTraderProfile
            updatedAnswers <-
              Future.fromTry(request.userAnswers.set(HasNirmsUpdatePage, traderProfile.nirmsNumber.isDefined))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Ok(
            view(
              form.fill(traderProfile.nirmsNumber.isDefined),
              controllers.profile.routes.HasNirmsController.onSubmitUpdate(mode),
              mode,
              isCreateJourney = false
            )
          )
        case Some(value) =>
          Future.successful(
            Ok(
              view(
                form.fill(value),
                controllers.profile.routes.HasNirmsController.onSubmitUpdate(mode),
                mode,
                isCreateJourney = false
              )
            )
          )
      }
    }

  def onSubmitUpdate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  controllers.profile.routes.HasNirmsController.onSubmitUpdate(mode),
                  mode,
                  isCreateJourney = false
                )
              )
            ),
          value =>
            traderProfileConnector.getTraderProfile.flatMap { traderProfile =>
              for {
                updatedAnswers                  <- Future.fromTry(request.userAnswers.set(HasNirmsUpdatePage, value))
                updatedAnswersWithTraderProfile <-
                  Future.fromTry(updatedAnswers.set(TraderProfileQuery, traderProfile))
                _                               <- sessionRepository.set(updatedAnswersWithTraderProfile)
              } yield Redirect(navigator.nextPage(HasNirmsUpdatePage, mode, updatedAnswersWithTraderProfile))

            }
        )
    }
}
