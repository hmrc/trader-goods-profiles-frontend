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
import base.TestConstants.{testEori, testRecordId}
import connectors.{GoodsRecordConnector, OttConnector}
import models.{CheckMode, Country, UpdateGoodsRecord}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{CommodityCodeUpdatePage, CountryOfOriginUpdatePage, GoodsDescriptionUpdatePage, HasCorrectGoodsCommodityCodeUpdatePage, TraderReferenceUpdatePage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers._
import viewmodels.govuk.SummaryListFluency
import views.html.CyaUpdateRecordView
import queries.CountriesQuery
import repositories.SessionRepository

import scala.concurrent.Future

class CyaUpdateRecordControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaUpdateRecordController" - {

    "for Country of Origin Update" - {
      val summaryValue    = "China"
      val summaryKey      = "countryOfOrigin.checkYourAnswersLabel"
      val summaryHidden   = "countryOfOrigin.change.hidden"
      val summaryUrl      = routes.CountryOfOriginController.onPageLoadUpdate(CheckMode, testRecordId).url
      val page            = CountryOfOriginUpdatePage(testRecordId)
      val answer          = "CN"
      val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, countryOfOrigin = Some(answer))
      val getUrl          = routes.CyaUpdateRecordController.onPageLoadCountryOfOrigin(testRecordId).url
      val call            = routes.CyaUpdateRecordController.onSubmitCountryOfOrigin(testRecordId)
      val postUrl         = routes.CyaUpdateRecordController.onSubmitCountryOfOrigin(testRecordId).url

      "for a GET" - {

        def createChangeList(app: Application): SummaryList = SummaryListViewModel(
          rows = Seq(
            UpdateRecordSummary.row(summaryValue, summaryKey, summaryHidden, summaryUrl)(messages(app))
          )
        )

        "must return OK and the correct view with valid mandatory data getting countries from connector" in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value

          val mockOttConnector = mock[OttConnector]
          when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
            Seq(Country("CN", "China"))
          )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[OttConnector].toInstance(mockOttConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaUpdateRecordView]
            val list = createChangeList(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call)(
              request,
              messages(application)
            ).toString
          }
        }

        "must return OK and the correct view with valid mandatory data getting countries from query" in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value
            .set(CountriesQuery, Seq(Country("CN", "China")))
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaUpdateRecordView]
            val list = createChangeList(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val application = applicationBuilder(Some(emptyUserAnswers)).build()
          val continueUrl = RedirectUrl(routes.HomePageController.onPageLoad().url)

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "for a POST" - {

        "when user answers can create a valid update goods record" - {

          "must update the goods record, cleanse the data and redirect to the Home Page" in {

            val userAnswers = emptyUserAnswers
              .set(page, answer)
              .success
              .value

            val mockConnector = mock[GoodsRecordConnector]
            when(mockConnector.updateGoodsRecord(any())(any())).thenReturn(Future.successful(Done))

            val mockSessionRepository = mock[SessionRepository]
            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockConnector),
                  bind[SessionRepository].toInstance(mockSessionRepository)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value
              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
              verify(mockConnector, times(1)).updateGoodsRecord(eqTo(expectedPayload))(any())
              verify(mockSessionRepository, times(1)).set(any())

            }
          }
        }

        "when user answers cannot create an update goods record" - {

          "must not submit anything, and redirect to Journey Recovery" in {

            val continueUrl = RedirectUrl(routes.HomePageController.onPageLoad().url)

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController
                .onPageLoad(Some(continueUrl))
                .url
            }
          }
        }

        "must let the play error handler deal with connector failure" in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value

          val mockConnector = mock[GoodsRecordConnector]
          when(mockConnector.updateGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
              .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(POST, postUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }

    "for Goods Description Update" - {
      val summaryKey      = "goodsDescription.checkYourAnswersLabel"
      val summaryHidden   = "goodsDescription.change.hidden"
      val summaryUrl      = routes.HasGoodDescriptionChangeController.onPageLoad(CheckMode, testRecordId).url
      val page            = GoodsDescriptionUpdatePage(testRecordId)
      val answer          = "Test"
      val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, goodsDescription = Some(answer))
      val getUrl          = routes.CyaUpdateRecordController.onPageLoadGoodsDescription(testRecordId).url
      val call            = routes.CyaUpdateRecordController.onSubmitGoodsDescription(testRecordId)
      val postUrl         = routes.CyaUpdateRecordController.onSubmitGoodsDescription(testRecordId).url

      "for a GET" - {

        def createChangeList(app: Application): SummaryList = SummaryListViewModel(
          rows = Seq(
            UpdateRecordSummary.row(answer, summaryKey, summaryHidden, summaryUrl)(messages(app))
          )
        )

        "must return OK and the correct view with valid mandatory data" in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaUpdateRecordView]
            val list = createChangeList(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val application = applicationBuilder(Some(emptyUserAnswers)).build()
          val continueUrl = RedirectUrl(routes.HomePageController.onPageLoad().url)

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "for a POST" - {

        "when user answers can create a valid update goods record" - {

          "must update the goods record and redirect to the Home Page" in {

            val userAnswers = emptyUserAnswers
              .set(page, answer)
              .success
              .value

            val mockConnector = mock[GoodsRecordConnector]
            when(mockConnector.updateGoodsRecord(any())(any())).thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
              verify(mockConnector, times(1)).updateGoodsRecord(eqTo(expectedPayload))(any())
            }
          }
        }

        "when user answers cannot create an update goods record" - {

          "must not submit anything, and redirect to Journey Recovery" in {

            val continueUrl = RedirectUrl(routes.HomePageController.onPageLoad().url)

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController
                .onPageLoad(Some(continueUrl))
                .url
            }
          }
        }

        "must let the play error handler deal with connector failure" in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value

          val mockConnector = mock[GoodsRecordConnector]
          when(mockConnector.updateGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
              .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(POST, postUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }

    "for Trader Reference Update" - {
      val summaryKey      = "traderReference.checkYourAnswersLabel"
      val summaryHidden   = "traderReference.change.hidden"
      val summaryUrl      = routes.TraderReferenceController.onPageLoadUpdate(CheckMode, testRecordId).url
      val page            = TraderReferenceUpdatePage(testRecordId)
      val answer          = "Test"
      val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, traderReference = Some(answer))
      val getUrl          = routes.CyaUpdateRecordController.onPageLoadTraderReference(testRecordId).url
      val call            = routes.CyaUpdateRecordController.onSubmitTraderReference(testRecordId)
      val postUrl         = routes.CyaUpdateRecordController.onSubmitTraderReference(testRecordId).url

      "for a GET" - {

        def createChangeList(app: Application): SummaryList = SummaryListViewModel(
          rows = Seq(
            UpdateRecordSummary.row(answer, summaryKey, summaryHidden, summaryUrl)(messages(app))
          )
        )

        "must return OK and the correct view with valid mandatory data" in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaUpdateRecordView]
            val list = createChangeList(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val application = applicationBuilder(Some(emptyUserAnswers)).build()
          val continueUrl = RedirectUrl(routes.HomePageController.onPageLoad().url)

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "for a POST" - {

        "when user answers can create a valid update goods record" - {

          "must update the goods record and redirect to the Home Page" in {

            val userAnswers = emptyUserAnswers
              .set(page, answer)
              .success
              .value

            val mockConnector = mock[GoodsRecordConnector]
            when(mockConnector.updateGoodsRecord(any())(any())).thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
              verify(mockConnector, times(1)).updateGoodsRecord(eqTo(expectedPayload))(any())
            }
          }
        }

        "when user answers cannot create an update goods record" - {

          "must not submit anything, and redirect to Journey Recovery" in {

            val continueUrl = RedirectUrl(routes.HomePageController.onPageLoad().url)

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController
                .onPageLoad(Some(continueUrl))
                .url
            }
          }
        }

        "must let the play error handler deal with connector failure" in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value

          val mockConnector = mock[GoodsRecordConnector]
          when(mockConnector.updateGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
              .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(POST, postUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }

    "for Commodity Code Update" - {
      val summaryKey      = "commodityCode.checkYourAnswersLabel"
      val summaryHidden   = "commodityCode.change.hidden"
      val summaryUrl      = routes.HasCommodityCodeChangeController.onPageLoad(CheckMode, testRecordId).url
      val page            = CommodityCodeUpdatePage(testRecordId)
      val answer          = "Test"
      val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, commodityCode = Some(answer))
      val getUrl          = routes.CyaUpdateRecordController.onPageLoadCommodityCode(testRecordId).url
      val call            = routes.CyaUpdateRecordController.onSubmitCommodityCode(testRecordId)
      val postUrl         = routes.CyaUpdateRecordController.onSubmitCommodityCode(testRecordId).url

      "for a GET" - {

        def createChangeList(app: Application): SummaryList = SummaryListViewModel(
          rows = Seq(
            UpdateRecordSummary.row(answer, summaryKey, summaryHidden, summaryUrl)(messages(app))
          )
        )

        "must return OK and the correct view with valid mandatory data" in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value
            .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaUpdateRecordView]
            val list = createChangeList(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val application = applicationBuilder(Some(emptyUserAnswers)).build()
          val continueUrl = RedirectUrl(routes.HomePageController.onPageLoad().url)

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "for a POST" - {

        "when user answers can create a valid update goods record" - {

          "must update the goods record and redirect to the Home Page" in {

            val userAnswers = emptyUserAnswers
              .set(page, answer)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value

            val mockConnector = mock[GoodsRecordConnector]
            when(mockConnector.updateGoodsRecord(any())(any())).thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
              verify(mockConnector, times(1)).updateGoodsRecord(eqTo(expectedPayload))(any())
            }
          }
        }

        "when user answers cannot create an update goods record" - {

          "must not submit anything, and redirect to Journey Recovery" in {

            val continueUrl = RedirectUrl(routes.HomePageController.onPageLoad().url)

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController
                .onPageLoad(Some(continueUrl))
                .url
            }
          }
        }

        "must let the play error handler deal with connector failure " in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value
            .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
            .success
            .value

          val mockConnector = mock[GoodsRecordConnector]
          when(mockConnector.updateGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
              .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }
          }
        }

        "must redirect to Journey Recovery if no existing data is found (Country of Origin example)" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaUpdateRecordController.onSubmitCountryOfOrigin(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }

  }
}
