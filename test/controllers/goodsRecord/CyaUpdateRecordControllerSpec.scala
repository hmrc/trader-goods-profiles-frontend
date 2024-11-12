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

package controllers.goodsRecord

import base.SpecBase
import base.TestConstants.{testEori, testRecordId}
import config.FrontendAppConfig
import connectors.{GoodsRecordConnector, OttConnector}
import models.{CheckMode, Commodity, Country, UpdateGoodsRecord}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.HasCorrectGoodsCommodityCodeUpdatePage
import pages.goodsRecord._
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CommodityUpdateQuery, CountriesQuery}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.{commodityCodeKey, countryOfOriginKey, goodsDescriptionKey, traderReferenceKey}
import viewmodels.checkAnswers.goodsRecord.UpdateRecordSummary
import viewmodels.govuk.SummaryListFluency
import views.html.goodsRecord.CyaUpdateRecordView

import java.time.Instant
import scala.concurrent.Future

class CyaUpdateRecordControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  private lazy val journeyRecoveryContinueUrl =
    controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url

  "CyaUpdateRecordController" - {

    val record = goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ).copy(recordId = testRecordId, eori = testEori)

    "for Country of Origin Update" - {
      val summaryValue    = "China"
      val summaryKey      = "countryOfOrigin.checkYourAnswersLabel"
      val summaryHidden   = "countryOfOrigin.change.hidden"
      val summaryUrl      =
        controllers.goodsRecord.routes.CountryOfOriginController.onPageLoadUpdate(CheckMode, testRecordId).url
      val page            = CountryOfOriginUpdatePage(testRecordId)
      val answer          = "CN"
      val expectedPayload =
        UpdateGoodsRecord(testEori, testRecordId, countryOfOrigin = Some(answer))
      val getUrl          = controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadCountryOfOrigin(testRecordId).url
      val call            = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitCountryOfOrigin(testRecordId)
      val postUrl         = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitCountryOfOrigin(testRecordId).url
      val warningPage     = HasCountryOfOriginChangePage(testRecordId)

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
            .set(warningPage, true)
            .success
            .value

          val mockOttConnector = mock[OttConnector]
          val mockAuditService = mock[AuditService]
          when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
            Seq(Country("CN", "China"))
          )

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
            .successful(record)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[OttConnector].toInstance(mockOttConnector))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaUpdateRecordView]
            val list = createChangeList(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call, countryOfOriginKey)(
              request,
              messages(application)
            ).toString

            withClue("must not try and submit an audit") {
              verify(mockAuditService, never()).auditFinishUpdateGoodsRecord(any(), any(), any())(any())
            }
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
            .set(warningPage, true)
            .success
            .value

          val mockAuditService = mock[AuditService]

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
            .successful(record)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaUpdateRecordView]
            val list = createChangeList(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call, countryOfOriginKey)(
              request,
              messages(application)
            ).toString

            withClue("must not try and submit an audit") {
              verify(mockAuditService, never()).auditFinishUpdateGoodsRecord(any(), any(), any())(any())
            }
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
            .successful(record)

          val application = applicationBuilder(Some(emptyUserAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url

          }
        }

        "must redirect to Journey Recovery if no record is found" in {

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
            .failed(new RuntimeException("Something went very wrong"))

          val application = applicationBuilder(Some(emptyUserAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(continueUrl = Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url
          }
        }

        "must redirect to Journey Recovery if getCountryOfOriginAnswer returns None" in {

          val userAnswers = emptyUserAnswers
            .set(warningPage, true)
            .success
            .value

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
            .successful(record)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(continueUrl = Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }

      "for a POST" - {

        "when user answers can create a valid update goods record" - {

          "must update the goods record, cleanse the data and redirect to the Goods record Page" in {

            val userAnswers = emptyUserAnswers
              .set(page, answer)
              .success
              .value
              .set(warningPage, true)
              .success
              .value

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            val mockAuditService         = mock[AuditService]
            val mockSessionRepository    = mock[SessionRepository]
            val mockAppConfig            = mock[FrontendAppConfig]

            when(mockAppConfig.useEisPatchMethod).thenReturn(false)

            when(mockGoodsRecordConnector.updateGoodsRecord(any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[AuditService].toInstance(mockAuditService),
                  bind[FrontendAppConfig].toInstance(mockAppConfig)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value
              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url
              verify(mockGoodsRecordConnector).updateGoodsRecord(eqTo(expectedPayload))(any())
              verify(mockSessionRepository).set(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService)
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

          "must PUT the goods record, cleanse the data and redirect to the Goods record Page" in {

            val userAnswers = emptyUserAnswers
              .set(page, answer)
              .success
              .value
              .set(warningPage, true)
              .success
              .value

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            val mockAuditService         = mock[AuditService]
            val mockSessionRepository    = mock[SessionRepository]
            val mockAppConfig            = mock[FrontendAppConfig]

            when(mockAppConfig.useEisPatchMethod).thenReturn(true)

            when(mockGoodsRecordConnector.putGoodsRecord(any(), any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[AuditService].toInstance(mockAuditService),
                  bind[FrontendAppConfig].toInstance(mockAppConfig)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value
              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url
              verify(mockGoodsRecordConnector).putGoodsRecord(any(), any())(any())
              verify(mockSessionRepository).set(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService)
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

        }

        "when user answers cannot create an update goods record" - {

          "must not submit anything, and redirect to Journey Recovery" in {

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                  .url
              verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
            }
          }

          "must not submit anything when record is not found, and must let the play error handler deal with connector failure" in {

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .failed(new RuntimeException("Something went very wrong"))

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              intercept[RuntimeException] {
                await(route(application, request).value)
                verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
              }
            }
          }
        }

        "must let the play error handler deal with connector failure when updating" in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value
            .set(warningPage, true)
            .success
            .value

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          val mockAuditService         = mock[AuditService]
          when(mockGoodsRecordConnector.updateGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))
          when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))

          when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
            .successful(record)

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService, atLeastOnce())
                .auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(
                  any()
                )
              verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
              verify(mockGoodsRecordConnector).updateGoodsRecord(any())(any())
            }
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(POST, postUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }
    }

    "for Goods Description Update" - {
      val summaryKey      = "goodsDescription.checkYourAnswersLabel"
      val summaryHidden   = "goodsDescription.change.hidden"
      val summaryUrl      =
        controllers.goodsRecord.routes.GoodsDescriptionController.onPageLoadUpdate(CheckMode, testRecordId).url
      val page            = GoodsDescriptionUpdatePage(testRecordId)
      val answer          = "Test"
      val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, goodsDescription = Some(answer))
      val getUrl          = controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadGoodsDescription(testRecordId).url
      val call            = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitGoodsDescription(testRecordId)
      val postUrl         = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitGoodsDescription(testRecordId).url

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

          val mockAuditService = mock[AuditService]

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaUpdateRecordView]
            val list = createChangeList(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call, goodsDescriptionKey)(
              request,
              messages(application)
            ).toString

            withClue("must not try and submit an audit") {
              verify(mockAuditService, never()).auditFinishUpdateGoodsRecord(any(), any(), any())(any())
            }
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val application = applicationBuilder(Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url

          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
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

            val mockConnector    = mock[GoodsRecordConnector]
            val mockAuditService = mock[AuditService]
            val mockAppConfig    = mock[FrontendAppConfig]

            when(mockAppConfig.useEisPatchMethod).thenReturn(false)

            when(mockConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(record))

            when(mockConnector.updateGoodsRecord(any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockConnector),
                  bind[AuditService].toInstance(mockAuditService)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url
              verify(mockConnector).updateGoodsRecord(eqTo(expectedPayload))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService)
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
                verify(mockConnector).getRecord(any(), any())(any())
                verify(mockConnector).updateGoodsRecord(any())(any())
              }
            }
          }
        }

        "when user answers cannot create an update goods record" - {

          "must not submit anything, and redirect to Journey Recovery" in {

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                  .url
            }
          }
        }

        "must let the play error handler deal with connector failure when updating" in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value

          val mockConnector    = mock[GoodsRecordConnector]
          val mockAuditService = mock[AuditService]

          when(mockConnector.updateGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))
          when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService)
                .auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(
                  any()
                )
              verify(mockConnector, never()).updateGoodsRecord(any())(any())
            }
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(POST, postUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }
    }

    "for Trader Reference Update" - {
      val summaryKey      = "traderReference.checkYourAnswersLabel"
      val summaryHidden   = "traderReference.change.hidden"
      val summaryUrl      =
        controllers.goodsRecord.routes.TraderReferenceController.onPageLoadUpdate(CheckMode, testRecordId).url
      val page            = TraderReferenceUpdatePage(testRecordId)
      val answer          = "Test"
      val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, traderReference = Some(answer))
      val getUrl          = controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadTraderReference(testRecordId).url
      val call            = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitTraderReference(testRecordId)
      val postUrl         = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitTraderReference(testRecordId).url

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

          val mockAuditService = mock[AuditService]

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaUpdateRecordView]
            val list = createChangeList(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call, traderReferenceKey)(
              request,
              messages(application)
            ).toString

            withClue("must not try and submit an audit") {
              verify(mockAuditService, never()).auditFinishUpdateGoodsRecord(any(), any(), any())(any())
            }
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val application = applicationBuilder(Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url

          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
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

            val mockConnector    = mock[GoodsRecordConnector]
            val mockAuditService = mock[AuditService]
            val mockAppConfig    = mock[FrontendAppConfig]

            when(mockAppConfig.useEisPatchMethod).thenReturn(false)

            when(mockConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(record))
            when(mockConnector.updateGoodsRecord(any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockConnector),
                  bind[AuditService].toInstance(mockAuditService)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url
              verify(mockConnector, atLeastOnce()).updateGoodsRecord(eqTo(expectedPayload))(any())
              verify(mockConnector, atLeastOnce()).getRecord(eqTo(testEori), eqTo(testRecordId))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService, atLeastOnce())
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

          "must PATCH the goods record, cleanse the data and redirect to the Goods record Page" in {

            val userAnswers = emptyUserAnswers
              .set(page, answer)
              .success
              .value

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            val mockAuditService         = mock[AuditService]
            val mockSessionRepository    = mock[SessionRepository]
            val mockAppConfig            = mock[FrontendAppConfig]

            when(mockAppConfig.useEisPatchMethod).thenReturn(true)

            when(mockGoodsRecordConnector.patchGoodsRecord(any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[AuditService].toInstance(mockAuditService),
                  bind[FrontendAppConfig].toInstance(mockAppConfig)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value
              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url
              verify(mockGoodsRecordConnector, atLeastOnce()).patchGoodsRecord(any())(any())
              verify(mockSessionRepository).set(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService, atLeastOnce())
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

          "when trader reference has not been changed must not update the goods record and redirect to the Home Page" in {
            val answer          = record.traderRef
            val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, traderReference = Some(answer))

            val userAnswers = emptyUserAnswers
              .set(page, answer)
              .success
              .value

            val mockConnector    = mock[GoodsRecordConnector]
            val mockAuditService = mock[AuditService]

            when(mockConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(record))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockConnector),
                  bind[AuditService].toInstance(mockAuditService)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url
              verify(mockConnector, never()).updateGoodsRecord(any())(any())
              verify(mockConnector, atLeastOnce()).getRecord(eqTo(testEori), eqTo(testRecordId))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService, atLeastOnce())
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }
        }

        "when user answers cannot create an update goods record" - {

          "must not submit anything, and redirect to Journey Recovery" in {

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                  .url
            }
          }
        }

        "must let the play error handler deal with connector failure when updating" in {

          val userAnswers = emptyUserAnswers
            .set(page, answer)
            .success
            .value

          val mockConnector    = mock[GoodsRecordConnector]
          val mockAuditService = mock[AuditService]

          when(mockConnector.updateGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))
          when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService)
                .auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(
                  any()
                )
            }
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(POST, postUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }
    }

    "for Commodity Code Update" - {
      val summaryKey      = "commodityCode.checkYourAnswersLabel"
      val summaryHidden   = "commodityCode.change.hidden"
      val shorterCommCode = "174290"
      val summaryUrl      =
        controllers.goodsRecord.routes.CommodityCodeController.onPageLoadUpdate(CheckMode, testRecordId).url
      val page            = CommodityCodeUpdatePage(testRecordId)
      val expectedPayload =
        UpdateGoodsRecord(testEori, testRecordId, commodityCode = Some(testCommodity))
      val getUrl          = controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadCommodityCode(testRecordId).url
      val call            = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitCommodityCode(testRecordId)
      val postUrl         = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitCommodityCode(testRecordId).url
      val warningPage     = HasCommodityCodeChangePage(testRecordId)

      "for a GET" - {

        def createChangeList(app: Application): SummaryList = SummaryListViewModel(
          rows = Seq(
            UpdateRecordSummary.row(testCommodity.commodityCode, summaryKey, summaryHidden, summaryUrl)(messages(app))
          )
        )

        def createChangeListShorterCommCode(app: Application): SummaryList = SummaryListViewModel(
          rows = Seq(
            UpdateRecordSummary.row(shorterCommCode, summaryKey, summaryHidden, summaryUrl)(messages(app))
          )
        )

        "must return OK and the correct view with valid mandatory data" in {

          val userAnswers = emptyUserAnswers
            .set(page, testCommodity.commodityCode)
            .success
            .value
            .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
            .success
            .value
            .set(warningPage, true)
            .success
            .value
            .set(HasCommodityCodeChangePage(testRecordId), true)
            .success
            .value
            .set(CommodityUpdateQuery(testRecordId), testCommodity)
            .success
            .value

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
            .successful(record)

          val mockAuditService = mock[AuditService]
          val application      = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaUpdateRecordView]
            val list = createChangeList(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call, commodityCodeKey)(
              request,
              messages(application)
            ).toString

            withClue("must not try and submit an audit") {
              verify(mockAuditService, never()).auditFinishUpdateGoodsRecord(any(), any(), any())(any())
            }
          }
        }

        "display shorter commodity code as received from B&T / until it is categorised and longer comm code entered" in {

          val userAnswers = emptyUserAnswers
            .set(page, shorterCommCode)
            .success
            .value
            .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
            .success
            .value
            .set(warningPage, true)
            .success
            .value
            .set(HasCommodityCodeChangePage(testRecordId), true)
            .success
            .value
            .set(CommodityUpdateQuery(testRecordId), testShorterCommodityQuery)
            .success
            .value

          val mockAuditService = mock[AuditService]

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
            .successful(record)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaUpdateRecordView]
            val list = createChangeListShorterCommCode(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call, commodityCodeKey)(
              request,
              messages(application)
            ).toString

            withClue("must not try and submit an audit") {
              verify(mockAuditService, never()).auditFinishUpdateGoodsRecord(any(), any(), any())(any())
            }
          }
        }

        "must redirect to Journey Recovery if no answers are found" in {

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
            .successful(record)

          val application = applicationBuilder(Some(emptyUserAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url

          }
        }

        "must redirect to Journey Recovery if no record is found" in {

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
            .failed(new RuntimeException("Something went very wrong"))

          val application = applicationBuilder(Some(emptyUserAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(continueUrl = Some(RedirectUrl(journeyRecoveryContinueUrl)))
                .url
            verify(mockGoodsRecordConnector).getRecord(any(), any())(any())

          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, getUrl)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }

      "for a POST" - {

        "when user answers can create a valid update goods record" - {

          "must update the goods record and redirect to the Goods record Page" in {
            val userAnswers = emptyUserAnswers
              .set(page, testCommodity.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), testCommodity)
              .success
              .value

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            val mockAuditService         = mock[AuditService]
            val mockAppConfig            = mock[FrontendAppConfig]

            when(mockAppConfig.useEisPatchMethod).thenReturn(false)

            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockGoodsRecordConnector.updateGoodsRecord(any())(any())).thenReturn(Future.successful(Done))

            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
                )
                .overrides(bind[AuditService].toInstance(mockAuditService))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url
              verify(mockGoodsRecordConnector).updateGoodsRecord(eqTo(expectedPayload))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService)
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

          "must PUT the goods record, cleanse the data and redirect to the Goods record Page" in {

            val userAnswers = emptyUserAnswers
              .set(page, testCommodity.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), testCommodity)
              .success
              .value

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            val mockAuditService         = mock[AuditService]
            val mockSessionRepository    = mock[SessionRepository]
            val mockAppConfig            = mock[FrontendAppConfig]

            when(mockAppConfig.useEisPatchMethod).thenReturn(true)

            when(mockGoodsRecordConnector.putGoodsRecord(any(), any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[AuditService].toInstance(mockAuditService),
                  bind[FrontendAppConfig].toInstance(mockAppConfig)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value
              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url
              verify(mockGoodsRecordConnector).putGoodsRecord(any(), any())(any())
              verify(mockSessionRepository).set(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService)
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

          "when commodity code has not been changed must not update the goods record and redirect to the Home Page" in {

            val answer          = Commodity(record.comcode, List("test"), validityStartDate, None)
            val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, commodityCode = Some(answer))

            val userAnswers = emptyUserAnswers
              .set(page, answer.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), answer)
              .success
              .value

            val mockConnector    = mock[GoodsRecordConnector]
            val mockAuditService = mock[AuditService]

            when(mockConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(record))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockConnector),
                  bind[AuditService].toInstance(mockAuditService)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url
              verify(mockConnector, never()).updateGoodsRecord(any())(any())
              verify(mockConnector).getRecord(eqTo(testEori), eqTo(testRecordId))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService, atLeastOnce())
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

        }

        "when user answers cannot create an update goods record" - {

          "must not submit anything, and redirect to Journey Recovery" in {

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                  .url
              verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any(), any())(any())
            }
          }

          "must not submit anything when record is not found, and must let the play error handler deal with connector failure" in {

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .failed(new RuntimeException("Something went very wrong"))

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              intercept[RuntimeException] {
                await(route(application, request).value)
                verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any(), any())(any())
              }
            }
          }
        }

        "must let the play error handler deal with connector failure when updating" in {
          val userAnswers = emptyUserAnswers
            .set(page, testCommodity.commodityCode)
            .success
            .value
            .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
            .success
            .value
            .set(warningPage, true)
            .success
            .value
            .set(HasCommodityCodeChangePage(testRecordId), true)
            .success
            .value
            .set(CommodityUpdateQuery(testRecordId), testCommodity)
            .success
            .value

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          val mockAuditService         = mock[AuditService]
          when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))
          when(mockGoodsRecordConnector.updateGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
            .successful(record)

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
              .overrides(bind[AuditService].toInstance(mockAuditService))
              .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService, atLeastOnce())
                .auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(
                  any()
                )
              verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any(), any())(any())
              verify(mockGoodsRecordConnector, atLeastOnce()).updateGoodsRecord(any())(any())
            }
          }
        }

        "must redirect to Journey Recovery if no existing data is found (Country of Origin example)" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(
              POST,
              controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitCountryOfOrigin(testRecordId).url
            )

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }
    }
  }
}
