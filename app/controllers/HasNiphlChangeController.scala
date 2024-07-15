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

import cats.data
import connectors.TraderProfileConnector
import controllers.actions._
import forms.HasNiphlChangeFormProvider

import javax.inject.Inject
import models.{NormalMode, TraderProfile, UserAnswers, ValidationError}
import navigation.Navigator
import pages.{HasNiphlChangePage, HasNiphlUpdatePage, NiphlNumberUpdatePage}
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HasNiphlChangeView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class HasNiphlChangeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: HasNiphlChangeFormProvider,
  traderProfileConnector: TraderProfileConnector,
  val controllerComponents: MessagesControllerComponents,
  view: HasNiphlChangeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form        = formProvider()
  private val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Ok(view(form))
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        value =>
          request.userAnswers.set(HasNiphlChangePage, value) match {
            case Success(answers) =>
              sessionRepository.set(answers).flatMap { _ =>
                if (value) {
                  traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
                    TraderProfile.buildNiphl(answers, request.eori, traderProfile) match {
                      case Right(model) =>
                        for {
                          _              <- traderProfileConnector.submitTraderProfile(model, request.eori)
                          updatedAnswers <- cleanseNiphlData(answers)
                        } yield Redirect(navigator.nextPage(HasNiphlChangePage, NormalMode, updatedAnswers))
                      case Left(errors) => logErrorsAndContinue(errors, answers)
                    }
                  }
                } else {
                  Future.successful(Redirect(navigator.nextPage(HasNiphlChangePage, NormalMode, answers)))
                }
              }
          }
      )
  }

  def cleanseNiphlData(answers: UserAnswers): Future[UserAnswers] =
    for {
      updatedAnswersRemovedHasNiphl       <-
        Future.fromTry(answers.remove(HasNiphlUpdatePage))
      updatedAnswersRemovedHasNiphlChange <-
        Future.fromTry(updatedAnswersRemovedHasNiphl.remove(HasNiphlChangePage))
      updatedAnswers                      <-
        Future.fromTry(updatedAnswersRemovedHasNiphlChange.remove(NiphlNumberUpdatePage))
      _                                   <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers

  def logErrorsAndContinue(errors: data.NonEmptyChain[ValidationError], answers: UserAnswers): Future[Result] = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")
    logger.warn(s"Unable to update Trader profile.  Missing pages: $errorMessages")

    cleanseNiphlData(answers).map(_ => Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl))))
  }
}
