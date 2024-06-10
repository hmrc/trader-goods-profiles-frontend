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
import models.AssessmentAnswer
import models.AssessmentAnswer.NoExemption
import models.ott.{AdditionalCode, CategorisationInfo, CategoryAssessment, Certificate}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AssessmentPage, HasSupplementaryUnitPage, SupplementaryUnitPage}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.CategorisationQuery
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers.{AssessmentsSummary, HasSupplementaryUnitSummary, SupplementaryUnitSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.CyaCategorisationView

class CyaCategorisationControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaCategorisationController" - {

    "for a GET" - {

      "must return OK and the correct view" - {
        "when all category assessments answered" in {

          val categoryQuery = CategorisationInfo(
            "1234567890",
            Seq(
              CategoryAssessment("1", 1, Seq(Certificate("Y994", "Y994", "Goods are not from warzone"))),
              CategoryAssessment("2", 1, Seq(AdditionalCode("NC123", "NC123", "Not required"))),
              CategoryAssessment(
                "3",
                2,
                Seq(
                  Certificate("Y737", "Y737", "Goods not containing ivory"),
                  Certificate("X812", "X812", "Goods not containing seal products")
                )
              )
            )
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationQuery, categoryQuery)
            .success
            .value
            .set(AssessmentPage("1"), AssessmentAnswer.Exemption("Y994"))
            .success
            .value
            .set(AssessmentPage("2"), AssessmentAnswer.Exemption("NC123"))
            .success
            .value
            .set(AssessmentPage("3"), AssessmentAnswer.Exemption("X812"))
            .success
            .value

          val application                      = applicationBuilder(userAnswers = Some(userAnswers)).build()
          implicit val localMessages: Messages = messages(application)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad("123").url)

            val result = route(application, request).value

            val view                   = application.injector.instanceOf[CyaCategorisationView]
            val expectedAssessmentList = SummaryListViewModel(
              rows = Seq(
                //TODO copying test data about to pass in here bad
                //TODO this is an option??
                AssessmentsSummary
                  .row(userAnswers, "1", 1, 3, Seq(Certificate("Y994", "Y994", "Goods are not from warzone")))
                  .get,
                AssessmentsSummary
                  .row(userAnswers, "2", 2, 3, Seq(AdditionalCode("NC123", "NC123", "Not required")))
                  .get,
                AssessmentsSummary
                  .row(
                    userAnswers,
                    "3",
                    3,
                    3,
                    Seq(
                      Certificate("Y737", "Y737", "Goods not containing ivory"),
                      Certificate("X812", "X812", "Goods not containing seal products")
                    )
                  )
                  .get
              )
            )

            val list2 = SummaryListViewModel(
              rows = Seq.empty
            )
            status(result) mustEqual OK
            contentAsString(result) mustEqual view("123", expectedAssessmentList, list2)(
              request,
              messages(application)
            ).toString
          }
        }

        "when no exemption is used, meaning some assessment pages are not answered" in {

          val categoryQuery = CategorisationInfo(
            "1234567890",
            Seq(
              CategoryAssessment("1", 1, Seq(Certificate("Y994", "Y994", "Goods are not from warzone"))),
              CategoryAssessment("2", 1, Seq(AdditionalCode("NC123", "NC123", "Not required"))),
              CategoryAssessment(
                "3",
                2,
                Seq(
                  Certificate("Y737", "Y737", "Goods not containing ivory"),
                  Certificate("X812", "X812", "Goods not containing seal products")
                )
              )
            )
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationQuery, categoryQuery)
            .success
            .value
            .set(AssessmentPage("1"), AssessmentAnswer.Exemption("Y994"))
            .success
            .value
            .set(AssessmentPage("2"), NoExemption)
            .success
            .value

          val application                      = applicationBuilder(userAnswers = Some(userAnswers)).build()
          implicit val localMessages: Messages = messages(application)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad("123").url)

            val result = route(application, request).value

            val view                   = application.injector.instanceOf[CyaCategorisationView]
            val expectedAssessmentList = SummaryListViewModel(
              rows = Seq(
                //TODO copying test data about to pass in here bad
                //TODO this is an option??
                AssessmentsSummary
                  .row(userAnswers, "1", 1, 3, Seq(Certificate("Y994", "Y994", "Goods are not from warzone")))
                  .get,
                AssessmentsSummary
                  .row(userAnswers, "2", 2, 3, Seq(AdditionalCode("NC123", "NC123", "Not required")))
                  .get
              )
            )

            val list2 = SummaryListViewModel(
              rows = Seq.empty
            )
            status(result) mustEqual OK
            contentAsString(result) mustEqual view("123", expectedAssessmentList, list2)(
              request,
              messages(application)
            ).toString
          }
        }

        "when supplementary unit is supplied" in {

          val categoryQuery = CategorisationInfo(
            "1234567890",
            Seq(
              CategoryAssessment("1", 1, Seq(Certificate("Y994", "Y994", "Goods are not from warzone"))),
              CategoryAssessment("2", 1, Seq(AdditionalCode("NC123", "NC123", "Not required"))),
              CategoryAssessment(
                "3",
                2,
                Seq(
                  Certificate("Y737", "Y737", "Goods not containing ivory"),
                  Certificate("X812", "X812", "Goods not containing seal products")
                )
              )
            )
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationQuery, categoryQuery)
            .success
            .value
            .set(AssessmentPage("1"), AssessmentAnswer.Exemption("Y994"))
            .success
            .value
            .set(AssessmentPage("2"), AssessmentAnswer.Exemption("NC123"))
            .success
            .value
            .set(AssessmentPage("3"), AssessmentAnswer.Exemption("X812"))
            .success
            .value
            .set(HasSupplementaryUnitPage, true)
            .success
            .value
            .set(SupplementaryUnitPage, 1234)
            .success
            .value

          val application                      = applicationBuilder(userAnswers = Some(userAnswers)).build()
          implicit val localMessages: Messages = messages(application)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad("123").url)

            val result = route(application, request).value

            val view                   = application.injector.instanceOf[CyaCategorisationView]
            val expectedAssessmentList = SummaryListViewModel(
              rows = Seq(
                //TODO copying test data about to pass in here bad
                //TODO this is an option??
                AssessmentsSummary
                  .row(userAnswers, "1", 1, 3, Seq(Certificate("Y994", "Y994", "Goods are not from warzone")))
                  .get,
                AssessmentsSummary
                  .row(userAnswers, "2", 2, 3, Seq(AdditionalCode("NC123", "NC123", "Not required")))
                  .get,
                AssessmentsSummary
                  .row(
                    userAnswers,
                    "3",
                    3,
                    3,
                    Seq(
                      Certificate("Y737", "Y737", "Goods not containing ivory"),
                      Certificate("X812", "X812", "Goods not containing seal products")
                    )
                  )
                  .get
              )
            )

            val expectedSupplementaryUnitList = SummaryListViewModel(
              rows = Seq(
                HasSupplementaryUnitSummary.row(userAnswers, "123"),
                SupplementaryUnitSummary.row(userAnswers)
              ).flatten
            )
            status(result) mustEqual OK
            contentAsString(result) mustEqual view("123", expectedAssessmentList, expectedSupplementaryUnitList)(
              request,
              messages(application)
            ).toString
          }
        }

        "when supplementary unit is not supplied" in {

          val categoryQuery = CategorisationInfo(
            "1234567890",
            Seq(
              CategoryAssessment("1", 1, Seq(Certificate("Y994", "Y994", "Goods are not from warzone"))),
              CategoryAssessment("2", 1, Seq(AdditionalCode("NC123", "NC123", "Not required"))),
              CategoryAssessment(
                "3",
                2,
                Seq(
                  Certificate("Y737", "Y737", "Goods not containing ivory"),
                  Certificate("X812", "X812", "Goods not containing seal products")
                )
              )
            )
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationQuery, categoryQuery)
            .success
            .value
            .set(AssessmentPage("1"), AssessmentAnswer.Exemption("Y994"))
            .success
            .value
            .set(AssessmentPage("2"), AssessmentAnswer.Exemption("NC123"))
            .success
            .value
            .set(AssessmentPage("3"), AssessmentAnswer.Exemption("X812"))
            .success
            .value
            .set(HasSupplementaryUnitPage, false)
            .success
            .value

          val application                      = applicationBuilder(userAnswers = Some(userAnswers)).build()
          implicit val localMessages: Messages = messages(application)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad("123").url)

            val result = route(application, request).value

            val view                   = application.injector.instanceOf[CyaCategorisationView]
            val expectedAssessmentList = SummaryListViewModel(
              rows = Seq(
                //TODO copying test data about to pass in here bad
                //TODO this is an option??
                AssessmentsSummary
                  .row(userAnswers, "1", 1, 3, Seq(Certificate("Y994", "Y994", "Goods are not from warzone")))
                  .get,
                AssessmentsSummary
                  .row(userAnswers, "2", 2, 3, Seq(AdditionalCode("NC123", "NC123", "Not required")))
                  .get,
                AssessmentsSummary
                  .row(
                    userAnswers,
                    "3",
                    3,
                    3,
                    Seq(
                      Certificate("Y737", "Y737", "Goods not containing ivory"),
                      Certificate("X812", "X812", "Goods not containing seal products")
                    )
                  )
                  .get
              )
            )

            val expectedSupplementaryUnitList = SummaryListViewModel(
              rows = Seq(
                HasSupplementaryUnitSummary.row(userAnswers, "123")
              ).flatten
            )
            status(result) mustEqual OK
            contentAsString(result) mustEqual view("123", expectedAssessmentList, expectedSupplementaryUnitList)(
              request,
              messages(application)
            ).toString
          }
        }

      }

      "must redirect to Journey Recovery" - {

        "when no answers are found" in {
          val application = applicationBuilder(Some(emptyUserAnswers)).build()
          val continueUrl = RedirectUrl(routes.CategoryGuidanceController.onPageLoad("123").url)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad("123").url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

          }
        }

        "when no existing data is found" in {
          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad("123").url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when validation errors" in {

          val categoryQuery = CategorisationInfo(
            "1234567890",
            Seq(
              CategoryAssessment("1", 1, Seq(Certificate("Y994", "Y994", "Goods are not from warzone"))),
              CategoryAssessment("2", 1, Seq(AdditionalCode("NC123", "NC123", "Not required"))),
              CategoryAssessment(
                "3",
                2,
                Seq(
                  Certificate("Y737", "Y737", "Goods not containing ivory"),
                  Certificate("X812", "X812", "Goods not containing seal products")
                )
              )
            )
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationQuery, categoryQuery)
            .success
            .value
            .set(SupplementaryUnitPage, 123)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          val continueUrl = RedirectUrl(routes.CategoryGuidanceController.onPageLoad("123").url)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad("123").url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url
          }
        }

      }

    }

    "for a POST" - {

      "must redirect to ???" in {

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad("test").url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.IndexController.onPageLoad.url
        }
      }
    }
  }

}
