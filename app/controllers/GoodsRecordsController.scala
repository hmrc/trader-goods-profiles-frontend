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
import models.router.responses.GetGoodsRecordResponse
import pages.GoodsRecordsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.Pagination
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{GoodsRecordsEmptyView, GoodsRecordsView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsRecordsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: GoodsRecordsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: GoodsRecordsView,
  emptyView: GoodsRecordsEmptyView,
  goodsRecordConnector: GoodsRecordConnector,
  ottConnector: OttConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(page: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      for {
        goodsRecordResponse <- goodsRecordConnector.getRecords(request.eori, Some(page))
        countries           <- ottConnector.getCountries
      } yield
        if (goodsRecordResponse.pagination.totalRecords != 0) {
          val preparedForm                                                   = request.userAnswers.get(GoodsRecordsPage) match {
            case None        => form
            case Some(value) => form.fill(value)
          }
          implicit val goodsRecordOrdering: Ordering[GetGoodsRecordResponse] = Ordering.by(_.updatedDateTime)

          Ok(
            view(
              preparedForm,
              goodsRecordResponse.goodsItemRecords.sorted,
              goodsRecordResponse.pagination.totalRecords,
              getFirstRecord(goodsRecordResponse),
              getLastRecord(goodsRecordResponse),
              countries,
              getPagination(goodsRecordResponse.pagination),
              page
            )
          )
        } else {
          Redirect(routes.GoodsRecordsController.onPageLoadNoRecords())
        }
  }

  def onPageLoadNoRecords(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Ok(emptyView())
  }

  def onSearch(page: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, Seq.empty, 0, 0, 0, Seq.empty, Pagination(), page))
            ),
          value =>
            for {
              updatedAnswers      <- Future.fromTry(request.userAnswers.set(GoodsRecordsPage, value))
              _                   <- sessionRepository.set(updatedAnswers)
              goodsRecordResponse <- goodsRecordConnector.getRecords(request.eori, Some(page))
              countries           <- ottConnector.getCountries
            } yield Ok(
              view(
                form.fill(value),
                goodsRecordResponse.goodsItemRecords,
                goodsRecordResponse.pagination.totalRecords,
                getFirstRecord(goodsRecordResponse),
                getLastRecord(goodsRecordResponse),
                countries,
                getPagination(goodsRecordResponse.pagination),
                page
              )
            )
        )
  }
}
