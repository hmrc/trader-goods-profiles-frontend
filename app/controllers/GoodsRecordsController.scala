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
import controllers.actions._
import forms.GoodsRecordsFormProvider
import models.GoodsRecordsPagination._
import pages.GoodsRecordsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.SessionData.{dataUpdated, pageUpdated}
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
  ottConnector: OttConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form     = formProvider()
  private val pageSize = 10

  def onPageLoad(page: Int): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      if (page < 1) {
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      } else {
        goodsRecordConnector.getRecordsCount(request.eori).flatMap {
          case 0 => Future.successful(Ok(emptyView()))
          case _ =>
            goodsRecordConnector.getRecords(request.eori, page, pageSize).flatMap {
              case Right(goodsRecordResponse) =>
                if (goodsRecordResponse.pagination.totalRecords != 0) {
                  ottConnector.getCountries.map { countries =>
                    //TODO cleanse GoodsRecordsPage from session
                    val firstRecord = getFirstRecordIndex(goodsRecordResponse.pagination, pageSize)
                    Ok(
                      view(
                        form,
                        goodsRecordResponse.goodsItemRecords,
                        goodsRecordResponse.pagination.totalRecords,
                        getFirstRecordIndex(goodsRecordResponse.pagination, pageSize),
                        getLastRecordIndex(firstRecord, goodsRecordResponse.goodsItemRecords.size),
                        countries,
                        getPagination(
                          goodsRecordResponse.pagination.currentPage,
                          goodsRecordResponse.pagination.totalPages
                        ),
                        page
                      )
                    ).removingFromSession(dataUpdated, pageUpdated)
                  }
                } else {
                  Future.successful(
                    Ok(emptyView())
                      .removingFromSession(dataUpdated, pageUpdated)
                  )
                }
              case Left(_)                    =>
                Future.successful(
                  Redirect(
                    routes.GoodsRecordsLoadingController
                      .onPageLoad(Some(routes.GoodsRecordsController.onPageLoad(page).url))
                  )
                )
            }
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
              case Right(goodsRecordResponse) =>
                ottConnector.getCountries.map { countries =>
                  val firstRecord = getFirstRecordIndex(goodsRecordResponse.pagination, pageSize)
                  BadRequest(
                    view(
                      formWithErrors,
                      goodsRecordResponse.goodsItemRecords,
                      goodsRecordResponse.pagination.totalRecords,
                      getFirstRecordIndex(goodsRecordResponse.pagination, pageSize),
                      getLastRecordIndex(firstRecord, pageSize),
                      countries,
                      getPagination(
                        goodsRecordResponse.pagination.currentPage,
                        goodsRecordResponse.pagination.totalPages
                      ),
                      page
                    )
                  )
                }
              case Left(_)                    =>
                Future.successful(
                  Redirect(
                    routes.GoodsRecordsLoadingController
                      .onPageLoad(
                        Some(routes.GoodsRecordsController.onPageLoad(page).url)
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
