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
import forms.UkimsNumberFormProvider
import models.Mode
import navigation.Navigator
import pages.{UkimsNumberPage, UkimsNumberUpdatePage}
import play.api.data.{Form, FormError}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.UkimsNumberView
import views.html.NewUkimsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkimsNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  formProvider: UkimsNumberFormProvider,
  traderProfileConnector: TraderProfileConnector,
  val controllerComponents: MessagesControllerComponents,
  view: UkimsNumberView,
  newView: NewUkimsNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(UkimsNumberPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, routes.UkimsNumberController.onSubmitCreate(mode), isCreateJourney = true))
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, routes.UkimsNumberController.onSubmitCreate(mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(UkimsNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(UkimsNumberPage, mode, updatedAnswers))
        )
  }

  def onPageLoadUpdate(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(UkimsNumberUpdatePage) match {
        case None        =>
          for {
            traderProfile  <- traderProfileConnector.getTraderProfile(request.eori)
            updatedAnswers <-
              Future.fromTry(request.userAnswers.set(UkimsNumberUpdatePage, traderProfile.ukimsNumber))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Ok(
            view(form.fill(traderProfile.ukimsNumber), routes.UkimsNumberController.onSubmitUpdate(mode: Mode))
          )
        case Some(value) =>
          Future.successful(Ok(view(form.fill(value), routes.UkimsNumberController.onSubmitUpdate(mode: Mode))))
      }
    }

  def onSubmitUpdate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(BadRequest(view(formWithErrors, routes.UkimsNumberController.onSubmitUpdate(mode: Mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(UkimsNumberUpdatePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(UkimsNumberUpdatePage, mode, updatedAnswers))
        )
  }

  def onPageLoadCreateNew(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(UkimsNumberPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(newView(preparedForm, routes.UkimsNumberController.onSubmitCreateNew(mode), isCreateJourney = true))
    }

  def onSubmitCreateNew(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(BadRequest(newView(formWithErrors, routes.UkimsNumberController.onSubmitCreateNew(mode)))),
          value =>
            for {
              profile        <- traderProfileConnector.getTraderProfile(request.eori)
              updatedAnswers <- Future.fromTry(request.userAnswers.set(UkimsNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield
              if (value == profile.ukimsNumber) {
                val formWithApiErrors =
                  createFormWithErrors(form, value, "ukimsNumberChangeController.duplicateUkimsNumber")
                BadRequest(view(formWithApiErrors, routes.UkimsNumberController.onSubmitCreateNew(mode)))
              } else {
                Redirect(navigator.nextPage(UkimsNumberPage, mode, updatedAnswers))
              }
        )
  }

  private def createFormWithErrors[T](form: Form[T], value: T, errorMessageKey: String, field: String = "value")(
    implicit messages: Messages
  ): Form[T] =
    form
      .fill(value)
      .copy(errors = Seq(FormError(field, messages(errorMessageKey))))
}
