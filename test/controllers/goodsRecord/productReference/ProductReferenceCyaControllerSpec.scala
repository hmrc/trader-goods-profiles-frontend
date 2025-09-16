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

package controllers.goodsRecord.productReference

import base.SpecBase
import base.TestConstants.{testEori, testRecordId}
import connectors.{GoodsRecordConnector, OttConnector}
import models.*
import models.router.responses.GetGoodsRecordResponse
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.*
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{AuditService, AutoCategoriseService, CommodityService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.*
import viewmodels.checkAnswers.goodsRecord.UpdateRecordSummary
import viewmodels.govuk.SummaryListFluency
import views.html.goodsRecord.CyaUpdateRecordView

import java.time.Instant
import scala.concurrent.Future

class ProductReferenceCyaControllerSpec
    extends SpecBase
    with SummaryListFluency
    with MockitoSugar
    with BeforeAndAfterEach {

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

    val summaryKey      = "productReference.checkYourAnswersLabel"
    val summaryHidden   = "productReference.change.hidden"
    val summaryUrl      = controllers.goodsRecord.productReference.routes.UpdateProductReferenceController
      .onPageLoad(CheckMode, testRecordId)
      .url
    val page            = ProductReferenceUpdatePage(testRecordId)
    val answer          = "Test"
    val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, productReference = Some(answer))
    val getUrl          =
      controllers.goodsRecord.productReference.routes.ProductReferenceCyaController.onPageLoad(testRecordId).url
    val call            = controllers.goodsRecord.productReference.routes.ProductReferenceCyaController.onSubmit(testRecordId)
    val postUrl         =
      controllers.goodsRecord.productReference.routes.ProductReferenceCyaController.onSubmit(testRecordId).url

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

          when(mockGoodsRecordConnector.getRecord(any())(any())).thenReturn(Future.successful(Some(record)))
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
          when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future.successful(Some(record))

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

          when(mockGoodsRecordConnector.getRecord(any())(any())).thenReturn(Future.successful(Some(record)))
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

          when(mockGoodsRecordConnector.getRecord(any())(any())).thenReturn(Future.successful(Some(record)))
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

}
