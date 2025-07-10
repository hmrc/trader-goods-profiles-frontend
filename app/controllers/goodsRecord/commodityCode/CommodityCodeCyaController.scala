/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.goodsRecord.commodityCode

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
import play.api.i18n.MessagesApi
import play.api.mvc.*
import repositories.SessionRepository
import services._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.*
import viewmodels.checkAnswers.goodsRecord.*
import viewmodels.govuk.summarylist.*
import views.html.goodsRecord.CyaUpdateRecordView
import play.api.Logger

import java.time.{LocalDate, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

class CommodityCodeCyaController @Inject() (
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
  autoCategoriseService: AutoCategoriseService,
  commodityService: CommodityService,
  goodsRecordUpdateService: GoodsRecordUpdateService
)(implicit ec: ExecutionContext)
    extends BaseController
    with GoodsRecordRecovery {
  override val recoveryLogger: Logger = Logger(this.getClass)

  private val errorMessage: String = "Unable to update Goods Record."

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      goodsRecordConnector
        .getRecord(recordId)
        .flatMap { recordResponse =>
          commodityService
            .isCommodityCodeValid(recordResponse.comcode, recordResponse.countryOfOrigin)(request)
            .flatMap { isCommCodeValid =>
              UpdateGoodsRecord
                .validateCommodityCode(
                  request.userAnswers,
                  recordId,
                  recordResponse.category.isDefined,
                  !isCommCodeValid
                ) match {
                case Right(commodity) =>
                  val onSubmitAction =
                    controllers.goodsRecord.commodityCode.routes.CommodityCodeCyaController.onSubmit(recordId)

                  val list = SummaryListViewModel(
                    Seq(
                      CommodityCodeSummary
                        .rowUpdateCya(
                          commodity.commodityCode,
                          recordId,
                          CheckMode
                        )
                    )
                  )
                  Future.successful(Ok(view(list, onSubmitAction, commodityCodeKey)))

                case Left(errors) =>
                  Future.successful(
                    logErrorsAndContinue(
                      errorMessage,
                      controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
                    )
                  )
              }
            }
        }
        .recoverWith { case e: Exception =>
          logger.error(s"Unable to fetch record $recordId")
          Future.successful(
            navigator.journeyRecovery(
              Some(RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId).url))
            )
          )
        }
    }

  def onSubmit(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val maybeOriginalCommodityCode: Option[String] =
        request.session.get("originalCommodityCode").map(_.trim)

      val resultFuture = for {
        oldRecord <- goodsRecordConnector.getRecord(recordId)

        isCommCodeExpired = oldRecord.comcodeEffectiveToDate.exists(
                              _.isBefore(LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant)
                            )

        commodity <- UpdateGoodsRecord.validateCommodityCode(
                       request.userAnswers,
                       recordId,
                       oldRecord.category.isDefined,
                       isCommCodeExpired
                     ) match {
                       case Right(value) => Future.successful(value)
                       case Left(errors) => Future.failed(GoodsRecordBuildFailure(errors))
                     }

        updateGoodsRecord = UpdateGoodsRecord(
                              eori = request.eori,
                              recordId = recordId,
                              commodityCode = Some(commodity)
                            )

        _ = auditService.auditFinishUpdateGoodsRecord(
              recordId,
              request.affinityGroup,
              updateGoodsRecord
            )

        _ <- goodsRecordUpdateService.updateIfChanged(
               oldValue = oldRecord.comcode,
               newValue = commodity.commodityCode,
               updateGoodsRecord = updateGoodsRecord,
               oldRecord = oldRecord,
               patch = false
             )

        updatedAnswersWithChange <- Future.fromTry(
                                      request.userAnswers.remove(HasCommodityCodeChangePage(recordId))
                                    )

        updatedAnswers <- Future.fromTry(
                            updatedAnswersWithChange.remove(CommodityCodeUpdatePage(recordId))
                          )

        _ <- sessionRepository.set(updatedAnswers)

        autoCategoriseScenario <- autoCategoriseService.autoCategoriseRecord(recordId, updatedAnswers)

      } yield {
        val newCode                 = commodity.commodityCode.trim
        val originalCode            = maybeOriginalCommodityCode.getOrElse("").trim
        val commodityCodeHasChanged = newCode != originalCode

        if (commodityCodeHasChanged) {
          if (autoCategoriseScenario.isDefined) {
            Redirect(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId))
          } else {
            Redirect(controllers.goodsRecord.commodityCode.routes.UpdatedCommodityCodeController.onPageLoad(recordId))
          }
        } else {
          Redirect(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId))
            .removingFromSession("showCommodityCodeChangeBanner")
        }
      }

      resultFuture.recover(handleRecover(recordId))
    }
}
