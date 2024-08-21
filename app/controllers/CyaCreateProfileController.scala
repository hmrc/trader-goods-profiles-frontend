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
import connectors.TraderProfileConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileCheckAction}
import logging.Logging
import models.helper.CreateProfileJourney
import models.requests.DataRequest
import models.{NormalMode, TraderProfile, ValidationError}
import navigation.Navigator
import pages.CyaCreateProfilePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AuditService, DataCleansingService}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.CyaCreateProfileView

import scala.concurrent.{ExecutionContext, Future}

class CyaCreateProfileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  dataCleansingService: DataCleansingService,
  val controllerComponents: MessagesControllerComponents,
  view: CyaCreateProfileView,
  traderProfileConnector: TraderProfileConnector,
  auditService: AuditService,
  navigator: Navigator
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen checkProfile andThen getData andThen requireData) {
    implicit request =>
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
        case Left(errors) => logErrorsAndContinue(errors, request)
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    TraderProfile.build(request.userAnswers, request.eori) match {
      case Right(model) =>
        auditService.auditProfileSetUp(model, request.affinityGroup)
        traderProfileConnector.submitTraderProfile(model, request.eori).map { _ =>
          dataCleansingService.deleteMongoData(request.userAnswers.id, CreateProfileJourney)
          Redirect(navigator.nextPage(CyaCreateProfilePage, NormalMode, request.userAnswers))
        }

      case Left(errors) => Future.successful(logErrorsAndContinue(errors, request))
    }

  }

  def logErrorsAndContinue(errors: data.NonEmptyChain[ValidationError], request: DataRequest[AnyContent]): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    val continueUrl = RedirectUrl(routes.ProfileSetupController.onPageLoad().url)

    logger.error(s"Unable to create Trader profile.  Missing pages: $errorMessages")
    dataCleansingService.deleteMongoData(request.userAnswers.id, CreateProfileJourney)
    navigator.journeyRecovery(Some(continueUrl))
  }
}
