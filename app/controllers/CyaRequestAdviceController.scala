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
import connectors.AccreditationConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import logging.Logging
import models.helper.RequestAdviceJourney
import models.requests.DataRequest
import models.{AdviceRequest, NormalMode, ValidationError}
import navigation.Navigator
import pages.CyaRequestAdvicePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.AuditService
import services.DataCleansingService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{EmailSummary, NameSummary}
import viewmodels.govuk.summarylist._
import views.html.CyaRequestAdviceView

import scala.concurrent.{ExecutionContext, Future}

class CyaRequestAdviceController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  dataCleansingService: DataCleansingService,
  auditService: AuditService,
  val controllerComponents: MessagesControllerComponents,
  view: CyaRequestAdviceView,
  accreditationConnector: AccreditationConnector,
  navigator: Navigator
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      AdviceRequest.build(request.userAnswers, request.eori, recordId) match {
        case Right(_)     =>
          val list = SummaryListViewModel(
            rows = Seq(
              NameSummary.row(request.userAnswers, recordId),
              EmailSummary.row(request.userAnswers, recordId)
            ).flatten
          )
          Ok(view(list, recordId))
        case Left(errors) => logErrorsAndContinue(errors, recordId, request)
      }
  }

  def logErrorsAndContinue(
    errors: data.NonEmptyChain[ValidationError],
    recordId: String,
    request: DataRequest[AnyContent]
  ): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    val continueUrl = RedirectUrl(routes.AdviceStartController.onPageLoad(recordId).url)

    logger.error(s"Unable to create Request Advice.  Missing pages: $errorMessages")
    dataCleansingService.deleteMongoData(request.userAnswers.id, RequestAdviceJourney)
    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  def onSubmit(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      AdviceRequest.build(request.userAnswers, request.eori, recordId) match {
        case Right(model) =>
          auditService.auditRequestAdvice(request.affinityGroup, model)
          accreditationConnector
            .submitRequestAccreditation(model)
            .map { _ =>
              dataCleansingService.deleteMongoData(request.userAnswers.id, RequestAdviceJourney)
              Redirect(navigator.nextPage(CyaRequestAdvicePage(recordId), NormalMode, request.userAnswers))
            }
        case Left(errors) => Future.successful(logErrorsAndContinue(errors, recordId, request))
      }
  }
}
