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
import base.TestConstants.{testEori, testRecordId, userAnswersId}
import connectors.{GoodsRecordConnector, TraderProfileConnector}
import models.helper.CategorisationUpdate
import models.{Category1, Category1NoExemptions, Category2, NiphlsOnly, NoRedirectScenario, NormalMode, RecordCategorisations, StandardNoAssessments, TraderProfile}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.CategoryGuidancePage
import play.api.inject._
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{RecategorisingQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import services.{AuditService, CategorisationService}
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.CategoryGuidanceView

import scala.concurrent.Future

class CategoryGuidanceControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val categorisationService      = mock[CategorisationService]
  private val mockGoodsRecordsConnector  = mock[GoodsRecordConnector]
  private val mockTraderProfileConnector = mock[TraderProfileConnector]

  private val mockNavigator = mock[Navigator]
  private val onwardRoute   = Call("", "")

  private val traderNoNiphls = TraderProfile(testEori, "ukims1", None, None)
  private val traderNiphls   = TraderProfile(testEori, "ukims2", None, Some("niphls123"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
      Future.successful(userAnswersForCategorisation)
    )
    when(mockGoodsRecordsConnector.updateCategoryForGoodsRecord(any(), any(), any())(any()))
      .thenReturn(Future.successful(Done))

    when(mockNavigator.nextPage(any(), any(), any())).thenReturn(onwardRoute)
    when(mockTraderProfileConnector.getTraderProfile(any())(any())).thenReturn(Future.successful(traderNoNiphls))
  }

  override def afterEach(): Unit = {
    super.afterEach()
    reset(categorisationService)
    reset(mockGoodsRecordsConnector)
    reset(mockNavigator)
    reset(mockTraderProfileConnector)
  }

  "CategoryGuidance Controller" - {

    "GET" - {

      "must call categorisation service to load and save ott info" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(userAnswersForCategorisation))
          .overrides(
            bind[CategorisationService].toInstance(categorisationService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {

          val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
          val result  = route(application, request).value

          status(result) mustEqual OK
          verify(categorisationService, times(1)).requireCategorisation(any(), any())(any())

        }
      }

      "must redirect to JourneyRecovery" - {

        "when determining scenario fails due to invalid user answers" in {

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(emptyUserAnswers)
          )

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when call to connector fails in a redirect scenario" in {

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(uaForCategorisationStandardNoAssessments)
          )

          when(mockGoodsRecordsConnector.updateCategoryForGoodsRecord(any(), any(), any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Something went very wrong")))

          val application = applicationBuilder(userAnswers = Some(uaForCategorisationStandardNoAssessments))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when scenario is NiphlOnly and trader profile connector fails" in {

          when(mockTraderProfileConnector.getTraderProfile(any())(any())).thenReturn(
            Future.failed(new RuntimeException())
          )

          val userAnswers = emptyUserAnswers
            .set(
              RecordCategorisationsQuery,
              RecordCategorisations(Map(testRecordId -> categorisationInfoNiphlsNoOtherAssessments))
            )
            .success
            .value

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(userAnswers)
          )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector),
              bind[Navigator].toInstance(mockNavigator),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when scenario is NiphlOnly and goods records connector fails" in {

          when(mockGoodsRecordsConnector.updateCategoryForGoodsRecord(any(), any(), any())(any())).thenReturn(
            Future.failed(new RuntimeException())
          )

          val userAnswers = emptyUserAnswers
            .set(
              RecordCategorisationsQuery,
              RecordCategorisations(Map(testRecordId -> categorisationInfoNiphlsNoOtherAssessments))
            )
            .success
            .value

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(userAnswers)
          )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector),
              bind[Navigator].toInstance(mockNavigator),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when scenario is NiphlsAndOthers and trader profile connector fails" in {

          when(mockTraderProfileConnector.getTraderProfile(any())(any())).thenReturn(
            Future.failed(new RuntimeException())
          )

          val userAnswers = emptyUserAnswers
            .set(
              RecordCategorisationsQuery,
              RecordCategorisations(Map(testRecordId -> categorisationInfoNiphlsWithOtherAssessments))
            )
            .success
            .value

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(userAnswers)
          )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector),
              bind[Navigator].toInstance(mockNavigator),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when scenario is NiphlsAndOthers and goods records connector fails" in {

          when(mockGoodsRecordsConnector.updateCategoryForGoodsRecord(any(), any(), any())(any())).thenReturn(
            Future.failed(new RuntimeException())
          )

          val userAnswers = emptyUserAnswers
            .set(
              RecordCategorisationsQuery,
              RecordCategorisations(Map(testRecordId -> categorisationInfoNiphlsWithOtherAssessments))
            )
            .success
            .value

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(userAnswers)
          )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector),
              bind[Navigator].toInstance(mockNavigator),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

      }

      "must redirect via the Navigator" - {
        "when scenario is StandardNoAssessments" in {

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(uaForCategorisationStandardNoAssessments)
          )

          val application = applicationBuilder(userAnswers = Some(uaForCategorisationStandardNoAssessments))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector),
              bind[Navigator].toInstance(mockNavigator)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual onwardRoute.url

            withClue("must pass the correct scenario to the navigator") {
              verify(mockNavigator)
                .nextPage(eqTo(CategoryGuidancePage(testRecordId, Some(StandardNoAssessments))), eqTo(NormalMode), any)
            }

            withClue("must make a call to update goodsRecord with category info") {
              verify(mockGoodsRecordsConnector, times(1)).updateCategoryForGoodsRecord(
                any(),
                any(),
                any()
              )(any())
            }
          }
        }

        "when scenario is Category1NoExemptions" in {

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(uaForCategorisationCategory1NoExemptions)
          )

          val application = applicationBuilder(userAnswers = Some(uaForCategorisationCategory1NoExemptions))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector),
              bind[Navigator].toInstance(mockNavigator)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual onwardRoute.url

            withClue("must pass the correct scenario to the navigator") {
              verify(mockNavigator)
                .nextPage(eqTo(CategoryGuidancePage(testRecordId, Some(Category1NoExemptions))), eqTo(NormalMode), any)
            }

            withClue("must make a call to update goodsRecord with category info") {
              verify(mockGoodsRecordsConnector, times(1)).updateCategoryForGoodsRecord(
                any(),
                any(),
                any()
              )(any())
            }
          }
        }

        "with Category 1 when scenario is NiphlOnly and user does not have Niphls" in {

          val userAnswers = emptyUserAnswers
            .set(
              RecordCategorisationsQuery,
              RecordCategorisations(Map(testRecordId -> categorisationInfoNiphlsNoOtherAssessments))
            )
            .success
            .value

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(userAnswers)
          )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector),
              bind[Navigator].toInstance(mockNavigator),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual onwardRoute.url

            withClue("must pass the correct scenario to the navigator") {
              verify(mockNavigator)
                .nextPage(eqTo(CategoryGuidancePage(testRecordId, Some(Category1))), eqTo(NormalMode), any)
            }

            withClue("must make a call to update goodsRecord with category info") {
              verify(mockGoodsRecordsConnector, times(1)).updateCategoryForGoodsRecord(
                any(),
                any(),
                any()
              )(any())
            }
          }
        }

        "with Category 2 when scenario is NiphlOnly and user has Niphls" in {

          val userAnswers = emptyUserAnswers
            .set(
              RecordCategorisationsQuery,
              RecordCategorisations(Map(testRecordId -> categorisationInfoNiphlsNoOtherAssessments))
            )
            .success
            .value

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(userAnswers)
          )

          when(mockTraderProfileConnector.getTraderProfile(any())(any())).thenReturn(
            Future.successful(traderNiphls)
          )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector),
              bind[Navigator].toInstance(mockNavigator),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual onwardRoute.url

            withClue("must pass the Category 2 to the navigator") {
              verify(mockNavigator)
                .nextPage(eqTo(CategoryGuidancePage(testRecordId, Some(Category2))), eqTo(NormalMode), any)
            }

            withClue("must make a call to update goodsRecord with category info") {
              verify(mockGoodsRecordsConnector, times(1)).updateCategoryForGoodsRecord(
                any(),
                any(),
                any()
              )(any())
            }
          }
        }

        "with Category 1 when scenario is NiphlsAndOthers and user does not have Niphls" in {

          val userAnswers = emptyUserAnswers
            .set(
              RecordCategorisationsQuery,
              RecordCategorisations(Map(testRecordId -> categorisationInfoNiphlsWithOtherAssessments))
            )
            .success
            .value

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(userAnswers)
          )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector),
              bind[Navigator].toInstance(mockNavigator),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual onwardRoute.url

            withClue("must pass the correct scenario to the navigator") {
              verify(mockNavigator)
                .nextPage(eqTo(CategoryGuidancePage(testRecordId, Some(Category1))), eqTo(NormalMode), any)
            }

            withClue("must make a call to update goodsRecord with category info") {
              verify(mockGoodsRecordsConnector, times(1)).updateCategoryForGoodsRecord(
                any(),
                any(),
                any()
              )(any())
            }
          }
        }

        "when scenario is NoRedirectScenario and we are recategorising so do not want to display the page again" in {

          val userAnswers = userAnswersForCategorisation
            .set(RecategorisingQuery(testRecordId), true)
            .success
            .value

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(userAnswers)
          )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector),
              bind[Navigator].toInstance(mockNavigator)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual onwardRoute.url

            withClue("must pass the correct scenario to the navigator") {
              verify(mockNavigator)
                .nextPage(eqTo(CategoryGuidancePage(testRecordId, Some(NoRedirectScenario))), eqTo(NormalMode), any)
            }

            withClue("must NOT make a call to update goodsRecord with category info") {
              verify(mockGoodsRecordsConnector, never()).updateCategoryForGoodsRecord(
                any(),
                any(),
                any()
              )(any())
            }
          }
        }
      }

      "must OK with correct view when" - {

        "scenario is NoRedirectScenario and not recategorising" in {

          val application = applicationBuilder(userAnswers = Some(userAnswersForCategorisation))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CategoryGuidanceView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(NormalMode, testRecordId)(request, messages(application)).toString
          }

          withClue("must NOT make a call to update goodsRecord with category info") {
            verify(mockGoodsRecordsConnector, never()).updateCategoryForGoodsRecord(
              any(),
              any(),
              any()
            )(any())
          }
        }

        "when scenario is NiphlAndOthers and user has Niphls" in {

          val userAnswers = emptyUserAnswers
            .set(
              RecordCategorisationsQuery,
              RecordCategorisations(Map(testRecordId -> categorisationInfoNiphlsWithOtherAssessments))
            )
            .success
            .value

          when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
            Future.successful(userAnswers)
          )

          when(mockTraderProfileConnector.getTraderProfile(any())(any())).thenReturn(
            Future.successful(traderNiphls)
          )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[CategorisationService].toInstance(categorisationService),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordsConnector),
              bind[Navigator].toInstance(mockNavigator),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(NormalMode, testRecordId).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CategoryGuidanceView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(NormalMode, testRecordId)(request, messages(application)).toString
          }

          withClue("must NOT make a call to update goodsRecord with category info") {
            verify(mockGoodsRecordsConnector, never()).updateCategoryForGoodsRecord(
              any(),
              any(),
              any()
            )(any())
          }
        }

      }
    }

    "POST" - {
      "must redirect to the categorisation page when the user click continue button" in {

        val mockAuditService = mock[AuditService]

        when(mockAuditService.auditStartUpdateGoodsRecord(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val application = applicationBuilder(userAnswers = Some(userAnswersForCategorisation))
          .overrides(
            bind[CategorisationService].toInstance(categorisationService),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CategoryGuidanceController.onSubmit(NormalMode, testRecordId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.AssessmentController
            .onPageLoad(NormalMode, testRecordId, 0)
            .url

          withClue("must call the audit service with the correct details") {
            verify(mockAuditService, times(1))
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(CategorisationUpdate),
                eqTo(testRecordId)
              )(any())
          }
        }
      }
    }
  }
}
