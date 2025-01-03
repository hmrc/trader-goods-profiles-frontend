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

import config.FrontendAppConfig
import connectors.{GoodsRecordConnector, OttConnector}
import controllers.actions._
import controllers.BaseController
import forms.goodsProfile.GoodsRecordsFormProvider
import models.GoodsRecordsPagination._
import navigation.GoodsProfileNavigator
import pages.goodsProfile.GoodsRecordsPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}
import views.html.goodsProfile.{GoodsRecordsEmptyView, GoodsRecordsSearchResultEmptyView, GoodsRecordsSearchResultView, GoodsRecordsView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsRecordsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: GoodsRecordsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: GoodsRecordsView,
  emptyView: GoodsRecordsEmptyView,
  goodsRecordConnector: GoodsRecordConnector,
  ottConnector: OttConnector,
  navigator: GoodsProfileNavigator,
  appConfig: FrontendAppConfig,
  view2: GoodsRecordsSearchResultView,
  emptyView2: GoodsRecordsSearchResultEmptyView,
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form     = formProvider()
  private val pageSize = 10

  def onPageLoad(page: Int): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      if (page < 1) {
        Future.successful(navigator.journeyRecovery())
      } else {
        goodsRecordConnector.getRecords(request.eori, page, pageSize).flatMap {
          case Some(goodsRecordsResponse) if goodsRecordsResponse.pagination.totalRecords > 0 =>
            for {
              countries      <- ottConnector.getCountries
              updatedAnswers <- Future.fromTry(request.userAnswers.remove(GoodsRecordsPage))
              _              <- sessionRepository.set(updatedAnswers)
            } yield {
              val firstRecord = getFirstRecordIndex(goodsRecordsResponse.pagination, pageSize)
              Ok(
                view(
                  form,
                  goodsRecordsResponse.goodsItemRecords,
                  goodsRecordsResponse.pagination.totalRecords,
                  firstRecord,
                  getLastRecordIndex(firstRecord, goodsRecordsResponse.goodsItemRecords.size),
                  countries,
                  getPagination(
                    goodsRecordsResponse.pagination.currentPage,
                    goodsRecordsResponse.pagination.totalPages
                  ),
                  page,
                  pageSize,
                  None,
                  None

                )
              ).removingFromSession(dataUpdated, pageUpdated, dataRemoved)
            }
          case Some(_)                                                                        =>
            Future.successful(Ok(emptyView()).removingFromSession(dataUpdated, pageUpdated, dataRemoved))
          case None                                                                           =>
            Future.successful(
              Redirect(
                controllers.goodsProfile.routes.GoodsRecordsLoadingController
                  .onPageLoad(
                    Some(RedirectUrl(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(page).url))
                  )
              )
            )
        }
      }
    }

  def onSearch(page: Int): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            goodsRecordConnector.getRecords(request.eori, page, pageSize).flatMap {
              case Some(goodsRecordsResponse) =>
                for {
                  countries <- ottConnector.getCountries
                } yield {
                  val firstRecord = getFirstRecordIndex(goodsRecordsResponse.pagination, pageSize)
                  BadRequest(
                    view(
                      formWithErrors,
                      goodsRecordsResponse.goodsItemRecords,
                      goodsRecordsResponse.pagination.totalRecords,
                      getFirstRecordIndex(goodsRecordsResponse.pagination, pageSize),
                      getLastRecordIndex(firstRecord, pageSize),
                      countries,
                      getPagination(
                        goodsRecordsResponse.pagination.currentPage,
                        goodsRecordsResponse.pagination.totalPages
                      ),
                      page,
                      pageSize,
                      None,
                      None
                    )
                  )
                }
              case None                       =>
                Future.successful(
                  Redirect(
                    controllers.goodsProfile.routes.GoodsRecordsLoadingController
                      .onPageLoad(
                        Some(RedirectUrl(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(page).url))
                      )
                  )
                )
            },
          searchTerms =>

              for {

                updatedAnswers <- Future.fromTry(request.userAnswers.set(GoodsRecordsPage, searchTerms))
                _ <- sessionRepository.set(updatedAnswers)
                result <- {
                  if (appConfig.enhancedSearch) {
                    Future.successful(
                      Redirect(
                        controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(1)
                      )
                    )
                  } else {
                    onPageLoadFilter(page)(request)
                  }
                }
              } yield result
          )
    }

  def onPageLoadFilter(page: Int): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(GoodsRecordsPage) match {
        case Some(searchText) =>
          if (page < 1) {
            Future.successful(navigator.journeyRecovery())
          } else {
            goodsRecordConnector.searchRecords(request.eori, Some(searchText), exactMatch = false, countryOfOrigin = Some("AL"),
              IMMIReady = Some(false),
              notReadyForIMMI = Some(false),
              actionNeeded = Some(false), page, pageSize).flatMap {
              case Some(searchResponse) =>
                if (searchResponse.pagination.totalRecords != 0) {
                  ottConnector.getCountries.map { countries =>
                    val firstRecord = getFirstRecordIndex(searchResponse.pagination, pageSize)
                    Ok(
                      view(
                        form,
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
                        pageSize,
                        Some(searchText),
                        Some(searchResponse.pagination.totalPages)
                      )
                    )
                  }
                } else {
                  onPageLoad(page)(request)
//                  request.userAnswers.get(GoodsRecordsPage) match {
//                    case Some(searchText) => Future.successful(Ok(emptyView2(searchText)))
//                    case None             => Future.successful(navigator.journeyRecovery())
//                  }
                }
              case None                 =>
                Future.successful(
                  Redirect(
                    controllers.goodsProfile.routes.GoodsRecordsLoadingController
                      .onPageLoad(
                        Some(
                          RedirectUrl(
                            controllers.goodsProfile.routes.GoodsRecordsController.onPageLoadFilter(page).url
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
