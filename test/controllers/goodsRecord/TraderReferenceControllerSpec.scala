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
import base.TestConstants.{testEori, testRecordId, userAnswersId}
import connectors.GoodsRecordConnector
import forms.goodsRecord.TraderReferenceFormProvider
import models.helper.GoodsDetailsUpdate
import models.{NormalMode, UserAnswers}
import navigation.{FakeGoodsRecordNavigator, GoodsRecordNavigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.{TraderReferencePage, TraderReferenceUpdatePage}
import play.api.data.FormError
import play.api.inject.bind
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.SessionData.{dataUpdated, pageUpdated}
import views.html.goodsRecord.TraderReferenceView

import java.time.Instant
import scala.concurrent.Future

class TraderReferenceControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider   = new TraderReferenceFormProvider()
  private val form   = formProvider()
  private val record = goodsRecordResponse(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  )

  "TraderReference Controller" - {

    "for create journey" - {

      lazy val traderReferenceRoute =
        controllers.goodsRecord.routes.TraderReferenceController.onPageLoadCreate(NormalMode).url
      lazy val onSubmitAction       = controllers.goodsRecord.routes.TraderReferenceController.onSubmitCreate(NormalMode)

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, traderReferenceRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[TraderReferenceView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, onSubmitAction)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(TraderReferencePage, "answer").success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, traderReferenceRoute)

          val view = application.injector.instanceOf[TraderReferenceView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("answer"), onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]

        when(mockGoodsRecordConnector.isTraderReferenceUnique(any())(any())) thenReturn Future
          .successful(
            true
          )

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, traderReferenceRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockSessionRepository).set(any())
          verify(mockGoodsRecordConnector).isTraderReferenceUnique(any())(any())
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, traderReferenceRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[TraderReferenceView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, onSubmitAction)(request, messages(application)).toString
        }
      }

      "must return a Bad Request and errors when an existing data is submitted" in {

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]

        when(mockGoodsRecordConnector.isTraderReferenceUnique(any())(any())) thenReturn Future
          .successful(
            false
          )

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, traderReferenceRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val boundForm = form
            .fill("answer")
            .copy(errors =
              Seq(elems =
                FormError("value", "This trader reference is already in your TGP. Enter a unique trader reference.")
              )
            )

          val view = application.injector.instanceOf[TraderReferenceView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, onSubmitAction)(request, messages(application)).toString
          verify(mockGoodsRecordConnector).isTraderReferenceUnique(any())(any())
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request = FakeRequest(GET, traderReferenceRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request =
            FakeRequest(POST, traderReferenceRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for update journey" - {

      lazy val traderReferenceRoute =
        controllers.goodsRecord.routes.TraderReferenceController.onPageLoadUpdate(NormalMode, testRecordId).url
      lazy val onSubmitAction       =
        controllers.goodsRecord.routes.TraderReferenceController.onSubmitUpdate(NormalMode, testRecordId)

      "must return OK and the correct view for a GET" in {
        val mockAuditService = mock[AuditService]

        when(mockAuditService.auditStartUpdateGoodsRecord(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, traderReferenceRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[TraderReferenceView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, onSubmitAction)(request, messages(application)).toString

          withClue("must call the audit service with the correct details") {
            verify(mockAuditService)
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(GoodsDetailsUpdate),
                eqTo(testRecordId),
                any()
              )(any())
          }
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers =
          UserAnswers(userAnswersId).set(TraderReferenceUpdatePage(testRecordId), "answer").success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, traderReferenceRoute)

          val view = application.injector.instanceOf[TraderReferenceView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("answer"), onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]

        when(mockGoodsRecordConnector.isTraderReferenceUnique(any())(any())) thenReturn Future
          .successful(
            true
          )

        when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
          .successful(
            record
          )

        val application =
          applicationBuilder(userAnswers =
            Some(emptyUserAnswers.set(TraderReferenceUpdatePage(recordId = testRecordId), "oldAnswer").success.value)
          )
            .overrides(
              bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, traderReferenceRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockSessionRepository).set(any())
          verify(mockGoodsRecordConnector).isTraderReferenceUnique(any())(any())
          verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
        }
      }

      "must redirect to the next page when no change has been made" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]

        when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
          .successful(
            record
          )

        when(mockGoodsRecordConnector.isTraderReferenceUnique(any())(any())) thenReturn Future
          .successful(
            false
          )

        val application =
          applicationBuilder(userAnswers =
            Some(emptyUserAnswers.set(TraderReferenceUpdatePage(recordId = testRecordId), "BAN0010011").success.value)
          )
            .overrides(
              bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, traderReferenceRoute)
              .withFormUrlEncodedBody(("value", "BAN0010011"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockSessionRepository).set(any())
          verify(mockGoodsRecordConnector).isTraderReferenceUnique(any())(any())
          verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
        }
      }

      "must set changesMade to true if trader reference is updated " in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]

        when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
          .successful(
            record
          )

        when(mockGoodsRecordConnector.isTraderReferenceUnique(any())(any())) thenReturn Future
          .successful(
            true
          )

        val userAnswers =
          UserAnswers(userAnswersId).set(TraderReferenceUpdatePage(testRecordId), "oldValue").success.value
        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val controller = application.injector.instanceOf[TraderReferenceController]
          val request    =
            FakeRequest(POST, traderReferenceRoute)
              .withFormUrlEncodedBody(("value", "newValue"))

          val result: Future[Result] = controller.onSubmitUpdate(NormalMode, testRecordId)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          session(result).get(dataUpdated) must be(Some("true"))
          session(result).get(pageUpdated) must be(Some("trader reference"))

          verify(mockSessionRepository).set(any())
          verify(mockGoodsRecordConnector).isTraderReferenceUnique(any())(any())
          verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
        }
      }

      "must set changesMade to false if trader reference is not updated" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]

        when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
          .successful(
            record
          )

        when(mockGoodsRecordConnector.isTraderReferenceUnique(any())(any())) thenReturn Future
          .successful(
            false
          )

        val userAnswers =
          UserAnswers(userAnswersId).set(TraderReferenceUpdatePage(testRecordId), "BAN0010011").success.value
        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val controller = application.injector.instanceOf[TraderReferenceController]
          val request    =
            FakeRequest(POST, traderReferenceRoute)
              .withFormUrlEncodedBody(("value", "BAN0010011"))

          val result: Future[Result] = controller.onSubmitUpdate(NormalMode, testRecordId)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          session(result).get(dataUpdated) must be(Some("false"))

          verify(mockSessionRepository).set(any())
          verify(mockGoodsRecordConnector).isTraderReferenceUnique(any())(any())
          verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, traderReferenceRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[TraderReferenceView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, onSubmitAction)(request, messages(application)).toString
        }
      }

      "must return a Bad Request and errors when an existing data is submitted" in {

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]

        when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
          .successful(
            record
          )

        when(mockGoodsRecordConnector.isTraderReferenceUnique(any())(any())) thenReturn Future
          .successful(
            false
          )

        val application =
          applicationBuilder(userAnswers =
            Some(emptyUserAnswers.set(TraderReferenceUpdatePage(recordId = testRecordId), "oldAnswer").success.value)
          )
            .overrides(
              bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, traderReferenceRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val boundForm = form
            .fill("answer")
            .copy(errors =
              Seq(elems =
                FormError("value", "This trader reference is already in your TGP. Enter a unique trader reference.")
              )
            )

          val view = application.injector.instanceOf[TraderReferenceView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, onSubmitAction)(request, messages(application)).toString

          verify(mockGoodsRecordConnector).isTraderReferenceUnique(any())(any())
          verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request = FakeRequest(GET, traderReferenceRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request =
            FakeRequest(POST, traderReferenceRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
