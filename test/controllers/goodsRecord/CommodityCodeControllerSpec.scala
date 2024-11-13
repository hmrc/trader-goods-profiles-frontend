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
import connectors.OttConnector
import forms.goodsRecord.CommodityCodeFormProvider
import models.helper.GoodsDetailsUpdate
import models.{Commodity, NormalMode, UserAnswers}
import navigation.{FakeGoodsRecordNavigator, GoodsRecordNavigator}
import org.apache.pekko.Done
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages._
import pages.goodsRecord._
import play.api.data.FormError
import play.api.http.Status.NOT_FOUND
import play.api.inject.bind
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CommodityQuery, CommodityUpdateQuery}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.SessionData.{dataUpdated, pageUpdated}
import views.html.goodsRecord.CommodityCodeView

import java.time.Instant
import scala.concurrent.Future

class CommodityCodeControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new CommodityCodeFormProvider()
  private val form = formProvider()

  "CommodityCode Controller" - {

    "For create journey" - {
      val commodityCodeRoute = controllers.goodsRecord.routes.CommodityCodeController.onPageLoadCreate(NormalMode).url

      lazy val onSubmitAction: Call = controllers.goodsRecord.routes.CommodityCodeController.onSubmitCreate(NormalMode)

      val page: QuestionPage[String] = CommodityCodePage

      runCommodityCodeControllerTests(commodityCodeRoute, onSubmitAction, page)
    }

    "For update journey" - {
      val commodityCodeRoute =
        controllers.goodsRecord.routes.CommodityCodeController.onPageLoadUpdate(NormalMode, testRecordId).url

      lazy val onSubmitAction: Call =
        controllers.goodsRecord.routes.CommodityCodeController.onSubmitUpdate(NormalMode, testRecordId)

      val page: QuestionPage[String] = CommodityCodeUpdatePage(testRecordId)

      runCommodityCodeControllerTests(commodityCodeRoute, onSubmitAction, page)

    }

    def runCommodityCodeControllerTests(
      commodityCodeRoute: String,
      onSubmitAction: Call,
      page: QuestionPage[String]
    ): Unit = {

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
          val request = FakeRequest(GET, commodityCodeRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CommodityCodeView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, onSubmitAction)(request, messages(application)).toString

          if (page == CommodityCodePage) {
            withClue("must not audit") {
              verify(mockAuditService, never()).auditStartUpdateGoodsRecord(any(), any(), any(), any(), any())(any())
            }
          } else {
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
      }

      "must not audit if already done on the previous page" in {

        val mockAuditService = mock[AuditService]

        val userAnswers = emptyUserAnswers.set(HasCommodityCodeChangePage(testRecordId), true).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, commodityCodeRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CommodityCodeView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, onSubmitAction)(request, messages(application)).toString

          withClue("must not audit") {
            verify(mockAuditService, never()).auditStartUpdateGoodsRecord(any(), any(), any(), any(), any())(any())
          }

        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(page, "654321").success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, commodityCodeRoute)

          val view = application.injector.instanceOf[CommodityCodeView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("654321"), onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        val mockOttConnector = mock[OttConnector]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any(), any())(any())) thenReturn Future
          .successful(
            Commodity(
              "6543210000",
              List("Class level1 desc", "Class level2 desc", "Class level3 desc"),
              Instant.now,
              None
            )
          )

        val userAnswers =
          UserAnswers(userAnswersId)
            .set(CountryOfOriginPage, "CX")
            .success
            .value
            .set(CountryOfOriginUpdatePage(testRecordId), "CX")
            .success
            .value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[OttConnector].toInstance(mockOttConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, commodityCodeRoute)
              .withFormUrlEncodedBody(("value", "654321"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockOttConnector)
            .getCommodityCode(eqTo("654321"), eqTo(testEori), any(), any(), any(), any())(
              any()
            )

          withClue("must save commodity as user entered it rather than in the ott-formatted version") {
            val userAnswersSent: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockSessionRepository).set(userAnswersSent.capture())

            val commodityDetails = if (page == CommodityCodePage) {
              userAnswersSent.getValue.get(CommodityQuery).get
            } else {
              userAnswersSent.getValue.get(CommodityUpdateQuery(testRecordId)).get
            }

            commodityDetails.commodityCode mustBe "654321"

          }
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val userAnswers =
          UserAnswers(userAnswersId)
            .set(CommodityCodeUpdatePage(testRecordId), "654321")
            .success
            .value
            .set(CountryOfOriginPage, "CX")
            .success
            .value
            .set(CountryOfOriginUpdatePage(testRecordId), "CX")
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, commodityCodeRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[CommodityCodeView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, onSubmitAction)(request, messages(application)).toString
        }
      }

      "must return a Bad Request and errors when incorrect data format is submitted" in {

        val userAnswers =
          UserAnswers(userAnswersId)
            .set(CommodityCodeUpdatePage(testRecordId), "654321")
            .success
            .value
            .set(CountryOfOriginPage, "CX")
            .success
            .value
            .set(CountryOfOriginUpdatePage(testRecordId), "CX")
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, commodityCodeRoute)
              .withFormUrlEncodedBody(("value", "abc"))

          val boundForm = form.bind(Map("value" -> "abc"))

          val view = application.injector.instanceOf[CommodityCodeView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, onSubmitAction)(request, messages(application)).toString
        }
      }

      "must return a Bad Request and errors when correct data format but wrong data is submitted" in {

        val mockOttConnector = mock[OttConnector]

        when(mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any(), any())(any())) thenReturn Future
          .failed(
            UpstreamErrorResponse(" ", NOT_FOUND)
          )

        val userAnswers =
          UserAnswers(userAnswersId)
            .set(CommodityCodeUpdatePage(testRecordId), "654321")
            .success
            .value
            .set(CountryOfOriginPage, "CX")
            .success
            .value
            .set(CountryOfOriginUpdatePage(testRecordId), "CX")
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, commodityCodeRoute)
              .withFormUrlEncodedBody(("value", "654321"))

          val boundForm = form.copy(errors = Seq(elems = FormError("value", "Enter a real commodity code")))

          val view = application.injector.instanceOf[CommodityCodeView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, onSubmitAction)(request, messages(application)).toString

          verify(mockOttConnector)
            .getCommodityCode(eqTo("654321"), eqTo(testEori), any(), any(), any(), any())(
              any()
            )
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request = FakeRequest(GET, commodityCodeRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, commodityCodeRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }

    "must set changesMade to true if commodity code is updated" in {

      val commodityCodeRoute =
        controllers.goodsRecord.routes.CommodityCodeController.onPageLoadUpdate(NormalMode, testRecordId).url

      val mockSessionRepository = mock[SessionRepository]

      val mockOttConnector = mock[OttConnector]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any(), any())(any())) thenReturn Future
        .successful(
          Commodity("654321", List("Class level1 desc", "Class level2 desc", "Class level3 desc"), Instant.now, None)
        )

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(CommodityCodeUpdatePage(testRecordId), "654321")
          .success
          .value
          .set(CountryOfOriginPage, "CX")
          .success
          .value
          .set(CountryOfOriginUpdatePage(testRecordId), "CX")
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

      running(application) {
        val controller = application.injector.instanceOf[CommodityCodeController]
        val request    =
          FakeRequest(POST, commodityCodeRoute)
            .withFormUrlEncodedBody(("value", "654322"))

        val result: Future[Result] = controller.onSubmitUpdate(NormalMode, testRecordId)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        session(result).get(dataUpdated) must be(Some("true"))
        session(result).get(pageUpdated) must be(Some("commodity code"))

        verify(mockOttConnector).getCommodityCode(anyString(), any(), any(), any(), any(), any())(any())

      }
    }

    "must set changesMade to false if commodity code is not updated" in {

      val commodityCodeRoute =
        controllers.goodsRecord.routes.CommodityCodeController.onPageLoadUpdate(NormalMode, testRecordId).url

      val mockSessionRepository = mock[SessionRepository]

      val mockOttConnector = mock[OttConnector]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any(), any())(any())) thenReturn Future
        .successful(
          Commodity("654321", List("Class level1 desc", "Class level2 desc", "Class level3 desc"), Instant.now, None)
        )

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(CommodityCodeUpdatePage(testRecordId), "654321")
          .success
          .value
          .set(CountryOfOriginUpdatePage(testRecordId), "CX")
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

      running(application) {
        val controller = application.injector.instanceOf[CommodityCodeController]
        val request    =
          FakeRequest(POST, commodityCodeRoute)
            .withFormUrlEncodedBody(("value", "654321"))

        val result: Future[Result] = controller.onSubmitUpdate(NormalMode, testRecordId)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        session(result).get(dataUpdated) must be(Some("false"))
        verify(mockOttConnector).getCommodityCode(anyString(), any(), any(), any(), any(), any())(any())

      }
    }

  }
}
