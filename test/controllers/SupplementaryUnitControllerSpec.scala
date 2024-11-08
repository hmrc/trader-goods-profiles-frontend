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
import base.TestConstants.{testEori, testRecordId, userAnswersId}
import connectors.{GoodsRecordConnector, TraderProfileConnector}
import forms.SupplementaryUnitFormProvider
import models.helper.SupplementaryUnitUpdate
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigation, Navigation}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{HasSupplementaryUnitUpdatePage, SupplementaryUnitPage, SupplementaryUnitUpdatePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import repositories.SessionRepository
import services.{AuditService, OttService}
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.SupplementaryUnitView

import java.time.Instant
import scala.concurrent.Future

class SupplementaryUnitControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new SupplementaryUnitFormProvider()
  private val form         = formProvider()

  private def onwardRoute = Call("GET", "/foo")

  private val validAnswer = "10.0"

  lazy val submitAction: Call = routes.SupplementaryUnitController.onSubmit(NormalMode, testRecordId)

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

  private val record                       = goodsRecordResponseWithSupplementaryUnit(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId)
  private lazy val supplementaryUnitRoute2 =
    routes.SupplementaryUnitController.onPageLoad(NormalMode, testRecordId).url

  "SupplementaryUnit Controller" - {

    "categorisation journey" - {

      "when no longer commodity code" - {
        "for a GET" - {

          "must return OK and the correct view" in {

            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request = FakeRequest(GET, supplementaryUnitRoute2)

              val result = route(application, request).value

              val view = application.injector.instanceOf[SupplementaryUnitView]

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                Some("Weight, in kilograms"),
                submitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }

          "must return OK and the correct view when Measurement Unit is Empty" in {

            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfoWithEmptyMeasurementUnit)
              .success
              .value

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request = FakeRequest(GET, supplementaryUnitRoute2)

              val result = route(application, request).value

              val view = application.injector.instanceOf[SupplementaryUnitView]

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(form, NormalMode, testRecordId, None, submitAction)(
                request,
                messages(application)
              ).toString
            }
          }

          "must populate the view correctly when the question has previously been answered" in {

            val userAnswers = UserAnswers(userAnswersId)
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value
              .set(SupplementaryUnitPage(testRecordId), validAnswer)
              .success
              .value

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request = FakeRequest(GET, supplementaryUnitRoute2)

              val view = application.injector.instanceOf[SupplementaryUnitView]

              val result = route(application, request).value

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form.fill(validAnswer),
                NormalMode,
                testRecordId,
                Some("Weight, in kilograms"),
                submitAction
              )(request, messages(application)).toString
            }
          }

          "must redirect to Journey Recovery for a GET if no answers are found" in {

            val application = applicationBuilder(userAnswers = None).build()

            running(application) {
              val request = FakeRequest(GET, supplementaryUnitRoute2)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad()
                .url
            }
          }

          "must redirect to Journey Recovery for a GET if no category info is found" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request = FakeRequest(GET, supplementaryUnitRoute2)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad()
                .url
            }
          }

        }

        "for a POST" - {

          "must redirect to the next page when valid data is submitted" in {

            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value

            val mockSessionRepository = mock[SessionRepository]

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository)
                )
                .build()

            running(application) {
              val request =
                FakeRequest(POST, supplementaryUnitRoute2)
                  .withFormUrlEncodedBody(("value", validAnswer))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url

              verify(mockSessionRepository).set(any())
            }
          }

          "must return a Bad Request and errors when invalid data is submitted" in {

            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request =
                FakeRequest(POST, supplementaryUnitRoute2)
                  .withFormUrlEncodedBody(("value", "invalid value"))

              val boundForm = form.bind(Map("value" -> "invalid value"))

              val view = application.injector.instanceOf[SupplementaryUnitView]

              val result = route(application, request).value

              status(result) mustEqual BAD_REQUEST
              contentAsString(result) mustEqual view(
                boundForm,
                NormalMode,
                testRecordId,
                Some("Weight, in kilograms"),
                submitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }

          "must return a Bad Request and errors when invalid data is submitted and Measurement Unit is Empty" in {

            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfoWithEmptyMeasurementUnit)
              .success
              .value

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request =
                FakeRequest(POST, supplementaryUnitRoute2)
                  .withFormUrlEncodedBody(("value", "invalid value"))

              val boundForm = form.bind(Map("value" -> "invalid value"))

              val view = application.injector.instanceOf[SupplementaryUnitView]

              val result = route(application, request).value

              status(result) mustEqual BAD_REQUEST
              contentAsString(result) mustEqual view(boundForm, NormalMode, testRecordId, None, submitAction)(
                request,
                messages(application)
              ).toString
            }
          }

          "must redirect to Journey Recovery if no answers are found" in {

            val application = applicationBuilder(userAnswers = None).build()

            running(application) {
              val request =
                FakeRequest(POST, supplementaryUnitRoute2)
                  .withFormUrlEncodedBody(("value", validAnswer))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad()
                .url
            }
          }

          "must redirect to Journey Recovery if invalid answer and no category info is found" in {
            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request =
                FakeRequest(POST, supplementaryUnitRoute2)
                  .withFormUrlEncodedBody(("value", "invalid"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad()
                .url
            }
          }

        }
      }

      "when longer commodity code" - {
        "for a GET" - {

          "must return OK and the correct view" in {

            val userAnswers = emptyUserAnswers
              .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request = FakeRequest(GET, supplementaryUnitRoute2)

              val result = route(application, request).value

              val view = application.injector.instanceOf[SupplementaryUnitView]

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                Some("Weight, in kilograms"),
                submitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }

          "must return OK and the correct view when Measurement Unit is Empty" in {

            val userAnswers = emptyUserAnswers
              .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfoWithEmptyMeasurementUnit)
              .success
              .value

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request = FakeRequest(GET, supplementaryUnitRoute2)

              val result = route(application, request).value

              val view = application.injector.instanceOf[SupplementaryUnitView]

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(form, NormalMode, testRecordId, None, submitAction)(
                request,
                messages(application)
              ).toString
            }
          }

          "must populate the view correctly when the question has previously been answered" in {

            val userAnswers = UserAnswers(userAnswersId)
              .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value
              .set(SupplementaryUnitPage(testRecordId), validAnswer)
              .success
              .value

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request = FakeRequest(GET, supplementaryUnitRoute2)

              val view = application.injector.instanceOf[SupplementaryUnitView]

              val result = route(application, request).value

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form.fill(validAnswer),
                NormalMode,
                testRecordId,
                Some("Weight, in kilograms"),
                submitAction
              )(request, messages(application)).toString
            }
          }

        }

        "for a POST" - {

          "must redirect to the next page when valid data is submitted" in {

            val userAnswers = emptyUserAnswers
              .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value

            val mockSessionRepository = mock[SessionRepository]

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository)
                )
                .build()

            running(application) {
              val request =
                FakeRequest(POST, supplementaryUnitRoute2)
                  .withFormUrlEncodedBody(("value", validAnswer))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url

              verify(mockSessionRepository).set(any())
            }
          }

          "must return a Bad Request and errors when invalid data is submitted" in {

            val userAnswers = emptyUserAnswers
              .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request =
                FakeRequest(POST, supplementaryUnitRoute2)
                  .withFormUrlEncodedBody(("value", "invalid value"))

              val boundForm = form.bind(Map("value" -> "invalid value"))

              val view = application.injector.instanceOf[SupplementaryUnitView]

              val result = route(application, request).value

              status(result) mustEqual BAD_REQUEST
              contentAsString(result) mustEqual view(
                boundForm,
                NormalMode,
                testRecordId,
                Some("Weight, in kilograms"),
                submitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }

          "must return a Bad Request and errors when invalid data is submitted and Measurement Unit is Empty" in {

            val userAnswers = emptyUserAnswers
              .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfoWithEmptyMeasurementUnit)
              .success
              .value

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request =
                FakeRequest(POST, supplementaryUnitRoute2)
                  .withFormUrlEncodedBody(("value", "invalid value"))

              val boundForm = form.bind(Map("value" -> "invalid value"))

              val view = application.injector.instanceOf[SupplementaryUnitView]

              val result = route(application, request).value

              status(result) mustEqual BAD_REQUEST
              contentAsString(result) mustEqual view(boundForm, NormalMode, testRecordId, None, submitAction)(
                request,
                messages(application)
              ).toString
            }
          }

        }

      }

    }

    "update journey" - {

      "must return OK and the correct view for a GET" in {
        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        val mockAuditService         = mock[AuditService]

        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.successful(record))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.SupplementaryUnitController.onPageLoadUpdate(NormalMode, testRecordId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SupplementaryUnitView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill("1234567890.123456"),
            NormalMode,
            testRecordId,
            Some("grams"),
            routes.SupplementaryUnitController.onSubmitUpdate(NormalMode, testRecordId)
          )(
            request,
            messages(application)
          ).toString

          withClue("must call the audit service with the correct details") {
            verify(mockAuditService)
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(SupplementaryUnitUpdate),
                eqTo(testRecordId),
                any()
              )(any())
            verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
          }
        }
      }

      "must return OK and the correct view for a GET when the HasSupplementaryUnitUpdatePage has been filled in" in {
        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        val mockAuditService         = mock[AuditService]
        val answers                  = emptyUserAnswers
          .set(HasSupplementaryUnitUpdatePage(testRecordId), true)
          .success
          .value

        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.successful(record))

        val application =
          applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.SupplementaryUnitController.onPageLoadUpdate(NormalMode, testRecordId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SupplementaryUnitView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill("1234567890.123456"),
            NormalMode,
            testRecordId,
            Some("grams"),
            routes.SupplementaryUnitController.onSubmitUpdate(NormalMode, testRecordId)
          )(
            request,
            messages(application)
          ).toString

          withClue("must not call the audit service with the correct details") {
            verify(mockAuditService, never())
              .auditStartUpdateGoodsRecord(
                any(),
                any(),
                any(),
                any(),
                any()
              )(any())
            verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
          }
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers      = UserAnswers(userAnswersId)
          .set(SupplementaryUnitUpdatePage(testRecordId), validAnswer)
          .success
          .value
        val mockAuditService = mock[AuditService]

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]

        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.successful(record))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[AuditService].toInstance(mockAuditService),
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.SupplementaryUnitController.onPageLoadUpdate(NormalMode, testRecordId).url)

          val view = application.injector.instanceOf[SupplementaryUnitView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(validAnswer),
            NormalMode,
            testRecordId,
            Some("grams"),
            routes.SupplementaryUnitController.onSubmitUpdate(NormalMode, testRecordId)
          )(
            request,
            messages(application)
          ).toString

          withClue("must call the audit service with the correct details") {
            verify(mockAuditService)
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(SupplementaryUnitUpdate),
                eqTo(testRecordId),
                any()
              )(any())
            verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
          }
        }
      }

      "must redirect to the next page when valid data is submitted" in {
        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        val mockOttService = mock[OttService]
        when(mockOttService.getMeasurementUnit(any(), any())(any())) thenReturn Future.successful(Some("litres"))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[OttService].toInstance(mockOttService)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.SupplementaryUnitController.onSubmitUpdate(NormalMode, testRecordId).url)
              .withFormUrlEncodedBody(("value", validAnswer))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockSessionRepository).set(any())
          verify(mockOttService).getMeasurementUnit(any(), any())(any())
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        val mockOttService = mock[OttService]

        when(mockOttService.getMeasurementUnit(any(), any())(any())) thenReturn Future.successful(Some(""))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[OttService].toInstance(mockOttService),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.SupplementaryUnitController.onSubmitUpdate(NormalMode, testRecordId).url)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[SupplementaryUnitView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            NormalMode,
            testRecordId,
            None,
            routes.SupplementaryUnitController.onSubmitUpdate(NormalMode, testRecordId)
          )(
            request,
            messages(application)
          ).toString
          verify(mockOttService).getMeasurementUnit(any(), any())(any())
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.SupplementaryUnitController.onPageLoadUpdate(NormalMode, testRecordId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.SupplementaryUnitController.onSubmitUpdate(NormalMode, testRecordId).url)
              .withFormUrlEncodedBody(("value", validAnswer))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if record connector fails" in {

        val mockOttService = mock[OttService]

        when(mockOttService.getMeasurementUnit(any(), any())(any())) thenReturn Future.failed(new RuntimeException())
        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[OttService].toInstance(mockOttService)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.SupplementaryUnitController.onSubmitUpdate(NormalMode, testRecordId).url)
              .withFormUrlEncodedBody(("value", validAnswer))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockOttService, never()).getMeasurementUnit(any(), any())(any())
        }
      }

      "must redirect to Journey Recovery for GET if record connector fails" in {
        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        val mockAuditService         = mock[AuditService]

        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException()))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.SupplementaryUnitController.onPageLoadUpdate(NormalMode, testRecordId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url

          withClue("must call the audit service with the correct details") {
            verify(mockAuditService)
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(SupplementaryUnitUpdate),
                eqTo(testRecordId),
                any()
              )(any())

            verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
          }
        }
      }
    }
  }
}
