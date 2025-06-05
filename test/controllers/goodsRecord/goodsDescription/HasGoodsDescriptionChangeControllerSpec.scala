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
import forms.goodsRecord.HasGoodsDescriptionChangeFormProvider
import models.helper.GoodsDetailsUpdate
import models.{NormalMode, UserAnswers}
import navigation.{FakeGoodsRecordNavigator, GoodsRecordNavigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, verify, when, reset}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.HasGoodsDescriptionChangePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.goodsRecord.HasGoodsDescriptionChangeView
import scala.concurrent.Future

class HasGoodsDescriptionChangeControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new HasGoodsDescriptionChangeFormProvider()
  private val form = formProvider()
  private lazy val hasGoodsDescriptionChangeRoute = controllers.goodsRecord.goodsDescription.routes.HasGoodsDescriptionChangeController.onPageLoad(NormalMode, testRecordId).url
  private val mockAuditService = mock[AuditService]
  private val mockSessionRepository = mock[SessionRepository]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditService, mockSessionRepository)
  }

  "HasGoodsDescriptionChange Controller" - {

    "must return OK and the correct view for a GET" in {
      when(mockAuditService.auditStartUpdateGoodsRecord(any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(Done))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[AuditService].toInstance(mockAuditService))
        .build()

      running(application) {
        val request = FakeRequest(GET, hasGoodsDescriptionChangeRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[HasGoodsDescriptionChangeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testRecordId)(request, messages(application)).toString

        withClue("must call the audit service with the correct details") {
          verify(mockAuditService, atLeastOnce()).auditStartUpdateGoodsRecord(eqTo(testEori), eqTo(AffinityGroup.Individual), eqTo(GoodsDetailsUpdate), eqTo(testRecordId), any())(any())
        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId).set(HasGoodsDescriptionChangePage(testRecordId), true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, hasGoodsDescriptionChangeRoute)
        val view = application.injector.instanceOf[HasGoodsDescriptionChangeView]
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, testRecordId)(request, messages(application)).toString
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

      running(application) {
        val request = FakeRequest(POST, hasGoodsDescriptionChangeRoute).withFormUrlEncodedBody(("value", "true"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, hasGoodsDescriptionChangeRoute).withFormUrlEncodedBody(("value", ""))
        val boundForm = form.bind(Map("value" -> ""))
        val view = application.injector.instanceOf[HasGoodsDescriptionChangeView]
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testRecordId)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, hasGoodsDescriptionChangeRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, hasGoodsDescriptionChangeRoute).withFormUrlEncodedBody(("value", "true"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
