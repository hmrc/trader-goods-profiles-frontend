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

package controllers.newUkims

import connectors.TraderProfileConnector
import controllers.BaseController
import controllers.actions._
import forms.profile.UkimsNumberFormProvider
import models.Mode
import navigation.NewUkimsNavigator
import pages.newUkims.NewUkimsNumberPage
import play.api.data.{Form, FormError}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.newUkims.NewUkimsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NewUkimsNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: NewUkimsNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: UkimsNumberFormProvider,
  traderProfileConnector: TraderProfileConnector,
  val controllerComponents: MessagesControllerComponents,
  view: NewUkimsNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(NewUkimsNumberPage) match {
        case None        =>
          traderProfileConnector.getTraderProfile(request.eori).map { profile =>
            Ok(
              view(form.fill(profile.ukimsNumber), controllers.newUkims.routes.NewUkimsNumberController.onSubmit(mode))
            )
          }
        case Some(value) =>
          Future.successful(
            Ok(view(form.fill(value), controllers.newUkims.routes.NewUkimsNumberController.onSubmit(mode)))
          )
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(
                BadRequest(view(formWithErrors, controllers.newUkims.routes.NewUkimsNumberController.onSubmit(mode)))
              ),
          value =>
            for {
              profile        <- traderProfileConnector.getTraderProfile(request.eori)
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NewUkimsNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield
              if (value == profile.ukimsNumber) {
                val formWithApiErrors =
                  createFormWithErrors(form, value, "ukimsNumberChangeController.duplicateUkimsNumber")
                BadRequest(view(formWithApiErrors, controllers.newUkims.routes.NewUkimsNumberController.onSubmit(mode)))
              } else {
                Redirect(navigator.nextPage(NewUkimsNumberPage, mode, updatedAnswers))
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
