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
import config.FrontendAppConfig
import connectors.{GoodsRecordConnector, OttConnector}
import forms.goodsProfile.GoodsRecordsFormProvider
import models.GoodsRecordsPagination.firstPage
import models.router.responses.GetRecordsResponse
import models.{Country, GoodsRecordsPagination, SearchForm}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.Pagination
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{PaginationItem, PaginationLink}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.goodsProfile.{GoodsRecordsEmptyView, GoodsRecordsView}

import java.time.Instant
import scala.concurrent.Future

class GoodsRecordsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val formProvider                         = new GoodsRecordsFormProvider()
  private val form                         = formProvider()
  private val currentPage                  = firstPage
  private val totalRecords                 = 23
  private val numberOfPages                = 3
  private val firstRecord                  = 1
  private val lastRecord                   = 10
  private val pageSize                     = 10
  private lazy val goodsRecordsRoute       =
    controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(currentPage).url
  private lazy val goodsRecordsSearchRoute =
    controllers.goodsProfile.routes.GoodsRecordsController.onSearch(currentPage).url

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
          href = controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(currentPage).url,
          ellipsis = Some(false)
        ),
        PaginationItem(
          number = Some((1 + currentPage).toString),
          current = Some(false),
          href = controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(1 + currentPage).url,
          ellipsis = Some(false)
        ),
        PaginationItem(
          number = Some((2 + currentPage).toString),
          current = Some(false),
          href = controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(2 + currentPage).url,
          ellipsis = Some(true)
        )
      )
    ),
    previous = None,
    next = Some(PaginationLink(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(1 + currentPage).url))
  )

  private val mockGoodsRecordConnector = mock[GoodsRecordConnector]
  private val mockOttConnector         = mock[OttConnector]
  private val mockSessionRepository    = mock[SessionRepository]
  private val mockFrontendAppConfig    = mock[FrontendAppConfig]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGoodsRecordConnector, mockOttConnector, mockSessionRepository, mockFrontendAppConfig)
  }

  "GoodsRecords Controller" - {
    "must return OK and the correct view for a GET with records and latest records are stored" - {
      "when file is not requested" in {
        when(mockGoodsRecordConnector.getRecords(eqTo(currentPage), any())(any())) thenReturn Future.successful(
          Some(response)
        )
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("EC", "Ecuador")))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, goodsRecordsRoute)
          val result  = route(application, request).value

          val view            = application.injector.instanceOf[GoodsRecordsView]
          val emptySearchForm = SearchForm(None, None, List.empty)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            response.goodsItemRecords,
            response.pagination,
            firstRecord,
            lastRecord,
            Seq(Country("EC", "Ecuador")),
            pagination,
            currentPage,
            pageSize,
            emptySearchForm,
            None
          )(request, messages(application)).toString

          verify(mockOttConnector, atLeastOnce()).getCountries(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(eqTo(currentPage), any())(any())
        }
      }

      "when file is not requested but has been requested historically" in {
        when(mockGoodsRecordConnector.getRecords(eqTo(currentPage), any())(any())) thenReturn Future.successful(
          Some(response)
        )
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("EC", "Ecuador")))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, goodsRecordsRoute)
          val result  = route(application, request).value

          val view            = application.injector.instanceOf[GoodsRecordsView]
          val emptySearchForm = SearchForm(None, None, List.empty)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            response.goodsItemRecords,
            response.pagination,
            firstRecord,
            lastRecord,
            Seq(Country("EC", "Ecuador")),
            pagination,
            currentPage,
            pageSize,
            emptySearchForm,
            None
          )(request, messages(application)).toString
          verify(mockOttConnector, atLeastOnce()).getCountries(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(eqTo(currentPage), any())(any())
        }
      }

      "when file is in progress" in {
        when(mockGoodsRecordConnector.getRecords(eqTo(currentPage), any())(any())) thenReturn Future.successful(
          Some(response)
        )
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("EC", "Ecuador")))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, goodsRecordsRoute)
          val result  = route(application, request).value

          val view            = application.injector.instanceOf[GoodsRecordsView]
          val emptySearchForm = SearchForm(None, None, List.empty)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            response.goodsItemRecords,
            response.pagination,
            firstRecord,
            lastRecord,
            Seq(Country("EC", "Ecuador")),
            pagination,
            currentPage,
            pageSize,
            emptySearchForm,
            None
          )(request, messages(application)).toString

          verify(mockOttConnector, atLeastOnce()).getCountries(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(eqTo(currentPage), any())(any())
        }
      }

      "when file is ready" in {
        when(mockGoodsRecordConnector.getRecords(eqTo(currentPage), any())(any())) thenReturn Future.successful(
          Some(response)
        )
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("EC", "Ecuador")))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

        running(application) {
          val request         = FakeRequest(GET, goodsRecordsRoute)
          val result          = route(application, request).value
          val view            = application.injector.instanceOf[GoodsRecordsView]
          val emptySearchForm = SearchForm(None, None, List.empty)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            response.goodsItemRecords,
            response.pagination,
            firstRecord,
            lastRecord,
            Seq(Country("EC", "Ecuador")),
            pagination,
            currentPage,
            pageSize,
            emptySearchForm,
            None
          )(request, messages(application)).toString

          verify(mockOttConnector, atLeastOnce()).getCountries(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(eqTo(currentPage), any())(any())
        }
      }
    }

    "must redirect to Loading page if the records needing to be stored is more than one batch (will take time)" in {
      when(mockGoodsRecordConnector.getRecords(eqTo(currentPage), any())(any())) thenReturn Future.successful(None)
      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("EC", "Ecuador")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
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
        verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(eqTo(currentPage), any())(any())
      }
    }

    "must return OK and the correct view for a GET with records and latest records are stored when it is a middle page" in {
      val middlePage = 2
      val response   =
        GetRecordsResponse(records, GoodsRecordsPagination(totalRecords, middlePage, numberOfPages, None, None))
      when(mockGoodsRecordConnector.getRecords(eqTo(middlePage), any())(any())) thenReturn Future.successful(
        Some(response)
      )
      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("EC", "Ecuador")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      val middleGoodsRecordsRoute = controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(middlePage).url
      val pagination              = Pagination(
        items = Option(
          Seq(
            PaginationItem(
              number = Some((middlePage - 1).toString),
              current = Some(false),
              href = controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(middlePage - 1).url,
              ellipsis = Some(false)
            ),
            PaginationItem(
              number = Some(middlePage.toString),
              current = Some(true),
              href = controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(middlePage).url,
              ellipsis = Some(false)
            ),
            PaginationItem(
              number = Some((1 + middlePage).toString),
              current = Some(false),
              href = controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(1 + middlePage).url,
              ellipsis = Some(false)
            )
          )
        ),
        previous =
          Some(PaginationLink(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(middlePage - 1).url)),
        next =
          Some(PaginationLink(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(1 + middlePage).url))
      )

      running(application) {
        val request         = FakeRequest(GET, middleGoodsRecordsRoute)
        val result          = route(application, request).value
        val view            = application.injector.instanceOf[GoodsRecordsView]
        val firstRecord     = 11
        val lastRecord      = 20
        val emptySearchForm = SearchForm(None, None, List.empty)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          response.goodsItemRecords,
          response.pagination,
          firstRecord,
          lastRecord,
          Seq(Country("EC", "Ecuador")),
          pagination,
          middlePage,
          pageSize,
          emptySearchForm,
          None
        )(request, messages(application)).toString

        verify(mockOttConnector, atLeastOnce()).getCountries(any())
        verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(eqTo(middlePage), any())(any())
      }
    }

    "must return OK and the correct view for a GET without records" in {
      when(mockGoodsRecordConnector.getRecords(eqTo(currentPage), any())(any())) thenReturn Future.successful(
        Some(GetRecordsResponse(Seq.empty, GoodsRecordsPagination(0, 1, 0, None, None)))
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
        )
        .build()

      val view = application.injector.instanceOf[GoodsRecordsEmptyView]

      running(application) {
        val request = FakeRequest(GET, goodsRecordsRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString

        verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(eqTo(currentPage), any())(any())
      }
    }

    "must redirect to JourneyRecovery when page number is less than 1" in {
      val badPageRoute = controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(0).url

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, badPageRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to result page when valid data is submitted via onSearch" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[FrontendAppConfig].toInstance(mockFrontendAppConfig)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, goodsRecordsSearchRoute).withFormUrlEncodedBody(("value", "answer"))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.goodsProfile.routes.GoodsRecordsController
          .onPageLoadFilter(1)
          .url
      }
    }

    "must redirect to result page when invalid data is submitted via onSearch" in {
      when(mockGoodsRecordConnector.getRecords(eqTo(currentPage), any())(any())) thenReturn Future.successful(
        Some(response)
      )
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[FrontendAppConfig].toInstance(mockFrontendAppConfig)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, goodsRecordsSearchRoute).withFormUrlEncodedBody(("value", "answer"))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.goodsProfile.routes.GoodsRecordsController
          .onPageLoadFilter(1)
          .url
      }
    }
  }
}
