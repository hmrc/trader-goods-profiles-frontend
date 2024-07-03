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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import uk.gov.hmrc.govukfrontend.views.Aliases.Pagination
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{PaginationItem, PaginationLink}

class GoodsRecordsPaginationSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {
  private val pageSize = 10

  ".getPagination" - {

    "when currentPage 0" in {
      val currentPage = 0
      val totalPages  = 4
      val pagination  = Pagination(
        items = None,
        previous = None,
        next = None
      )
      val result      = GoodsRecordsPagination.getPagination(currentPage, totalPages)
      result.items mustEqual pagination.items
      result.previous mustEqual pagination.previous
      result.next mustEqual pagination.next

    }

    "when currentPage is bigger than totalPages" in {
      val currentPage = 10
      val totalPages  = 4
      val pagination  = Pagination(
        items = None,
        previous = None,
        next = None
      )
      val result      = GoodsRecordsPagination.getPagination(currentPage, totalPages)
      result.items mustEqual pagination.items
      result.previous mustEqual pagination.previous
      result.next mustEqual pagination.next

    }

    "when totalPages 0" in {
      val currentPage = 4
      val totalPages  = 0
      val pagination  = Pagination(
        items = None,
        previous = None,
        next = None
      )
      val result      = GoodsRecordsPagination.getPagination(currentPage, totalPages)
      result.items mustEqual pagination.items
      result.previous mustEqual pagination.previous
      result.next mustEqual pagination.next

    }

    "when currentPage 1" - {
      val currentPage = 1

      "when totalPages is 1" in {
        val totalPages = 1
        val items      = Seq(
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = None,
          next = None
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 2" in {
        val totalPages = 2
        val nextPage   = 2
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = None,
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 3" in {
        val totalPages = 3
        val nextPage   = 2
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some("3"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(3).url,
            ellipsis = Some(true)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = None,
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 4" in {
        val totalPages = 4
        val nextPage   = 2
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some("3"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(3).url,
            ellipsis = Some(true)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = None,
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

    }

    "when currentPage 2" - {
      val currentPage = 2

      "when totalPages is 2" in {
        val totalPages = 2
        val prevPage   = 1
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = None
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 3" in {
        val totalPages = 3
        val nextPage   = 3
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val prevPage   = 1
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 4" in {
        val totalPages = 4
        val nextPage   = 3
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val prevPage   = 1
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some("4"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(4).url,
            ellipsis = Some(true)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 5" in {
        val totalPages = 5
        val nextPage   = 3
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val prevPage   = 1
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some("4"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(4).url,
            ellipsis = Some(true)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

    }

    "when currentPage 3" - {
      val currentPage = 3

      "when totalPages is 3" in {
        val totalPages = 3
        val prevPage   = 2
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some("1"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(1).url,
            ellipsis = Some(true)
          ),
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = None
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 4" in {
        val totalPages = 4
        val nextPage   = 4
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val prevPage   = 2
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some("1"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(1).url,
            ellipsis = Some(true)
          ),
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 5" in {
        val totalPages = 5
        val nextPage   = 4
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val prevPage   = 2
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some("1"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(1).url,
            ellipsis = Some(true)
          ),
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some("5"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(5).url,
            ellipsis = Some(true)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 6" in {
        val totalPages = 6
        val nextPage   = 4
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val prevPage   = 2
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some("1"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(1).url,
            ellipsis = Some(true)
          ),
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some("5"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(5).url,
            ellipsis = Some(true)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

    }

    "when currentPage 4" - {
      val currentPage = 4

      "when totalPages is 4" in {
        val totalPages = 4
        val prevPage   = 3
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some("2"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(2).url,
            ellipsis = Some(true)
          ),
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = None
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 5" in {
        val totalPages = 5
        val nextPage   = 5
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val prevPage   = 3
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some("2"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(2).url,
            ellipsis = Some(true)
          ),
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 6" in {
        val totalPages = 6
        val nextPage   = 5
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val prevPage   = 3
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some("2"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(2).url,
            ellipsis = Some(true)
          ),
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some("6"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(6).url,
            ellipsis = Some(true)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

      "when totalPages is 7" in {
        val totalPages = 7
        val nextPage   = 5
        val next       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(nextPage).url
        )
        val prevPage   = 3
        val prev       = PaginationLink(
          routes.GoodsRecordsController.onPageLoad(prevPage).url
        )
        val items      = Seq(
          PaginationItem(
            number = Some("2"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(2).url,
            ellipsis = Some(true)
          ),
          PaginationItem(
            number = Some(prevPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(prevPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(currentPage.toString),
            current = Some(true),
            href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some(nextPage.toString),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(nextPage).url,
            ellipsis = Some(false)
          ),
          PaginationItem(
            number = Some("6"),
            current = Some(false),
            href = routes.GoodsRecordsController.onPageLoad(6).url,
            ellipsis = Some(true)
          )
        )
        val pagination = Pagination(
          items = Some(items),
          previous = Some(prev),
          next = Some(next)
        )
        val result     = GoodsRecordsPagination.getPagination(currentPage, totalPages)
        result.items mustEqual pagination.items
        result.previous mustEqual pagination.previous
        result.next mustEqual pagination.next
      }

    }

  }
  ".getFirstRecord" - {
    "when firstRecord is 0 lastRecord is 0" in {
      val firstRecord = 0
      val len         = 100
      val result      = GoodsRecordsPagination.getLastRecord(firstRecord, len)
      result mustEqual 0
    }
    "when firstRecord and len is 1 lastRecord is 1" in {
      val firstRecord = 1
      val len         = 1
      val result      = GoodsRecordsPagination.getLastRecord(firstRecord, len)
      result mustEqual 1
    }
    "when firstRecord is bigger than 1(101)" - {
      val firstRecord = 101

      "when len is 1 lastRecord is 101" in {
        val len    = 1
        val result = GoodsRecordsPagination.getLastRecord(firstRecord, len)
        result mustEqual 101
      }
      "when len is bigger than 1(2) lastRecord is 102" in {
        val len    = 2
        val result = GoodsRecordsPagination.getLastRecord(firstRecord, len)
        result mustEqual 102
      }
    }
  }

  ".getFirstRecord" - {
    "when totalPages is 1" - {
      val totalPages = 1

      "when currentPage is 1" - {
        val currentPage = 1

        "when totalRecords are 1 first record is 1" in {
          val totalRecords = 1
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 1
        }

        "when totalRecords are 0 first record is 0" in {
          val totalRecords = 0
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 0
        }

        "when totalRecords are 5 first record is 1" in {
          val totalRecords = 5
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 1
        }
      }

      "when currentPage beyond total pages" - {
        val currentPage = 2

        "when totalRecords are 1 first record is 0" in {
          val totalRecords = 1
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 0
        }

        "when totalRecords are 0 first record is 0" in {
          val totalRecords = 0
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 0
        }

        "when totalRecords are 5 first record is 0" in {
          val totalRecords = 5
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 0
        }
      }

    }

    "when totalPages is 2" - {
      val totalPages = 2

      "when currentPage is 1" - {
        val currentPage = 1

        "when totalRecords are 5 first record is 1" in {
          val totalRecords = 5
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 1
        }
      }

      "when currentPage is 2" - {
        val currentPage = 2

        "when totalRecords are less than the pageSize and the currentPage is larger than 1 first record is 0" in {
          val totalRecords = 5
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 0
        }

        "when totalRecords are equal to the pageSize and the currentPage is larger than 1 first record is 0" in {
          val totalRecords = 10
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 0
        }

        "when totalRecords are 11 first record is 11" in {
          val totalRecords = 11
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 11
        }
      }

    }

    "when totalPages is 100" - {
      val totalPages = 100

      "when currentPage is 1" - {
        val currentPage = 1

        "when totalRecords are 999 first record is 1" in {
          val totalRecords = 999
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 1
        }
      }

      "when currentPage is 2" - {
        val currentPage = 2

        "when totalRecords are 999 first record is 11" in {
          val totalRecords = 999
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 11
        }
      }
      "when currentPage is 97" - {
        val currentPage = 97

        "when totalRecords are 999 first record is 961" in {
          val totalRecords = 999
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 961
        }
      }

      "when currentPage is 100" - {
        val currentPage = 100

        "when totalRecords are 999 first record is 991" in {
          val totalRecords = 999
          val pagination   = GoodsRecordsPagination(totalRecords, currentPage, totalPages, None, None)
          val result       = GoodsRecordsPagination.getFirstRecord(pagination, pageSize)
          result mustEqual 991
        }
      }
    }

  }
}
