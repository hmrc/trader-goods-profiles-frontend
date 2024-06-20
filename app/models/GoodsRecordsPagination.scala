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

package models

import controllers.routes
import models.router.responses.GetRecordsResponse
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.govukfrontend.views.Aliases.Pagination
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{PaginationItem, PaginationLink}

case class GoodsRecordsPagination(
  totalRecords: Int,
  currentPage: Int,
  totalPages: Int,
  nextPage: Option[Int],
  prevPage: Option[Int]
)

object GoodsRecordsPagination {
  implicit val format: OFormat[GoodsRecordsPagination] = Json.format[GoodsRecordsPagination]

  private def getPageSize(totalRecords: Int, recordsSize: Int, totalPages: Int): Int =
    (totalRecords - recordsSize) / (totalPages - 1)

  def getFirstRecord(goodsRecordResponse: GetRecordsResponse): Int =
    getFirstRecordPos(
      goodsRecordResponse.pagination.totalRecords,
      goodsRecordResponse.goodsItemRecords.size,
      goodsRecordResponse.pagination.totalPages,
      goodsRecordResponse.pagination.currentPage
    ) + 1

  private def getFirstRecordPos(totalRecords: Int, recordsSize: Int, totalPages: Int, currentPage: Int): Int =
    (currentPage - 1) * getPageSize(totalRecords, recordsSize, totalPages)

  def getLastRecord(goodsRecordResponse: GetRecordsResponse): Int =
    goodsRecordResponse.goodsItemRecords.size + getFirstRecordPos(
      goodsRecordResponse.pagination.totalRecords,
      goodsRecordResponse.goodsItemRecords.size,
      goodsRecordResponse.pagination.totalPages,
      goodsRecordResponse.pagination.currentPage
    )

  def getPagination(pagination: GoodsRecordsPagination): Pagination =
    Pagination(
      items = Some((0 until pagination.totalPages).map { index =>
        PaginationItem(
          number = Some((index + 1).toString()),
          current = Some(pagination.currentPage == (index + 1)),
          href = routes.GoodsRecordsController.onPageLoad(index + 1).url
        )
      }),
      previous = if (pagination.currentPage == 1) {
        None
      } else {
        Some(PaginationLink(routes.GoodsRecordsController.onPageLoad(pagination.currentPage - 1).url))
      },
      next = if (pagination.currentPage == pagination.totalPages) {
        None
      } else {
        Some(PaginationLink(routes.GoodsRecordsController.onPageLoad(pagination.currentPage + 1).url))
      }
    )
}
