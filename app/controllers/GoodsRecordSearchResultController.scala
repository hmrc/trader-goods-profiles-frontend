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

import connectors.{GoodsRecordConnector, OttConnector}
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.GoodsRecordsPagination.{getFirstRecordIndex, getLastRecordIndex, getPagination, getSearchPagination}
import models.NormalMode
import pages.{CommodityCodeUpdatePage, CountryOfOriginUpdatePage, GoodsDescriptionUpdatePage, GoodsRecordsPage, TraderReferenceUpdatePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.{GoodsRecordSearchResultEmptyView, GoodsRecordSearchResultView, SingleRecordView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsRecordSearchResultController @Inject() (
  override val messagesApi: MessagesApi,
  goodsRecordConnector: GoodsRecordConnector,
  ottConnector: OttConnector,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  val controllerComponents: MessagesControllerComponents,
  view: GoodsRecordSearchResultView,
  emptyView: GoodsRecordSearchResultEmptyView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val pageSize = 10

  def onPageLoad(page: Int): Action[AnyContent] = identify.async { implicit request =>
    //    val preparedForm = request.userAnswers.get(GoodsRecordsPage) match {
    //      case None        => form
    //      case Some(value) => form.fill(value)
    //    }

    if (page < 1) {
      Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    } else {
      goodsRecordConnector.getRecordsCount(request.eori).flatMap {
        case 0 => Future.successful(Redirect(routes.GoodsRecordsController.onPageLoadNoRecords()))
        case _ =>
          for {
            searchResponse <- goodsRecordConnector.getRecords(request.eori, "000", page, pageSize)
            countries      <- ottConnector.getCountries
          } yield
            if (searchResponse.pagination.totalRecords != 0) {
              val firstRecord = getFirstRecordIndex(searchResponse.pagination, pageSize)
              Ok(
                view(
                  searchResponse.goodsItemRecords,
                  searchResponse.pagination.totalRecords,
                  getFirstRecordIndex(searchResponse.pagination, pageSize),
                  getLastRecordIndex(firstRecord, searchResponse.goodsItemRecords.size),
                  countries,
                  getSearchPagination(
                    searchResponse.pagination.currentPage,
                    searchResponse.pagination.totalPages
                  ),
                  page,
                  "search text"
                )
              )
//              Ok(view())
            } else {
              Redirect(routes.GoodsRecordSearchResultController.onPageLoadNoRecords())
            }
      }
    }
  }

  def onPageLoadNoRecords(): Action[AnyContent] = identify { implicit request =>
    Ok(emptyView())
  }
}
