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
import connectors.{GoodsRecordConnector, OttConnector}
import models.*
import models.ott.CategorisationInfo
import models.requests.DataRequest
import models.router.responses.GetGoodsRecordResponse
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.*
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.CountriesQuery
import repositories.SessionRepository
import services.{AuditService, AutoCategoriseService, GoodsRecordUpdateService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.Future

class CountryOfOriginCyaControllerSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfterEach {

  private val mockAuditService             = mock[AuditService]
  private val mockGoodsRecordConnector     = mock[GoodsRecordConnector]
  private val mockOttConnector             = mock[OttConnector]
  private val mockSessionRepository        = mock[SessionRepository]
  private val mockGoodsRecordUpdateService = mock[GoodsRecordUpdateService]
  private val mockAutoCategoriseService    = mock[AutoCategoriseService]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockAuditService,
      mockGoodsRecordConnector,
      mockOttConnector,
      mockSessionRepository,
      mockAutoCategoriseService,
      mockGoodsRecordUpdateService
    )
  }

  val testRecordId = "record-123"
  val testEori     = "eori-123"
  val answer       = "CN"
  val countryName  = "China"
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, controllers.goodsRecord.countryOfOrigin.routes.CountryOfOriginCyaController.onSubmit(testRecordId).url)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val recordAutoCategorised: GetGoodsRecordResponse = goodsRecordResponse(
    Instant.parse("2025-01-01T00:00:00Z"),
    Instant.parse("2025-01-01T00:00:00Z")
  ).copy(recordId = testRecordId, eori = testEori, category = Some(3), countryOfOrigin = "CN")

  val page: CountryOfOriginUpdatePage = CountryOfOriginUpdatePage(testRecordId)
  val warningPage: HasCountryOfOriginChangePage = HasCountryOfOriginChangePage(testRecordId)

  "CountryOfOriginCyaController" - {

    "GET onPageLoad" - {

      "must return OK with view when country exists in session" in {
        val userAnswers = emptyUserAnswers
          .set(page, answer).success.value
          .set(warningPage, true).success.value
          .set(CountriesQuery, Seq(Country("CN", "China"))).success.value

        when(mockGoodsRecordConnector.getRecord(any())(any()))
          .thenReturn(Future.successful(recordAutoCategorised))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[OttConnector].toInstance(mockOttConnector),
            bind[AuditService].toInstance(mockAuditService)
          ).build()

        running(application) {
          val controller = application.injector.instanceOf[CountryOfOriginCyaController]
          val request = FakeRequest(GET, controllers.goodsRecord.countryOfOrigin.routes.CountryOfOriginCyaController.onPageLoad(testRecordId).url)

          val result = controller.onPageLoad(testRecordId).apply(request)
          status(result) mustEqual OK
          contentAsString(result) must include(countryName)

          verify(mockAuditService, never()).auditFinishUpdateGoodsRecord(any(), any(), any())(any())
        }
      }

      "must redirect if country missing from session" in {
        val userAnswers = emptyUserAnswers

        when(mockGoodsRecordConnector.getRecord(any())(any()))
          .thenReturn(Future.successful(recordAutoCategorised))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
          .build()

        running(application) {
          val controller = application.injector.instanceOf[CountryOfOriginCyaController]
          val request = FakeRequest(GET, controllers.goodsRecord.countryOfOrigin.routes.CountryOfOriginCyaController.onPageLoad(testRecordId).url)

          val result = controller.onPageLoad(testRecordId).apply(request)
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value must include(controllers.problem.routes.JourneyRecoveryController.onPageLoad().url)
        }
      }
    }

    "POST onSubmit" - {

      "must update, audit, set session and redirect to SingleRecord when country did not change" in {

        val userAnswers = emptyUserAnswers
          .set(CountryOfOriginUpdatePage(testRecordId), "US").success.value
          .set(OriginalCountryOfOriginPage(testRecordId), "US").success.value
          .set(HasCountryOfOriginChangePage(testRecordId), true).success.value

        val record = recordAutoCategorised.copy(category = Some(1), countryOfOrigin = "US")

        when(mockGoodsRecordConnector.getRecord(any())(any()))
          .thenReturn(Future.successful(record))

        when(mockAutoCategoriseService.getCategorisationInfoForRecord(
          any[String], any[UserAnswers]
        )(any[DataRequest[_]], any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        when(mockGoodsRecordUpdateService.updateIfChanged(
          any[String], any[String], any[UpdateGoodsRecord], any[GetGoodsRecordResponse], any[Boolean]
        )(any[HeaderCarrier])).thenReturn(Future.successful(Done))

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockAuditService.auditFinishUpdateGoodsRecord(any[String], any[AffinityGroup], any[UpdateGoodsRecord])
          (any[HeaderCarrier])).thenReturn(Future.successful(Done))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[AuditService].toInstance(mockAuditService),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AutoCategoriseService].toInstance(mockAutoCategoriseService),
            bind[GoodsRecordUpdateService].toInstance(mockGoodsRecordUpdateService)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[CountryOfOriginCyaController]
          val request = FakeRequest(POST, controllers.goodsRecord.countryOfOrigin.routes.CountryOfOriginCyaController.onSubmit(testRecordId).url)

          val result = await(controller.onSubmit(testRecordId).apply(request))

          result.header.status mustEqual SEE_OTHER
          result.header.headers("Location") mustEqual
            controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url

          verify(mockAuditService).auditFinishUpdateGoodsRecord(
            any[String], any[AffinityGroup], any[UpdateGoodsRecord]
          )(any[HeaderCarrier])

          verify(mockSessionRepository, times(2)).set(any())
          verify(mockGoodsRecordUpdateService).updateIfChanged(
            any[String], any[String], any[UpdateGoodsRecord], any[GetGoodsRecordResponse], any[Boolean]
          )(any[HeaderCarrier])
        }
      }

      "must update, audit, set session and redirect to SingleRecord when auto-categorisable and country changed" in {
        val userAnswers = emptyUserAnswers
          .set(CountryOfOriginUpdatePage(testRecordId), "US").success.value
          .set(OriginalCountryOfOriginPage(testRecordId), "CN").success.value
          .set(HasCountryOfOriginChangePage(testRecordId), true).success.value

        val record = recordAutoCategorised.copy(category = Some(1), countryOfOrigin = "CN")

        val categorisationInfo = CategorisationInfo.empty.copy(countryOfOrigin = "US")
        when(mockGoodsRecordConnector.getRecord(any())(any())).thenReturn(Future.successful(record))
        when(mockAutoCategoriseService.getCategorisationInfoForRecord(any[String], any[UserAnswers])(any(), any()))
          .thenReturn(Future.successful(Some(categorisationInfo)))
        when(mockGoodsRecordUpdateService.updateIfChanged(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[AuditService].toInstance(mockAuditService),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AutoCategoriseService].toInstance(mockAutoCategoriseService),
            bind[GoodsRecordUpdateService].toInstance(mockGoodsRecordUpdateService)
          ).build()

        running(application) {
          val controller = application.injector.instanceOf[CountryOfOriginCyaController]
          val request = FakeRequest(POST, controllers.goodsRecord.countryOfOrigin.routes.CountryOfOriginCyaController.onSubmit(testRecordId).url)
          val result = await(controller.onSubmit(testRecordId).apply(request))

          result.header.status mustEqual SEE_OTHER
          result.header.headers("Location") mustEqual controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url

          verify(mockAuditService).auditFinishUpdateGoodsRecord(any(), any(), any())(any())
          verify(mockSessionRepository, times(2)).set(any())
          verify(mockGoodsRecordUpdateService).updateIfChanged(any(), any(), any(), any(), any())(any())
        }
      }

      "must update, audit, set session and redirect to SingleRecord when country changed but NOT auto-categorisable" in {

        val userAnswers = emptyUserAnswers
          .set(CountryOfOriginUpdatePage(testRecordId), "US").success.value
          .set(OriginalCountryOfOriginPage(testRecordId), "CN").success.value
          .set(HasCountryOfOriginChangePage(testRecordId), true).success.value

        val record = recordAutoCategorised.copy(category = Some(1), countryOfOrigin = "CN")

        when(mockGoodsRecordConnector.getRecord(any())(any()))
          .thenReturn(Future.successful(record))

        when(mockAutoCategoriseService.getCategorisationInfoForRecord(any[String], any[UserAnswers])(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockGoodsRecordUpdateService.updateIfChanged(
          any[String],
          any[String],
          any[UpdateGoodsRecord],
          any[GetGoodsRecordResponse],
          any[Boolean]
        )(any[HeaderCarrier]))
          .thenReturn(Future.successful(Done))

        when(mockGoodsRecordUpdateService.removeManualCategory(
          any[String],
          any[String],
          any[GetGoodsRecordResponse]
        )(any[HeaderCarrier]))
          .thenReturn(Future.successful(Done))

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[AuditService].toInstance(mockAuditService),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AutoCategoriseService].toInstance(mockAutoCategoriseService),
            bind[GoodsRecordUpdateService].toInstance(mockGoodsRecordUpdateService)
          ).build()

        running(application) {
          val controller = application.injector.instanceOf[CountryOfOriginCyaController]
          val request = FakeRequest(POST, controllers.goodsRecord.countryOfOrigin.routes.CountryOfOriginCyaController.onSubmit(testRecordId).url)
          val result = await(controller.onSubmit(testRecordId).apply(request))


          result.header.headers("Location") mustEqual
            controllers.goodsRecord.countryOfOrigin.routes.UpdatedCountryOfOriginController.onPageLoad(testRecordId).url

          verify(mockAuditService).auditFinishUpdateGoodsRecord(any(), any(), any())(any())
          verify(mockSessionRepository, times(2)).set(any())
          verify(mockGoodsRecordUpdateService).updateIfChanged(
            any[String],
            any[String],
            any[UpdateGoodsRecord],
            any[GetGoodsRecordResponse],
            any[Boolean]
          )(any[HeaderCarrier])
          verify(mockGoodsRecordUpdateService).removeManualCategory(
            any[String],
            any[String],
            any[GetGoodsRecordResponse]
          )(any[HeaderCarrier])
        }
      }

    }
  }



}
