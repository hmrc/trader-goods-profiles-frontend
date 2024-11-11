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
import controllers.routes
import forms.goodsRecord.HasCommodityCodeChangeFormProvider
import models.helper.GoodsDetailsUpdate
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.HasCommodityCodeChangePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.Constants.adviceProvided
import views.html.HasCommodityCodeChangeView

import scala.concurrent.Future

class HasCommodityCodeChangeControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new HasCommodityCodeChangeFormProvider()
  private val form = formProvider()

  private lazy val hasCommodityCodeChangeRoute =
    routes.HasCommodityCodeChangeController.onPageLoad(NormalMode, testRecordId).url

  private val goodsRecord              = goodsRecordResponse()
  private val goodsRecordCatNoAdvice   = goodsRecord.copy(
    category = Some(2),
    adviceStatus = "Not requested"
  )
  private val goodsRecordNoCatAdvice   = goodsRecord.copy(
    category = None,
    adviceStatus = adviceProvided
  )
  private val goodsRecordCatAdvice     = goodsRecord.copy(
    category = Some(2),
    adviceStatus = adviceProvided
  )
  private val mockGoodsRecordConnector = mock[GoodsRecordConnector]

  override def beforeEach(): Unit = {

    when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(
      Future.successful(goodsRecord)
    )

    super.beforeEach()
  }

  "HasCommodityCodeChange Controller" - {

    "must return OK and the correct view for a GET" - {

      "when categorisation has happened" in {
        val mockAuditService = mock[AuditService]

        when(mockAuditService.auditStartUpdateGoodsRecord(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))
        when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(
          Future.successful(goodsRecordCatNoAdvice)
        )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, hasCommodityCodeChangeRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasCommodityCodeChangeView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            NormalMode,
            testRecordId,
            adviceWarning = false,
            categoryWarning = true
          )(request, messages(application)).toString
          withClue("must call the audit service with the correct details") {
            verify(mockAuditService, atLeastOnce())
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

      "when advice status has happened" in {

        when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(
          Future.successful(goodsRecordNoCatAdvice)
        )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, hasCommodityCodeChangeRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasCommodityCodeChangeView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            NormalMode,
            testRecordId,
            adviceWarning = true,
            categoryWarning = false
          )(request, messages(application)).toString
        }
      }

      "when categorisation and advice has happened" in {

        when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(
          Future.successful(goodsRecordCatAdvice)
        )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, hasCommodityCodeChangeRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasCommodityCodeChangeView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            NormalMode,
            testRecordId,
            adviceWarning = true,
            categoryWarning = true
          )(request, messages(application)).toString
        }
      }

    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(
        Future.successful(goodsRecordCatNoAdvice)
      )
      val userAnswers = UserAnswers(userAnswersId).set(HasCommodityCodeChangePage(testRecordId), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, hasCommodityCodeChangeRoute)

        val view = application.injector.instanceOf[HasCommodityCodeChangeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(true),
          NormalMode,
          testRecordId,
          adviceWarning = false,
          categoryWarning = true
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
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, hasCommodityCodeChangeRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(
        Future.successful(goodsRecordCatAdvice)
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, hasCommodityCodeChangeRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[HasCommodityCodeChangeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          NormalMode,
          testRecordId,
          adviceWarning = true,
          categoryWarning = true
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, hasCommodityCodeChangeRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, hasCommodityCodeChangeRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when POST and goods connector fails" in {

      when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(
        Future.failed(new Exception(":("))
      )

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, hasCommodityCodeChangeRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when GET and goods connector fails" in {

      when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(
        Future.failed(new Exception(":("))
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, hasCommodityCodeChangeRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
