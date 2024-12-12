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

package controllers.goodsProfile

import connectors.{GoodsRecordConnector, OttConnector}
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import controllers.BaseController
import models.GoodsRecordsPagination.{getFirstRecordIndex, getLastRecordIndex, getSearchPagination}
import navigation.GoodsProfileNavigator
import pages.goodsProfile.GoodsRecordsPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.goodsProfile.{GoodsRecordsSearchResultEmptyView, GoodsRecordsSearchResultView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsRecordsSearchResultController @Inject() (
  override val messagesApi: MessagesApi,
  goodsRecordConnector: GoodsRecordConnector,
  ottConnector: OttConnector,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  identify: IdentifierAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: GoodsRecordsSearchResultView,
  emptyView: GoodsRecordsSearchResultEmptyView,
  navigator: GoodsProfileNavigator
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val pageSize = 10

  def onPageLoad(page: Int): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(GoodsRecordsPage) match {
        case Some(searchText) =>
          if (page < 1) {
            Future.successful(navigator.journeyRecovery())
          } else {
            goodsRecordConnector.searchRecords(request.eori, searchText, exactMatch = false, page, pageSize).flatMap {
              case Some(searchResponse) =>
                if (searchResponse.pagination.totalRecords != 0) {
                  ottConnector.getCountries.map { countries =>
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
                        searchText,
                        searchResponse.pagination.totalPages
                      )
                    )
                  }
                } else {
                  request.userAnswers.get(GoodsRecordsPage) match {
                    case Some(searchText) => Future.successful(Ok(emptyView(searchText)))
                    case None             => Future.successful(navigator.journeyRecovery())
                  }
                }
              case None                 =>
                Future.successful(
                  Redirect(
                    controllers.goodsProfile.routes.GoodsRecordsLoadingController
                      .onPageLoad(
                        Some(
                          RedirectUrl(
                            controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(page).url
                          )
                        )
                      )
                  )
                )

            }
          }
        case None             => Future(navigator.journeyRecovery())
      }
    }
}
