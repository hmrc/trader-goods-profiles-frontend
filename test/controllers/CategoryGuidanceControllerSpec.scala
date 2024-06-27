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
import base.TestConstants.{testEori, testRecordId}
import models.helper.CategorisationUpdate
import models.{Category1NoExemptions, Commodity, NormalMode, StandardNoAssessments}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.CommodityQuery
import repositories.SessionRepository
import services.{AuditService, CategorisationService}
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.CategoryGuidanceView

import java.time.Instant
import scala.concurrent.Future

class CategoryGuidanceControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val categorisationService = mock[CategorisationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
      Future.successful(userAnswersForCategorisation)
    )
  }

  override def afterEach(): Unit = {
    super.afterEach()
    reset(categorisationService)
  }

  "CategoryGuidance Controller" - {

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

        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(testRecordId).url)
        val result  = route(application, request).value

        status(result) mustEqual OK
        verify(categorisationService, times(1)).requireCategorisation(any(), any())(any())

      }
    }

    "must redirect to JourneyRecovery when determining scenario fails due to invalid user answers" in {

      when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
        Future.successful(emptyUserAnswers)
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[CategorisationService].toInstance(categorisationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(testRecordId).url)
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to CategorisationResult when scenario is StandardNoAssessments" in {

      when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
        Future.successful(uaForCategorisationStandardNoAssessments)
      )

      val application = applicationBuilder(userAnswers = Some(uaForCategorisationStandardNoAssessments))
        .overrides(
          bind[CategorisationService].toInstance(categorisationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(testRecordId).url)
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual routes.CategorisationResultController
          .onPageLoad(testRecordId, StandardNoAssessments)
          .url
      }
    }

    "must redirect to CategorisationResult when scenario is Category1NoExemptions" in {

      when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
        Future.successful(uaForCategorisationCategory1NoExemptions)
      )

      val application = applicationBuilder(userAnswers = Some(uaForCategorisationCategory1NoExemptions))
        .overrides(
          bind[CategorisationService].toInstance(categorisationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(testRecordId).url)
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual routes.CategorisationResultController
          .onPageLoad(testRecordId, Category1NoExemptions)
          .url
      }
    }

    "must OK with correct view and not redirect on a GET when scenario is NOT Category1NoExemptions or StandardNoAssessments" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersForCategorisation))
        .overrides(
          bind[CategorisationService].toInstance(categorisationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(testRecordId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CategoryGuidanceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(testRecordId)(request, messages(application)).toString
      }
    }

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
        val request = FakeRequest(POST, routes.CategoryGuidanceController.onSubmit(testRecordId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AssessmentController.onPageLoad(NormalMode, testRecordId, 0).url

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
