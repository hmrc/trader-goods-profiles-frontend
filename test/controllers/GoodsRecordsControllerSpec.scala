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
import forms.GoodsRecordsFormProvider
import models.router.responses.{GetGoodsRecordResponse, GetRecordsResponse}
import models.{Country, GoodsRecordsPagination, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.GoodsRecordsPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.Pagination
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{PaginationItem, PaginationLink}
import views.html.{GoodsRecordsEmptyView, GoodsRecordsView}

import java.time.Instant
import scala.concurrent.Future

class GoodsRecordsControllerSpec extends SpecBase with MockitoSugar {

  val formProvider                   = new GoodsRecordsFormProvider()
  private val form                   = formProvider()
  private val currentPage            = 1
  private val totalRecords           = 23
  private val numberOfPages          = 3
  private val firstRecord            = 1
  private val lastRecord             = 10
  private lazy val goodsRecordsRoute = routes.GoodsRecordsController.onPageLoad(currentPage).url

  private val records = Seq(
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "EC",
      "BAN0010011",
      "Organic bananas",
      "Not requested",
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "EC",
      "BAN0010011",
      "Organic bananas",
      "Not requested",
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "EC",
      "BAN0010011",
      "Organic bananas",
      "Not requested",
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "EC",
      "BAN0010011",
      "Organic bananas",
      "Not requested",
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    GetGoodsRecordResponse(
      "2",
      "10410100",
      "EC",
      "BAN0010012",
      "Organic bananas",
      "Not requested",
      Instant.parse("2023-11-18T23:20:19Z"),
      Instant.parse("2023-11-18T23:20:19Z")
    ),
    GetGoodsRecordResponse(
      "3",
      "10410100",
      "EC",
      "BAN0010013",
      "Organic bananas",
      "Not requested",
      Instant.parse("2024-11-18T23:20:19Z"),
      Instant.parse("2024-11-18T23:20:19Z")
    ),
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "EC",
      "BAN0010011",
      "Organic bananas",
      "Not requested",
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "EC",
      "BAN0010011",
      "Organic bananas",
      "Not requested",
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "EC",
      "BAN0010011",
      "Organic bananas",
      "Not requested",
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ),
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "EC",
      "BAN0010011",
      "Organic bananas",
      "Not requested",
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    )
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
          href = routes.GoodsRecordsController.onPageLoad(currentPage).url,
          ellipsis = Some(false)
        ),
        PaginationItem(
          number = Some((1 + currentPage).toString),
          current = Some(false),
          href = routes.GoodsRecordsController.onPageLoad(1 + currentPage).url,
          ellipsis = Some(false)
        ),
        PaginationItem(
          number = Some((2 + currentPage).toString),
          current = Some(false),
          href = routes.GoodsRecordsController.onPageLoad(2 + currentPage).url,
          ellipsis = Some(true)
        )
      )
    ),
    previous = None,
    next = Some(PaginationLink(routes.GoodsRecordsController.onPageLoad(1 + currentPage).url))
  )

  "GoodsRecords Controller" - {

    "must return OK and the correct view for a GET with records and latest records are stored" in {

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]

      when(mockGoodsRecordConnector.getRecords(eqTo(testEori), eqTo(currentPage), any())(any())) thenReturn Future
        .successful(response)
      when(mockGoodsRecordConnector.getRecordsCount(eqTo(testEori))(any())) thenReturn Future
        .successful(totalRecords)

      when(mockGoodsRecordConnector.storeLatestRecords(eqTo(testEori))(any())) thenReturn Future
        .successful(Done)

      val mockOttConnector = mock[OttConnector]
      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
        Seq(Country("EC", "Ecuador"))
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, goodsRecordsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GoodsRecordsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          response.goodsItemRecords,
          totalRecords,
          firstRecord,
          lastRecord,
          Seq(Country("EC", "Ecuador")),
          pagination,
          currentPage
        )(
          request,
          messages(application)
        ).toString
        verify(mockGoodsRecordConnector, times(1)).storeLatestRecords(any())(any())
      }
    }

    "must return OK and the correct view for a GET with records and latest records are stored when it is a middle page" in {

      val middlePage = 2

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]
      val response                 = GetRecordsResponse(
        records,
        GoodsRecordsPagination(totalRecords, middlePage, numberOfPages, None, None)
      )
      when(mockGoodsRecordConnector.getRecords(eqTo(testEori), eqTo(middlePage), any())(any())) thenReturn Future
        .successful(response)
      when(mockGoodsRecordConnector.getRecordsCount(eqTo(testEori))(any())) thenReturn Future
        .successful(totalRecords)

      when(mockGoodsRecordConnector.storeLatestRecords(eqTo(testEori))(any())) thenReturn Future
        .successful(Done)

      val mockOttConnector = mock[OttConnector]
      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
        Seq(Country("EC", "Ecuador"))
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      val middleGoodsRecordsRoute = routes.GoodsRecordsController.onPageLoad(middlePage).url

      val pagination = Pagination(
        items = Option(
          Seq(
            PaginationItem(
              number = Some((middlePage - 1).toString),
              current = Some(false),
              href = routes.GoodsRecordsController.onPageLoad(middlePage - 1).url,
              ellipsis = Some(false)
            ),
            PaginationItem(
              number = Some(middlePage.toString),
              current = Some(true),
              href = routes.GoodsRecordsController.onPageLoad(middlePage).url,
              ellipsis = Some(false)
            ),
            PaginationItem(
              number = Some((1 + middlePage).toString),
              current = Some(false),
              href = routes.GoodsRecordsController.onPageLoad(1 + middlePage).url,
              ellipsis = Some(false)
            )
          )
        ),
        previous = Some(PaginationLink(routes.GoodsRecordsController.onPageLoad(middlePage - 1).url)),
        next = Some(PaginationLink(routes.GoodsRecordsController.onPageLoad(1 + middlePage).url))
      )

      running(application) {
        val request = FakeRequest(GET, middleGoodsRecordsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GoodsRecordsView]

        val firstRecord = 11
        val lastRecord  = 20

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          response.goodsItemRecords,
          totalRecords,
          firstRecord,
          lastRecord,
          Seq(Country("EC", "Ecuador")),
          pagination,
          middlePage
        )(
          request,
          messages(application)
        ).toString
        verify(mockGoodsRecordConnector, times(1)).storeLatestRecords(any())(any())
      }
    }

    "must return OK and the correct view for a GET without records" in {

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]
      when(mockGoodsRecordConnector.getRecordsCount(eqTo(testEori))(any())) thenReturn Future
        .successful(0)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, goodsRecordsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.GoodsRecordsController.onPageLoadNoRecords().url
      }
    }

    "must return OK and the empty view onPageLoadNoRecords" in {
      val emptyGoodsRecordsRoute = routes.GoodsRecordsController.onPageLoadNoRecords().url

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()
      val view        = application.injector.instanceOf[GoodsRecordsEmptyView]

      running(application) {
        val request = FakeRequest(GET, emptyGoodsRecordsRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }

    "must redirect to JourneyRecovery when page number is less than 1" in {
      val badPageRoute = routes.GoodsRecordsController.onPageLoad(0).url

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, badPageRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the search has previously been filled in" in {

      val userAnswers = UserAnswers(userAnswersId).set(GoodsRecordsPage, "answer").success.value

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]

      when(mockGoodsRecordConnector.getRecords(eqTo(testEori), eqTo(currentPage), any())(any())) thenReturn Future
        .successful(response)
      when(mockGoodsRecordConnector.getRecordsCount(eqTo(testEori))(any())) thenReturn Future
        .successful(response.goodsItemRecords.size)

      when(mockGoodsRecordConnector.storeLatestRecords(eqTo(testEori))(any())) thenReturn Future
        .successful(Done)

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

        val view = application.injector.instanceOf[GoodsRecordsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill("answer"),
          response.goodsItemRecords,
          totalRecords,
          firstRecord,
          lastRecord,
          Seq(Country("EC", "Ecuador")),
          pagination,
          currentPage
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must refresh page when valid data is submitted via onSearch" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]

      when(mockGoodsRecordConnector.getRecords(eqTo(testEori), eqTo(currentPage), any())(any())) thenReturn Future
        .successful(response)

      val mockOttConnector = mock[OttConnector]
      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
        Seq(Country("EC", "Ecuador"))
      )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, goodsRecordsRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        val view = application.injector.instanceOf[GoodsRecordsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill("answer"),
          response.goodsItemRecords,
          totalRecords,
          firstRecord,
          lastRecord,
          Seq(Country("EC", "Ecuador")),
          pagination,
          currentPage
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, goodsRecordsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[GoodsRecordsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          Seq.empty,
          0,
          0,
          0,
          Seq.empty,
          Pagination(),
          currentPage
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, goodsRecordsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, goodsRecordsRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
