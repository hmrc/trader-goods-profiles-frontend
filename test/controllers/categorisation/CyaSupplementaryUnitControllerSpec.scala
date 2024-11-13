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

package controllers.categorisation

import base.SpecBase
import base.TestConstants.{testEori, testRecordId, userAnswersId}
import connectors.GoodsRecordConnector
import controllers.routes
import models.helper.SupplementaryUnitUpdateJourney
import models.{NormalMode, SupplementaryRequest, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.SupplementaryUnitUpdatePage
import pages.categorisation.HasSupplementaryUnitUpdatePage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.MeasurementQuery
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.SessionData._
import viewmodels.checkAnswers.{HasSupplementaryUnitSummary, SupplementaryUnitSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.categorisation.CyaSupplementaryUnitView

import java.time.Instant
import scala.concurrent.Future

class CyaSupplementaryUnitControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaSupplementaryUnitController" - {

    val record = goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ).copy(recordId = testRecordId, eori = testEori)

    "for a GET" - {

      "must return OK and the correct view with valid data" in {

        val application                      = applicationBuilder(userAnswers = Some(mandatorySupplementaryUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.categorisation.routes.CyaSupplementaryUnitController.onPageLoad(testRecordId).url
          )

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaSupplementaryUnitView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            SummaryListViewModel(
              rows = Seq(
                HasSupplementaryUnitSummary.rowUpdate(mandatorySupplementaryUserAnswers, testRecordId),
                SupplementaryUnitSummary.rowUpdate(mandatorySupplementaryUserAnswers, testRecordId)
              ).flatten
            ),
            testRecordId
          )(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery if no answers are found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.categorisation.routes.CyaSupplementaryUnitController.onPageLoad("recordId").url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when user answers cannot create a supplementary request" - {

        "must not submit anything, and redirect to Journey Recovery" in {

          val continueUrl =
            RedirectUrl(
              controllers.categorisation.routes.HasSupplementaryUnitController
                .onPageLoadUpdate(NormalMode, testRecordId)
                .url
            )

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .build()

          running(application) {
            val request = FakeRequest(
              GET,
              controllers.categorisation.routes.CyaSupplementaryUnitController.onPageLoad(testRecordId).url
            )

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
              .url

          }
        }
      }

    }

    "for a POST" - {

      "when user answers can create a valid supplementary request" - {

        "must submit the supplementary request and redirect to the SingleRecordController and cleanse userAnswers" in {

          val userAnswers = mandatorySupplementaryUserAnswers

          val mockAuditService = mock[AuditService]

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(record))
          when(mockGoodsRecordConnector.updateSupplementaryUnitForGoodsRecord(any(), any(), any(), any())(any()))
            .thenReturn(Future.successful(Done))

          val sessionRepository = mock[SessionRepository]
          when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[SessionRepository].toInstance(sessionRepository)
            )
            .build()

          running(application) {
            val request = FakeRequest(
              POST,
              controllers.categorisation.routes.CyaSupplementaryUnitController.onSubmit(testRecordId).url
            )

            val result = route(application, request).value

            val expectedPayload = SupplementaryRequest(
              eori = testEori,
              recordId = testRecordId,
              hasSupplementaryUnit = Some(true),
              supplementaryUnit = Some("1234567890.123456"),
              measurementUnit = Some("litres")
            )

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
              .onPageLoad(testRecordId)
              .url
            verify(mockGoodsRecordConnector)
              .updateSupplementaryUnitForGoodsRecord(eqTo(testEori), eqTo(testRecordId), eqTo(expectedPayload), any())(
                any()
              )

            withClue("must cleanse the user answers data") {
              verify(sessionRepository).clearData(eqTo(userAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
            }

            withClue("must submit an audit") {
              verify(mockAuditService).auditFinishUpdateSupplementaryUnitGoodsRecord(any(), any(), any())(any())
            }
          }
        }

        "must set dataUpdated to true if supplementary question is updated" in {

          val userAnswers = UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitUpdatePage(testRecordId), true)
            .success
            .value
            .set(SupplementaryUnitUpdatePage(testRecordId), "200")
            .success
            .value
            .set(MeasurementQuery(testRecordId), "litres")
            .success
            .value

          val mockAuditService = mock[AuditService]

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(record))
          when(mockGoodsRecordConnector.updateSupplementaryUnitForGoodsRecord(any(), any(), any(), any())(any()))
            .thenReturn(Future.successful(Done))

          val sessionRepository = mock[SessionRepository]
          when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[SessionRepository].toInstance(sessionRepository)
            )
            .build()

          running(application) {
            val controller = application.injector.instanceOf[CyaSupplementaryUnitController]
            val request    = FakeRequest(
              POST,
              controllers.categorisation.routes.CyaSupplementaryUnitController.onSubmit(testRecordId).url
            )
              .withSession(
                initialValueOfHasSuppUnit -> "false"
              )

            val result: Future[Result] = controller.onSubmit(testRecordId)(request)

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
              .onPageLoad(testRecordId)
              .url

            session(result).get(dataUpdated) must be(Some("true"))
            session(result).get(pageUpdated) must be(Some("supplementary unit"))

            withClue("must submit an audit") {
              verify(mockAuditService).auditFinishUpdateSupplementaryUnitGoodsRecord(any(), any(), any())(any())
            }
          }
        }

        "must set dataUpdated to true if supplementary unit is updated" in {

          val userAnswers = UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitUpdatePage(testRecordId), true)
            .success
            .value
            .set(SupplementaryUnitUpdatePage(testRecordId), "200")
            .success
            .value
            .set(MeasurementQuery(testRecordId), "litres")
            .success
            .value

          val mockAuditService = mock[AuditService]

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(record))
          when(mockGoodsRecordConnector.updateSupplementaryUnitForGoodsRecord(any(), any(), any(), any())(any()))
            .thenReturn(Future.successful(Done))

          val sessionRepository = mock[SessionRepository]
          when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[SessionRepository].toInstance(sessionRepository)
            )
            .build()

          running(application) {
            val controller = application.injector.instanceOf[CyaSupplementaryUnitController]
            val request    = FakeRequest(
              POST,
              controllers.categorisation.routes.CyaSupplementaryUnitController.onSubmit(testRecordId).url
            )
              .withSession(
                initialValueOfHasSuppUnit -> "true",
                initialValueOfSuppUnit    -> "300"
              )

            val result: Future[Result] = controller.onSubmit(testRecordId)(request)

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
              .onPageLoad(testRecordId)
              .url

            session(result).get(dataUpdated) must be(Some("true"))
            session(result).get(pageUpdated) must be(Some("supplementary unit"))

            withClue("must submit an audit") {
              verify(mockAuditService).auditFinishUpdateSupplementaryUnitGoodsRecord(any(), any(), any())(any())
            }
          }
        }

        "must set dataUpdated to false if no updates made" in {

          val userAnswers = UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitUpdatePage(testRecordId), true)
            .success
            .value
            .set(SupplementaryUnitUpdatePage(testRecordId), "200.0")
            .success
            .value
            .set(MeasurementQuery(testRecordId), "litres")
            .success
            .value

          val mockAuditService = mock[AuditService]

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(record))
          when(mockGoodsRecordConnector.updateSupplementaryUnitForGoodsRecord(any(), any(), any(), any())(any()))
            .thenReturn(Future.successful(Done))

          val sessionRepository = mock[SessionRepository]
          when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[SessionRepository].toInstance(sessionRepository)
            )
            .build()

          running(application) {
            val controller = application.injector.instanceOf[CyaSupplementaryUnitController]
            val request    = FakeRequest(
              POST,
              controllers.categorisation.routes.CyaSupplementaryUnitController.onSubmit(testRecordId).url
            )
              .withSession(
                initialValueOfHasSuppUnit -> "true",
                initialValueOfSuppUnit    -> "200"
              )

            val result: Future[Result] = controller.onSubmit(testRecordId)(request)

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
              .onPageLoad(testRecordId)
              .url

            session(result).get(dataUpdated) must be(Some("false"))

            withClue("must submit an audit") {
              verify(mockAuditService).auditFinishUpdateSupplementaryUnitGoodsRecord(any(), any(), any())(any())
            }
          }
        }
      }

      "must set dataRemoved to true if supplementary unit is removed" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(HasSupplementaryUnitUpdatePage(testRecordId), false)
          .success
          .value

        val mockAuditService = mock[AuditService]

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(record))
        when(mockGoodsRecordConnector.updateSupplementaryUnitForGoodsRecord(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[AuditService].toInstance(mockAuditService),
            bind[SessionRepository].toInstance(sessionRepository)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[CyaSupplementaryUnitController]
          val request    = FakeRequest(
            POST,
            controllers.categorisation.routes.CyaSupplementaryUnitController.onSubmit(testRecordId).url
          )
            .withSession(
              initialValueOfHasSuppUnit -> "true"
            )

          val result: Future[Result] = controller.onSubmit(testRecordId)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController
            .onPageLoad(testRecordId)
            .url

          session(result).get(dataRemoved) must be(Some("true"))
          session(result).get(pageUpdated) must be(Some("supplementary unit"))

          withClue("must submit an audit") {
            verify(mockAuditService, atLeastOnce()).auditFinishUpdateSupplementaryUnitGoodsRecord(any(), any(), any())(
              any()
            )
          }
        }
      }

      "must let the play error handler deal with connector failure" in {

        val userAnswers = mandatorySupplementaryUserAnswers

        val mockAuditService = mock[AuditService]

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(record))
        when(mockGoodsRecordConnector.updateSupplementaryUnitForGoodsRecord(any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Connector failed")))

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[AuditService].toInstance(mockAuditService),
            bind[SessionRepository].toInstance(sessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(
            POST,
            controllers.categorisation.routes.CyaSupplementaryUnitController.onSubmit(testRecordId).url
          )

          intercept[RuntimeException] {
            await(route(application, request).value)
          }

          withClue("must submit an audit") {
            verify(mockAuditService, atLeastOnce()).auditFinishUpdateSupplementaryUnitGoodsRecord(any(), any(), any())(
              any()
            )
          }

          withClue("must not cleanse the user answers data when connector fails") {
            verify(sessionRepository, never()).clearData(eqTo(userAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
          }
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.categorisation.routes.CyaSupplementaryUnitController.onSubmit("recordId").url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when user answers cannot create a supplementary request" - {

        "must not submit anything, and redirect to Journey Recovery" in {

          val mockAuditService = mock[AuditService]

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          val continueUrl              =
            RedirectUrl(
              controllers.categorisation.routes.HasSupplementaryUnitController
                .onPageLoadUpdate(NormalMode, testRecordId)
                .url
            )

          val sessionRepository = mock[SessionRepository]
          when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[SessionRepository].toInstance(sessionRepository)
            )
            .build()

          running(application) {
            val request = FakeRequest(
              POST,
              controllers.categorisation.routes.CyaSupplementaryUnitController.onSubmit(testRecordId).url
            )

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
              .url
            verify(mockGoodsRecordConnector, never()).updateSupplementaryUnitForGoodsRecord(any(), any(), any(), any())(
              any()
            )

            withClue("must cleanse the user answers data") {
              verify(sessionRepository, atLeastOnce())
                .clearData(eqTo(emptyUserAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
            }

            withClue("must not submit an audit") {
              verify(mockAuditService, never()).auditFinishUpdateSupplementaryUnitGoodsRecord(any(), any(), any())(
                any()
              )
            }
          }
        }
      }
    }
  }
}
