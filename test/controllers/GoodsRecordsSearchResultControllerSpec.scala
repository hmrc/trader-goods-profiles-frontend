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

import base.SpecBase
import base.TestConstants.{testEori, userAnswersId}
import connectors.{GoodsRecordConnector, OttConnector}
import models.GoodsRecordsPagination.firstPage
import models.router.responses.GetRecordsResponse
import models.{Country, GoodsRecordsPagination, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.GoodsRecordsPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.Aliases.Pagination
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{PaginationItem, PaginationLink}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.{GoodsRecordsSearchResultEmptyView, GoodsRecordsSearchResultView}

import java.time.Instant
import scala.concurrent.Future

class GoodsRecordsSearchResultControllerSpec extends SpecBase with MockitoSugar {

  private val currentPage            = firstPage
  private val totalRecords           = 23
  private val numberOfPages          = 3
  private val firstRecord            = 1
  private val lastRecord             = 10
  private lazy val goodsRecordsRoute = routes.GoodsRecordsSearchResultController.onPageLoad(currentPage).url

  val searchText = "bananas"

  private val records = Seq(
    goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    goodsRecordResponse(
      Instant.parse("2023-11-18T23:20:19Z"),
      Instant.parse("2023-11-18T23:20:19Z")
    ),
    goodsRecordResponse(
      Instant.parse("2024-11-18T23:20:19Z"),
      Instant.parse("2024-11-18T23:20:19Z")
    ),
    goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    goodsRecordResponse(Instant.parse("2022-11-18T23:20:19Z"), Instant.parse("2022-11-18T23:20:19Z"))
  )

  private val response = GetRecordsResponse(
    records,
    GoodsRecordsPagination(totalRecords, currentPage, numberOfPages, None, None)
  )

  private val pagination = Pagination(
    items = Option(
      Seq(
        PaginationItem(
          number = Some(currentPage.toString),
          current = Some(true),
          href = routes.GoodsRecordsSearchResultController.onPageLoad(currentPage).url,
          ellipsis = Some(false)
        ),
        PaginationItem(
          number = Some((1 + currentPage).toString),
          current = Some(false),
          href = routes.GoodsRecordsSearchResultController.onPageLoad(1 + currentPage).url,
          ellipsis = Some(false)
        ),
        PaginationItem(
          number = Some((2 + currentPage).toString),
          current = Some(false),
          href = routes.GoodsRecordsSearchResultController.onPageLoad(2 + currentPage).url,
          ellipsis = Some(true)
        )
      )
    ),
    previous = None,
    next = Some(PaginationLink(routes.GoodsRecordsSearchResultController.onPageLoad(1 + currentPage).url))
  )

  "GoodsRecordsSearch Controller" - {

    "must return OK and the correct view for a GET with search results records" in {

      val userAnswers = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchText).success.value

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]

      when(
        mockGoodsRecordConnector.searchRecords(eqTo(testEori), eqTo(searchText), any(), eqTo(currentPage), any())(any())
      ) thenReturn Future
        .successful(Some(response))

      val mockOttConnector = mock[OttConnector]
      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
        Seq(Country("EC", "Ecuador"))
      )

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, goodsRecordsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GoodsRecordsSearchResultView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          response.goodsItemRecords,
          totalRecords,
          firstRecord,
          lastRecord,
          Seq(Country("EC", "Ecuador")),
          pagination,
          currentPage,
          searchText,
          numberOfPages
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the loading page when the records need updating" in {

      val userAnswers = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchText).success.value

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]

      when(
        mockGoodsRecordConnector.searchRecords(eqTo(testEori), eqTo(searchText), any(), eqTo(currentPage), any())(any())
      ) thenReturn Future
        .successful(None)

      val mockOttConnector = mock[OttConnector]
      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
        Seq(Country("EC", "Ecuador"))
      )

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, goodsRecordsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.GoodsRecordsLoadingController
          .onPageLoad(Some(RedirectUrl(goodsRecordsRoute)))
          .url
      }
    }

    "must return OK and the correct view for a GET with  search results records when it is a middle page" in {

      val middlePage = 2

      val userAnswers = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchText).success.value

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]
      val response                 = GetRecordsResponse(
        records,
        GoodsRecordsPagination(totalRecords, middlePage, numberOfPages, None, None)
      )
      when(
        mockGoodsRecordConnector.searchRecords(eqTo(testEori), eqTo(searchText), any(), eqTo(middlePage), any())(any())
      ) thenReturn Future
        .successful(Some(response))

      val mockOttConnector = mock[OttConnector]
      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
        Seq(Country("EC", "Ecuador"))
      )

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      val middleGoodsRecordsRoute = routes.GoodsRecordsSearchResultController.onPageLoad(middlePage).url

      val pagination = Pagination(
        items = Option(
          Seq(
            PaginationItem(
              number = Some((middlePage - 1).toString),
              current = Some(false),
              href = routes.GoodsRecordsSearchResultController.onPageLoad(middlePage - 1).url,
              ellipsis = Some(false)
            ),
            PaginationItem(
              number = Some(middlePage.toString),
              current = Some(true),
              href = routes.GoodsRecordsSearchResultController.onPageLoad(middlePage).url,
              ellipsis = Some(false)
            ),
            PaginationItem(
              number = Some((1 + middlePage).toString),
              current = Some(false),
              href = routes.GoodsRecordsSearchResultController.onPageLoad(1 + middlePage).url,
              ellipsis = Some(false)
            )
          )
        ),
        previous = Some(PaginationLink(routes.GoodsRecordsSearchResultController.onPageLoad(middlePage - 1).url)),
        next = Some(PaginationLink(routes.GoodsRecordsSearchResultController.onPageLoad(1 + middlePage).url))
      )

      running(application) {
        val request = FakeRequest(GET, middleGoodsRecordsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GoodsRecordsSearchResultView]

        val firstRecord = 11
        val lastRecord  = 20

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          response.goodsItemRecords,
          totalRecords,
          firstRecord,
          lastRecord,
          Seq(Country("EC", "Ecuador")),
          pagination,
          middlePage,
          searchText,
          numberOfPages
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to no records page GET without records" in {

      val emptyResultResponse = GetRecordsResponse(
        Seq.empty,
        GoodsRecordsPagination(0, 0, 1, None, None)
      )

      val userAnswers = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchText).success.value

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]
      when(
        mockGoodsRecordConnector.searchRecords(eqTo(testEori), eqTo(searchText), any(), eqTo(currentPage), any())(any())
      ) thenReturn Future
        .successful(Some(emptyResultResponse))

      val mockOttConnector = mock[OttConnector]
      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
        Seq(Country("EC", "Ecuador"))
      )

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()
      val view        = application.injector.instanceOf[GoodsRecordsSearchResultEmptyView]

      running(application) {
        val request = FakeRequest(GET, goodsRecordsRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(searchText)(request, messages(application)).toString
      }
    }

    "must redirect to JourneyRecovery when search text is not defined in onPageLoad" in {

      val badPageRoute = routes.GoodsRecordsSearchResultController.onPageLoad(1).url

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, badPageRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery when page number is less than 1" in {

      val userAnswers = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchText).success.value

      val badPageRoute = routes.GoodsRecordsSearchResultController.onPageLoad(0).url

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, badPageRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
