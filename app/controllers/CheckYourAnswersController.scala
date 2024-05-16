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
import com.google.inject.Inject
import connectors.RouterConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import logging.Logging
import models.{TraderProfile, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{HasNiphlSummary, HasNirmsSummary, NiphlNumberSummary, NirmsNumberSummary, UkimsNumberSummary}
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  routerConnector: RouterConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    TraderProfile.build(request.userAnswers, request.eori) match {
      case Right(_)     =>
        val list = SummaryListViewModel(
          rows = Seq(
            UkimsNumberSummary.row(request.userAnswers),
            HasNirmsSummary.row(request.userAnswers),
            NirmsNumberSummary.row(request.userAnswers),
            HasNiphlSummary.row(request.userAnswers),
            NiphlNumberSummary.row(request.userAnswers)
          ).flatten
        )
        Ok(view(list))
      case Left(errors) => logErrorsAndContinue(errors)
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    TraderProfile.build(request.userAnswers, request.eori) match {
      case Right(model) =>
        routerConnector.submitTraderProfile(model, request.eori).map { _ =>
          Redirect(routes.HomePageController.onPageLoad())
        }
      case Left(errors) => Future.successful(logErrorsAndContinue(errors))
    }
  }

  def logErrorsAndContinue(errors: data.NonEmptyChain[ValidationError]): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    val continueUrl = RedirectUrl(routes.ProfileSetupController.onPageLoad().url)

    logger.warn(s"Unable to create Trader profile.  Missing pages: $errorMessages")
    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }
}
