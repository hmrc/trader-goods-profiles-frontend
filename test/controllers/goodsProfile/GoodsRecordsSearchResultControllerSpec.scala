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

import base.SpecBase
import base.TestConstants.{testEori, userAnswersId}
import connectors.{GoodsRecordConnector, OttConnector}
import models.GoodsRecordsPagination.firstPage
import models.router.responses.GetRecordsResponse
import models.{Country, GoodsRecordsPagination, SearchForm, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsProfile.GoodsRecordsPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.Aliases.Pagination
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{PaginationItem, PaginationLink}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.goodsProfile.{GoodsRecordsSearchResultEmptyView, GoodsRecordsSearchResultView}

import java.time.Instant
import scala.concurrent.Future

class GoodsRecordsSearchResultControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val currentPage            = firstPage
  private val totalRecords           = 23
  private val numberOfPages          = 3
  private val firstRecord            = 1
  private val lastRecord             = 10
  private lazy val goodsRecordsRoute =
    controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(currentPage).url

  private val searchText = "bananas"

  private val searchFormData = SearchForm(
    searchTerm = Some("bananas"),
    statusValue = Seq.empty,
    countryOfOrigin = None
  )

  private val records = Seq(
    goodsRecordResponse(Instant.parse("2022-11-18T23:20:19Z"), Instant.parse("2022-11-18T23:20:19Z")),
    goodsRecordResponse(Instant.parse("2022-11-18T23:20:19Z"), Instant.parse("2022-11-18T23:20:19Z")),
    goodsRecordResponse(Instant.parse("2022-11-18T23:20:19Z"), Instant.parse("2022-11-18T23:20:19Z")),
    goodsRecordResponse(Instant.parse("2022-11-18T23:20:19Z"), Instant.parse("2022-11-18T23:20:19Z")),
    goodsRecordResponse(Instant.parse("2023-11-18T23:20:19Z"), Instant.parse("2023-11-18T23:20:19Z")),
    goodsRecordResponse(Instant.parse("2024-11-18T23:20:19Z"), Instant.parse("2024-11-18T23:20:19Z")),
    goodsRecordResponse(Instant.parse("2022-11-18T23:20:19Z"), Instant.parse("2022-11-18T23:20:19Z")),
    goodsRecordResponse(Instant.parse("2022-11-18T23:20:19Z"), Instant.parse("2022-11-18T23:20:19Z")),
    goodsRecordResponse(Instant.parse("2022-11-18T23:20:19Z"), Instant.parse("2022-11-18T23:20:19Z")),
    goodsRecordResponse(Instant.parse("2022-11-18T23:20:19Z"), Instant.parse("2022-11-18T23:20:19Z"))
  )

  private val response =
    GetRecordsResponse(records, GoodsRecordsPagination(totalRecords, currentPage, numberOfPages, None, None))

  private val pagination = Pagination(
    items = Option(
      Seq(
        PaginationItem(
          number = Some(currentPage.toString),
          current = Some(true),
          href = controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(currentPage).url,
          ellipsis = Some(false)
        ),
        PaginationItem(
          number = Some((1 + currentPage).toString),
          current = Some(false),
          href = controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(1 + currentPage).url,
          ellipsis = Some(false)
        ),
        PaginationItem(
          number = Some((2 + currentPage).toString),
          current = Some(false),
          href = controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(2 + currentPage).url,
          ellipsis = Some(true)
        )
      )
    ),
    previous = None,
    next = Some(
      PaginationLink(controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(1 + currentPage).url)
    )
  )

  private val mockGoodsRecordConnector = mock[GoodsRecordConnector]
  private val mockOttConnector         = mock[OttConnector]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGoodsRecordConnector, mockOttConnector)
  }

  "GoodsRecordsSearch Controller" - {
    "must return OK and the correct view for a GET with search results records" in {
      val userAnswers = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchFormData).success.value

      when(
        mockGoodsRecordConnector.searchRecords(
          eqTo(testEori),
          eqTo(Some(searchText)),
          any(),
          eqTo(Some("")),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(currentPage),
          any()
        )(any())
      ) thenReturn Future.successful(Some(response))

      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("EC", "Ecuador")))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, goodsRecordsRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[GoodsRecordsSearchResultView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          response.goodsItemRecords,
          totalRecords,
          firstRecord,
          lastRecord,
          Seq(Country("EC", "Ecuador")),
          pagination,
          currentPage,
          Some(searchText),
          numberOfPages
        )(request, messages(application)).toString

        verify(mockOttConnector, atLeastOnce()).getCountries(any())
        verify(mockGoodsRecordConnector, atLeastOnce())
          .searchRecords(
            eqTo(testEori),
            eqTo(Some(searchText)),
            any(),
            eqTo(Some("")),
            eqTo(Some(false)),
            eqTo(Some(false)),
            eqTo(Some(false)),
            eqTo(currentPage),
            any()
          )(any())
      }
    }

    "must redirect to the loading page when the records need updating" in {
      val userAnswers = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchFormData).success.value

      when(
        mockGoodsRecordConnector.searchRecords(
          eqTo(testEori),
          eqTo(Some(searchText)),
          any(),
          eqTo(Some("")),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(currentPage),
          any()
        )(any())
      ) thenReturn Future.successful(None)

      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("EC", "Ecuador")))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, goodsRecordsRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.goodsProfile.routes.GoodsRecordsLoadingController
          .onPageLoad(Some(RedirectUrl(goodsRecordsRoute)))
          .url

        verify(mockOttConnector, never()).getCountries(any())
        verify(mockGoodsRecordConnector, atLeastOnce())
          .searchRecords(
            eqTo(testEori),
            eqTo(Some(searchText)),
            any(),
            eqTo(Some("")),
            eqTo(Some(false)),
            eqTo(Some(false)),
            eqTo(Some(false)),
            eqTo(currentPage),
            any()
          )(any())
      }
    }

    "must return OK and the correct view for a GET with  search results records when it is a middle page" in {
      val middlePage               = 2
      val userAnswers              = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchFormData).success.value
      val mockGoodsRecordConnector = mock[GoodsRecordConnector]
      val response                 =
        GetRecordsResponse(records, GoodsRecordsPagination(totalRecords, middlePage, numberOfPages, None, None))

      when(
        mockGoodsRecordConnector.searchRecords(
          eqTo(testEori),
          eqTo(Some(searchText)),
          any(),
          eqTo(Some("")),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(middlePage),
          any()
        )(any())
      ) thenReturn Future.successful(Some(response))

      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("EC", "Ecuador")))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      val middleGoodsRecordsRoute =
        controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(middlePage).url

      val pagination = Pagination(
        items = Option(
          Seq(
            PaginationItem(
              number = Some((middlePage - 1).toString),
              current = Some(false),
              href = controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(middlePage - 1).url,
              ellipsis = Some(false)
            ),
            PaginationItem(
              number = Some(middlePage.toString),
              current = Some(true),
              href = controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(middlePage).url,
              ellipsis = Some(false)
            ),
            PaginationItem(
              number = Some((1 + middlePage).toString),
              current = Some(false),
              href = controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(1 + middlePage).url,
              ellipsis = Some(false)
            )
          )
        ),
        previous = Some(
          PaginationLink(
            controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(middlePage - 1).url
          )
        ),
        next = Some(
          PaginationLink(
            controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(1 + middlePage).url
          )
        )
      )

      running(application) {
        val request = FakeRequest(GET, middleGoodsRecordsRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[GoodsRecordsSearchResultView]

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
          Some(searchText),
          numberOfPages
        )(request, messages(application)).toString

        verify(mockOttConnector, atLeastOnce()).getCountries(any())
        verify(mockGoodsRecordConnector, atLeastOnce()).searchRecords(
          eqTo(testEori),
          eqTo(Some(searchText)),
          any(),
          eqTo(Some("")),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(middlePage),
          any()
        )(any())
      }
    }

    "must redirect to no records page GET without records" in {
      val emptyResultResponse = GetRecordsResponse(Seq.empty, GoodsRecordsPagination(0, 0, 1, None, None))
      val userAnswers         = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchFormData).success.value

      when(
        mockGoodsRecordConnector.searchRecords(
          eqTo(testEori),
          eqTo(Some(searchText)),
          any(),
          eqTo(Some("")),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(currentPage),
          any()
        )(any())
      ) thenReturn Future.successful(Some(emptyResultResponse))

      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("EC", "Ecuador")))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      val view = application.injector.instanceOf[GoodsRecordsSearchResultEmptyView]

      running(application) {
        val request = FakeRequest(GET, goodsRecordsRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(searchFormData.searchTerm)(request, messages(application)).toString

        verify(mockOttConnector, never()).getCountries(any())
        verify(mockGoodsRecordConnector, atLeastOnce()).searchRecords(
          eqTo(testEori),
          eqTo(Some(searchText)),
          any(),
          eqTo(Some("")),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(Some(false)),
          eqTo(currentPage),
          any()
        )(any())
      }
    }

    "must redirect to JourneyRecovery when search text is not defined in onPageLoad" in {
      val badPageRoute = controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(1).url
      val application  = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, badPageRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery when page number is less than 1" in {
      val userAnswers  = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchFormData).success.value
      val badPageRoute = controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(0).url
      val application  = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, badPageRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
