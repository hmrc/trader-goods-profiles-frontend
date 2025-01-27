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

package controllers.commodityCodeResult

import cats.data
import cats.data.EitherNec
import config.FrontendAppConfig
import connectors.GoodsRecordConnector
import controllers.BaseController
import controllers.actions._
import forms.HasCorrectGoodsFormProvider
import models.requests.DataRequest
import models.router.requests.PutRecordRequest
import models.{Mode, NormalMode, UpdateGoodsRecord, UserAnswers, ValidationError}
import navigation.Navigation
import org.apache.pekko.Done
import pages._
import pages.goodsRecord.{CommodityCodeUpdatePage, HasCommodityCodeChangePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import utils.SessionData.fromExpiredCommodityCodePage
import views.html.HasCorrectGoodsView

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class UpdateCommodityCodeResultController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigation,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: HasCorrectGoodsFormProvider,
  goodsRecordConnector: GoodsRecordConnector,
  auditService: AuditService,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: HasCorrectGoodsView
)(implicit @unused ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(HasCorrectGoodsCommodityCodeUpdatePage(recordId), form)
      val submitAction =
        controllers.commodityCodeResult.routes.UpdateCommodityCodeResultController.onSubmit(mode, recordId)
      request.userAnswers.get(CommodityUpdateQuery(recordId)) match {
        case Some(commodity) => Ok(view(preparedForm, commodity, submitAction, mode, Some(recordId)))
        case None            => navigator.journeyRecovery()
      }
    }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val submitAction =
        controllers.commodityCodeResult.routes.UpdateCommodityCodeResultController.onSubmit(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(CommodityUpdateQuery(recordId)) match {
              case Some(commodity) =>
                Future.successful(BadRequest(view(formWithErrors, commodity, submitAction, mode, Some(recordId))))
              case None            => Future.successful(navigator.journeyRecovery())
            },
          value => {
            val futureUpdatedAnswers = for {
              updatedAnswers <-
                Future.fromTry(request.userAnswers.set(HasCorrectGoodsCommodityCodeUpdatePage(recordId), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield updatedAnswers

            futureUpdatedAnswers.flatMap { updatedAnswers =>
              if (value) {
                updateCommodityCode(recordId, request, updatedAnswers)
              } else {
                Future.successful(
                  Redirect(
                    navigator
                      .nextPage(HasCorrectGoodsCommodityCodeUpdatePage(recordId), NormalMode, updatedAnswers)
                  )
                )
              }
            }
          }
        )
    }

  private def updateCommodityCode(recordId: String, request: DataRequest[AnyContent], updatedUserAnswers: UserAnswers)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    (for {
      oldRecord                <- goodsRecordConnector.getRecord(recordId)
      commodity                <-
        handleValidateError(
          UpdateGoodsRecord
            .validateCommodityCode(
              updatedUserAnswers,
              recordId,
              oldRecord.category.isDefined,
              request.session.get(fromExpiredCommodityCodePage).contains("true")
            )
        )
      updateGoodsRecord        <-
        Future.successful(UpdateGoodsRecord(request.eori, recordId, commodityCode = Some(commodity)))
      putGoodsRecord           <- Future.successful(
                                    PutRecordRequest(
                                      actorId = oldRecord.eori,
                                      traderRef = oldRecord.traderRef,
                                      comcode = commodity.commodityCode,
                                      goodsDescription = oldRecord.goodsDescription,
                                      countryOfOrigin = oldRecord.countryOfOrigin,
                                      category = None,
                                      assessments = oldRecord.assessments,
                                      supplementaryUnit = oldRecord.supplementaryUnit,
                                      measurementUnit = oldRecord.measurementUnit,
                                      comcodeEffectiveFromDate = oldRecord.comcodeEffectiveFromDate,
                                      comcodeEffectiveToDate = oldRecord.comcodeEffectiveToDate
                                    )
                                  )
      _                         = auditService.auditFinishUpdateGoodsRecord(recordId, request.affinityGroup, updateGoodsRecord)
      _                        <- updateGoodsRecordIfPutValueChanged(
                                    commodity.commodityCode,
                                    oldRecord.comcode,
                                    updateGoodsRecord,
                                    putGoodsRecord
                                  )
      updatedAnswersWithChange <-
        Future.fromTry(request.userAnswers.remove(HasCommodityCodeChangePage(recordId)))
      updatedAnswers           <- Future.fromTry(updatedAnswersWithChange.remove(CommodityCodeUpdatePage(recordId)))
      _                        <- sessionRepository.set(updatedAnswers)
    } yield Redirect(
      navigator.nextPage(HasCorrectGoodsCommodityCodeUpdatePage(recordId), NormalMode, updatedAnswers)
    )).recover { case e: GoodsRecordBuildFailure =>
      logErrorsAndContinue(
        e.getMessage,
        controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
      )
    }

  private def handleValidateError[T](result: EitherNec[ValidationError, T]): Future[T] =
    result match {
      case Right(value) => Future.successful(value)
      case Left(errors) => Future.failed(GoodsRecordBuildFailure(errors))
    }

  private case class GoodsRecordBuildFailure(errors: data.NonEmptyChain[ValidationError]) extends Exception {
    private val errorsAsString       = errors.toChain.toList.map(_.message).mkString(", ")
    private val errorMessage: String = "Unable to update Goods Record."

    override def getMessage: String = s"$errorMessage Missing pages: $errorsAsString"
  }

  private def updateGoodsRecordIfPutValueChanged(
    newValue: String,
    oldValue: String,
    newUpdateGoodsRecord: UpdateGoodsRecord,
    putRecordRequest: PutRecordRequest
  )(implicit hc: HeaderCarrier): Future[Done] =
    if (newValue != oldValue) {

      if (config.useEisPatchMethod) {
        goodsRecordConnector.putGoodsRecord(
          putRecordRequest,
          newUpdateGoodsRecord.recordId
        )
      } else {
        goodsRecordConnector.updateGoodsRecord(
          newUpdateGoodsRecord
        )
      }

    } else {
      Future.successful(Done)
    }

}
