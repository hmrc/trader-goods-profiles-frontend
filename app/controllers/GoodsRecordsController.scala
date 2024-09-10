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

import connectors.{DownloadDataConnector, GoodsRecordConnector, OttConnector}
import controllers.actions._
import forms.GoodsRecordsFormProvider
import models.DownloadDataStatus.{FileInProgress, FileReadySeen, FileReadyUnseen, RequestFile}
import models.DownloadDataSummary
import models.GoodsRecordsPagination._
import navigation.Navigator
import pages.GoodsRecordsPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}
import views.html.{GoodsRecordsEmptyView, GoodsRecordsView}

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
  downloadDataConnector: DownloadDataConnector,
  ottConnector: OttConnector,
  navigator: Navigator
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
              countries              <- ottConnector.getCountries
              updatedAnswers         <- Future.fromTry(request.userAnswers.remove(GoodsRecordsPage))
              _                      <- sessionRepository.set(updatedAnswers)
              downloadDataSummaryOpt <- downloadDataConnector.getDownloadDataSummary(request.eori)
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
                  getDownloadLinkMessagesKey(downloadDataSummaryOpt),
                  getDownloadLinkRoute(downloadDataSummaryOpt)
                )
              ).removingFromSession(dataUpdated, pageUpdated, dataRemoved)
            }
          case Some(_)                                                                        =>
            Future.successful(Ok(emptyView()).removingFromSession(dataUpdated, pageUpdated, dataRemoved))
          case None                                                                           =>
            Future.successful(
              Redirect(
                routes.GoodsRecordsLoadingController
                  .onPageLoad(Some(RedirectUrl(routes.GoodsRecordsController.onPageLoad(page).url)))
              )
            )
        }
      }
    }

  def getDownloadLinkMessagesKey(opt: Option[DownloadDataSummary]): String =
    opt match {
      case Some(downloadDataSummary) if downloadDataSummary.status == RequestFile     =>
        "goodsRecords.downloadLinkText.requestFile"
      case Some(downloadDataSummary) if downloadDataSummary.status == FileInProgress  =>
        "goodsRecords.downloadLinkText.fileInProgress"
      case Some(downloadDataSummary) if downloadDataSummary.status == FileReadyUnseen =>
        "goodsRecords.downloadLinkText.fileReady"
      case Some(downloadDataSummary) if downloadDataSummary.status == FileReadySeen   =>
        "goodsRecords.downloadLinkText.fileReady"
      case _                                                                          =>
        "goodsRecords.downloadLinkText.requestFile"
    }

  def getDownloadLinkRoute(opt: Option[DownloadDataSummary]): String =
    opt match {
      case Some(downloadDataSummary) if downloadDataSummary.status == RequestFile     =>
        routes.RequestDataController.onPageLoad().url
      case Some(downloadDataSummary) if downloadDataSummary.status == FileInProgress  =>
        routes.FileInProgressController.onPageLoad().url
      case Some(downloadDataSummary) if downloadDataSummary.status == FileReadySeen   =>
        routes.FileReadyController.onPageLoad().url
      case Some(downloadDataSummary) if downloadDataSummary.status == FileReadyUnseen =>
        routes.FileReadyController.onPageLoad().url
      case _                                                                          => routes.RequestDataController.onPageLoad().url
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
                  countries              <- ottConnector.getCountries
                  downloadDataSummaryOpt <- downloadDataConnector.getDownloadDataSummary(request.eori)
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
                      getDownloadLinkMessagesKey(downloadDataSummaryOpt),
                      getDownloadLinkRoute(downloadDataSummaryOpt)
                    )
                  )
                }
              case None                       =>
                Future.successful(
                  Redirect(
                    routes.GoodsRecordsLoadingController
                      .onPageLoad(
                        Some(RedirectUrl(routes.GoodsRecordsController.onPageLoad(page).url))
                      )
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(GoodsRecordsPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(routes.GoodsRecordsSearchResultController.onPageLoad(1))
        )
    }
}
