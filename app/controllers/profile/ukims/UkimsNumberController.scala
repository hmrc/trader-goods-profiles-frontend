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

package controllers.profile.ukims

import connectors.TraderProfileConnector
import controllers.BaseController
import controllers.actions._
import models.Mode
import navigation.ProfileNavigator
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.profile.UkimsNumberView
import controllers.profile.ukims.routes._
import forms.profile.ukims.UkimsNumberFormProvider
import pages.profile.ukims.{UkimsNumberPage, UkimsNumberUpdatePage}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkimsNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: ProfileNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: UkimsNumberFormProvider,
  traderProfileConnector: TraderProfileConnector,
  val controllerComponents: MessagesControllerComponents,
  view: UkimsNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(UkimsNumberPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(
        view(
          preparedForm,
          UkimsNumberController.onSubmitCreate(mode),
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
              BadRequest(view(formWithErrors, UkimsNumberController.onSubmitCreate(mode)))
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(UkimsNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(UkimsNumberPage, mode, updatedAnswers))
        )
    }

  def onPageLoadUpdate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(UkimsNumberUpdatePage) match {
        case None        =>
          for {
            traderProfile  <- traderProfileConnector.getTraderProfile(request.eori)
            updatedAnswers <-
              Future.fromTry(request.userAnswers.set(UkimsNumberUpdatePage, traderProfile.ukimsNumber))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Ok(
            view(
              form.fill(traderProfile.ukimsNumber),
              UkimsNumberController.onSubmitUpdate(mode: Mode)
            )
          )
        case Some(value) =>
          Future.successful(
            Ok(view(form.fill(value), UkimsNumberController.onSubmitUpdate(mode: Mode)))
          )
      }
    }

  def onSubmitUpdate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(
                BadRequest(
                  view(formWithErrors, UkimsNumberController.onSubmitUpdate(mode: Mode))
                )
              ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(UkimsNumberUpdatePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(UkimsNumberUpdatePage, mode, updatedAnswers))
        )
    }
}
