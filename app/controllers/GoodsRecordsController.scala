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
import controllers.actions._
import forms.GoodsRecordsFormProvider
import models.router.responses.GetGoodsRecordResponse

import javax.inject.Inject
import pages.GoodsRecordsPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.{TableRow, Text}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.GoodsRecordsView
import viewmodels.govuk.table._

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
  goodsRecordConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val preparedForm = request.userAnswers.get(GoodsRecordsPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    goodsRecordConnector.getRecords(request.eori).map { goodsRecordResponse =>
      val list = TableViewModel(
        rows = Seq(headers()) ++ rows(goodsRecordResponse.goodsItemRecords)
      )

      val numRecordsOnPage =
        math.ceil(goodsRecordResponse.pagination.totalRecords / goodsRecordResponse.pagination.totalPages).toInt
      val firstRecordPos   = goodsRecordResponse.pagination.currentPage * numRecordsOnPage
      val firstRecord      = firstRecordPos + 1
      val lastRecord       = goodsRecordResponse.goodsItemRecords.size + firstRecordPos

      Ok(view(preparedForm, list, goodsRecordResponse.pagination.totalRecords, firstRecord, lastRecord))
    }
  }

  private[this] def rows(goodsRecords: Seq[GetGoodsRecordResponse])(implicit messages: Messages): Seq[Seq[TableRow]] =
    goodsRecords.map { goodsRecord =>
      Seq(
        TableRowViewModel(
          content = Text(goodsRecord.traderRef)
        ),
        TableRowViewModel(
          content = Text(goodsRecord.goodsDescription)
        ),
        TableRowViewModel(
          content = Text(goodsRecord.countryOfOrigin)
        ),
        TableRowViewModel(
          content = Text(goodsRecord.commodityCode)
        ),
        TableRowViewModel(
          content = Text("to be implemented in TGP-1220")
        ),
        TableRowViewModel(
          content = Text("ACTIONS")
        )
      )
    }

  private[this] def headers()(implicit messages: Messages): Seq[TableRow] =
    Seq(
      TableRowViewModel(
        content = Text(messages("goodsRecords.tableHeader.traderReference"))
      ).withCssClass("govuk-!-font-weight-bold"),
      TableRowViewModel(
        content = Text(messages("goodsRecords.tableHeader.goodsDescription"))
      ).withCssClass("govuk-!-font-weight-bold"),
      TableRowViewModel(
        content = Text(messages("goodsRecords.tableHeader.countryOfOrigin"))
      ).withCssClass("govuk-!-font-weight-bold"),
      TableRowViewModel(
        content = Text(messages("goodsRecords.tableHeader.commodityCode"))
      ).withCssClass("govuk-!-font-weight-bold"),
      TableRowViewModel(
        content = Text(messages("goodsRecords.tableHeader.status"))
      ).withCssClass("govuk-!-font-weight-bold"),
      TableRowViewModel(
        content = Text(messages("goodsRecords.tableHeader.actions"))
      ).withCssClass("govuk-!-font-weight-bold")
    )

  def onSearch: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, TableViewModel(rows = Seq.empty), 0, 0, 0))),
        value =>
          for {
            updatedAnswers      <- Future.fromTry(request.userAnswers.set(GoodsRecordsPage, value))
            _                   <- sessionRepository.set(updatedAnswers)
            goodsRecordResponse <- goodsRecordConnector.getRecords(request.eori)
          } yield {
            val list = TableViewModel(
              rows = Seq(headers()) ++ rows(goodsRecordResponse.goodsItemRecords)
            )

            val numRecordsOnPage =
              math.ceil(goodsRecordResponse.pagination.totalRecords / goodsRecordResponse.pagination.totalPages).toInt
            val firstRecordPos   = goodsRecordResponse.pagination.currentPage * numRecordsOnPage
            val firstRecord      = firstRecordPos + 1
            val lastRecord       = goodsRecordResponse.goodsItemRecords.size + firstRecordPos

            Ok(view(form.fill(value), list, goodsRecordResponse.pagination.totalRecords, firstRecord, lastRecord))
          }
      )
  }
}
