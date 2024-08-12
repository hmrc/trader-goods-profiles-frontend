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
import forms.HasSupplementaryUnitFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{HasSupplementaryUnitPage, HasSupplementaryUnitUpdatePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.HasSupplementaryUnitView

import java.time.Instant
import scala.concurrent.Future

class HasSupplementaryUnitControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new HasSupplementaryUnitFormProvider()
  private val form         = formProvider()
  private val recordId     = "record id"

  private lazy val hasSupplementaryUnitRoute  =
    routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId).url
  private lazy val hasSupplementaryUnitRoute2 =
    routes.HasSupplementaryUnitController.onPageLoad2(NormalMode, recordId).url

  private lazy val onSubmitAction: Call = routes.HasSupplementaryUnitController.onSubmit(NormalMode, recordId)

  private lazy val hasSupplementaryUnitUpdateRoute =
    routes.HasSupplementaryUnitController.onPageLoadUpdate(NormalMode, recordId).url

  private lazy val onSubmitUpdateAction: Call =
    routes.HasSupplementaryUnitController.onSubmitUpdate(NormalMode, recordId)

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

  private val record = goodsRecordResponseWithSupplementaryUnit(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId)

  private val recordWithoutSuppUnit = goodsRecordResponseWithOutSupplementaryUnit(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId)

  "HasSupplementaryUnit Controller" - {
    "for a GET 2" - {
      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasSupplementaryUnitRoute2)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasSupplementaryUnitView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, recordId, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(HasSupplementaryUnitPage(recordId), true).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasSupplementaryUnitRoute2)

          val view = application.injector.instanceOf[HasSupplementaryUnitView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), NormalMode, recordId, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

    }

    ".create journey" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, hasSupplementaryUnitRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasSupplementaryUnitView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, recordId, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(HasSupplementaryUnitPage(recordId), true).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, hasSupplementaryUnitRoute)

          val view = application.injector.instanceOf[HasSupplementaryUnitView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), NormalMode, recordId, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must not populate the view on a GET when the question has previously been answered for another recordId" in {

        val userAnswers = UserAnswers(userAnswersId).set(HasSupplementaryUnitPage(s"${recordId}2"), true).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, hasSupplementaryUnitRoute)

          val view = application.injector.instanceOf[HasSupplementaryUnitView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, recordId, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, hasSupplementaryUnitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, hasSupplementaryUnitRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[HasSupplementaryUnitView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, recordId, onSubmitAction)(
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
          val request = FakeRequest(GET, hasSupplementaryUnitRoute)

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
            FakeRequest(POST, hasSupplementaryUnitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
    ".update journey" - {
      "must return OK and the correct view for a GET" in {

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
          .successful(record)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, hasSupplementaryUnitUpdateRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasSupplementaryUnitView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), NormalMode, recordId, onSubmitUpdateAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and the correct view for a GET - should set to false when supplementary value is 0" in {

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
          .successful(recordWithoutSuppUnit)

        when(mockGoodsRecordConnector.updateSupplementaryUnitForGoodsRecord(any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, hasSupplementaryUnitUpdateRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasSupplementaryUnitView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(false), NormalMode, recordId, onSubmitUpdateAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = UserAnswers(userAnswersId).set(HasSupplementaryUnitUpdatePage(recordId), true).success.value

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
          .successful(record)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.HasSupplementaryUnitController.onPageLoadUpdate(NormalMode, recordId).url)

          val view = application.injector.instanceOf[HasSupplementaryUnitView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(true),
            NormalMode,
            recordId,
            routes.HasSupplementaryUnitController.onSubmitUpdate(NormalMode, recordId)
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {
        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.HasSupplementaryUnitController.onSubmitUpdate(NormalMode, recordId).url)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.HasSupplementaryUnitController.onSubmitUpdate(NormalMode, recordId).url)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[HasSupplementaryUnitView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            NormalMode,
            recordId,
            routes.HasSupplementaryUnitController.onSubmitUpdate(NormalMode, recordId)
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
            FakeRequest(GET, routes.HasSupplementaryUnitController.onPageLoadUpdate(NormalMode, recordId).url)

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
            FakeRequest(POST, routes.HasSupplementaryUnitController.onSubmitUpdate(NormalMode, recordId).url)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }
  }

}
