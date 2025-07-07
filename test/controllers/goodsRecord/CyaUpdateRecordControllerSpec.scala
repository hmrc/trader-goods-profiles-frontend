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
import connectors.{GoodsRecordConnector, OttConnector}
import models.router.requests.PutRecordRequest
import models.router.responses.GetGoodsRecordResponse
import models.*
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, argThat, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.HasCorrectGoodsCommodityCodeUpdatePage
import pages.goodsRecord.*
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{CommodityUpdateQuery, CountriesQuery}
import repositories.SessionRepository
import services.{AuditService, AutoCategoriseService, CommodityService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.*
import viewmodels.checkAnswers.goodsRecord.UpdateRecordSummary
import viewmodels.govuk.SummaryListFluency
import views.html.goodsRecord.CyaUpdateRecordView
import pages.goodsRecord.OriginalCountryOfOriginPage
import models.Country

import java.time.Instant
import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

class CyaUpdateRecordControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private lazy val journeyRecoveryContinueUrl =
    controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url

  private val mockCommodityService      = mock[CommodityService]
  private val mockAuditService          = mock[AuditService]
  private val mockGoodsRecordConnector  = mock[GoodsRecordConnector]
  private val mockOttConnector          = mock[OttConnector]
  private val mockSessionRepository     = mock[SessionRepository]
  private val mockAutoCategoriseService = mock[AutoCategoriseService]
  implicit val hc: HeaderCarrier        = HeaderCarrier()
  val effectiveFrom: Instant            = Instant.now
  val effectiveTo: Instant              = effectiveFrom.plusSeconds(1)
  private val commodity                 =
    Commodity(
      "1704900000",
      List(
        "Sea urchins",
        "Live, fresh or chilled",
        "Aquatic invertebrates other than crustaceans and molluscs "
      ),
      effectiveFrom,
      Some(effectiveTo)
    )
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

  "CyaUpdateRecordController" - {
    val record = goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ).copy(recordId = testRecordId, eori = testEori)

    "for Country of Origin Update" - {
      val summaryValue    = "China"
      val summaryKey      = "countryOfOrigin.checkYourAnswersLabel"
      val summaryHidden   = "countryOfOrigin.change.hidden"
      val summaryUrl      = controllers.goodsRecord.countryOfOrigin.routes.UpdateCountryOfOriginController
        .onPageLoad(CheckMode, testRecordId)
        .url
      val page            = CountryOfOriginUpdatePage(testRecordId)
      val answer          = "CN"
      val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, countryOfOrigin = Some(answer))
      val getUrl          = controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadCountryOfOrigin(testRecordId).url
      val call            = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitCountryOfOrigin(testRecordId)
      val postUrl         = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitCountryOfOrigin(testRecordId).url
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

          when(mockGoodsRecordConnector.putGoodsRecord(any(), any())(any()))
            .thenReturn(Future.successful(Done))

          when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any()))
            .thenReturn(Future.successful(Done))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          // Mock the overload with (String, UserAnswers) explicitly:
          when(mockAutoCategoriseService.autoCategoriseRecord(any[String], any[UserAnswers])(any(), any()))
            .thenReturn(Future.successful(None))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
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

            verify(mockGoodsRecordConnector).putGoodsRecord(any(), any())(any())
            verify(mockSessionRepository).set(any())

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

    "for Goods Description Update" - {
      val summaryKey      = "goodsDescription.checkYourAnswersLabel"
      val summaryHidden   = "goodsDescription.change.hidden"
      val summaryUrl      = controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController
        .onPageLoad(CheckMode, testRecordId)
        .url
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
//this test
        "must return OK and the correct view with valid mandatory data" in {
          val userAnswers = emptyUserAnswers.set(page, answer).success.value

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)
            val result  = route(application, request).value
            val view    = application.injector.instanceOf[CyaUpdateRecordView]
            val list    = createChangeList(application)

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
            val result  = route(application, request).value

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
            val result  = route(application, request).value

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

            val userAnswers = emptyUserAnswers.set(page, answer).success.value

            when(mockGoodsRecordConnector.getRecord(any())(any())).thenReturn(Future.successful(record))
            when(mockGoodsRecordConnector.patchGoodsRecord(any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[AuditService].toInstance(mockAuditService)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url
              verify(mockGoodsRecordConnector).patchGoodsRecord(eqTo(expectedPayload))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService).auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(any())
                verify(mockGoodsRecordConnector).getRecord(any())(any())
                verify(mockGoodsRecordConnector).patchGoodsRecord(any())(any())
              }
            }
          }

          "when future fails with openAccreditationError redirect to the record is locked page" in {
            val userAnswers = emptyUserAnswers.set(page, answer).success.value

            when(mockGoodsRecordConnector.getRecord(any())(any())).thenReturn(Future.successful(record))
            when(mockGoodsRecordConnector.patchGoodsRecord(any())(any()))
              .thenReturn(Future.failed(UpstreamErrorResponse(openAccreditationErrorCode, BAD_REQUEST)))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[AuditService].toInstance(mockAuditService)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.routes.RecordLockedController
                .onPageLoad(testRecordId)
                .url
              verify(mockGoodsRecordConnector).patchGoodsRecord(eqTo(expectedPayload))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService).auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(any())
                verify(mockGoodsRecordConnector).getRecord(any())(any())
                verify(mockGoodsRecordConnector).patchGoodsRecord(any())(any())
              }
            }
          }
        }

        "when user answers cannot create an update goods record" - {
          "must not submit anything, and redirect to Journey Recovery" in {
            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[CommodityService].toInstance(mockCommodityService))
              .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                  .url
            }
          }
        }

        "must let the play error handler deal with connector failure when updating" in {
          val userAnswers = emptyUserAnswers.set(page, answer).success.value

          when(mockGoodsRecordConnector.patchGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))
          when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[CommodityService].toInstance(mockCommodityService)
            )
            .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService).auditFinishUpdateGoodsRecord(
                eqTo(testRecordId),
                eqTo(AffinityGroup.Individual),
                eqTo(expectedPayload)
              )(any())
              verify(mockGoodsRecordConnector, never()).patchGoodsRecord(any())(any())
            }
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

    "for product reference Update" - {
      val summaryKey      = "productReference.checkYourAnswersLabel"
      val summaryHidden   = "productReference.change.hidden"
      val summaryUrl      = controllers.goodsRecord.productReference.routes.UpdateProductReferenceController
        .onPageLoad(CheckMode, testRecordId)
        .url
      val page            = ProductReferenceUpdatePage(testRecordId)
      val answer          = "Test"
      val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, productReference = Some(answer))
      val getUrl          = controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadProductReference(testRecordId).url
      val call            = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitproductReference(testRecordId)
      val postUrl         = controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitproductReference(testRecordId).url

      "for a GET" - {
        def createChangeList(app: Application): SummaryList = SummaryListViewModel(
          rows = Seq(
            UpdateRecordSummary.row(answer, summaryKey, summaryHidden, summaryUrl)(messages(app))
          )
        )

        "must return OK and the correct view with valid mandatory data" in {
          val userAnswers = emptyUserAnswers.set(page, answer).success.value

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)
            val result  = route(application, request).value
            val view    = application.injector.instanceOf[CyaUpdateRecordView]
            val list    = createChangeList(application)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(list, call, productReferenceKey)(
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
            val result  = route(application, request).value

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
            val result  = route(application, request).value

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
            val userAnswers = emptyUserAnswers.set(page, answer).success.value

            when(mockGoodsRecordConnector.getRecord(any())(any())).thenReturn(Future.successful(record))
            when(mockGoodsRecordConnector.patchGoodsRecord(any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url

              verify(mockGoodsRecordConnector, atLeastOnce()).patchGoodsRecord(eqTo(expectedPayload))(any())
              verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(eqTo(testRecordId))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService, atLeastOnce()).auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(any())
              }
            }
          }

          "must PATCH the goods record, cleanse the data and redirect to the Goods record Page" in {
            val userAnswers = emptyUserAnswers.set(page, answer).success.value

            when(mockGoodsRecordConnector.patchGoodsRecord(any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
            when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value
              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url

              verify(mockGoodsRecordConnector, atLeastOnce()).patchGoodsRecord(any())(any())
              verify(mockSessionRepository).set(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService, atLeastOnce()).auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(any())
              }
            }
          }

          "when product reference has not been changed must not update the goods record and redirect to the Home Page" in {
            val answer          = record.traderRef
            val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, productReference = Some(answer))
            val userAnswers     = emptyUserAnswers.set(page, answer).success.value

            when(mockGoodsRecordConnector.getRecord(any())(any())).thenReturn(Future.successful(record))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url

              verify(mockGoodsRecordConnector, never()).patchGoodsRecord(any())(any())
              verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(eqTo(testRecordId))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService, atLeastOnce()).auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(any())
              }
            }
          }

          "when future fails with openAccreditationError redirect to the record is locked page" in {
            val userAnswers = emptyUserAnswers.set(page, answer).success.value

            when(mockGoodsRecordConnector.getRecord(any())(any())).thenReturn(Future.successful(record))
            when(mockGoodsRecordConnector.patchGoodsRecord(any())(any()))
              .thenReturn(Future.failed(UpstreamErrorResponse(openAccreditationErrorCode, BAD_REQUEST)))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[AuditService].toInstance(mockAuditService)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.routes.RecordLockedController
                .onPageLoad(testRecordId)
                .url

              verify(mockGoodsRecordConnector, atLeastOnce()).patchGoodsRecord(eqTo(expectedPayload))(any())
              verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(eqTo(testRecordId))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService, atLeastOnce()).auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(any())
              }
            }
          }
        }

        "when user answers cannot create an update goods record" - {
          "must not submit anything, and redirect to Journey Recovery" in {
            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[CommodityService].toInstance(mockCommodityService))
              .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                  .url
            }
          }
        }

        "must let the play error handler deal with connector failure when updating" in {
          val userAnswers = emptyUserAnswers.set(page, answer).success.value

          when(mockGoodsRecordConnector.patchGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))
          when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
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
              verify(mockAuditService).auditFinishUpdateGoodsRecord(
                eqTo(testRecordId),
                eqTo(AffinityGroup.Individual),
                eqTo(expectedPayload)
              )(any())
            }
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

    "for Commodity Code Update" - {
      val summaryKey      = "commodityCode.checkYourAnswersLabel"
      val summaryHidden   = "commodityCode.change.hidden"
      val shorterCommCode = "174290"
      val summaryUrl      = controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController
        .onPageLoad(CheckMode, testRecordId)
        .url
      val page            = CommodityCodeUpdatePage(testRecordId)
      val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, commodityCode = Some(testCommodity))
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
          rows = Seq(UpdateRecordSummary.row(shorterCommCode, summaryKey, summaryHidden, summaryUrl)(messages(app)))
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

          when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .overrides(bind[CommodityService].toInstance(mockCommodityService))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)
            val result  = route(application, request).value
            val view    = application.injector.instanceOf[CyaUpdateRecordView]
            val list    = createChangeList(application)

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

          when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .overrides(bind[CommodityService].toInstance(mockCommodityService))
            .build()

          running(application) {
            val request = FakeRequest(GET, getUrl)
            val result  = route(application, request).value
            val view    = application.injector.instanceOf[CyaUpdateRecordView]
            val list    = createChangeListShorterCommCode(application)

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
            verify(mockGoodsRecordConnector).getRecord(any())(any())
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

            // Mock audit service
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            // Mock OttConnector getCountries
            when(mockOttConnector.getCountries)
              .thenReturn(
                Future.successful(
                  Seq(
                    Country("GB", "United Kingdom"),
                    Country("FR", "France")
                  )
                )
              )

            // Mock session repository set
            when(mockSessionRepository.set(any()))
              .thenReturn(Future.successful(true))

            // Mock GoodsRecordConnector methods
            when(mockGoodsRecordConnector.putGoodsRecord(any(), any())(any()))
              .thenReturn(Future.successful(Done))

            when(mockGoodsRecordConnector.getRecord(any())(any()))
              .thenReturn(Future.successful(record)) // avoids real HTTP call

            // Mock AutoCategoriseService to return Future[Option[UserAnswers]]
            when(mockAutoCategoriseService.autoCategoriseRecord(any[String], any[UserAnswers])(any(), any()))
              .thenReturn(Future.successful(Some(emptyUserAnswers))) // <-- return Some(UserAnswers) wrapped in Future

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[AuditService].toInstance(mockAuditService),
                bind[CommodityService].toInstance(mockCommodityService),
                bind[OttConnector].toInstance(mockOttConnector),
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url

              verify(mockGoodsRecordConnector).putGoodsRecord(any(), any())(any())
              verify(mockAuditService).auditFinishUpdateGoodsRecord(
                eqTo(testRecordId),
                eqTo(AffinityGroup.Individual),
                eqTo(expectedPayload)
              )(any())
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

            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            when(mockOttConnector.getCountries)
              .thenReturn(
                Future.successful(
                  Seq(
                    Country("GB", "United Kingdom"),
                    Country("FR", "France")
                  )
                )
              )

            when(mockSessionRepository.set(any()))
              .thenReturn(Future.successful(true))

            when(mockGoodsRecordConnector.putGoodsRecord(any(), any())(any()))
              .thenReturn(Future.successful(Done))

            when(mockGoodsRecordConnector.getRecord(any())(any()))
              .thenReturn(Future.successful(record))

            when(mockAutoCategoriseService.autoCategoriseRecord(any[String], any[UserAnswers])(any(), any()))
              .thenReturn(Future.successful(Some(emptyUserAnswers)))
            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[AuditService].toInstance(mockAuditService),
                bind[CommodityService].toInstance(mockCommodityService),
                bind[OttConnector].toInstance(mockOttConnector),
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url

              verify(mockGoodsRecordConnector).putGoodsRecord(
                argThat { req =>
                  req.actorId == record.eori &&
                  req.traderRef == record.traderRef &&
                  req.comcode == testCommodity.commodityCode &&
                  req.goodsDescription == record.goodsDescription &&
                  req.countryOfOrigin == record.countryOfOrigin &&
                  req.category.isEmpty
                },
                eqTo(testRecordId)
              )(any())

              verify(mockSessionRepository).set(any())

              verify(mockAuditService).auditFinishUpdateGoodsRecord(
                eqTo(testRecordId),
                eqTo(AffinityGroup.Individual),
                eqTo(expectedPayload)
              )(any())
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

            // Mock necessary dependencies
            when(mockGoodsRecordConnector.getRecord(any())(any()))
              .thenReturn(Future.successful(record))

            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            when(mockOttConnector.getCountries)
              .thenReturn(
                Future.successful(
                  Seq(
                    Country("GB", "United Kingdom"),
                    Country("FR", "France")
                  )
                )
              )

            when(mockSessionRepository.set(any()))
              .thenReturn(Future.successful(true))

            when(mockAutoCategoriseService.autoCategoriseRecord(any[String], any[UserAnswers])(any(), any()))
              .thenReturn(Future.successful(Some(emptyUserAnswers)))

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[AuditService].toInstance(mockAuditService),
                bind[CommodityService].toInstance(mockCommodityService),
                bind[OttConnector].toInstance(mockOttConnector), // ✅ important for fixing 401
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
                .onPageLoad(testRecordId)
                .url

              verify(mockGoodsRecordConnector, never()).patchGoodsRecord(any())(any())
              verify(mockGoodsRecordConnector).getRecord(eqTo(testRecordId))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService, atLeastOnce()).auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(any())
              }
            }
          }

          "when future fails with openAccreditationError redirect to the record is locked page" in {
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

            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockGoodsRecordConnector.putGoodsRecord(any(), any())(any()))
              .thenReturn(Future.failed(UpstreamErrorResponse(openAccreditationErrorCode, BAD_REQUEST)))
            when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)

            val application = applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[AuditService].toInstance(mockAuditService),
                bind[CommodityService].toInstance(mockCommodityService)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              val result  = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.routes.RecordLockedController
                .onPageLoad(testRecordId)
                .url
              verify(mockGoodsRecordConnector).putGoodsRecord(any(), any())(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService).auditFinishUpdateGoodsRecord(
                  eqTo(testRecordId),
                  eqTo(AffinityGroup.Individual),
                  eqTo(expectedPayload)
                )(any())
              }
            }
          }
        }

        "when user answers cannot create an update goods record" - {
          "must not submit anything, and redirect to Journey Recovery" in {
            when(mockGoodsRecordConnector.getRecord(any())(any()))
              .thenReturn(Future.successful(record))

            when(mockOttConnector.getCountries)
              .thenReturn(Future.successful(Seq.empty))

            val userAnswersWithMissingPage: UserAnswers = emptyUserAnswers
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), commodity)
              .success
              .value
              .set(CommodityCodeUpdatePage(testRecordId), "0208402002")
              .success
              .value

            val application = applicationBuilder(userAnswers = Some(userAnswersWithMissingPage))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[CommodityService].toInstance(mockCommodityService),
                bind[OttConnector].toInstance(mockOttConnector)
              )
              .build()

            running(application) {
              val request = FakeRequest(GET, routes.CyaUpdateRecordController.onPageLoadCommodityCode(testRecordId).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual
                controllers.problem.routes.JourneyRecoveryController
                  .onPageLoad(
                    Some(
                      RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url)
                    )
                  )
                  .url

              verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any())(any())
            }
          }

          "must not submit anything when record is not found, and must let the play error handler deal with connector failure" in {
            when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future
              .failed(new RuntimeException("Something went very wrong"))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
              .build()

            running(application) {
              val request = FakeRequest(POST, postUrl)
              intercept[RuntimeException] {
                await(route(application, request).value)
                verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any())(any())
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

          when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
            .thenReturn(Future.successful(Done))
          when(mockGoodsRecordConnector.patchGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))
          when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(record)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .overrides(bind[CommodityService].toInstance(mockCommodityService))
            .build()

          running(application) {
            val request = FakeRequest(POST, postUrl)
            intercept[RuntimeException] {
              await(route(application, request).value)
            }

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService, atLeastOnce()).auditFinishUpdateGoodsRecord(
                eqTo(testRecordId),
                eqTo(AffinityGroup.Individual),
                eqTo(expectedPayload)
              )(any())
              verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any())(any())
              verify(mockGoodsRecordConnector, atLeastOnce()).putGoodsRecord(any(), any())(any())
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
}
