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
import controllers.actions.IdentifierAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{AdviceStatusSummary, CategorySummary, CommodityCodeSummary, CountryOfOriginSummary, GoodsDescriptionSummary, TraderReferenceSummary}
import viewmodels.govuk.summarylist._
import views.html.SingleRecordView

import javax.inject.Inject
import scala.concurrent.ExecutionContext
class SingleRecordController @Inject() (
  override val messagesApi: MessagesApi,
  goodsRecordConnector: GoodsRecordConnector,
  identify: IdentifierAction,
  val controllerComponents: MessagesControllerComponents,
  view: SingleRecordView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(recordId: String): Action[AnyContent] = identify.async { implicit request =>
    goodsRecordConnector.getRecord(request.eori, recordId).map { record =>
      val detailsList = SummaryListViewModel(
        rows = Seq(
          TraderReferenceSummary.row(record.traderRef),
          GoodsDescriptionSummary.row(record.goodsDescription),
          CountryOfOriginSummary.row(record.countryOfOrigin),
          CommodityCodeSummary.row(record.commodityCode)
        )
      )

      val categorisationList = SummaryListViewModel(
        rows = Seq(
          CategorySummary.row(record.category.toString)
        )
      )

      val adviceList = SummaryListViewModel(
        rows = Seq(
          AdviceStatusSummary.row(record.adviceStatus)
        )
      )

      Ok(view(detailsList, categorisationList, adviceList))
    }
  }

}
