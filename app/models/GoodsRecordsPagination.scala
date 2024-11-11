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
  implicit val format: OFormat[GoodsRecordsPagination]                            = Json.format[GoodsRecordsPagination]
  private val defaultRecord                                                       = 0
  val firstPage                                                                   = 1
  def getFirstRecordIndex(pagination: GoodsRecordsPagination, pageSize: Int): Int =
    if (
      pagination.totalRecords == 0
      || pagination.currentPage > pagination.totalPages
      || (pagination.currentPage > firstPage && pageSize >= pagination.totalRecords)
    ) {
      defaultRecord
    } else {
      (
        (pagination.currentPage - 1) * pageSize
      ) + 1
    }

  def getLastRecordIndex(firstRecordIndex: Int, numOfRecords: Int): Int =
    if (firstRecordIndex == 0) {
      defaultRecord
    } else {
      numOfRecords + firstRecordIndex - 1
    }

  def getPagination(currentPage: Int, totalPages: Int): Pagination =
    if (currentPage < firstPage || totalPages < firstPage || currentPage > totalPages) {
      Pagination(None, None, None)
    } else {

      val start = if (currentPage <= 2) {
        1
      } else {
        currentPage - 2
      }

      val end = if (currentPage >= totalPages - 2) {
        totalPages + 1
      } else {
        currentPage + 3
      }

      Pagination(
        items = Some((start until end).map { page =>
          val ellipsis = if (page < currentPage - 1 || page > currentPage + 1) {
            true
          } else {
            false
          }
          PaginationItem(
            number = Some(page.toString),
            current = Some(currentPage == page),
            href = controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(page).url,
            ellipsis = Some(ellipsis)
          )
        }),
        previous = if (currentPage == firstPage) {
          None
        } else {
          Some(PaginationLink(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(currentPage - 1).url))
        },
        next = if (currentPage == totalPages) {
          None
        } else {
          Some(PaginationLink(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(currentPage + 1).url))
        }
      )
    }

  def getSearchPagination(currentPage: Int, totalPages: Int): Pagination =
    if (currentPage < firstPage || totalPages < firstPage || currentPage > totalPages) {
      Pagination(None, None, None)
    } else {

      val start = if (currentPage <= 2) {
        1
      } else {
        currentPage - 2
      }

      val end = if (currentPage >= totalPages - 2) {
        totalPages + 1
      } else {
        currentPage + 3
      }

      Pagination(
        items = Some((start until end).map { page =>
          val ellipsis = if (page < currentPage - 1 || page > currentPage + 1) {
            true
          } else {
            false
          }
          PaginationItem(
            number = Some(page.toString),
            current = Some(currentPage == page),
            href = controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(page).url,
            ellipsis = Some(ellipsis)
          )
        }),
        previous = if (currentPage == firstPage) {
          None
        } else {
          Some(
            PaginationLink(
              controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(currentPage - 1).url
            )
          )
        },
        next = if (currentPage == totalPages) {
          None
        } else {
          Some(
            PaginationLink(
              controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(currentPage + 1).url
            )
          )
        }
      )
    }
}
