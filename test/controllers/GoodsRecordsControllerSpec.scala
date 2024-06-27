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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.GoodsRecordsPage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.{Pagination, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{PaginationItem, PaginationLink}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import viewmodels.govuk.table._
import views.html.{GoodsRecordsEmptyView, GoodsRecordsView}

import java.time.Instant
import scala.concurrent.Future

class GoodsRecordsControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new GoodsRecordsFormProvider()
  private val form = formProvider()

  private lazy val goodsRecordsRoute = routes.GoodsRecordsController.onPageLoad(1).url

  private val response = GetRecordsResponse(
    Seq(
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
      )
    ),
    GoodsRecordsPagination(10, 1, 4, None, None)
  )

  private val pagination = Pagination(
    items = Option(
      Seq(
        PaginationItem(
          number = Some("1"),
          current = Some(true),
          href = routes.GoodsRecordsController.onPageLoad(1).url
        ),
        PaginationItem(
          number = Some("2"),
          current = Some(false),
          href = routes.GoodsRecordsController.onPageLoad(2).url
        ),
        PaginationItem(
          number = Some("3"),
          current = Some(false),
          href = routes.GoodsRecordsController.onPageLoad(3).url
        ),
        PaginationItem(
          number = Some("4"),
          current = Some(false),
          href = routes.GoodsRecordsController.onPageLoad(4).url
        )
      )
    ),
    previous = None,
    next = Some(PaginationLink(routes.GoodsRecordsController.onPageLoad(2).url))
  )

  "GoodsRecords Controller" - {

    "must return OK and the correct view for a GET with records" in {

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]

      when(mockGoodsRecordConnector.getRecords(eqTo(testEori), eqTo(Some(1)), any())(any())) thenReturn Future
        .successful(response)

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
          10,
          1,
          3,
          Seq(Country("EC", "Ecuador")),
          pagination,
          1
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET without records" in {

      val emptyResponse = GetRecordsResponse(
        Seq.empty,
        GoodsRecordsPagination(0, 0, 0, None, None)
      )

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]

      when(mockGoodsRecordConnector.getRecords(eqTo(testEori), eqTo(Some(1)), any())(any())) thenReturn Future
        .successful(emptyResponse)

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

        val view = application.injector.instanceOf[GoodsRecordsEmptyView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the search has previously been filled in" in {

      val userAnswers = UserAnswers(userAnswersId).set(GoodsRecordsPage, "answer").success.value

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]

      when(mockGoodsRecordConnector.getRecords(eqTo(testEori), eqTo(Some(1)), any())(any())) thenReturn Future
        .successful(response)

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
          10,
          1,
          3,
          Seq(Country("EC", "Ecuador")),
          pagination,
          1
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

      when(mockGoodsRecordConnector.getRecords(eqTo(testEori), eqTo(Some(1)), any())(any())) thenReturn Future
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
          10,
          1,
          3,
          Seq(Country("EC", "Ecuador")),
          pagination,
          1
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]

      when(mockGoodsRecordConnector.getRecords(eqTo(testEori), eqTo(Some(1)), any())(any())) thenReturn Future
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
          1
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

    "must return OK and the correct view for a GET with records on a middle page" in {

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]
      val middlePageResponse       = response.copy(
        pagination = GoodsRecordsPagination(10, 2, 4, Some(3), Some(1))
      )

      when(mockGoodsRecordConnector.getRecords(eqTo(testEori), eqTo(Some(2)), any())(any())) thenReturn Future
        .successful(middlePageResponse)

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
        val request = FakeRequest(GET, routes.GoodsRecordsController.onPageLoad(2).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GoodsRecordsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          middlePageResponse.goodsItemRecords,
          10,
          3,
          5,
          Seq(Country("EC", "Ecuador")),
          GoodsRecordsPagination.getPagination(middlePageResponse.pagination),
          2
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET with records on the last page" in {

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]
      val lastPageResponse         = response.copy(
        pagination = GoodsRecordsPagination(10, 4, 4, None, Some(3))
      )

      when(mockGoodsRecordConnector.getRecords(eqTo(testEori), eqTo(Some(4)), any())(any())) thenReturn Future
        .successful(lastPageResponse)

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
        val request = FakeRequest(GET, routes.GoodsRecordsController.onPageLoad(4).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GoodsRecordsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          lastPageResponse.goodsItemRecords,
          10,
          7,
          9,
          Seq(Country("EC", "Ecuador")),
          GoodsRecordsPagination.getPagination(lastPageResponse.pagination),
          4
        )(
          request,
          messages(application)
        ).toString
      }
    }

  }
}
