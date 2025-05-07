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
import base.TestConstants.testEori
import models.helper.CreateRecordJourney
import org.apache.pekko.Done
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.CountriesQuery
import services.AuditService
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers.goodsRecord.{CommodityCodeSummary, CountryOfOriginSummary, GoodsDescriptionSummary, ProductReferenceSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.goodsRecord.CyaCreateRecordView
import play.api.test.CSRFTokenHelper.*
import connectors.{GoodsRecordConnector, OttConnector, TraderProfileConnector}
import generators.Generators
import models.*
import models.ott.response.{CategoryAssessmentRelationship, ExemptionType => ResponseExemptionType, *}
import models.ott.*
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class CyaCreateRecordControllerSpec
    extends SpecBase
    with SummaryListFluency
    with MockitoSugar
    with BeforeAndAfterEach
    with Generators {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val mockGoodsRecordConnector   = mock[GoodsRecordConnector]
  private val mockAuditService           = mock[AuditService]
  private val mockSessionRepository      = mock[SessionRepository]
  private val mockTraderProfileConnector = mock[TraderProfileConnector]
  private val mockOttConnector           = mock[OttConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockGoodsRecordConnector,
      mockAuditService,
      mockSessionRepository,
      mockTraderProfileConnector,
      mockOttConnector
    )

    when(mockGoodsRecordConnector.submitGoodsRecord(any())(any())).thenReturn(Future.successful("test"))
    when(mockAuditService.auditFinishCreateGoodsRecord(any(), any(), any())(any())).thenReturn(Future.successful(Done))
    when(mockSessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
    when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))
    when(mockTraderProfileConnector.checkTraderProfile(any())(any[HeaderCarrier])).thenReturn(Future.successful(true))
    when(mockTraderProfileConnector.getTraderProfile(any[HeaderCarrier])).thenReturn(
      Future.successful(TraderProfile(testEori, "1", None, None, eoriChanged = false))
    )
  }

  def createChangeList(userAnswers: UserAnswers, app: Application): SummaryList = SummaryListViewModel(
    rows = Seq(
      ProductReferenceSummary.row(userAnswers)(messages(app)),
      GoodsDescriptionSummary.row(userAnswers)(messages(app)),
      CountryOfOriginSummary.row(userAnswers, Seq(Country("CN", "China")))(messages(app)),
      CommodityCodeSummary.row(userAnswers)(messages(app))
    ).flatten
  )

  private def mockOttResponse(comCode: String = "1234567890") = OttResponse(
    GoodsNomenclatureResponse("some id", comCode, Some("Weight, in kilograms"), Instant.EPOCH, None, List("test")),
    categoryAssessmentRelationships = Seq(
      CategoryAssessmentRelationship("assessmentId2")
    ),
    includedElements = Seq(
      ThemeResponse("themeId1", 1, "theme description"),
      CategoryAssessmentResponse(
        "assessmentId2",
        "themeId2",
        Seq(
          ExemptionResponse("exemptionId1", ResponseExemptionType.Certificate),
          ExemptionResponse("exemptionId2", ResponseExemptionType.AdditionalCode)
        ),
        "regulationId1"
      ),
      ThemeResponse("themeId2", 2, "theme description"),
      CertificateResponse("exemptionId1", "code1", "description1"),
      AdditionalCodeResponse("exemptionId2", "code2", "description2"),
      ThemeResponse("ignoredTheme", 3, "theme description"),
      CertificateResponse("ignoredExemption", "code3", "description3"),
      LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1"))
    ),
    descendents = Seq.empty[Descendant]
  )

  "CyaCreateRecordController" - {

    "for a GET" - {

      "must return OK and the correct view with valid mandatory data" in {

        val mockOttConnector = mock[OttConnector]
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(Seq(Country("CN", "China")))

        val application =
          applicationBuilder(userAnswers = Some(mandatoryRecordUserAnswers))
            .overrides(bind[OttConnector].toInstance(mockOttConnector))
            .build()

        running(application) {
          val request = FakeRequest(GET, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaCreateRecordView]
          val list = createChangeList(mandatoryRecordUserAnswers, application)

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

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaCreateRecordView]
          val list = createChangeList(userAnswers, application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
          verify(mockOttConnector, atLeastOnce()).getCountries(any())
        }
      }

      "must return OK and the correct view when countries query has countries in it" in {

        val userAnswers = fullRecordUserAnswers.set(CountriesQuery, Seq(Country("CN", "China"))).success.value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaCreateRecordView]
          val list = createChangeList(userAnswers, application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery if no answers are found" in {

        val application = applicationBuilder(Some(emptyUserAnswers)).build()
        val continueUrl = RedirectUrl(controllers.goodsRecord.routes.CreateRecordStartController.onPageLoad().url)

        running(application) {
          val request = FakeRequest(GET, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)

          val result = route(application, request).value

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

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for a POST" - {

      "when user answers can create a valid goods record" - {

        "must submit the goods record and redirect to the CreateRecordSuccessController and cleanse userAnswers" in {

          val userAnswers = mandatoryRecordUserAnswers

          when(
            mockOttConnector.getCategorisationInfo(
              eqTo("1234567890"),
              eqTo(testEori),
              eqTo(AffinityGroup.Individual),
              eqTo(Some("test")),
              eqTo("1"),
              any[LocalDate]
            )(any[HeaderCarrier])
          ).thenReturn(Future.successful(mockOttResponse()))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                bind[AuditService].toInstance(mockAuditService),
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[OttConnector].toInstance(mockOttConnector)
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaCreateRecordController.onPageLoad().url).withCSRFToken
            val result  = route(application, request).value

            val expectedPayload = GoodsRecord(
              testEori,
              "123",
              testCommodity,
              "DESCRIPTION",
              "1"
            )

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.CreateRecordSuccessController.onPageLoad("test").url
            verify(mockGoodsRecordConnector, atLeastOnce()).submitGoodsRecord(eqTo(expectedPayload))(any())
            verify(mockAuditService, atLeastOnce())
              .auditFinishCreateGoodsRecord(eqTo(testEori), eqTo(AffinityGroup.Individual), eqTo(userAnswers))(any())
            verify(mockSessionRepository, atLeastOnce()).clearData(eqTo(userAnswers.id), eqTo(CreateRecordJourney))
          }
        }
      }

      //TODO - write tests for all cases related to autocategorisation - stubs/test data may need to be created

      "when user answers cannot create a goods record" - {

        "must not submit anything, and redirect to Journey Recovery" in {

          val mockConnector    = mock[GoodsRecordConnector]
          val mockAuditService = mock[AuditService]
          val continueUrl      = RedirectUrl(controllers.goodsRecord.routes.CreateRecordStartController.onPageLoad().url)

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

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
              .url
            verify(mockConnector, never()).submitGoodsRecord(any())(any())

            withClue("must not try and submit an audit") {
              verify(mockAuditService, never()).auditFinishCreateGoodsRecord(any(), any(), any())(any())
            }
            withClue("must cleanse the user answers data") {
              verify(sessionRepository).clearData(eqTo(emptyUserAnswers.id), eqTo(CreateRecordJourney))
            }
          }

        }
      }

      "must let the play error handler deal with connector failure" in {

        val userAnswers = mandatoryRecordUserAnswers

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
          withClue("must call the audit connector with the supplied details") {
            verify(mockAuditService)
              .auditFinishCreateGoodsRecord(eqTo(testEori), eqTo(AffinityGroup.Individual), eqTo(userAnswers))(any())
          }
          withClue("must not cleanse the user answers data when connector fails") {
            verify(sessionRepository, never()).clearData(eqTo(userAnswers.id), eqTo(CreateRecordJourney))
          }

        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
