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

package controllers.goodsProfile

import config.FrontendAppConfig
import connectors.{GoodsRecordConnector, OttConnector}
import controllers.BaseController
import controllers.actions._
import forms.goodsProfile.GoodsRecordsFormProvider
import models.GoodsRecordsPagination._
import models.SearchForm
import models.requests.DataRequest
import models.router.responses.GetRecordsResponse
import navigation.GoodsProfileNavigator
import pages.goodsProfile.GoodsRecordsPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}
import views.html.goodsProfile.{GoodsRecordsEmptyView, GoodsRecordsView}

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
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form            = formProvider()
  private val pageSize        = 10
  private val emptySearchForm = SearchForm(None, None, List.empty)

  def onPageLoad(page: Int): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      if (page < 1) {
        Future.successful(navigator.journeyRecovery())
      } else {
        goodsRecordConnector.getRecords(page, pageSize).flatMap {
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
                  goodsRecordsResponse.pagination,
                  firstRecord,
                  getLastRecordIndex(firstRecord, goodsRecordsResponse.goodsItemRecords.size),
                  countries,
                  getPagination(
                    goodsRecordsResponse.pagination.currentPage,
                    goodsRecordsResponse.pagination.totalPages
                  ),
                  page,
                  pageSize,
                  emptySearchForm,
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
      SearchForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => handleFormErrors(page, formWithErrors),
          searchFormData => processSearchForm(searchFormData)
        )
    }

  private def handleFormErrors(page: Int, formWithErrors: Form[SearchForm])(implicit request: DataRequest[AnyContent]) =
    goodsRecordConnector.getRecords(page, pageSize).flatMap {
      case Some(goodsRecordsResponse) =>
        for {
          countries <- ottConnector.getCountries
        } yield {
          val firstRecord = getFirstRecordIndex(goodsRecordsResponse.pagination, pageSize)
          BadRequest(
            view(
              formWithErrors,
              goodsRecordsResponse.goodsItemRecords,
              goodsRecordsResponse.pagination,
              firstRecord,
              getLastRecordIndex(firstRecord, pageSize),
              countries,
              getPagination(goodsRecordsResponse.pagination.currentPage, goodsRecordsResponse.pagination.totalPages),
              page,
              pageSize,
              emptySearchForm,
              None
            )
          )
        }
      case None                       =>
        Future.successful(
          Redirect(
            controllers.goodsProfile.routes.GoodsRecordsLoadingController.onPageLoad(
              Some(RedirectUrl(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(page).url))
            )
          )
        )
    }

  private def processSearchForm(searchFormData: SearchForm)(implicit request: DataRequest[AnyContent]) =
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(GoodsRecordsPage, searchFormData))
      _              <- sessionRepository.set(updatedAnswers)
      result         <- if (!appConfig.enhancedSearch) {
                          Future.successful(
                            Redirect(controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(1))
                          )
                        } else {
                          Future.successful(
                            Redirect(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoadFilter(1))
                          )
                        }
    } yield result

  def onPageLoadFilter(page: Int): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(GoodsRecordsPage) match {
        case Some(searchText) if page >= 1 =>
          executeSearch(page, searchText)
        case _                             =>
          Future.successful(navigator.journeyRecovery())
      }
    }

  def normalizeString(str: String): String =
    str.replaceAll("\\s+", "").toLowerCase

  def matchesSearchTerms(record: String, searchTerms: List[String]): Boolean = {
    val normalizedRecord = normalizeString(record)

    searchTerms.forall(term => normalizedRecord.contains(term))
  }

  private def executeSearch(page: Int, searchText: SearchForm)(implicit request: DataRequest[AnyContent]) = {
    val searchTerms = searchText.searchTerm
      .map(_.split("\\s+").map(normalizeString).toList)
      .getOrElse(List())

    val searchTermOption = if (searchTerms.isEmpty) None else Some(searchTerms.mkString(" "))

    goodsRecordConnector
      .searchRecords(
        request.eori,
        searchTermOption,
        exactMatch = false,
        countryOfOrigin = searchText.countryOfOrigin,
        IMMIReady = Some(searchText.statusValue.contains("IMMIReady")),
        notReadyForIMMI = Some(searchText.statusValue.contains("notReadyForImmi")),
        actionNeeded = Some(searchText.statusValue.contains("actionNeeded")),
        page,
        pageSize
      )
      .flatMap {
        case Some(searchResponse) =>
          val filteredRecords = searchResponse.goodsItemRecords.filter { record =>
            matchesSearchTerms(record.goodsDescription, searchTerms) // Match description against search terms
          }

          if (filteredRecords.nonEmpty) {
            renderSearchResults(page, searchResponse.copy(goodsItemRecords = filteredRecords), searchText)
          } else {
            Future.successful(
              Redirect(
                controllers.goodsProfile.routes.GoodsRecordsLoadingController.onPageLoad(
                  Some(RedirectUrl(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoadFilter(page).url))
                )
              )
            )
          }

        case _ =>
          Future.successful(
            Redirect(
              controllers.goodsProfile.routes.GoodsRecordsLoadingController.onPageLoad(
                Some(RedirectUrl(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoadFilter(page).url))
              )
            )
          )
      }
  }

  private def renderSearchResults(page: Int, searchResponse: GetRecordsResponse, searchText: SearchForm)(implicit
    request: DataRequest[AnyContent]
  ) =
    ottConnector.getCountries.map { countries =>
      val firstRecord = getFirstRecordIndex(searchResponse.pagination, pageSize)
      Ok(
        view(
          SearchForm.form.fill(searchText),
          searchResponse.goodsItemRecords,
          searchResponse.pagination,
          firstRecord,
          getLastRecordIndex(firstRecord, searchResponse.goodsItemRecords.size),
          countries,
          getSearchPaginationFilter(searchResponse.pagination.currentPage, searchResponse.pagination.totalPages),
          page,
          pageSize,
          searchText,
          Some(searchResponse.pagination.totalPages)
        )
      )
    }
}
