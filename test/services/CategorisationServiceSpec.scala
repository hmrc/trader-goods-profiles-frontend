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

package services

import base.SpecBase
import base.TestConstants.{NiphlsCode, NirmsCode, testRecordId}
import connectors.{GoodsRecordConnector, OttConnector, TraderProfileConnector}
import models.ott._
import models.ott.response.{CategoryAssessmentRelationship, ExemptionType => ResponseExemptionType, _}
import models.requests.DataRequest
import models.router.responses.GetGoodsRecordResponse
import models.{AssessmentAnswer, Category1NoExemptionsScenario, Category1Scenario, Category2Scenario, StandardGoodsNoAssessmentsScenario, StandardGoodsScenario, TraderProfile}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{AssessmentPage, ReassessmentPage}
import play.api.mvc.AnyContent
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CategorisationServiceSpec extends SpecBase with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val mockSessionRepository      = mock[SessionRepository]
  private val mockOttConnector           = mock[OttConnector]
  private val mockGoodsRecordsConnector  = mock[GoodsRecordConnector]
  private val mockTraderProfileConnector = mock[TraderProfileConnector]

  private def mockOttResponse(comCode: String = "1234567890") = OttResponse(
    GoodsNomenclatureResponse("some id", comCode, Some("Weight, in kilograms"), Instant.EPOCH, None, List("test")),
    categoryAssessmentRelationships = Seq(
      CategoryAssessmentRelationship("assessmentId2")
    ),
    includedElements = Seq(
      ThemeResponse("themeId1", 1),
      CategoryAssessmentResponse(
        "assessmentId2",
        "themeId2",
        Seq(
          ExemptionResponse("exemptionId1", ResponseExemptionType.Certificate),
          ExemptionResponse("exemptionId2", ResponseExemptionType.AdditionalCode)
        )
      ),
      ThemeResponse("themeId2", 2),
      CertificateResponse("exemptionId1", "code1", "description1"),
      AdditionalCodeResponse("exemptionId2", "code2", "description2"),
      ThemeResponse("ignoredTheme", 3),
      CertificateResponse("ignoredExemption", "code3", "description3")
    ),
    descendents = Seq.empty[Descendant]
  )

  private val mockGoodsRecordResponse = GetGoodsRecordResponse(
    "recordId",
    "eori",
    "actorId",
    "traderRef",
    "comcode",
    "adviceStatus",
    "goodsDescription",
    "countryOfOrigin",
    Some(1),
    None,
    None,
    None,
    Instant.now(),
    None,
    1,
    active = true,
    toReview = true,
    None,
    "declarable",
    None,
    None,
    None,
    Instant.now(),
    Instant.now()
  )

  private val testTraderProfileResponseWithoutNiphl = TraderProfile("actorId", "ukims number", None, None)

  private val categorisationService =
    new CategorisationService(mockOttConnector, mockTraderProfileConnector)

  private val mockDataRequest = mock[DataRequest[AnyContent]]

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
    when(mockOttConnector.getCategorisationInfo(any(), any(), any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(mockOttResponse()))
    when(mockGoodsRecordsConnector.getRecord(any(), any())(any()))
      .thenReturn(Future.successful(mockGoodsRecordResponse))
    when(mockTraderProfileConnector.getTraderProfile(any())(any()))
      .thenReturn(Future.successful(testTraderProfileResponseWithoutNiphl))

    when(mockDataRequest.eori).thenReturn("eori")
    when(mockDataRequest.affinityGroup).thenReturn(AffinityGroup.Individual)

  }

  override def afterEach(): Unit = {
    super.afterEach()
    reset(mockSessionRepository)
    reset(mockGoodsRecordsConnector)
    reset(mockOttConnector)
    reset(mockTraderProfileConnector)
  }

  "getCategorisationInfo" - {

    "create a categorisation info record for the given commodity code" in {
      val expectedAssessments = Seq(
        CategoryAssessment(
          "assessmentId2",
          2,
          Seq(
            Certificate("exemptionId1", "code1", "description1"),
            AdditionalCode("exemptionId2", "code2", "description2")
          )
        )
      )

      await(categorisationService.getCategorisationInfo(mockDataRequest, "1234567890", "BV", testRecordId)) mustBe
        CategorisationInfo("1234567890", expectedAssessments, expectedAssessments, Some("Weight, in kilograms"), 0)

      withClue("should ask for details for this commodity and country from OTT") {
        verify(mockOttConnector).getCategorisationInfo(eqTo("1234567890"), any(), any(), any(), eqTo("BV"), any())(
          any()
        )
      }

    }

    "should return future failed when the call to OTT fails" in {
      val expectedException = new RuntimeException("Failed communicating with OTT")
      when(mockOttConnector.getCategorisationInfo(any(), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.failed(expectedException))

      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)

      val actualException = intercept[RuntimeException] {
        val result = categorisationService.getCategorisationInfo(mockDataRequest, "comCode", "DE", testRecordId)
        await(result)
      }

      actualException mustBe expectedException
    }

    "should return future failed when categorisation info does not build" in {

      val mockOttResponseThatIsBroken = OttResponse(
        GoodsNomenclatureResponse(
          "some id",
          "brokenComCode",
          Some("Weight, in kilograms"),
          Instant.EPOCH,
          None,
          List("test")
        ),
        categoryAssessmentRelationships = Seq(
          CategoryAssessmentRelationship("assessmentId1")
        ),
        Seq[IncludedElement](),
        Seq[Descendant]()
      )
      when(mockOttConnector.getCategorisationInfo(any(), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(mockOttResponseThatIsBroken))

      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)

      val actualException = intercept[RuntimeException] {
        val result = categorisationService.getCategorisationInfo(mockDataRequest, "comCode", "DE", testRecordId)
        await(result)
      }

      actualException.getMessage mustEqual "Could not build categorisation info"
    }

  }

  "calculateResult" - {

    "return Category 1" - {
      "if a category 1 question is No" in {
        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
          .success
          .value

        categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        ) mustEqual Category1Scenario

      }

      "if some category 1 are answered No and there are category 2 with no exemptions" in {
        val assessment1 = CategoryAssessment(
          "ass1",
          1,
          Seq(
            Certificate("cert1", "cert1code", "cert1desc")
          )
        )

        val assessment2 = CategoryAssessment(
          "ass2",
          1,
          Seq(
            Certificate("cert2", "cert2code", "cert2desc")
          )
        )

        val assessment3 = CategoryAssessment(
          "ass3",
          2,
          Seq(
            Certificate("cert3", "cert3code", "cert3desc")
          )
        )

        val assessment4 = CategoryAssessment(
          "ass4",
          2,
          Seq.empty
        )

        val categorisationInfo = CategorisationInfo(
          "1234567890",
          Seq(
            assessment1,
            assessment2,
            assessment4,
            assessment3
          ),
          Seq(assessment1, assessment2),
          None,
          1
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
          .success
          .value

        categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        ) mustEqual Category1Scenario

      }

      "if NIPHL is not authorised and has NIPHL assessments" in {
        val assessment1 = CategoryAssessment(
          "ass1",
          1,
          Seq(
            Certificate(NiphlsCode, "cert1code", "cert1desc")
          )
        )

        val assessment2 = CategoryAssessment(
          "ass2",
          1,
          Seq(
            Certificate(NiphlsCode, "cert2code", "cert2desc")
          )
        )

        val assessment3 = CategoryAssessment(
          "ass3",
          2,
          Seq.empty
        )

        val categorisationInfo = CategorisationInfo(
          "1234567890",
          Seq(
            assessment1,
            assessment2,
            assessment3
          ),
          Seq.empty,
          None,
          1
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value

        categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        ) mustEqual Category1Scenario
      }
    }

    "return Category 1 No Exemptions if a category 1 question has no exemptions" in {
      val categorisationInfo = CategorisationInfo(
        "1234567890",
        Seq(
          CategoryAssessment(
            "ass1",
            1,
            Seq.empty
          ),
          CategoryAssessment(
            "ass2",
            1,
            Seq(Certificate("cert1", "cert1c", "cert1desc"))
          )
        ),
        Seq.empty,
        None,
        1
      )

      val userAnswers = emptyUserAnswers
        .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
        .success
        .value

      categorisationService.calculateResult(
        categorisationInfo,
        userAnswers,
        testRecordId
      ) mustEqual Category1NoExemptionsScenario

    }

    "return Category 2" - {
      "if a category 2 question is No" in {
        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
          .success
          .value
          .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
          .success
          .value

        categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        ) mustEqual Category2Scenario

      }

      "if no category 1 assessments and category 2 question has no exemptions" in {
        val categorisationInfo = CategorisationInfo(
          "1234567890",
          Seq(
            CategoryAssessment(
              "ass1",
              2,
              Seq.empty
            ),
            CategoryAssessment(
              "ass2",
              2,
              Seq(Certificate("cert1", "cert1c", "cert1desc"))
            )
          ),
          Seq.empty,
          None,
          1
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value

        categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        ) mustEqual Category2Scenario

      }

      "if all category 1 are answered Yes and there are category 2 with no exemptions" in {
        val assessment1 = CategoryAssessment(
          "ass1",
          1,
          Seq(
            Certificate("cert1", "cert1code", "cert1desc")
          )
        )

        val assessment2 = CategoryAssessment(
          "ass2",
          1,
          Seq(
            Certificate("cert2", "cert2code", "cert2desc")
          )
        )

        val assessment3 = CategoryAssessment(
          "ass3",
          2,
          Seq(
            Certificate("cert3", "cert3code", "cert3desc")
          )
        )

        val assessment4 = CategoryAssessment(
          "ass4",
          2,
          Seq.empty
        )

        val categorisationInfo = CategorisationInfo(
          "1234567890",
          Seq(
            assessment1,
            assessment2,
            assessment4,
            assessment3
          ),
          Seq(assessment1, assessment2),
          None,
          1
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
          .success
          .value

        categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        ) mustEqual Category2Scenario

      }

      ".NIPHL" - {
        "is authorised and has only NIPHL assessments" in {
          val assessment1 = CategoryAssessment(
            "ass1",
            1,
            Seq(
              Certificate(NiphlsCode, "cert1code", "cert1desc")
            )
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(
              Certificate(NiphlsCode, "cert2code", "cert2desc")
            )
          )

          val assessment3 = CategoryAssessment(
            "ass3",
            2,
            Seq.empty
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            Seq(
              assessment1,
              assessment2,
              assessment3
            ),
            Seq.empty,
            None,
            1,
            isNiphlAuthorised = true
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category2Scenario
        }

        "is authorised and has NIPHL assessments when all category 1 are answered yes" in {
          val assessment1 = CategoryAssessment(
            "ass1",
            1,
            Seq(
              Certificate("cert1", "cert1code", "cert1desc")
            )
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(
              Certificate(NiphlsCode, "cert2code", "cert2desc")
            )
          )

          val assessment3 = CategoryAssessment(
            "ass3",
            2,
            Seq.empty
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            Seq(
              assessment1,
              assessment2,
              assessment3
            ),
            Seq(assessment1),
            None,
            1,
            isNiphlAuthorised = true
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category2Scenario
        }

        "is authorised and has NIPHL assessments when category 1 is answered no" in {
          val assessment1 = CategoryAssessment(
            "ass1",
            1,
            Seq(
              Certificate("cert1", "cert1code", "cert1desc")
            )
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(
              Certificate(NiphlsCode, "cert2code", "cert2desc")
            )
          )

          val assessment3 = CategoryAssessment(
            "ass1",
            1,
            Seq(
              Certificate("cert3", "cert3code", "cert3desc")
            )
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            Seq(
              assessment1,
              assessment2,
              assessment3
            ),
            Seq(assessment1, assessment2, assessment3),
            None,
            1,
            isNiphlAuthorised = true
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category1Scenario
        }
      }

      ".NIRMS" - {

        "is not authorised and has NIRMS assessment, category 1 assessment and category 2 assessment when all are answered yes" in {
          val nirmsAssessment = CategoryAssessment(
            "nirms1",
            2,
            Seq(
              Certificate(NirmsCode, "cert1code", "cert1desc")
            )
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(
              Certificate("cert2", "cert2code", "cert2desc")
            )
          )

          val assessment3 = CategoryAssessment(
            "ass3",
            2,
            Seq(
              Certificate("cert3", "cert3code", "cert3desc")
            )
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            Seq(
              nirmsAssessment,
              assessment2,
              assessment3
            ),
            Seq(assessment2, assessment3),
            None,
            1
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category2Scenario
        }
      }

    }

    "return StandardNoAssessments" - {

      "if all answers are Yes" in {

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
          .success
          .value
          .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption)
          .success
          .value

        categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        ) mustEqual StandardGoodsScenario

      }

      "if no assessments" in {
        val categoryInfoNoAssessments = CategorisationInfo(
          "1234567890",
          Seq.empty,
          Seq.empty,
          None,
          1
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
          .success
          .value

        categorisationService.calculateResult(
          categoryInfoNoAssessments,
          userAnswers,
          testRecordId
        ) mustBe StandardGoodsNoAssessmentsScenario
      }

      ".NIRMS" - {

        "is authorised and has NIRMS assessment, category 1 assessment and category 2 assessment when all are answered yes" in {
          val nirmsAssessment = CategoryAssessment(
            "nirms1",
            2,
            Seq(
              Certificate(NirmsCode, "cert1code", "cert1desc")
            )
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(
              Certificate("cert2", "cert2code", "cert2desc")
            )
          )

          val assessment3 = CategoryAssessment(
            "ass3",
            2,
            Seq(
              Certificate("cert3", "cert3code", "cert3desc")
            )
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            Seq(
              nirmsAssessment,
              assessment2,
              assessment3
            ),
            Seq(assessment2, assessment3),
            None,
            1,
            isNirmsAuthorised = true
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual StandardGoodsScenario
        }
      }
    }

  }

  "updatingAnswersForRecategorisation2" - {

    "should return the user answers with the reassessment answers set if old and new category assessments are the same" in {
      val expectedUserAnswers = userAnswersForCategorisation
        .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
        .success
        .value
        .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
        .success
        .value
        .set(ReassessmentPage(testRecordId, 2), AssessmentAnswer.Exemption)
        .success
        .value

      val result = categorisationService
        .updatingAnswersForRecategorisation(
          userAnswersForCategorisation,
          testRecordId,
          categorisationInfo,
          categorisationInfo
        )
        .success
        .value
      result shouldBe expectedUserAnswers
    }

    "should not copy across the old answers if all the assessments are different" in {
      val newCat                     = CategoryAssessment("0", 1, Seq(Certificate("Y199", "Y199", "Goods are not from warzone")))
      val newCommodityCategorisation = CategorisationInfo(
        "12345",
        Seq(newCat),
        Seq(newCat),
        None,
        1
      )
      val result                     = categorisationService
        .updatingAnswersForRecategorisation(
          userAnswersForCategorisation,
          testRecordId,
          categorisationInfo,
          newCommodityCategorisation
        )
        .success
        .value

      result.get(ReassessmentPage(testRecordId, 0)) shouldBe Some(AssessmentAnswer.NotAnsweredYet)
      result.get(ReassessmentPage(testRecordId, 1)) shouldBe None
      result.get(ReassessmentPage(testRecordId, 2)) shouldBe None
    }

    "should copy across all the old answers when new category info has a new assessment" in {
      val assList                    = Seq(
        category1,
        category2,
        category3,
        CategoryAssessment("0", 1, Seq(Certificate("Y199", "Y199", "Goods are not from warzone")))
      )
      val newCommodityCategorisation = CategorisationInfo(
        "1234567890",
        assList,
        assList,
        Some("Weight, in kilograms"),
        0
      )
      val result                     = categorisationService
        .updatingAnswersForRecategorisation(
          userAnswersForCategorisation,
          testRecordId,
          categorisationInfo,
          newCommodityCategorisation
        )
        .success
        .value
      result.get(ReassessmentPage(testRecordId, 0)) shouldBe Some(AssessmentAnswer.Exemption)
      result.get(ReassessmentPage(testRecordId, 1)) shouldBe Some(AssessmentAnswer.Exemption)
      result.get(ReassessmentPage(testRecordId, 2)) shouldBe Some(AssessmentAnswer.Exemption)
    }

    "should copy the old answers to the right position if they are in different order in the new categorisation" in {

      val category4                  = CategoryAssessment("0", 1, Seq(Certificate("Y199", "Y199", "Goods are not from warzone")))
      val newCommodityCategorisation = CategorisationInfo(
        "1234567890",
        Seq(category3, category1, category4, category2),
        Seq(category3, category1, category4, category2),
        Some("Weight, in kilograms"),
        0
      )

      val oldUserAnswers = emptyUserAnswers
        .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
        .success
        .value
        .set(LongerCategorisationDetailsQuery(testRecordId), newCommodityCategorisation)
        .success
        .value
        .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
        .success
        .value
        .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
        .success
        .value
        .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption)
        .success
        .value

      val newUserAnswers = categorisationService
        .updatingAnswersForRecategorisation(
          oldUserAnswers,
          testRecordId,
          categorisationInfo,
          newCommodityCategorisation
        )
        .success
        .value
      newUserAnswers.get(ReassessmentPage(testRecordId, 0)) mustBe Some(AssessmentAnswer.Exemption)
      newUserAnswers.get(ReassessmentPage(testRecordId, 1)) mustBe Some(AssessmentAnswer.Exemption)
      newUserAnswers.get(ReassessmentPage(testRecordId, 2)) mustBe Some(AssessmentAnswer.NotAnsweredYet)
      newUserAnswers.get(ReassessmentPage(testRecordId, 3)) mustBe Some(AssessmentAnswer.Exemption)
    }

    "should move the old answers to the right position if only some are in the new categorisation" in {

      val oldUserAnswers = emptyUserAnswers
        .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
        .success
        .value
        .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
        .success
        .value
        .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
        .success
        .value
        .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption)
        .success
        .value

      val category4                  = CategoryAssessment("0", 1, Seq(Certificate("Y199", "Y199", "Goods are not from warzone")))
      val newCommodityCategorisation = CategorisationInfo(
        "1234567890",
        Seq(category1, category4),
        Seq(category1, category4),
        Some("Weight, in kilograms"),
        0
      )

      val newUserAnswers = categorisationService
        .updatingAnswersForRecategorisation(
          oldUserAnswers,
          testRecordId,
          categorisationInfo,
          newCommodityCategorisation
        )
        .success
        .value
      newUserAnswers.get(ReassessmentPage(testRecordId, 0)) mustBe Some(AssessmentAnswer.Exemption)
      newUserAnswers.get(ReassessmentPage(testRecordId, 1)) mustBe Some(AssessmentAnswer.NotAnsweredYet)
      newUserAnswers.get(ReassessmentPage(testRecordId, 2)) mustBe None
    }

  }

}
