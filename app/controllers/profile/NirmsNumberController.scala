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
import forms.profile.NirmsNumberFormProvider
import models.Mode
import navigation.ProfileNavigator
import pages.profile.{HasNirmsUpdatePage, NirmsNumberPage, NirmsNumberUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.profile.NirmsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NirmsNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: ProfileNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: NirmsNumberFormProvider,
  traderProfileConnector: TraderProfileConnector,
  checkProfile: ProfileCheckAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: NirmsNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(NirmsNumberPage, form)
      Ok(view(preparedForm, controllers.profile.routes.NirmsNumberController.onSubmitCreate(mode)))
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, controllers.profile.routes.NirmsNumberController.onSubmitCreate(mode)))
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NirmsNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(NirmsNumberPage, mode, updatedAnswers))
        )
    }

  def onPageLoadUpdate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      traderProfileConnector.getTraderProfile.flatMap { traderProfile =>
        val previousAnswerOpt = request.userAnswers.get(NirmsNumberUpdatePage)
        val nirmsNumberOpt    = previousAnswerOpt.orElse(traderProfile.nirmsNumber)
        val preparedForm      = nirmsNumberOpt match {
          case Some(nirmsNumber) => form.fill(nirmsNumber)
          case None              => form
        }

        val futureOkResult =
          Future
            .successful(Ok(view(preparedForm, controllers.profile.routes.NirmsNumberController.onSubmitUpdate(mode))))

        request.userAnswers.getPageValue(HasNirmsUpdatePage) match {
          case Right(true)                                    => futureOkResult
          case Left(_) if traderProfile.nirmsNumber.isDefined => futureOkResult
          case Right(false)                                   =>
            Future.successful(
              logErrorsAndContinue(
                "Expected HasNirmsUpdate answer to be true",
                controllers.profile.routes.ProfileController.onPageLoad()
              )
            )
          case Left(errors)                                   =>
            Future.successful(
              logErrorsAndContinue(
                "Expected HasNirmsUpdate to be answered",
                controllers.profile.routes.ProfileController.onPageLoad(),
                errors
              )
            )
        }
      }
    }

  def onSubmitUpdate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, controllers.profile.routes.NirmsNumberController.onSubmitUpdate(mode)))
            ),
          value =>
            for {
              updatedAnswers <-
                Future.fromTry(
                  request.userAnswers.set(NirmsNumberUpdatePage, value).flatMap(_.set(HasNirmsUpdatePage, true))
                )
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(NirmsNumberUpdatePage, mode, updatedAnswers))
        )
    }

}
