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

package controllers.goodsRecord.countryOfOrigin

import base.SpecBase
import base.TestConstants.{testEori, testRecordId}
import connectors.{GoodsRecordConnector, OttConnector}
import models.*
import models.router.responses.GetGoodsRecordResponse
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.*
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.CountriesQuery
import repositories.SessionRepository
import services.{AuditService, AutoCategoriseService, CommodityService, GoodsRecordUpdateService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.*
import viewmodels.checkAnswers.goodsRecord.UpdateRecordSummary
import viewmodels.govuk.SummaryListFluency
import views.html.goodsRecord.CyaUpdateRecordView

import java.time.Instant
import scala.concurrent.Future

class CountryOfOriginCyaControllerSpec
    extends SpecBase
    with SummaryListFluency
    with MockitoSugar
    with BeforeAndAfterEach {

  private lazy val journeyRecoveryContinueUrl =
    controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url

  private val mockCommodityService         = mock[CommodityService]
  private val mockAuditService             = mock[AuditService]
  private val mockGoodsRecordConnector     = mock[GoodsRecordConnector]
  private val mockOttConnector             = mock[OttConnector]
  private val mockSessionRepository        = mock[SessionRepository]
  private val mockGoodsRecordUpdateService = mock[GoodsRecordUpdateService]
  private val mockAutoCategoriseService    = mock[AutoCategoriseService]
  implicit val hc: HeaderCarrier           = HeaderCarrier()
  val effectiveFrom: Instant               = Instant.now
  val effectiveTo: Instant                 = effectiveFrom.plusSeconds(1)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockAuditService,
      mockGoodsRecordConnector,
      mockOttConnector,
      mockSessionRepository,
      mockAutoCategoriseService
    )
    when(mockCommodityService.isCommodityCodeValid(any(), any())(any(), any())).thenReturn(Future.successful(true))
  }

  "CountryOfOriginCyaController" - {
    val record = goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ).copy(recordId = testRecordId, eori = testEori)

    val summaryValue    = "China"
    val summaryKey      = "countryOfOrigin.checkYourAnswersLabel"
    val summaryHidden   = "countryOfOrigin.change.hidden"
    val summaryUrl      = controllers.goodsRecord.countryOfOrigin.routes.UpdateCountryOfOriginController
      .onPageLoad(CheckMode, testRecordId)
      .url
    val page            = CountryOfOriginUpdatePage(testRecordId)
    val answer          = "CN"
    val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, countryOfOrigin = Some(answer))
    val getUrl          =
      controllers.goodsRecord.countryOfOrigin.routes.CountryOfOriginCyaController.onPageLoad(testRecordId).url
    val call            = controllers.goodsRecord.countryOfOrigin.routes.CountryOfOriginCyaController.onSubmit(testRecordId)
    val postUrl         = controllers.goodsRecord.countryOfOrigin.routes.CountryOfOriginCyaController.onSubmit(testRecordId).url
    val warningPage     = HasCountryOfOriginChangePage(testRecordId)

    "for a GET" - {
      def createChangeList(app: Application): SummaryList = SummaryListViewModel(
        rows = Seq(UpdateRecordSummary.countryRow(summaryValue, summaryKey, summaryHidden, summaryUrl)(messages(app)))
      )

      "must return OK and the correct view with valid mandatory data getting countries from connector" in {
        val userAnswers = emptyUserAnswers
          .set(page, answer)
          .success
          .value
          .set(warningPage, true)
          .success
          .value

        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("CN", "China")))
        when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[OttConnector].toInstance(mockOttConnector))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, getUrl)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[CyaUpdateRecordView]
          val list    = createChangeList(application)

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

        when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, getUrl)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[CyaUpdateRecordView]
          val list    = createChangeList(application)

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
        when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)

        val application = applicationBuilder(Some(emptyUserAnswers))
          .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, getUrl)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
              .url

        }
      }

      "must redirect to Journey Recovery if no record is found" in {
        when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.failed(
          new RuntimeException("Something went very wrong")
        )

        val application = applicationBuilder(Some(emptyUserAnswers))
          .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, getUrl)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(continueUrl = Some(RedirectUrl(journeyRecoveryContinueUrl)))
              .url
        }
      }

      "must redirect to Journey Recovery if getCountryOfOriginAnswer returns None" in {
        val userAnswers = emptyUserAnswers.set(warningPage, true).success.value

        when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, getUrl)
          val result  = route(application, request).value

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
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
            .onPageLoad()
            .url
        }
      }
    }

    "for a POST" - {
      "must update the goods record, cleanse the data and redirect to the Goods record Page" in {

        val userAnswers = emptyUserAnswers
          .set(page, answer)
          .success
          .value
          .set(warningPage, true)
          .success
          .value
          .set(OriginalCountryOfOriginPage(testRecordId), "GB")
          .success
          .value

        when(mockGoodsRecordConnector.getRecord(eqTo(testRecordId))(any()))
          .thenReturn(Future.successful(record))

        when(
          mockGoodsRecordUpdateService.updateIfChanged(
            any[String],
            any[String],
            any[UpdateGoodsRecord],
            any[GetGoodsRecordResponse],
            any[Boolean]
          )(any())
        )
          .thenReturn(Future.successful(Done))

        when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        when(mockAutoCategoriseService.autoCategoriseRecord(any[String], any[UserAnswers])(any(), any()))
          .thenReturn(Future.successful(None))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[GoodsRecordUpdateService].toInstance(mockGoodsRecordUpdateService),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AuditService].toInstance(mockAuditService),
            bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, postUrl)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.goodsRecord.countryOfOrigin.routes.UpdatedCountryOfOriginController
              .onPageLoad(testRecordId)
              .url

          // Verify the service was called instead of putGoodsRecord
          verify(mockGoodsRecordUpdateService).updateIfChanged(
            any[String],
            any[String],
            any[UpdateGoodsRecord],
            any[GetGoodsRecordResponse],
            any[Boolean]
          )(any())

          // Verify session updates
          verify(mockSessionRepository, times(2)).set(any())

          // Verify audit
          withClue("must call the audit connector with the supplied details") {
            verify(mockAuditService).auditFinishUpdateGoodsRecord(
              eqTo(testRecordId),
              eqTo(AffinityGroup.Individual),
              eqTo(expectedPayload)
            )(any())
          }
        }
      }
      "when user answers cannot create an update goods record" - {
        "must not submit anything, and redirect to Journey Recovery" in {
          val userAnswersWithCountryOrigin = emptyUserAnswers
            .set(OriginalCountryOfOriginPage(testRecordId), "United Kingdom")
            .success
            .value

          when(mockGoodsRecordConnector.getRecord(any())(any())).thenReturn(Future.successful(record))

          val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryOrigin))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[CommodityService].toInstance(mockCommodityService)
            )
            .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
              .url

            verify(mockGoodsRecordConnector).getRecord(any())(any())
          }
        }

        "must not submit anything when record is not found, and must let the play error handler deal with connector failure" in {
          when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future
            .failed(new RuntimeException("Something went very wrong"))

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
              .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            intercept[RuntimeException] {
              await(route(application, request).value)
              verify(mockGoodsRecordConnector).getRecord(any())(any())
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

        when(mockGoodsRecordConnector.patchGoodsRecord(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Connector failed")))
        when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
          .thenReturn(Future.successful(Done))
        when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, postUrl)
          intercept[Exception] {
            await(route(application, request).value)
          }

          verify(mockGoodsRecordConnector).getRecord(any())(any())

        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, postUrl)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
            .onPageLoad()
            .url
        }
      }
    }

  }
}
