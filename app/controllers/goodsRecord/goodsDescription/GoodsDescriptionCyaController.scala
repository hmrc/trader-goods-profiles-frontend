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

package controllers.goodsRecord.goodsDescription

import cats.data.EitherNec
import com.google.inject.Inject
import connectors.GoodsRecordConnector
import controllers.BaseController
import controllers.actions.*
import exceptions.GoodsRecordBuildFailure.*
import models.*
import models.requests.DataRequest
import org.apache.pekko.Done
import pages.goodsRecord.*
import play.api.i18n.MessagesApi
import play.api.mvc.*
import repositories.SessionRepository
import services.{AuditService, GoodsRecordUpdateService}
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.Constants.*
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}
import viewmodels.checkAnswers.goodsRecord.*
import viewmodels.govuk.summarylist.*
import views.html.goodsRecord.CyaUpdateRecordView

import scala.concurrent.{ExecutionContext, Future}

class GoodsDescriptionCyaController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  auditService: AuditService,
  view: CyaUpdateRecordView,
  goodsRecordConnector: GoodsRecordConnector,
  sessionRepository: SessionRepository,
  goodsRecordUpdateService: GoodsRecordUpdateService // <-- Inject the service here
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String = "Unable to update Goods Record."

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      UpdateGoodsRecord.validateGoodsDescription(request.userAnswers, recordId) match {
        case Right(goodsDescription) =>
          val onSubmitAction =
            controllers.goodsRecord.goodsDescription.routes.GoodsDescriptionCyaController.onSubmit(recordId)

          val list = SummaryListViewModel(
            Seq(GoodsDescriptionSummary.rowUpdateCya(goodsDescription, recordId, CheckMode))
          )
          Future.successful(
            Ok(view(list, onSubmitAction, goodsDescriptionKey)).addingToSession(goodsDescription -> goodsDescription)
          )

        case Left(errors) =>
          Future.successful(
            logErrorsAndContinue(
              errorMessage,
              controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId),
              errors
            )
          )
      }
    }

  private def handleValidateError[T](result: EitherNec[ValidationError, T]): Future[T] =
    result match {
      case Right(value) => Future.successful(value)
      case Left(errors) => Future.failed(GoodsRecordBuildFailure(errors))
    }

  def onSubmit(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      (for {
        goodsDescription  <-
          handleValidateError(UpdateGoodsRecord.validateGoodsDescription(request.userAnswers, recordId))
        updateGoodsRecord <-
          Future.successful(UpdateGoodsRecord(request.eori, recordId, goodsDescription = Some(goodsDescription)))
        _                  = auditService.auditFinishUpdateGoodsRecord(recordId, request.affinityGroup, updateGoodsRecord)
        oldRecord         <- goodsRecordConnector.getRecord(recordId)
        _                 <- goodsRecordUpdateService.updateIfChanged(
                               oldValue = oldRecord.goodsDescription,
                               newValue = goodsDescription,
                               updateGoodsRecord = updateGoodsRecord,
                               oldRecord = oldRecord
                             )
        updatedAnswers    <- Future.fromTry(request.userAnswers.remove(GoodsDescriptionUpdatePage(recordId)))
        _                 <- sessionRepository.set(updatedAnswers)
      } yield {
        val hasGoodsDescriptionChanged = oldRecord.goodsDescription != goodsDescription
        if (hasGoodsDescriptionChanged) {
          Redirect(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId))
            .addingToSession("hasGoodsDescriptionChanged" -> hasGoodsDescriptionChanged.toString)
        } else {
          Redirect(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId))
            .removingFromSession(dataUpdated, "hasGoodsDescriptionChanged")
        }
      }).recover(handleRecover(recordId))
    }

  private def handleRecover(
    recordId: String
  )(implicit request: DataRequest[AnyContent]): PartialFunction[Throwable, Result] = {
    case e: GoodsRecordBuildFailure =>
      logErrorsAndContinue(
        e.getMessage,
        controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
      )

    case e: UpstreamErrorResponse if e.message.contains(openAccreditationErrorCode) =>
      Redirect(controllers.routes.RecordLockedController.onPageLoad(recordId))
        .removingFromSession(dataRemoved, dataUpdated, pageUpdated)
  }
}
