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

import com.google.inject.Inject
import connectors.AccreditationConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.helper.RequestAdviceJourney
import models.AdviceRequest
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.{AuditService, DataCleansingService}
import viewmodels.checkAnswers.{EmailSummary, NameSummary}
import viewmodels.govuk.summarylist._
import views.html.CyaRequestAdviceView

import scala.concurrent.{ExecutionContext, Future}

class CyaRequestAdviceController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  auditService: AuditService,
  val controllerComponents: MessagesControllerComponents,
  view: CyaRequestAdviceView,
  dataCleansingService: DataCleansingService,
  accreditationConnector: AccreditationConnector
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String                = "Unable to create Request Advice."
  private def continueUrl(recordId: String): Call = routes.AdviceStartController.onPageLoad(recordId)

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
        case Left(errors) => logErrorsAndContinue(errorMessage, continueUrl(recordId), errors, RequestAdviceJourney)
      }
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
              Redirect(routes.AdviceSuccessController.onPageLoad(recordId).url)
            }
        case Left(errors) =>
          dataCleansingService.deleteMongoData(request.userAnswers.id, RequestAdviceJourney)
          Future.successful(logErrorsAndContinue(errorMessage, continueUrl(recordId), errors))
      }
  }
}
