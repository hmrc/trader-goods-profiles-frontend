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

package controllers.goodsRecord.productReference

import cats.data.EitherNec
import com.google.inject.Inject
import connectors.GoodsRecordConnector
import controllers.BaseController
import controllers.actions.*
import exceptions.GoodsRecordBuildFailure
import helpers.GoodsRecordRecovery
import models.*
import navigation.GoodsRecordNavigator
import org.apache.pekko.Done
import pages.goodsRecord.*
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.*
import repositories.SessionRepository
import services.{AuditService, GoodsRecordUpdateService}
import utils.Constants.*
import viewmodels.checkAnswers.goodsRecord.*
import viewmodels.govuk.summarylist.*
import views.html.goodsRecord.CyaUpdateRecordView

import scala.concurrent.{ExecutionContext, Future}

class ProductReferenceCyaController @Inject() (
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
  navigator: GoodsRecordNavigator,
  goodsRecordUpdateService: GoodsRecordUpdateService
)(implicit ec: ExecutionContext)
    extends BaseController
    with GoodsRecordRecovery {

  override val recoveryLogger: Logger = Logger(this.getClass)
  private val errorMessage: String    = "Unable to update Goods Record."

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      UpdateGoodsRecord.validateproductReference(request.userAnswers, recordId) match {
        case Right(productReference) =>
          val onSubmitAction =
            controllers.goodsRecord.productReference.routes.ProductReferenceCyaController.onSubmit(recordId)

          val list = SummaryListViewModel(
            Seq(ProductReferenceSummary.row(productReference, recordId, CheckMode, recordLocked = false))
          )
          Future.successful(Ok(view(list, onSubmitAction, productReferenceKey)))

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
        productReference  <-
          handleValidateError(UpdateGoodsRecord.validateproductReference(request.userAnswers, recordId))
        updateGoodsRecord <-
          Future.successful(UpdateGoodsRecord(request.eori, recordId, productReference = Some(productReference)))
        _                  = auditService.auditFinishUpdateGoodsRecord(recordId, request.affinityGroup, updateGoodsRecord)
        oldRecord         <- goodsRecordConnector.getRecord(recordId)
        _                 <- goodsRecordUpdateService.updateIfChanged(
                               oldValue = oldRecord.traderRef,
                               newValue = productReference,
                               updateGoodsRecord = updateGoodsRecord,
                               oldRecord = oldRecord
                             )
        updatedAnswers    <- Future.fromTry(request.userAnswers.remove(ProductReferenceUpdatePage(recordId)))
        _                 <- sessionRepository.set(updatedAnswers)
      } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers)))
        .recover(handleRecover(recordId))
    }

}
