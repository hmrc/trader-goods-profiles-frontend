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

package controllers.goodsRecord.goodsDescription

import base.SpecBase
import base.TestConstants.{testEori, testRecordId, userAnswersId}
import forms.goodsRecord.GoodsDescriptionFormProvider
import models.helper.GoodsDetailsUpdate
import models.{NormalMode, UserAnswers}
import navigation.{FakeGoodsRecordNavigator, GoodsRecordNavigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.{GoodsDescriptionUpdatePage, HasGoodsDescriptionChangePage}
import play.api.inject.bind
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.SessionData.{dataUpdated, pageUpdated}
import views.html.goodsRecord.GoodsDescriptionView

import scala.concurrent.Future

class UpdateGoodsDescriptionControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new GoodsDescriptionFormProvider()
  private val form = formProvider()

  private val mockAuditService      = mock[AuditService]
  private val mockSessionRepository = mock[SessionRepository]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditService, mockSessionRepository)
  }

  "UpdateGoodsDescriptionController" - {
    lazy val goodsDescriptionUpdateRoute =
      controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController
        .onPageLoad(NormalMode, testRecordId)
        .url
    lazy val onSubmitAction: Call        = controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController
      .onSubmit(NormalMode, testRecordId)

    "must return OK and the correct view for a GET" in {
      when(mockAuditService.auditStartUpdateGoodsRecord(any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[AuditService].toInstance(mockAuditService))
        .build()

      running(application) {
        val request = FakeRequest(GET, goodsDescriptionUpdateRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[GoodsDescriptionView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, onSubmitAction)(
          request,
          messages(application)
        ).toString

        withClue("must call the audit service with the correct details") {
          verify(mockAuditService).auditStartUpdateGoodsRecord(
            eqTo(testEori),
            eqTo(AffinityGroup.Individual),
            eqTo(GoodsDetailsUpdate),
            eqTo(testRecordId),
            any()
          )(any())
        }
      }
    }

    "must not audit if already done on previous page" in {

      val userAnswers = emptyUserAnswers.set(HasGoodsDescriptionChangePage(testRecordId), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[AuditService].toInstance(mockAuditService))
        .build()

      running(application) {
        val request = FakeRequest(GET, goodsDescriptionUpdateRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[GoodsDescriptionView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, onSubmitAction)(
          request,
          messages(application)
        ).toString

        withClue("must not audit as already done on last page") {
          verify(mockAuditService, never()).auditStartUpdateGoodsRecord(any(), any(), any(), any(), any())(any())
        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId).set(GoodsDescriptionUpdatePage(testRecordId), "answer").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, goodsDescriptionUpdateRoute)
        val view    = application.injector.instanceOf[GoodsDescriptionView]
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode, onSubmitAction)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      val length              = 512
      val description: String = Gen.listOfN(length, Gen.alphaNumChar).map(_.mkString).sample.value

      running(application) {
        val request = FakeRequest(POST, goodsDescriptionUpdateRoute).withFormUrlEncodedBody(("value", description))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must set changesMade to true if goods description is updated" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers =
        UserAnswers(userAnswersId).set(GoodsDescriptionUpdatePage(testRecordId), "oldValue").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val controller             = application.injector.instanceOf[UpdateGoodsDescriptionController]
        val request                = FakeRequest(POST, goodsDescriptionUpdateRoute).withFormUrlEncodedBody(("value", "newValue"))
        val result: Future[Result] = controller.onSubmit(NormalMode, testRecordId)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        session(result).get(dataUpdated) must be(Some("true"))
        session(result).get(pageUpdated) must be(Some("goodsDescription"))
      }
    }

    "must set changesMade to false if goods description is not updated" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers =
        UserAnswers(userAnswersId).set(GoodsDescriptionUpdatePage(testRecordId), "OldValue").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val controller             = application.injector.instanceOf[UpdateGoodsDescriptionController]
        val request                = FakeRequest(POST, goodsDescriptionUpdateRoute).withFormUrlEncodedBody(("value", "OldValue"))
        val result: Future[Result] = controller.onSubmit(NormalMode, testRecordId)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        session(result).get(dataUpdated) must be(Some("false"))
      }
    }

    "must return a Bad Request and errors when no description is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request   = FakeRequest(POST, goodsDescriptionUpdateRoute).withFormUrlEncodedBody(("value", ""))
        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[GoodsDescriptionView]
        val result    = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, onSubmitAction)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when user submits a description longer than 512 characters" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val invalidLength              = 513
      val invalidDescription: String = Gen.listOfN(invalidLength, Gen.alphaNumChar).map(_.mkString).sample.value

      running(application) {
        val request   =
          FakeRequest(POST, goodsDescriptionUpdateRoute).withFormUrlEncodedBody(("value", invalidDescription))
        val boundForm = form.bind(Map("value" -> invalidDescription))
        val view      = application.injector.instanceOf[GoodsDescriptionView]
        val result    = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, onSubmitAction)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, goodsDescriptionUpdateRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, goodsDescriptionUpdateRoute).withFormUrlEncodedBody(("value", "answer"))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
