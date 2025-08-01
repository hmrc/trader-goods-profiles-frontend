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
import base.TestConstants.testRecordId
import connectors.{GoodsRecordConnector, OttConnector}
import models.helper.CreateRecordJourney
import models.requests.DataRequest
import models.{Country, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.CountriesQuery
import repositories.SessionRepository
import services.{AuditService, AutoCategoriseService, DataCleansingService}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers.goodsRecord.*
import viewmodels.govuk.SummaryListFluency
import views.html.goodsRecord.CyaCreateRecordView
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class CyaCreateRecordControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaCreateRecordController" - {
    def createChangeList(userAnswers: UserAnswers, app: Application): SummaryList = SummaryListViewModel(
      rows = Seq(
        ProductReferenceSummary.row(userAnswers)(messages(app)),
        GoodsDescriptionSummary.row(userAnswers)(messages(app)),
        CountryOfOriginSummary.row(userAnswers, Seq(Country("CN", "China")))(messages(app)),
        CommodityCodeSummary.row(userAnswers)(messages(app))
      ).flatten
    )

    "for a GET" - {
      "must return OK and the correct view with valid mandatory data" in {
        val mockOttConnector = mock[OttConnector]
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("CN", "China")))

        val application =
          applicationBuilder(userAnswers = Some(mandatoryRecordUserAnswers))
            .overrides(
              bind[OttConnector].toInstance(mockOttConnector)
            )
            .build()

        running(application) {
          val request = FakeRequest(GET, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[CyaCreateRecordView]
          val list    = createChangeList(mandatoryRecordUserAnswers, application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
          verify(mockOttConnector, atLeastOnce()).getCountries(any())
        }
      }

      "must return OK and the correct view with all data (including optional)" in {
        val mockOttConnector = mock[OttConnector]
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
          Seq(Country("CN", "China"))
        )

        val userAnswers = fullRecordUserAnswers
        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[OttConnector].toInstance(mockOttConnector)
            )
            .build()

        running(application) {
          val request = FakeRequest(GET, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[CyaCreateRecordView]
          val list    = createChangeList(userAnswers, application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
          verify(mockOttConnector, atLeastOnce()).getCountries(any())
        }
      }

      "must return OK and the correct view when countries query has countries in it" in {
        val userAnswers = fullRecordUserAnswers.set(CountriesQuery, Seq(Country("CN", "China"))).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[CyaCreateRecordView]
          val list    = createChangeList(userAnswers, application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery if no answers are found" in {
        val application = applicationBuilder(Some(emptyUserAnswers)).build()
        val continueUrl = RedirectUrl(controllers.goodsRecord.routes.CreateRecordStartController.onPageLoad().url)

        running(application) {
          val request = FakeRequest(GET, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
            .onPageLoad(Some(continueUrl))
            .url
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for a POST" - {
      "when user answers can create a valid goods record" - {

        "must submit the goods record and redirect to the CreateRecordSuccessController and cleanse userAnswers" in {
          val userAnswers               = mandatoryRecordUserAnswers
          val mockConnector             = mock[GoodsRecordConnector]
          val mockAutoCategoriseService = mock[AutoCategoriseService]
          val mockSessionRepository     = mock[SessionRepository]
          val mockAuditService          = mock[AuditService]
          val mockDataCleansingService  = mock[DataCleansingService]

          when(mockConnector.submitGoodsRecord(any())(any()))
            .thenReturn(Future.successful(testRecordId))

          when(
            mockAutoCategoriseService.autoCategoriseRecord(
              eqTo(testRecordId),
              eqTo(userAnswers)
            )(any[DataRequest[_]], any[HeaderCarrier])
          ).thenReturn(Future.successful(None))

          when(mockSessionRepository.get(eqTo(userAnswers.id)))
            .thenReturn(Future.successful(Some(userAnswers)))

          when(mockAuditService.auditFinishCreateGoodsRecord(any(), any(), any(), any(), any())(any()))
            .thenReturn(Future.successful(Done))

          when(mockDataCleansingService.deleteMongoData(eqTo(userAnswers.id), eqTo(CreateRecordJourney)))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockConnector),
              bind[AutoCategoriseService].toInstance(mockAutoCategoriseService),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[AuditService].toInstance(mockAuditService),
              bind[DataCleansingService].toInstance(mockDataCleansingService)
            )
            .build()

          running(application) {
            val request = FakeRequest(POST, controllers.goodsRecord.routes.CyaCreateRecordController.onSubmit().url)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual controllers.goodsRecord.routes.CreateRecordSuccessController
              .onPageLoad(testRecordId)
              .url

            verify(mockConnector, atLeastOnce()).submitGoodsRecord(any())(any())
            verify(mockAutoCategoriseService, atLeastOnce()).autoCategoriseRecord(
              eqTo(testRecordId),
              eqTo(userAnswers)
            )(any[DataRequest[_]], any[HeaderCarrier])
            verify(mockSessionRepository, atLeastOnce()).get(eqTo(userAnswers.id))
            verify(mockAuditService, atLeastOnce()).auditFinishCreateGoodsRecord(any(), any(), any(), any(), any())(
              any()
            )
            verify(mockDataCleansingService, atLeastOnce())
              .deleteMongoData(eqTo(userAnswers.id), eqTo(CreateRecordJourney))
          }
        }

      }

      "when user answers cannot create a goods record" - {
        "must not submit anything, and redirect to Journey Recovery" in {
          val mockConnector     = mock[GoodsRecordConnector]
          val mockAuditService  = mock[AuditService]
          val continueUrl       = RedirectUrl(controllers.goodsRecord.routes.CreateRecordStartController.onPageLoad().url)
          val sessionRepository = mock[SessionRepository]
          when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
              .overrides(bind[AuditService].toInstance(mockAuditService))
              .overrides(bind[SessionRepository].toInstance(sessionRepository))
              .build()

          running(application) {
            val request = FakeRequest(POST, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
              .url
            verify(mockConnector, never()).submitGoodsRecord(any())(any())

            withClue("must not try and submit an audit") {
              when(mockAuditService.auditFinishCreateGoodsRecord(any(), any(), any(), any(), any())(any()))
                .thenReturn(Future.successful(Done))
            }
            withClue("must cleanse the user answers data") {
              verify(sessionRepository).clearData(eqTo(emptyUserAnswers.id), eqTo(CreateRecordJourney))
            }
          }
        }
      }

      "must let the play error handler deal with connector failure" in {
        val userAnswers   = mandatoryRecordUserAnswers
        val mockConnector = mock[GoodsRecordConnector]
        when(mockConnector.submitGoodsRecord(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Connector failed")))

        val mockAuditService = mock[AuditService]
        when(mockAuditService.auditProfileSetUp(any(), any())(any())).thenReturn(Future.successful(Done))

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
            .overrides(bind[SessionRepository].toInstance(sessionRepository))
            .build()

        running(application) {
          val request = FakeRequest(POST, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)

          intercept[RuntimeException] {
            await(route(application, request).value)
          }

          withClue("must NOT call the audit connector when connector fails") {
            verify(mockAuditService, never()).auditFinishCreateGoodsRecord(any(), any(), any(), any(), any())(any())
          }

          withClue("must NOT cleanse the user answers data when connector fails") {
            verify(sessionRepository, never()).clearData(eqTo(userAnswers.id), eqTo(CreateRecordJourney))
          }
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
