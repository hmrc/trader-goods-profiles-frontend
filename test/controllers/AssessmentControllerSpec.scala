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
import forms.{AssessmentFormProvider, AssessmentFormProvider2}
import models.ott.{CategorisationInfo, CategorisationInfo2, CategoryAssessment, Certificate}
import models.{AssessmentAnswer, AssessmentAnswer2, NormalMode, RecordCategorisations}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AssessmentPage, AssessmentPage2, ReassessmentPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CategorisationDetailsQuery2, LongerCategorisationDetailsQuery, RecategorisingQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import views.html.{AssessmentView, AssessmentView2}

import scala.concurrent.Future

class AssessmentControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute      = Call("GET", "/foo")
  private val formProvider     = new AssessmentFormProvider()
  private val formProvider2    = new AssessmentFormProvider2()
  private def assessmentId     = "321"
  private def recordId         = testRecordId
  private def index            = 0
  private def assessmentRoute  = routes.AssessmentController.onPageLoad(NormalMode, recordId, index).url
  private def assessmentRoute2 = routes.AssessmentController.onPageLoad2(NormalMode, testRecordId, 0).url

  private def reassessmentRoute = routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, 0).url

  "AssessmentController 2" - {

    "for initial categorisation" - {
      "onPageLoad" - {

        "must render the view when an assessment can be found for this id" - {

          "and has not previously been answered" in {

            val answers =
              emptyUserAnswers.set(CategorisationDetailsQuery2(testRecordId), categorisationInfo2).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute2)

              val result = route(application, request).value

              val onSubmitAction    = routes.AssessmentController.onSubmit2(NormalMode, testRecordId, 0)
              val view              = application.injector.instanceOf[AssessmentView2]
              val form              = formProvider2(1)
              val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
                exemption =>
                  exemption.code + " - " + exemption.description
              }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                0,
                expectedListItems,
                categorisationInfo2.commodityCode,
                onSubmitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }

          "and has previously been answered" in {

            val answers =
              emptyUserAnswers
                .set(CategorisationDetailsQuery2(testRecordId), categorisationInfo2)
                .success
                .value
                .set(AssessmentPage2(testRecordId, 0), AssessmentAnswer2.NoExemption)
                .success
                .value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute2)

              val result = route(application, request).value

              val onSubmitAction    = routes.AssessmentController.onSubmit2(NormalMode, testRecordId, 0)
              val view              = application.injector.instanceOf[AssessmentView2]
              val form              = formProvider2(1).fill(AssessmentAnswer2.NoExemption)
              val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
                exemption =>
                  exemption.code + " - " + exemption.description
              }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                0,
                expectedListItems,
                categorisationInfo2.commodityCode,
                onSubmitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute2)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "when this assessment index cannot be found" in {

            val categorisationInfo =
              CategorisationInfo2("1234567890", Seq.empty[CategoryAssessment], Seq.empty[CategoryAssessment], None, 1)
            val answers            =
              emptyUserAnswers.set(CategorisationDetailsQuery2(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute2)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }
        }
      }

      "onSubmit" - {

        "must save the answer and redirect to the next page when a valid value is submitted" in {

          val mockRepository = mock[SessionRepository]
          when(mockRepository.set(any())).thenReturn(Future.successful(true))

          val answers =
            emptyUserAnswers.set(CategorisationDetailsQuery2(testRecordId), categorisationInfo2).success.value

          val application =
            applicationBuilder(userAnswers = Some(answers))
              .overrides(
                bind[SessionRepository].toInstance(mockRepository),
                bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, assessmentRoute2).withFormUrlEncodedBody(("value", "false"))

            val result = route(application, request).value

            val expectedAnswers =
              answers.set(AssessmentPage2(testRecordId, 0), AssessmentAnswer2.NoExemption).success.value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
            verify(mockRepository).set(eqTo(expectedAnswers))
          }
        }

        "must return a Bad Request and errors when invalid data is submitted" in {

          val answers =
            emptyUserAnswers.set(CategorisationDetailsQuery2(testRecordId), categorisationInfo2).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(POST, assessmentRoute2).withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            val onSubmitAction    = routes.AssessmentController.onSubmit2(NormalMode, testRecordId, 0)
            val view              = application.injector.instanceOf[AssessmentView2]
            val form              = formProvider2(1).fill(AssessmentAnswer2.NoExemption)
            val boundForm         = form.bind(Map("value" -> ""))
            val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
              exemption =>
                exemption.code + " - " + exemption.description
            }

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(
              boundForm,
              NormalMode,
              testRecordId,
              0,
              expectedListItems,
              categorisationInfo2.commodityCode,
              onSubmitAction
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request = FakeRequest(POST, assessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "when this assessment cannot be found" in {

            val categorisationInfo =
              CategorisationInfo2("1234567890", Seq.empty[CategoryAssessment], Seq.empty[CategoryAssessment], None, 1)
            val answers            =
              emptyUserAnswers.set(CategorisationDetailsQuery2(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(POST, assessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }
        }

      }

    }

    "for longer commodity code reassessment" - {
      "onPageLoad" - {

        "must render the view when an assessment can be found for this id" - {

          "and has not previously been answered" in {

            val answers =
              emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo2).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              val onSubmitAction    = routes.AssessmentController.onSubmitReassessment(NormalMode, testRecordId, 0)
              val view              = application.injector.instanceOf[AssessmentView2]
              val form              = formProvider2(1)
              val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
                exemption =>
                  exemption.code + " - " + exemption.description
              }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                0,
                expectedListItems,
                categorisationInfo2.commodityCode,
                onSubmitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }

          "and has previously been answered" in {

            val answers =
              emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo2)
                .success
                .value
                .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer2.NoExemption)
                .success
                .value

            val application    = applicationBuilder(userAnswers = Some(answers)).build()
            val onSubmitAction = routes.AssessmentController.onSubmitReassessment(NormalMode, testRecordId, 0)

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              val view              = application.injector.instanceOf[AssessmentView2]
              val form              = formProvider2(1).fill(AssessmentAnswer2.NoExemption)
              val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
                exemption =>
                  exemption.code + " - " + exemption.description
              }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                0,
                expectedListItems,
                categorisationInfo2.commodityCode,
                onSubmitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "when this assessment index cannot be found" in {

            val categorisationInfo =
              CategorisationInfo2("1234567890", Seq.empty[CategoryAssessment], Seq.empty[CategoryAssessment], None, 1)
            val answers            =
              emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }
        }
      }

      "onSubmit" - {

        "must save the answer and redirect to the next page when a valid value is submitted" in {

          val mockRepository = mock[SessionRepository]
          when(mockRepository.set(any())).thenReturn(Future.successful(true))

          val answers =
            emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo2).success.value

          val application =
            applicationBuilder(userAnswers = Some(answers))
              .overrides(
                bind[SessionRepository].toInstance(mockRepository),
                bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, reassessmentRoute).withFormUrlEncodedBody(("value", "false"))

            val result = route(application, request).value

            val expectedAnswers =
              answers.set(ReassessmentPage(testRecordId, 0), AssessmentAnswer2.NoExemption).success.value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
            verify(mockRepository).set(eqTo(expectedAnswers))
          }
        }

        "must return a Bad Request and errors when invalid data is submitted" in {

          val answers =
            emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo2).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(POST, reassessmentRoute).withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            val onSubmitAction    = routes.AssessmentController.onSubmitReassessment(NormalMode, testRecordId, 0)
            val view              = application.injector.instanceOf[AssessmentView2]
            val form              = formProvider2(1).fill(AssessmentAnswer2.NoExemption)
            val boundForm         = form.bind(Map("value" -> ""))
            val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
              exemption =>
                exemption.code + " - " + exemption.description
            }

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(
              boundForm,
              NormalMode,
              testRecordId,
              0,
              expectedListItems,
              categorisationInfo2.commodityCode,
              onSubmitAction
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request = FakeRequest(POST, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "when this assessment cannot be found" in {

            val categorisationInfo =
              CategorisationInfo2("1234567890", Seq.empty[CategoryAssessment], Seq.empty[CategoryAssessment], None, 1)
            val answers            =
              emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(POST, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }
        }

      }

    }
  }

  "AssessmentController" - {

    "for a GET" - {

      "must redirect" - {

        "to LongerCommodityCode if there's a cat 2 assessment without exemptions, the commodity code's short, and there are descendants" in {

          val commodityCode              = "1234560"
          val descendantCount            = 1
          val assessmentCat2NoExemptions = CategoryAssessment(assessmentId, 2, Seq())
          val categorisationInfo         =
            CategorisationInfo(
              commodityCode,
              Seq(assessmentCat2NoExemptions),
              Some("Weight, in kilograms"),
              descendantCount
            )
          val recordCategorisations      = RecordCategorisations(records = Map(recordId -> categorisationInfo))

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.LongerCommodityCodeController
              .onPageLoad(NormalMode, recordId)
              .url
          }
        }

        "to CyaCategorisation if there's a cat 2 assessment without exemptions and the user's not being redirected to LongerCommodityCode" in {

          val scenarios = Seq(
            ("12345600", 0), // short comcode, no descendants
            ("1234567800", 0), // long comcode, no descendants
            ("1234567800", 1) // long comcode, with descendants
          )

          scenarios.map { scenario =>
            val commodityCode              = scenario._1
            val descendantCount            = scenario._2
            val assessmentCat2NoExemptions = CategoryAssessment(assessmentId, 2, Seq())
            val categorisationInfo         =
              CategorisationInfo(
                commodityCode,
                Seq(assessmentCat2NoExemptions),
                Some("Weight, in kilograms"),
                descendantCount
              )
            val recordCategorisations      = RecordCategorisations(records = Map(recordId -> categorisationInfo))

            val answers =
              emptyUserAnswers
                .set(RecordCategorisationsQuery, recordCategorisations)
                .success
                .value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute)

              val result = route(application, request).value
              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CyaCategorisationController.onPageLoad(recordId).url
            }
          }
        }

        "when recategorising and has previously been answered" in {

          val assessment            = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
          val categorisationInfo    = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.NoExemption)
              .success
              .value
              .set(RecategorisingQuery(recordId), true)
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
            .build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
          }
        }

      }

      "must render the view when an assessment can be found for this id" - {

        "and has not previously been answered" in {

          val assessment            = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
          val categorisationInfo    = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))
          val answers               = emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisations).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            val view      = application.injector.instanceOf[AssessmentView]
            val form      = formProvider(1)
            val listItems = assessment.exemptions.map { exemption =>
              exemption.code + " - " + exemption.description
            }

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, recordId, index, listItems, "123")(
              request,
              messages(application)
            ).toString
          }
        }

        "when recategorising and has not previously been answered" in {

          val assessment            = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
          val categorisationInfo    = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))
          val answers               = emptyUserAnswers
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(RecategorisingQuery(testRecordId), true)
            .success
            .value
          val listItems             = assessment.exemptions.map { exemption =>
            exemption.code + " - " + exemption.description
          }

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            val view = application.injector.instanceOf[AssessmentView]
            val form = formProvider(1)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              form,
              NormalMode,
              recordId,
              index,
              listItems,
              categorisationInfo.commodityCode
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "and has previously been answered" in {

          val assessment            = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
          val categorisationInfo    = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.NoExemption)
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            val view      = application.injector.instanceOf[AssessmentView]
            val form      = formProvider(1).fill(AssessmentAnswer.NoExemption)
            val listItems = assessment.exemptions.map { exemption =>
              exemption.code + " - " + exemption.description
            }

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, recordId, index, listItems, "123")(
              request,
              messages(application)
            ).toString
          }
        }
      }

      "must redirect to Journey Recovery" - {

        "when categorisation information does not exist" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when this assessment index cannot be found" in {

          val categorisationInfo    = CategorisationInfo("123", Nil, Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))
          val answers               = emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisations).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }

    "for a POST" - {

//      "must save the answer and redirect to the next page when a valid value is submitted" in {
//
//        val assessment            = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
//        val categorisationInfo    = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
//        val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))
//
//        val mockRepository = mock[SessionRepository]
//        when(mockRepository.set(any())).thenReturn(Future.successful(true))
//
//        val answers = emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisations).success.value
//
//        val application =
//          applicationBuilder(userAnswers = Some(answers))
//            .overrides(
//              bind[SessionRepository].toInstance(mockRepository),
//              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
//            )
//            .build()
//
//        running(application) {
//          val request = FakeRequest(POST, assessmentRoute).withFormUrlEncodedBody(("value", "false"))
//
//          val result = route(application, request).value
//
//          val expectedAnswers = answers.set(AssessmentPage(recordId, index), AssessmentAnswer.NoExemption).success.value
//
//          status(result) mustEqual SEE_OTHER
//          redirectLocation(result).value mustEqual onwardRoute.url
//          verify(mockRepository).set(eqTo(expectedAnswers))
//        }
//      }
//
//      "must return a Bad Request and errors when invalid data is submitted" in {
//
//        val assessment            = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
//        val categorisationInfo    = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
//        val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))
//
//        val answers = emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisations).success.value
//
//        val application = applicationBuilder(userAnswers = Some(answers)).build()
//
//        running(application) {
//          val request = FakeRequest(POST, assessmentRoute).withFormUrlEncodedBody(("value", ""))
//
//          val result = route(application, request).value
//
//          val view      = application.injector.instanceOf[AssessmentView]
//          val form      = formProvider(1).fill(AssessmentAnswer.NoExemption)
//          val boundForm = form.bind(Map("value" -> ""))
//          val listItems = assessment.exemptions.map { exemption =>
//            exemption.code + " - " + exemption.description
//          }
//
//          status(result) mustEqual BAD_REQUEST
//          contentAsString(result) mustEqual view(boundForm, NormalMode, recordId, index, listItems, "123")(
//            request,
//            messages(application)
//          ).toString
//        }
//      }

      "must redirect to Journey Recovery" - {

        "when categorisation information does not exist" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(POST, assessmentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when this assessment cannot be found" in {

          val answers = emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisations).success.value

          val application     = applicationBuilder(userAnswers = Some(answers)).build()
          val assessmentRoute = routes.AssessmentController.onPageLoad(NormalMode, "differentRecordId", index).url

          running(application) {
            val request = FakeRequest(POST, assessmentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }
  }
}
