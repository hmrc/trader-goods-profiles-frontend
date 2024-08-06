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
import base.TestConstants.{testRecordId, userAnswersId}
import connectors.{GoodsRecordConnector, TraderProfileConnector}
import forms.SupplementaryUnitFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{SupplementaryUnitPage, SupplementaryUnitUpdatePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.RecordCategorisationsQuery
import repositories.SessionRepository
import services.OttService
import views.html.SupplementaryUnitView

import java.time.Instant
import scala.concurrent.Future

class SupplementaryUnitControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new SupplementaryUnitFormProvider()
  private val form         = formProvider()

  private def onwardRoute = Call("GET", "/foo")

  private val validAnswer = "10.0"

  private lazy val supplementaryUnitRoute = routes.SupplementaryUnitController.onPageLoad(NormalMode, testRecordId).url
  lazy val submitAction: Call             = routes.SupplementaryUnitController.onSubmit(NormalMode, testRecordId)

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

  private val record = goodsRecordResponseWithSupplementaryUnit(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId)

  "SupplementaryUnit Controller" - {

    "create journey" - {

      "must return OK and the correct view for a GET" in {

        val userAnswers = emptyUserAnswers
          .set(RecordCategorisationsQuery, recordCategorisations)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, supplementaryUnitRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SupplementaryUnitView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, testRecordId, "Weight, in kilograms", submitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and the correct view for a GET When Measurement Unit is Empty" in {

        val userAnswers = emptyUserAnswers
          .set(RecordCategorisationsQuery, recordCategorisationsEmptyMeasurementUnit)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, supplementaryUnitRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SupplementaryUnitView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, testRecordId, "", submitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(RecordCategorisationsQuery, recordCategorisations)
          .success
          .value
          .set(SupplementaryUnitPage(testRecordId), validAnswer)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, supplementaryUnitRoute)

          val view = application.injector.instanceOf[SupplementaryUnitView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(validAnswer),
            NormalMode,
            testRecordId,
            "Weight, in kilograms",
            submitAction
          )(request, messages(application)).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val userAnswers = emptyUserAnswers
          .set(RecordCategorisationsQuery, recordCategorisations)
          .success
          .value

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, supplementaryUnitRoute)
              .withFormUrlEncodedBody(("value", validAnswer))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val userAnswers = emptyUserAnswers
          .set(RecordCategorisationsQuery, recordCategorisations)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, supplementaryUnitRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[SupplementaryUnitView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            NormalMode,
            testRecordId,
            "Weight, in kilograms",
            submitAction
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must return a Bad Request and errors when invalid data is submitted and Measurement Unit is Empty" in {

        val userAnswers = emptyUserAnswers
          .set(RecordCategorisationsQuery, recordCategorisationsEmptyMeasurementUnit)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, supplementaryUnitRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[SupplementaryUnitView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, testRecordId, "", submitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, supplementaryUnitRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, supplementaryUnitRoute)
              .withFormUrlEncodedBody(("value", validAnswer))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the RecordCategorisationsQuery is empty for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, supplementaryUnitRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when invalid data is submitted and RecordCategorisationsQuery is empty" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, supplementaryUnitRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "update journey" - {
      "must return OK and the correct view for a GET" in {
        val mockGoodsRecordConnector = mock[GoodsRecordConnector]

        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.successful(record))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
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
            form.fill("1234.0"),
            NormalMode,
            testRecordId,
            "grams",
            routes.SupplementaryUnitController.onSubmitUpdate(NormalMode, testRecordId)
          )(
            request,
            messages(application)
          ).toString
        }
      }
      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(SupplementaryUnitUpdatePage(testRecordId), validAnswer)
          .success
          .value

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]

        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.successful(record))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
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
            "grams",
            routes.SupplementaryUnitController.onSubmitUpdate(NormalMode, testRecordId)
          )(
            request,
            messages(application)
          ).toString
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
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
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
            "",
            routes.SupplementaryUnitController.onSubmitUpdate(NormalMode, testRecordId)
          )(
            request,
            messages(application)
          ).toString
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
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
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

          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
