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
import forms.HasNiphlFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.{HasNiphlPage, HasNiphlUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.HasNiphlView

import scala.concurrent.{ExecutionContext, Future}

class HasNiphlController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
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
      val preparedForm = request.userAnswers.get(HasNiphlPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, routes.HasNiphlController.onSubmitCreate(mode)))
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, routes.HasNiphlController.onSubmitCreate(mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasNiphlPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasNiphlPage, mode, updatedAnswers))
        )
  }

  def onPageLoadUpdate: Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(HasNiphlUpdatePage) match {
        case None        =>
          for {
            traderProfile  <- traderProfileConnector.getTraderProfile(request.eori)
            updatedAnswers <-
              Future.fromTry(request.userAnswers.set(HasNiphlUpdatePage, traderProfile.niphlNumber.isDefined))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Ok(view(form.fill(traderProfile.niphlNumber.isDefined), routes.HasNiphlController.onSubmitUpdate))
        case Some(value) => Future.successful(Ok(view(form.fill(value), routes.HasNiphlController.onSubmitUpdate)))
      }
    }

  def onSubmitUpdate: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, routes.HasNiphlController.onSubmitUpdate))),
        value =>
          traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
            if (traderProfile.niphlNumber.isDefined == value) {
              Future.successful(Redirect(routes.ProfileController.onPageLoad()))
            } else {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(HasNiphlUpdatePage, value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(HasNiphlUpdatePage, NormalMode, updatedAnswers))
            }
          }
      )
  }
}
