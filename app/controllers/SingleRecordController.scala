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
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import pages.{CommodityCodeUpdatePage, CountryOfOriginUpdatePage, GoodsDescriptionUpdatePage, TraderReferenceUpdatePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.SessionData.{dataUpdated, pageUpdated}
import viewmodels.checkAnswers.{AdviceStatusSummary, CategorySummary, CommodityCodeSummary, CountryOfOriginSummary, GoodsDescriptionSummary, HasSupplementaryUnitSummary, StatusSummary, SupplementaryUnitSummary, TraderReferenceSummary}
import viewmodels.govuk.summarylist._
import views.html.SingleRecordView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
class SingleRecordController @Inject() (
  override val messagesApi: MessagesApi,
  goodsRecordConnector: GoodsRecordConnector,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: SingleRecordView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      for {
        record                             <- goodsRecordConnector.getRecord(request.eori, recordId)
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
        _                                  <- sessionRepository.set(updatedAnswersWithAll)
      } yield {
        val detailsList = SummaryListViewModel(
          rows = Seq(
            TraderReferenceSummary.row(record.traderRef, recordId, NormalMode),
            GoodsDescriptionSummary.row(record.goodsDescription, recordId, NormalMode),
            CountryOfOriginSummary.row(record.countryOfOrigin, recordId, NormalMode),
            CommodityCodeSummary.row(record.comcode, recordId, NormalMode),
            StatusSummary.row(record.declarable)
          )
        )

        val categorisationList    = SummaryListViewModel(
          rows = Seq(
            CategorySummary.row(record.category.toString, record.recordId)
          )
        )
        val supplementaryUnitList = SummaryListViewModel(
          rows = Seq(
            HasSupplementaryUnitSummary.row(record.supplementaryUnit.isDefined, recordId),
            SupplementaryUnitSummary
              .row(record.supplementaryUnit, record.measurementUnit, request.userAnswers, recordId)
          ).flatten
        )
        val adviceList            = SummaryListViewModel(
          rows = Seq(
            AdviceStatusSummary.row(record.adviceStatus, record.recordId)
          )
        )
        val changesMade           = request.session.get(dataUpdated).contains("true")
        val changedPage           = request.session.get(pageUpdated).getOrElse("")

        Ok(view(recordId, detailsList, categorisationList, supplementaryUnitList, adviceList, changesMade, changedPage))
      }
  }
}
