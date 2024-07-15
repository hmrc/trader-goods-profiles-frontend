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
import forms.HasNirmsFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.{HasNirmsPage, HasNirmsUpdatePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HasNirmsView

import scala.concurrent.{ExecutionContext, Future}

class HasNirmsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  traderProfileConnector: TraderProfileConnector,
  formProvider: HasNirmsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: HasNirmsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(HasNirmsPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, routes.HasNirmsController.onSubmitCreate(mode)))
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, routes.HasNirmsController.onSubmitCreate(mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasNirmsPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasNirmsPage, mode, updatedAnswers))
        )
    }

  def onPageLoadUpdate: Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(HasNirmsUpdatePage) match {
        case None        =>
          for {
            traderProfile  <- traderProfileConnector.getTraderProfile(request.eori)
            updatedAnswers <-
              Future.fromTry(request.userAnswers.set(HasNirmsUpdatePage, traderProfile.nirmsNumber.isDefined))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Ok(view(form.fill(traderProfile.nirmsNumber.isDefined), routes.HasNirmsController.onSubmitUpdate))
        case Some(value) => Future.successful(Ok(view(form.fill(value), routes.HasNirmsController.onSubmitUpdate)))
      }
    }

  def onSubmitUpdate: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, routes.HasNirmsController.onSubmitUpdate))),
        value =>
          traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
            if (traderProfile.nirmsNumber.isDefined == value) {
              Future.successful(Redirect(routes.ProfileController.onPageLoad()))
            } else {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(HasNirmsUpdatePage, value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(HasNirmsUpdatePage, NormalMode, updatedAnswers))
            }
          }
      )
  }
}
