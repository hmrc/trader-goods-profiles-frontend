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

package controllers.advice

import cats.data.NonEmptyChain
import com.google.inject.Inject
import connectors.AccreditationConnector
import controllers.BaseController
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import models.helper.RequestAdviceJourney
import models.{AdviceRequest, NormalMode, ValidationError}
import navigation.AdviceNavigator
import pages.advice.CyaRequestAdvicePage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditService, DataCleansingService}
import viewmodels.checkAnswers.{EmailSummary, NameSummary}
import viewmodels.govuk.summarylist.*
import views.html.advice.CyaRequestAdviceView

import scala.concurrent.{ExecutionContext, Future}

class CyaRequestAdviceController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  auditService: AuditService,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaRequestAdviceView,
  dataCleansingService: DataCleansingService,
  accreditationConnector: AccreditationConnector,
  navigator: AdviceNavigator
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String = "Unable to create Request Advice."

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      AdviceRequest.build(request.userAnswers, request.eori, recordId) match {
        case Right(_)     =>
          val list = SummaryListViewModel(
            rows = Seq(
              NameSummary.row(request.userAnswers, recordId),
              EmailSummary.row(request.userAnswers, recordId)
            ).flatten
          )
          Ok(view(list, recordId))
        case Left(errors) =>
          dataCleansingService.deleteMongoData(request.userAnswers.id, RequestAdviceJourney)
          handleError(recordId, errors)
      }
    }

  private def handleError(recordId: String, errors: NonEmptyChain[ValidationError]) =
    logErrorsAndContinue(
      errorMessage,
      controllers.advice.routes.AdviceStartController.onPageLoad(recordId),
      errors
    )

  def onSubmit(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      AdviceRequest.build(request.userAnswers, request.eori, recordId) match {
        case Right(model) =>
          auditService.auditRequestAdvice(request.affinityGroup, model)
          accreditationConnector
            .submitRequestAccreditation(model)
            .map { _ =>
              dataCleansingService.deleteMongoData(request.userAnswers.id, RequestAdviceJourney)
              Redirect(navigator.nextPage(CyaRequestAdvicePage(recordId), NormalMode, request.userAnswers))
            }
        case Left(errors) =>
          dataCleansingService.deleteMongoData(request.userAnswers.id, RequestAdviceJourney)
          Future.successful(
            handleError(recordId, errors)
          )
      }
    }
}
