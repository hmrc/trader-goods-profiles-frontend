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
import forms.profile.HasNiphlFormProvider
import models.Mode
import navigation.ProfileNavigator
import pages.profile.{HasNiphlPage, HasNiphlUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.TraderProfileQuery
import repositories.SessionRepository
import views.html.profile.HasNiphlView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasNiphlController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: ProfileNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  formProvider: HasNiphlFormProvider,
  traderProfileConnector: TraderProfileConnector,
  val controllerComponents: MessagesControllerComponents,
  view: HasNiphlView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(HasNiphlPage, form)
      Ok(view(preparedForm, controllers.profile.routes.HasNiphlController.onSubmitCreate(mode)))
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, controllers.profile.routes.HasNiphlController.onSubmitCreate(mode)))
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasNiphlPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasNiphlPage, mode, updatedAnswers))
        )
  }

  def onPageLoadUpdate(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(HasNiphlUpdatePage) match {
        case None        =>
          for {
            traderProfile  <- traderProfileConnector.getTraderProfile(request.eori)
            updatedAnswers <-
              Future.fromTry(request.userAnswers.set(HasNiphlUpdatePage, traderProfile.niphlNumber.isDefined))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Ok(
            view(
              form.fill(traderProfile.niphlNumber.isDefined),
              controllers.profile.routes.HasNiphlController.onSubmitUpdate(mode)
            )
          )
        case Some(value) =>
          Future.successful(
            Ok(view(form.fill(value), controllers.profile.routes.HasNiphlController.onSubmitUpdate(mode)))
          )
      }
    }

  def onSubmitUpdate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, controllers.profile.routes.HasNiphlController.onSubmitUpdate(mode)))
            ),
          value =>
            traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
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