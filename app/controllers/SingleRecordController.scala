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

import connectors.GoodsRecordConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import models.NormalMode
import models.helper.{CategorisationJourney, RequestAdviceJourney, SupplementaryUnitUpdateJourney, WithdrawAdviceJourney}
import models.requests.DataRequest
import pages.{CommodityCodeUpdatePage, CountryOfOriginUpdatePage, GoodsDescriptionUpdatePage, TraderReferenceUpdatePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.DataCleansingService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.SessionData._
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.SingleRecordView

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}
class SingleRecordController @Inject() (
  override val messagesApi: MessagesApi,
  goodsRecordConnector: GoodsRecordConnector,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  profileAuth: ProfileAuthenticateAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  dataCleansingService: DataCleansingService,
  val controllerComponents: MessagesControllerComponents,
  view: SingleRecordView
)(implicit @unused ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      for {
        record                             <- goodsRecordConnector.getRecord(request.eori, recordId)
        recordIsLocked                      = record.adviceStatus match {
                                                case status
                                                    if status.equalsIgnoreCase("Requested") ||
                                                      status.equalsIgnoreCase("In progress") ||
                                                      status.equalsIgnoreCase("Information Requested") =>
                                                  true
                                                case _ => false
                                              }
        updatedAnswersWithTraderReference  <-
          Future.fromTry(request.userAnswers.set(TraderReferenceUpdatePage(recordId), record.traderRef))
        updatedAnswersWithGoodsDescription <-
          Future.fromTry(
            updatedAnswersWithTraderReference.set(GoodsDescriptionUpdatePage(recordId), record.goodsDescription)
          )
        updatedAnswersWithCountryOfOrigin  <-
          Future.fromTry(
            updatedAnswersWithGoodsDescription.set(CountryOfOriginUpdatePage(recordId), record.countryOfOrigin)
          )
        updatedAnswersWithAll              <-
          Future.fromTry(
            updatedAnswersWithCountryOfOrigin.set(CommodityCodeUpdatePage(recordId), record.comcode)
          )

        _ <- sessionRepository.set(updatedAnswersWithAll)

      } yield {

        val detailsList = SummaryListViewModel(
          rows = Seq(
            TraderReferenceSummary.row(record.traderRef, recordId, NormalMode, recordIsLocked),
            GoodsDescriptionSummary.rowUpdate(record, recordId, NormalMode, recordIsLocked),
            CountryOfOriginSummary.rowUpdate(record, recordId, NormalMode, recordIsLocked),
            CommodityCodeSummary.rowUpdate(record, recordId, NormalMode, recordIsLocked),
            StatusSummary.row(record.declarable)
          )
        )

        val categorisationList    = SummaryListViewModel(
          rows = Seq(
            CategorySummary.row(record, recordIsLocked)
          )
        )
        val supplementaryUnitList = SummaryListViewModel(
          rows = Seq(
            HasSupplementaryUnitSummary.row(record, recordId, recordIsLocked),
            SupplementaryUnitSummary
              .row(record.supplementaryUnit, record.measurementUnit, recordId, recordIsLocked)
          ).flatten
        )
        val adviceList            = SummaryListViewModel(
          rows = Seq(
            AdviceStatusSummary.row(record.adviceStatus, record.recordId, recordIsLocked)
          )
        )
        val changesMade           = request.session.get(dataUpdated).contains("true")
        val pageRemoved           = request.session.get(dataRemoved).contains("true")
        val changedPage           = request.session.get(pageUpdated).getOrElse("")

        dataCleansing(request)

        Ok(
          view(
            recordId,
            detailsList,
            categorisationList,
            supplementaryUnitList,
            adviceList,
            changesMade,
            changedPage,
            pageRemoved,
            recordIsLocked
          )
        ).removingFromSession(initialValueOfHasSuppUnit, initialValueOfSuppUnit, fromExpiredCommodityCodePage)
      }
    }

  private def dataCleansing(request: DataRequest[AnyContent]): Unit = {
    dataCleansingService.deleteMongoData(request.userAnswers.id, SupplementaryUnitUpdateJourney)
    dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)
    dataCleansingService.deleteMongoData(request.userAnswers.id, RequestAdviceJourney)
    dataCleansingService.deleteMongoData(request.userAnswers.id, WithdrawAdviceJourney)
  }

}
