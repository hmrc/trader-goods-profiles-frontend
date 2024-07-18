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
import forms.HasCorrectGoodsFormProvider
import models.AssessmentAnswer.Exemption
import models.ott.{CategorisationInfo, CategoryAssessment, Certificate}
import models.{Commodity, NormalMode, RecordCategorisations, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CommodityQuery, CommodityUpdateQuery, LongerCommodityQuery, RecategorisingQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import services.CategorisationService
import views.html.HasCorrectGoodsView

import java.time.Instant
import scala.concurrent.Future
import scala.util.Success

class HasCorrectGoodsControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new HasCorrectGoodsFormProvider()
  private val form = formProvider()

  "HasCorrectGoodsController" - {

    "For create journey" - {
      lazy val hasCorrectGoodsCreateRoute = routes.HasCorrectGoodsController.onPageLoadCreate(NormalMode).url
      lazy val onSubmitAction: Call       = routes.HasCorrectGoodsController.onSubmitCreate(NormalMode)
      val page: QuestionPage[Boolean]     = HasCorrectGoodsPage

      "must return OK and the correct view for a GET" in {

        val userAnswers =
          emptyUserAnswers
            .set(CommodityQuery, Commodity("654321", List("Description", "Other"), Instant.now, None))
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsCreateRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            Commodity("654321", List("Description", "Other"), Instant.now, None),
            onSubmitAction
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect on GET to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsCreateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val commodity   = Commodity("654321", List("Description"), Instant.now, None)
        val userAnswers = emptyUserAnswers
          .set(CommodityQuery, commodity)
          .success
          .value
          .set(page, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsCreateRoute)

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), commodity, onSubmitAction)(
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
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must redirect on POST to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val commodity = Commodity("654321", List("Description"), Instant.now, None)

        val userAnswers =
          emptyUserAnswers.set(CommodityQuery, commodity).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, commodity, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsCreateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for the Longer Commodity Code Journey" - {

      lazy val hasCorrectGoodsRoute =
        routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(NormalMode, testRecordId).url
      lazy val onSubmitAction: Call =
        routes.HasCorrectGoodsController.onSubmitLongerCommodityCode(NormalMode, testRecordId)

      "for a GET" - {

        "must return OK and the correct view for a GET" in {

          val userAnswers =
            emptyUserAnswers
              .set(
                LongerCommodityQuery(testRecordId),
                Commodity("654321", List("Description", "Other"), Instant.now, None)
              )
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val result = route(application, request).value

            val view = application.injector.instanceOf[HasCorrectGoodsView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              form,
              Commodity("654321", List("Description", "Other"), Instant.now, None),
              onSubmitAction
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect on GET to JourneyRecovery Page if user doesn't have commodity answer" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must populate the view correctly on a GET when the question has previously been answered" in {

          val commodity   = Commodity("654321", List("Description"), Instant.now, None)
          val userAnswers = emptyUserAnswers
            .set(LongerCommodityQuery(testRecordId), commodity)
            .success
            .value
            .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val view = application.injector.instanceOf[HasCorrectGoodsView]

            val result = route(application, request).value

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form.fill(true), commodity, onSubmitAction)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery for a GET if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

      }

      "for a POST" - {
        "must redirect to the next page when valid data is submitted" - {

          val assessment1        = CategoryAssessment("id1", 1, Seq(Certificate("cert1", "code1", "description1")))
          val assessment2        = CategoryAssessment("id2", 2, Seq(Certificate("cert2", "code2", "description2")))
          val categorisationInfo =
            CategorisationInfo("1234567890", Seq(assessment1, assessment2), Some("some measure unit"), 0)

          "and do not need to recategorise because the assessments are the same and there are no supplementary units" in {

            val categorisationInfoNoSuppUnit = categorisationInfo.copy(measurementUnit = None)

            val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            val mockSessionRepository                 = mock[SessionRepository]
            when(mockSessionRepository.set(uaCaptor.capture())) thenReturn Future.successful(true)

            val pageCaptor: ArgumentCaptor[HasCorrectGoodsLongerCommodityCodePage] =
              ArgumentCaptor.forClass(classOf[HasCorrectGoodsLongerCommodityCodePage])
            val mockNavigator                                                      = mock[Navigator]
            when(mockNavigator.nextPage(pageCaptor.capture(), any(), any())).thenReturn(onwardRoute)

            val userAnswers = emptyUserAnswers
              .set(RecordCategorisationsQuery, RecordCategorisations(Map(testRecordId -> categorisationInfoNoSuppUnit)))
              .success
              .value
              .set(AssessmentPage(testRecordId, 0), Exemption("Y322"))
              .success
              .value

            val mockCategorisationService = mock[CategorisationService]
            when(mockCategorisationService.updateCategorisationWithLongerCommodityCode(any(), any())(any()))
              .thenReturn(Future.successful(userAnswers))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigator].toInstance(mockNavigator),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[CategorisationService].toInstance(mockCategorisationService)
                )
                .build()

            running(application) {
              val request =
                FakeRequest(POST, hasCorrectGoodsRoute)
                  .withFormUrlEncodedBody(("value", "true"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url

              withClue("must have told the navigator not to recategorise the goods") {
                val pageSentToNavigator = pageCaptor.getValue
                pageSentToNavigator.needToRecategorise mustBe false
              }

              withClue("must not have reset the user answers") {
                verify(mockCategorisationService, times(0)).cleanupOldAssessmentAnswers(any(), any())
              }

              val finalUserAnswers = uaCaptor.getValue
              withClue("must have kept the old assessment answers") {
                finalUserAnswers.isDefined(AssessmentPage(testRecordId, 0)) mustBe true
              }

              withClue("must not be in recategorisation mode") {
                finalUserAnswers.get(RecategorisingQuery(testRecordId)) mustBe Some(false)
              }

            }
          }

          "and need to recategorise" - {

            "because the assessments are different" in {

              val categorisationInfoNoSuppUnit = categorisationInfo.copy(measurementUnit = None)
              val categorisationInfoNew        = CategorisationInfo("12345678", Seq(assessment2), None, 0)

              val finalUserAnswerArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
              val mockSessionRepository                                 = mock[SessionRepository]
              when(mockSessionRepository.set(finalUserAnswerArgCaptor.capture())) thenReturn Future.successful(true)

              val pageCaptor: ArgumentCaptor[HasCorrectGoodsLongerCommodityCodePage] =
                ArgumentCaptor.forClass(classOf[HasCorrectGoodsLongerCommodityCodePage])
              val mockNavigator                                                      = mock[Navigator]
              when(mockNavigator.nextPage(pageCaptor.capture(), any(), any())).thenReturn(onwardRoute)

              val initialUserAnswers = emptyUserAnswers
                .set(
                  RecordCategorisationsQuery,
                  RecordCategorisations(Map(testRecordId -> categorisationInfoNoSuppUnit))
                )
                .success
                .value
                .set(AssessmentPage(testRecordId, 0), Exemption("Y322"))
                .success
                .value

              val updatedUserAnswers = initialUserAnswers
                .set(RecordCategorisationsQuery, RecordCategorisations(Map(testRecordId -> categorisationInfoNew)))
                .success
                .value

              val mockCategorisationService = mock[CategorisationService]
              when(mockCategorisationService.updateCategorisationWithLongerCommodityCode(any(), any())(any()))
                .thenReturn(Future.successful(updatedUserAnswers))
              when(mockCategorisationService.cleanupOldAssessmentAnswers(any(), any()))
                .thenReturn(Success(updatedUserAnswers))

              val application =
                applicationBuilder(userAnswers = Some(initialUserAnswers))
                  .overrides(
                    bind[Navigator].toInstance(mockNavigator),
                    bind[SessionRepository].toInstance(mockSessionRepository),
                    bind[CategorisationService].toInstance(mockCategorisationService)
                  )
                  .build()

              running(application) {
                val request =
                  FakeRequest(POST, hasCorrectGoodsRoute)
                    .withFormUrlEncodedBody(("value", "true"))

                val result = route(application, request).value

                status(result) mustEqual SEE_OTHER
                redirectLocation(result).value mustEqual onwardRoute.url

                withClue("must have told the navigator to recategorise the goods") {
                  val pageSentToNavigator = pageCaptor.getValue
                  pageSentToNavigator.needToRecategorise mustBe true
                }

                withClue("must not reset the user answers") {
                  verify(mockCategorisationService, times(1)).cleanupOldAssessmentAnswers(any(), any())
                }

                withClue("must have flagged we are in recategorisation mode") {
                  val finalUserAnswers = finalUserAnswerArgCaptor.getValue

                  finalUserAnswers.get(RecategorisingQuery(testRecordId)) mustBe Some(true)
                }

              }
            }
          }
        }

        "must redirect on POST to JourneyRecovery Page if user doesn't have commodity answer" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request =
              FakeRequest(POST, hasCorrectGoodsRoute)
                .withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must return a Bad Request and errors when invalid data is submitted" in {

          val commodity = Commodity("654321", List("Description"), Instant.now, None)

          val userAnswers =
            emptyUserAnswers.set(LongerCommodityQuery(testRecordId), commodity).success.value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request =
              FakeRequest(POST, hasCorrectGoodsRoute)
                .withFormUrlEncodedBody(("value", ""))

            val boundForm = form.bind(Map("value" -> ""))

            val view = application.injector.instanceOf[HasCorrectGoodsView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(boundForm, commodity, onSubmitAction)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery for a POST if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(POST, hasCorrectGoodsRoute)
                .withFormUrlEncodedBody(("value", "true"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

      }

    }

    "For update journey" - {
      lazy val hasCorrectGoodsUpdateRoute =
        routes.HasCorrectGoodsController.onPageLoadUpdate(NormalMode, testRecordId).url
      lazy val onSubmitAction: Call       = routes.HasCorrectGoodsController.onSubmitUpdate(NormalMode, testRecordId)
      val page: QuestionPage[Boolean]     = HasCorrectGoodsCommodityCodeUpdatePage(testRecordId)

      "must return OK and the correct view for a GET" in {

        val userAnswers =
          emptyUserAnswers
            .set(CommodityUpdateQuery(testRecordId), Commodity("654321", List("Description"), Instant.now, None))
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsUpdateRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            Commodity("654321", List("Description"), Instant.now, None),
            onSubmitAction
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect on GET to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsUpdateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val commodity   = Commodity("654321", List("Description"), Instant.now, None)
        val userAnswers = emptyUserAnswers
          .set(CommodityUpdateQuery(testRecordId), commodity)
          .success
          .value
          .set(page, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsUpdateRoute)

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), commodity, onSubmitAction)(
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
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsUpdateRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must redirect on POST to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsUpdateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val commodity = Commodity("654321", List("Description"), Instant.now, None)

        val userAnswers =
          emptyUserAnswers.set(CommodityUpdateQuery(testRecordId), commodity).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsUpdateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, commodity, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsUpdateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsUpdateRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }
  }
}
