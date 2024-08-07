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
import base.TestConstants.testRecordId
import connectors.{GoodsRecordConnector, OttConnector}
import forms.LongerCommodityCodeFormProvider
import models.{CheckMode, Commodity, NormalMode, RecordCategorisations, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{atMostOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.LongerCommodityCodePage
import play.api.data.FormError
import play.api.http.Status.NOT_FOUND
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{LongerCommodityQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.html.LongerCommodityCodeView

import java.time.Instant
import scala.concurrent.Future

class LongerCommodityCodeControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider                = new LongerCommodityCodeFormProvider()
  private val form                        = formProvider()
  private val shortCommodity              = "654321"
  private val categoryQueryShortCommodity =
    categoryQuery.copy(commodityCode = shortCommodity, originalCommodityCode = Some(shortCommodity))

  private val recordCategorisationsShortCommodity = RecordCategorisations(
    Map(testRecordId -> categoryQueryShortCommodity)
  )

  private val previouslyUpdatedCategoryInfo =
    categoryQuery.copy(commodityCode = shortCommodity + 1234, originalCommodityCode = Some(shortCommodity))
  private val previouslyUpdatedCommodity    = RecordCategorisations(
    Map(testRecordId -> previouslyUpdatedCategoryInfo)
  )

  private def onwardRoute = Call("GET", "/foo")

  private lazy val longerCommodityCodeRoute =
    routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId).url

  private lazy val longerCommodityCheckRoute =
    routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId).url

  "LongerCommodityCode Controller" - {

    "GET" - {
      "must return OK and the correct view" in {

        val userAnswers =
          emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisationsShortCommodity).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, longerCommodityCodeRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[LongerCommodityCodeView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, shortCommodity, testRecordId)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to JourneyRecovery Page" - {

        "if user doesn't have commodity answer" in {
          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, longerCommodityCodeRoute)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "if user's commodity code is 10 digits" in {
          val userAnswers = emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisations).success.value
          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, longerCommodityCodeRoute)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, longerCommodityCodeRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = emptyUserAnswers
          .set(RecordCategorisationsQuery, previouslyUpdatedCommodity)
          .success
          .value
          .set(LongerCommodityCodePage(testRecordId, true), "1234")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, longerCommodityCodeRoute)

          val view = application.injector.instanceOf[LongerCommodityCodeView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("1234"), NormalMode, shortCommodity, testRecordId)(
            request,
            messages(application)
          ).toString
        }
      }

    }

    "POST" - {

      "must redirect to CyaCategorisation when the same longer commodity code is submitted after clicking 'Change'" in {

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.successful(goodsRecordResponse(Instant.now, Instant.now)))

        val mockSessionRepository = mock[SessionRepository]
        val mockOttConnector      = mock[OttConnector]
        val userAnswers           = emptyUserAnswers
          .set(RecordCategorisationsQuery, previouslyUpdatedCommodity)
          .success
          .value
          .set(LongerCommodityCodePage(testRecordId), "1234")
          .success
          .value

        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(mockSessionRepository.set(uaCaptor.capture())) thenReturn Future.successful(true)

        val testCommodity = Commodity("6543211234", List("Description"), Instant.now, None)
        when(mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any(), any())(any())) thenReturn Future
          .successful(
            testCommodity
          )

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[OttConnector].toInstance(mockOttConnector),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, longerCommodityCheckRoute)
              .withFormUrlEncodedBody(("value", "1234"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CyaCategorisationController.onPageLoad(testRecordId).url

          withClue("ensure user answers are set, even in the redirect scenario") {
            verify(mockOttConnector, atMostOnce()).getCommodityCode(any(), any(), any(), any(), any(), any())(any())

            val finalUserAnswers = uaCaptor.getValue

            finalUserAnswers.get(LongerCommodityCodePage(testRecordId)).get mustBe "1234"
            finalUserAnswers.get(LongerCommodityQuery(testRecordId)).get mustBe testCommodity
          }

        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.successful(goodsRecordResponse(Instant.now, Instant.now)))

        val mockSessionRepository = mock[SessionRepository]
        val mockOttConnector      = mock[OttConnector]
        val userAnswers           = emptyUserAnswers
          .set(RecordCategorisationsQuery, recordCategorisationsShortCommodity)
          .success
          .value
          .set(LongerCommodityCodePage(testRecordId), "answer")
          .success
          .value

        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(mockSessionRepository.set(uaCaptor.capture())) thenReturn Future.successful(true)

        val testCommodity = Commodity("654321", List("Description"), Instant.now, None)
        when(mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any(), any())(any())) thenReturn Future
          .successful(
            testCommodity
          )

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[OttConnector].toInstance(mockOttConnector),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, longerCommodityCodeRoute)
              .withFormUrlEncodedBody(("value", "12"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockOttConnector).getCommodityCode(any(), any(), any(), any(), any(), any())(any())

          withClue("ensure user answers has set the new commodity query") {
            val finalUserAnswers = uaCaptor.getValue

            finalUserAnswers.get(LongerCommodityCodePage(testRecordId)).get mustBe "12"
            finalUserAnswers.get(LongerCommodityQuery(testRecordId)).get mustBe testCommodity
          }
        }
      }

      "must return a Bad Request and errors" - {

        "when invalid is submitted" in {
          val userAnswers = emptyUserAnswers
            .set(RecordCategorisationsQuery, recordCategorisationsShortCommodity)
            .success
            .value
            .set(LongerCommodityCodePage(testRecordId), "answer")
            .success
            .value
          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request =
              FakeRequest(POST, longerCommodityCodeRoute)
                .withFormUrlEncodedBody(("value", ""))

            val boundForm = form.bind(Map("value" -> ""))

            val view = application.injector.instanceOf[LongerCommodityCodeView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(boundForm, NormalMode, shortCommodity, testRecordId)(
              request,
              messages(application)
            ).toString
          }
        }

        "when correct data format but wrong data is submitted" in {
          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
            .thenReturn(Future.successful(goodsRecordResponse(Instant.now, Instant.now)))

          val mockOttConnector = mock[OttConnector]
          val userAnswers      = emptyUserAnswers
            .set(RecordCategorisationsQuery, recordCategorisationsShortCommodity)
            .success
            .value
            .set(LongerCommodityCodePage(testRecordId), "answer")
            .success
            .value

          when(
            mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any(), any())(any())
          ) thenReturn Future
            .failed(
              UpstreamErrorResponse(" ", NOT_FOUND)
            )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[OttConnector].toInstance(mockOttConnector),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

          running(application) {
            val request =
              FakeRequest(POST, longerCommodityCodeRoute)
                .withFormUrlEncodedBody(("value", "1234"))

            val boundForm = form.copy(errors = Seq(elems = FormError("value", "longerCommodityCode.error.invalid")))

            val view = application.injector.instanceOf[LongerCommodityCodeView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(boundForm, NormalMode, shortCommodity, testRecordId)(
              request,
              messages(application)
            ).toString

            verify(mockOttConnector).getCommodityCode(any(), any(), any(), any(), any(), any())(any())
          }

        }

        "must redirect to Journey Recovery for a GET if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, longerCommodityCodeRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(POST, longerCommodityCodeRoute)
                .withFormUrlEncodedBody(("value", "answer"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }
  }
}
