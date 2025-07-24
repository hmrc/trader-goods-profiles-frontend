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
import base.TestConstants._
import connectors.{GoodsRecordConnector, OttConnector, TraderProfileConnector}
import generators.Generators
import models.*
import models.DeclarableStatus.NotReadyForUse
import models.ott.*
import models.ott.response.{CategoryAssessmentRelationship, ExemptionType => ResponseExemptionType, *}
import models.requests.DataRequest
import models.router.responses.GetGoodsRecordResponse
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.RecoverMethods.recoverToExceptionIf
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.categorisation.{AssessmentPage, ReassessmentPage}
import play.api.mvc.AnyContent
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants.{countryOfOriginKey, goodsDescriptionKey}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CategorisationServiceSpec extends SpecBase with BeforeAndAfterEach with Generators with Matchers {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  private val mockSessionRepository           = mock[SessionRepository]
  private val mockOttConnector                = mock[OttConnector]
  private val mockGoodsRecordsConnector       = mock[GoodsRecordConnector]
  private val mockTraderProfileConnector      = mock[TraderProfileConnector]
  val invalidOttResponse: OttResponse         = OttResponse(
    goodsNomenclature = GoodsNomenclatureResponse("someId", "someCode", None, Instant.EPOCH, None, List()),
    categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentIdMissing")),
    includedElements = Seq(),
    descendents = Seq()
  )

  private def mockOttResponse(comCode: String = "1234567890") = OttResponse(
    GoodsNomenclatureResponse("some id", comCode, Some("Weight, in kilograms"), Instant.EPOCH, None, List("test")),
    categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId2")),
    includedElements = Seq(
      ThemeResponse("themeId1", 1, "theme description"),
      CategoryAssessmentResponse(
        "assessmentId2",
        "themeId2",
        Seq(
          ExemptionResponse("exemptionId1", ResponseExemptionType.Certificate),
          ExemptionResponse("exemptionId2", ResponseExemptionType.AdditionalCode)
        ),
        "regulationId1"
      ),
      ThemeResponse("themeId2", 2, "theme description"),
      CertificateResponse("exemptionId1", "code1", "description1"),
      AdditionalCodeResponse("exemptionId2", "code2", "description2"),
      ThemeResponse("ignoredTheme", 3, "theme description"),
      CertificateResponse("ignoredExemption", "code3", "description3"),
      LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1"))
    ),
    descendents = Seq.empty[Descendant]
  )

  private def mockOttResponseWithNiphlOnlyAssessment(comCode: String = "1234567890") = OttResponse(
    GoodsNomenclatureResponse("some id", comCode, Some("Weight, in kilograms"), Instant.EPOCH, None, List("test")),
    categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentNiphlOnly")),
    includedElements = Seq(
      ThemeResponse("themeId1", 1, "theme description"),
      CategoryAssessmentResponse(
        "assessmentNiphlOnly",
        "themeId1",
        Seq(
          ExemptionResponse("niphlExemptionId", ResponseExemptionType.Certificate)
        ),
        "regulationId1"
      ),
      CertificateResponse("niphlExemptionId", NiphlCode, "NIPHL Certificate Description"),
      LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1"))
    ),
    descendents = Seq.empty[Descendant]
  )

  private val mockGoodsRecordResponse = GetGoodsRecordResponse(
    "recordId",
    "eori",
    "actorId",
    "traderRef",
    "comcode",
    arbitraryAdviceStatus.sample.value,
    goodsDescriptionKey,
    countryOfOriginKey,
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
    NotReadyForUse,
    None,
    None,
    None,
    Instant.now(),
    Instant.now()
  )

  private val testTraderProfileResponseWithoutNiphl =
    TraderProfile("actorId", "ukims number", None, None, eoriChanged = false)
  private val categorisationService                 =
    new CategorisationService(mockOttConnector, mockTraderProfileConnector, mockSessionRepository)
  private val mockDataRequest                       = mock[DataRequest[AnyContent]]

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

    when(mockTraderProfileConnector.getTraderProfile)
      .thenReturn(Future.successful(testTraderProfileResponseWithoutNiphl))

    when(mockOttConnector.getCategorisationInfo(any(), any(), any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(mockOttResponse()))

    when(mockGoodsRecordsConnector.getRecord(any())(any())).thenReturn(Future.successful(mockGoodsRecordResponse))

    when(mockDataRequest.eori).thenReturn("eori")
    when(mockDataRequest.affinityGroup).thenReturn(AffinityGroup.Individual)
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
          ),
          "theme description",
          Some("regulationUrl1")
        )
      )

      await(categorisationService.getCategorisationInfo(mockDataRequest, "1234567890", "BV", testRecordId)) mustBe
        CategorisationInfo(
          "1234567890",
          "BV",
          None,
          expectedAssessments,
          expectedAssessments,
          Some("Weight, in kilograms"),
          0
        )

      withClue("should ask for details for this commodity and country from OTT") {
        verify(mockOttConnector).getCategorisationInfo(eqTo("1234567890"), any(), any(), any(), eqTo("BV"), any())(
          any()
        )
      }
    }

    "should return future failed when the call to trader profile connector fails" in {
      val expectedException = new RuntimeException("Failed communicating with Trader profile connector")
      when(mockTraderProfileConnector.getTraderProfile(any())).thenReturn(Future.failed(expectedException))

      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)

      val actualException = intercept[RuntimeException] {
        val result = categorisationService.getCategorisationInfo(mockDataRequest, "comCode", "DE", testRecordId)
        await(result)
      }
      actualException mustBe expectedException
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
      when(mockDataRequest.eori).thenReturn("eori")
      when(mockDataRequest.affinityGroup).thenReturn(AffinityGroup.Individual)

      when(
        mockOttConnector.getCategorisationInfo(
          eqTo("invalidCode"),
          eqTo("eori"),
          eqTo(AffinityGroup.Individual),
          any(),
          eqTo("BV"),
          any()
        )(any())
      ).thenReturn(Future.successful(invalidOttResponse))

      val futureResult = categorisationService.getCategorisationInfo(mockDataRequest, "invalidCode", "BV", testRecordId)

      recoverToExceptionIf[RuntimeException] {
        futureResult
      }.map { ex =>
        ex.getMessage must include("Could not build categorisation info")
      }
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

        categorisationService.calculateResult(categorisationInfo, userAnswers, testRecordId) mustEqual Category1Scenario
      }

      "if some category 1 are answered No and there are category 2 with no exemptions" in {
        val assessment1 = CategoryAssessment(
          "ass1",
          1,
          Seq(Certificate("cert1", "cert1code", "cert1desc")),
          "theme description",
          Some("regulationUrl1")
        )

        val assessment2 = CategoryAssessment(
          "ass2",
          1,
          Seq(Certificate("cert2", "cert2code", "cert2desc")),
          "theme description",
          Some("regulationUrl2")
        )

        val assessment3 = CategoryAssessment(
          "ass3",
          2,
          Seq(Certificate("cert3", "cert3code", "cert3desc")),
          "theme description",
          Some("regulationUrl3")
        )

        val assessment4 = CategoryAssessment("ass4", 2, Seq.empty, "theme description", Some("regulationUrl4"))

        val categorisationInfo = CategorisationInfo(
          "1234567890",
          "BV",
          Some(validityEndDate),
          Seq(assessment1, assessment2, assessment4, assessment3),
          Seq(assessment1, assessment2),
          None,
          1
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
          .success
          .value

        categorisationService.calculateResult(categorisationInfo, userAnswers, testRecordId) mustEqual Category1Scenario
      }

      "if NIPHL is not authorised and has a NIPHL assessment with other possible exemptions" in {
        val assessment1 = CategoryAssessment(
          "ass1",
          1,
          Seq(Certificate("cert1code", NiphlCode, "cert1desc"), Certificate("cert2", "cert2code", "cert2desc")),
          "theme description",
          Some("regulationUrl1")
        )

        val categorisationInfo =
          CategorisationInfo("1234567890", "BV", None, Seq(assessment1), Seq(assessment1), None, 1)
        val userAnswers        = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
          .success
          .value

        categorisationService.calculateResult(categorisationInfo, userAnswers, testRecordId) mustEqual Category1Scenario
      }

      "if NIPHL is authorised and has a NIPHL assessments but answer no to another question" in {
        val assessment1 = CategoryAssessment(
          "ass1",
          1,
          Seq(Certificate(NiphlCode, "cert1code", "cert1desc")),
          "theme description",
          Some("regulationUrl1")
        )

        val assessment2 = CategoryAssessment(
          "ass2",
          1,
          Seq(Certificate("Y992", "cert2code", "cert2desc")),
          "theme description",
          Some("regulationUrl2")
        )

        val assessment3 = CategoryAssessment("ass3", 2, Seq.empty, "theme description", Some("regulationUrl3"))

        val categorisationInfo = CategorisationInfo(
          "1234567890",
          "BV",
          None,
          Seq(assessment1, assessment2, assessment3),
          Seq(assessment2),
          None,
          1,
          isTraderNiphlAuthorised = true
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
          .success
          .value

        categorisationService.calculateResult(categorisationInfo, userAnswers, testRecordId) mustEqual Category1Scenario
      }

      ".NIRMS" - {
        "if NIRMS is authorised and has category 1 question but answered no" in {
          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            None,
            Seq(category2Nirms, category1),
            Seq(category1),
            None,
            1,
            isTraderNirmsAuthorised = true
          )

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

        "if NIRMS is not authorised and has category 1 question but answered no" in {
          val categorisationInfo =
            CategorisationInfo("1234567890", "BV", None, Seq(category2Nirms, category1), Seq(category1), None, 1)

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
            .success
            .value
          when(mockOttConnector.getCategorisationInfo(any(), any(), any(), any(), any(), any())(any()))
            .thenReturn(Future.successful(mockOttResponseWithNiphlOnlyAssessment()))

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category1Scenario
        }
      }
    }

    "return Category 1 No Exemptions if a category 1 question has no exemptions" - {

      "if NIPHL is not authorised and has a NIPHL assessment without other possible exemptions" in {
        val assessment1 = CategoryAssessment(
          id = "ass1",
          category = 1,
          exemptions = Seq(Certificate("cert1code", NiphlCode, "cert1desc")),
          themeDescription = "theme description",
          regulationUrl = Some("regulationUrl1")
        )

        assert(assessment1.isCategory1)
        assert(assessment1.onlyContainsNiphlAnswer)
        assert(!assessment1.hasNoExemptions)

        val categorisationInfo = CategorisationInfo(
          commodityCode = "1234567890",
          countryOfOrigin = "BV",
          comcodeEffectiveToDate = None,
          categoryAssessments = Seq(assessment1),
          categoryAssessmentsThatNeedAnswers = Seq.empty,
          measurementUnit = None,
          descendantCount = 1
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value

        val result = categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        )

        result mustEqual Category1NoExemptionsScenario
      }

      "and not a Niphl assessment" in {
        val categorisationInfo = CategorisationInfo(
          "1234567890",
          "BV",
          Some(validityEndDate),
          Seq(
            CategoryAssessment("ass1", 1, Seq.empty, "theme description", Some("regulationUrl1")),
            CategoryAssessment(
              "ass2",
              1,
              Seq(Certificate("cert1", "cert1c", "cert1desc")),
              "theme description",
              Some("regulationUrl2")
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

      "and is a Niphl assessment" in {
        val assessment1        = CategoryAssessment(
          "ass1",
          1,
          Seq(Certificate(NiphlCode, "cert1code", "cert1desc")),
          "theme description",
          Some("regulationUrl1")
        )
        val assessment2        = CategoryAssessment("ass2", 1, Seq.empty, "theme description", Some("regulationUrl2"))
        val assessment3        = CategoryAssessment("ass3", 2, Seq.empty, "theme description", Some("regulationUrl3"))
        val categorisationInfo = CategorisationInfo(
          "1234567890",
          "BV",
          None,
          Seq(assessment1, assessment2, assessment3),
          Seq.empty,
          None,
          1,
          isTraderNiphlAuthorised = true
        )

        val userAnswers =
          emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

        categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        ) mustEqual Category1NoExemptionsScenario
      }

      ".NIRMS" - {
        "if NIRMS is authorised and category 1 with no exemptions" in {
          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            None,
            Seq(category2Nirms, category1NoExemptions),
            Seq.empty,
            None,
            1,
            isTraderNirmsAuthorised = true
          )
          val userAnswers        =
            emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category1NoExemptionsScenario
        }

        "if NIRMS is not authorised and category 1 with no exemptions" in {
          val categorisationInfo =
            CategorisationInfo("1234567890", "BV", None, Seq(category2Nirms, category1NoExemptions), Seq.empty, None, 1)
          val userAnswers        =
            emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category1NoExemptionsScenario
        }
      }
    }

    "return Category 2" - {
      "if a category 2 question is No" in {
        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
          .success
          .value

        categorisationService.calculateResult(categorisationInfo, userAnswers, testRecordId) mustEqual Category2Scenario
      }

      "if no category 1 assessments and category 2 question has no exemptions must return cat 2 no exemptions" in {
        val categorisationInfo = CategorisationInfo(
          "1234567890",
          "BV",
          Some(validityEndDate),
          Seq(
            CategoryAssessment("ass1", 2, Seq.empty, "theme description", Some("regulationUrl1")),
            CategoryAssessment("ass2", 2, Seq.empty, "theme description", Some("regulationUrl2"))
          ),
          Seq.empty,
          None,
          1
        )

        val userAnswers =
          emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

        categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        ) mustEqual Category2NoExemptionsScenario
      }
      "if no category 1 assessments and category 2 question has exemptions must return Category2Scenario" in {
        val assessment1 = CategoryAssessment(
          "ass1",
          2,
          Seq(Certificate("cert1", "cert1c", "cert1desc")),
          "theme description",
          Some("regulationUrl1")
        )

        val assessment2 = CategoryAssessment(
          "ass2",
          2,
          Seq(Certificate("cert2", "cert2c", "cert2desc")),
          "theme description",
          Some("regulationUrl2")
        )

        val categorisationInfo = CategorisationInfo(
          "1234567890",
          "BV",
          Some(validityEndDate),
          Seq(assessment1, assessment2),
          Seq(assessment1, assessment2),
          None,
          1
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
          .success
          .value

        categorisationService.calculateResult(categorisationInfo, userAnswers, testRecordId) mustEqual Category2Scenario
      }

      "if all category 1 are answered Yes and there are category 2 with no exemptions" in {
        val assessment1 = CategoryAssessment(
          "ass1",
          1,
          Seq(Certificate("cert1", "cert1code", "cert1desc")),
          "theme description",
          Some("regulationUrl1")
        )

        val assessment2 = CategoryAssessment(
          "ass2",
          1,
          Seq(Certificate("cert2", "cert2code", "cert2desc")),
          "theme description",
          Some("regulationUrl2")
        )

        val assessment3 = CategoryAssessment(
          "ass3",
          2,
          Seq(Certificate("cert3", "cert3code", "cert3desc")),
          "theme description",
          Some("regulationUrl3")
        )

        val assessment4 = CategoryAssessment(
          "ass4",
          2,
          Seq.empty,
          "theme description",
          Some("regulationUrl4")
        )

        val categorisationInfo = CategorisationInfo(
          "1234567890",
          "BV",
          Some(validityEndDate),
          Seq(assessment1, assessment2, assessment4, assessment3),
          Seq(assessment1, assessment2),
          None,
          1
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value

        categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        ) mustEqual Category2NoExemptionsScenario
      }

      "if all category 1 are answered Yes and there are category 2 with exemptions" in {
        val assessment1 = CategoryAssessment(
          "ass1",
          1,
          Seq(Certificate("cert1", "cert1code", "cert1desc")),
          "theme description",
          Some("regulationUrl1")
        )

        val assessment2 = CategoryAssessment(
          "ass2",
          1,
          Seq(Certificate("cert2", "cert2code", "cert2desc")),
          "theme description",
          Some("regulationUrl2")
        )

        val assessment3 = CategoryAssessment(
          "ass3",
          2,
          Seq(Certificate("cert3", "cert3code", "cert3desc")),
          "theme description",
          Some("regulationUrl3")
        )

        val assessment4 = CategoryAssessment(
          "ass4",
          2,
          Seq(Certificate("cert4", "cert4code", "cert4desc")),
          "theme description",
          Some("regulationUrl4")
        )

        val categorisationInfo = CategorisationInfo(
          "1234567890",
          "BV",
          Some(validityEndDate),
          Seq(assessment1, assessment2, assessment4, assessment3),
          Seq(assessment1, assessment2, assessment4),
          None,
          1
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
          .success
          .value

        categorisationService.calculateResult(categorisationInfo, userAnswers, testRecordId) mustEqual Category2Scenario
      }

      ".NIPHL" - {
        "is authorised and has only NIPHL assessments" in {
          val assessment1 = CategoryAssessment(
            "ass1",
            1,
            Seq(Certificate(NiphlCode, "cert1code", "cert1desc")),
            "theme description",
            Some("regulationUrl1")
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(Certificate(NiphlCode, "cert2code", "cert2desc")),
            "theme description",
            Some("regulationUrl2")
          )

          val assessment3        = CategoryAssessment("ass3", 2, Seq.empty, "theme description", Some("regulationUrl3"))
          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            None,
            Seq(assessment1, assessment2, assessment3),
            Seq.empty,
            None,
            1,
            isTraderNiphlAuthorised = true
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category2NoExemptionsScenario
        }

        "is authorised and has NIPHL assessments when all category 1 are answered yes" in {
          val assessment1 = CategoryAssessment(
            "ass1",
            1,
            Seq(Certificate("cert1", "cert1code", "cert1desc")),
            "theme description",
            Some("regulationUrl1")
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(Certificate(NiphlCode, "cert2code", "cert2desc")),
            "theme description",
            Some("regulationUrl2")
          )

          val assessment3        = CategoryAssessment("ass3", 2, Seq.empty, "theme description", Some("regulationUrl3"))
          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            None,
            Seq(assessment1, assessment2, assessment3),
            Seq(assessment1),
            None,
            1,
            isTraderNiphlAuthorised = true
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category2NoExemptionsScenario
        }

        "is authorised and has NIPHL assessments when category 1 is answered no" in {
          val assessment1 = CategoryAssessment(
            "ass1",
            1,
            Seq(Certificate("cert1", "cert1code", "cert1desc")),
            "theme description",
            Some("regulationUrl1")
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(Certificate(NiphlCode, "cert2code", "cert2desc")),
            "theme description",
            Some("regulationUrl2")
          )

          val assessment3 = CategoryAssessment(
            "ass1",
            1,
            Seq(Certificate("cert3", "cert3code", "cert3desc")),
            "theme description",
            Some("regulationUrl3")
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            None,
            Seq(assessment1, assessment2, assessment3),
            Seq(assessment1, assessment2, assessment3),
            None,
            1,
            isTraderNiphlAuthorised = true
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("Y903")))
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
        "is NOT authorised and has only NIPHL assessments" in {
          val assessment1 = CategoryAssessment(
            "ass1",
            1,
            Seq(Certificate(NiphlCode, NiphlCode, "cert1desc")),
            "theme description",
            Some("regulationUrl1")
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(Certificate(NiphlCode, NiphlCode, "cert2desc")),
            "theme description",
            Some("regulationUrl2")
          )

          val assessment3        = CategoryAssessment("ass3", 2, Seq.empty, "theme description", Some("regulationUrl3"))
          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            None,
            Seq(assessment1, assessment2, assessment3),
            Seq.empty,
            None,
            1,
            isTraderNiphlAuthorised = false
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
        "is authorised but has mixed NIPHL and non-NIPHL assessments with one having no exemptions" in {
          val assessment1 = CategoryAssessment(
            "ass1",
            1,
            Seq(Certificate(NiphlCode, "cert1code", "cert1desc")),
            "theme description",
            Some("regulationUrl1")
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(Certificate("certX", "certXcode", "certXdesc")),
            "theme description",
            Some("regulationUrl2")
          )

          val assessment3        = CategoryAssessment("ass3", 2, Seq.empty, "theme description", Some("regulationUrl3"))
          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            None,
            Seq(assessment1, assessment2, assessment3),
            Seq(assessment2, assessment3),
            None,
            1,
            isTraderNiphlAuthorised = true
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category2NoExemptionsScenario
        }

        "is authorised but has mixed NIPHL and non-NIPHL assessments with one exemptions" in {
          val assessment1 = CategoryAssessment(
            "ass1",
            1,
            Seq(Certificate(NiphlCode, "cert1code", "cert1desc")),
            "theme description",
            Some("regulationUrl1")
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(Certificate("certX", "certXcode", "certXdesc")),
            "theme description",
            Some("regulationUrl2")
          )

          val assessment3 = CategoryAssessment(
            "ass3",
            2,
            Seq(Certificate("certX", "certXcode", "certXdesc")),
            "theme description",
            Some("regulationUrl3")
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            None,
            Seq(assessment1, assessment2, assessment3),
            Seq(assessment2, assessment3),
            None,
            1,
            isTraderNiphlAuthorised = true
          )

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
      }

      ".NIRMS" - {
        "is not authorised and has a NIRMS assessment, category 1 assessment and category 2 assessment when all are answered yes" in {
          val nirmsCert = Certificate(NirmsCode, NirmsCode, "NIRMS exemption description")

          val category1Assessment = CategoryAssessment(
            "ass1",
            1,
            Seq(Certificate("cert1code", "cert1code", "Category 1 exemption")),
            "theme description",
            Some("regulationUrl1")
          )

          val category2Assessment = CategoryAssessment(
            "ass2",
            2,
            Seq(Certificate("cert2code", "cert2code", "Category 2 exemption")),
            "theme description",
            Some("regulationUrl2")
          )

          val nirmsAssessment = CategoryAssessment(
            "nirmsAss",
            2,
            Seq(nirmsCert),
            "NIRMS theme",
            Some("regulationUrlNirms")
          )

          val categorisationInfo = CategorisationInfo(
            commodityCode = "1234567890",
            countryOfOrigin = "BV",
            comcodeEffectiveToDate = Some(validityEndDate),
            categoryAssessments = Seq(nirmsAssessment, category1Assessment, category2Assessment),
            categoryAssessmentsThatNeedAnswers = Seq(nirmsAssessment, category1Assessment, category2Assessment),
            measurementUnit = None,
            descendantCount = 1,
            isTraderNiphlAuthorised = false,
            isTraderNirmsAuthorised = false
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq(NirmsCode)))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("cert1code")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("cert2code")))
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category2NoExemptionsScenario
        }

        "is authorised and has a NIRMS assessment, no category 1 assessment and category 2 with no exemptions" in {
          val category2Exemption = CategoryAssessment(
            "1azbfb-1-dfsdaf-32",
            2,
            Seq.empty,
            "measure description",
            Some(
              "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D42I,YEAR_OJ%3D2022,PAGE_FIRST%3D0077&DB_COLL_OJ=oj-l&type=advanced&lang=en"
            )
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            Some(validityEndDate),
            Seq(category2Nirms, category2Exemption),
            Seq.empty,
            None,
            1,
            isTraderNirmsAuthorised = true
          )

          val userAnswers =
            emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category2NoExemptionsScenario
        }

        "is authorised and has a NIRMS assessment, no category 1 assessment and category 2 with exemptions" in {
          val category2Exemption = CategoryAssessment(
            "1azbfb-1-dfsdaf-32",
            2,
            Seq(Certificate("Y123", "Y990", "Nirms description")),
            "measure description",
            Some(
              "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D42I,YEAR_OJ%3D2022,PAGE_FIRST%3D0077&DB_COLL_OJ=oj-l&type=advanced&lang=en"
            )
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            Some(validityEndDate),
            Seq(category2Nirms, category2Exemption),
            Seq(category2Exemption),
            None,
            1,
            isTraderNirmsAuthorised = true
          )

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
          ) mustEqual Category2Scenario
        }

        "is not authorised and has a NIRMS assessment, no category 1 assessment and category 2 with no exemptions" in {
          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            Some(validityEndDate),
            Seq(category2Nirms, category2NoExemptions),
            Seq.empty,
            None,
            1
          )

          val userAnswers =
            emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category2NoExemptionsScenario
        }

        "is not authorised and has a NIRMS assessment, no category 1 assessment and category 2 with possible exemptions" in {
          val category2Exemption = CategoryAssessment(
            "1azbfb-1-dfsdaf-32",
            2,
            Seq(
              OtherExemption(NirmsCode, "Y990", "Nirms description"),
              Certificate("Y123", "Y990", "Nirms description")
            ),
            "measure description",
            Some(
              "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D42I,YEAR_OJ%3D2022,PAGE_FIRST%3D0077&DB_COLL_OJ=oj-l&type=advanced&lang=en"
            )
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            Some(validityEndDate),
            Seq(category2Exemption),
            Seq(category2Exemption),
            None,
            1
          )

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
          ) mustEqual Category2Scenario
        }

        "is authorised and has a NIRMS assessment, category 1 assessment and category 2 assessments when category 2 assessment answered no" in {
          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(Certificate("cert2", "cert2code", "cert2desc")),
            "theme description",
            Some("regulationUrl2")
          )

          val assessment3 = CategoryAssessment(
            "ass3",
            2,
            Seq(Certificate("cert3", "cert3code", "cert3desc")),
            "theme description",
            Some("regulationUrl3")
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            Some(validityEndDate),
            Seq(category2Nirms, assessment2, assessment3),
            Seq(assessment2, assessment3),
            None,
            1,
            isTraderNirmsAuthorised = true
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category2Scenario
        }

        "is not authorised and has a NIRMS assessment, category 1 assessment and category 2 assessments when category 2 assessment answered no" in {
          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(Certificate("cert2", "cert2code", "cert2desc")),
            "theme description",
            Some("regulationUrl2")
          )

          val assessment3 = CategoryAssessment(
            "ass3",
            2,
            Seq(Certificate("cert3", "cert3code", "cert3desc")),
            "theme description",
            Some("regulationUrl3")
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            Some(validityEndDate),
            Seq(category2Nirms, assessment2, assessment3),
            Seq(assessment2, assessment3),
            None,
            1
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual Category2Scenario
        }

        ".NIRMS" - {
          "is not authorised and has a NIRMS assessment, category 1 assessment and category 2 assessment when all are answered yes" in {
            val nirmsCert = Certificate(NirmsCode, NirmsCode, "NIRMS exemption description")

            val assessment1 = CategoryAssessment(
              "ass1",
              1,
              Seq(Certificate("cert1code", "cert1code", "Category 1 exemption")),
              "theme description",
              Some("regulationUrl1")
            )

            val assessment2 = CategoryAssessment(
              "ass2",
              2,
              Seq(nirmsCert),
              "theme description",
              Some("regulationUrl2")
            )

            val categorisationInfo = CategorisationInfo(
              commodityCode = "1234567890",
              countryOfOrigin = "BV",
              comcodeEffectiveToDate = Some(validityEndDate),
              categoryAssessments = Seq(assessment1, assessment2),
              categoryAssessmentsThatNeedAnswers = Seq(assessment1, assessment2),
              measurementUnit = None,
              descendantCount = 1,
              isTraderNiphlAuthorised = false,
              isTraderNirmsAuthorised = false
            )

            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value
              .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
              .success
              .value
              .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("Y903")))
              .success
              .value

            categorisationService.calculateResult(
              categorisationInfo,
              userAnswers,
              testRecordId
            ) mustEqual Category2NoExemptionsScenario
          }
        }

      }
    }

    "return Standard" - {
      "if all answers are Yes" in {
        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value

        categorisationService.calculateResult(
          categorisationInfo,
          userAnswers,
          testRecordId
        ) mustEqual StandardGoodsScenario
      }

      ".NIRMS" - {
        "is authorised and has a NIRMS assessment, category 1 assessment and category 2 assessment when all are answered yes" in {
          val nirmsAssessment = CategoryAssessment(
            "nirms1",
            2,
            Seq(Certificate(NirmsCode, "cert1code", "cert1desc")),
            "theme description",
            Some("regulationUrl1")
          )

          val assessment2 = CategoryAssessment(
            "ass2",
            1,
            Seq(Certificate("cert2", "cert2code", "cert2desc")),
            "theme description",
            Some("regulationUrl2")
          )

          val assessment3 = CategoryAssessment(
            "ass3",
            2,
            Seq(Certificate("cert3", "cert3code", "cert3desc")),
            "theme description",
            Some("regulationUrl3")
          )

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            None,
            Seq(nirmsAssessment, assessment2, assessment3),
            Seq(assessment2, assessment3),
            None,
            1,
            isTraderNirmsAuthorised = true
          )
          val userAnswers        = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("Y903")))
            .success
            .value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual StandardGoodsScenario
        }

        "is authorised and has only a NIRMS assessment" in {
          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            Some(validityEndDate),
            Seq(category2Nirms),
            Seq.empty,
            None,
            1,
            isTraderNirmsAuthorised = true
          )
          val userAnswers        =
            emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

          categorisationService.calculateResult(
            categorisationInfo,
            userAnswers,
            testRecordId
          ) mustEqual StandardGoodsScenario
        }
      }
    }

    "return StandardNoAssessments" - {
      "if no assessments" in {
        val categoryInfoNoAssessments =
          CategorisationInfo("1234567890", "BV", Some(validityEndDate), Seq.empty, Seq.empty, None, 1)
        val userAnswers               =
          emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments).success.value

        categorisationService.calculateResult(
          categoryInfoNoAssessments,
          userAnswers,
          testRecordId
        ) mustBe StandardGoodsNoAssessmentsScenario
      }
    }
  }

  "updatingAnswersForRecategorisation2" - {
    "should return the user answers with the reassessment answers set if old and new category assessments are the same" in {
      val expectedUserAnswers = userAnswersForCategorisation
        .set(
          ReassessmentPage(testRecordId, 0),
          ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")), isAnswerCopiedFromPreviousAssessment = true)
        )
        .success
        .value
        .set(
          ReassessmentPage(testRecordId, 1),
          ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")), isAnswerCopiedFromPreviousAssessment = true)
        )
        .success
        .value
        .set(
          ReassessmentPage(testRecordId, 2),
          ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")), isAnswerCopiedFromPreviousAssessment = true)
        )
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
      val newCat                     = CategoryAssessment(
        "0",
        1,
        Seq(Certificate("Y199", "Y199", "Goods are not from warzone")),
        "theme description",
        Some("regulationUrl")
      )
      val newCommodityCategorisation = CategorisationInfo(
        "12345",
        "BV",
        Some(validityEndDate),
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

      result.get(ReassessmentPage(testRecordId, 0)) shouldBe Some(ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
      result.get(ReassessmentPage(testRecordId, 1)) shouldBe None
      result.get(ReassessmentPage(testRecordId, 2)) shouldBe None
    }

    "should copy across all the old answers when new category info has a new assessment" in {
      val assList                    = Seq(
        category1,
        category2,
        category3,
        CategoryAssessment(
          "0",
          1,
          Seq(Certificate("Y199", "Y199", "Goods are not from warzone")),
          "theme description",
          Some("regulationUrl4")
        )
      )
      val newCommodityCategorisation = CategorisationInfo(
        "1234567890",
        "BV",
        Some(validityEndDate),
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
      result.get(ReassessmentPage(testRecordId, 0)) shouldBe
        Some(
          ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")), isAnswerCopiedFromPreviousAssessment = true)
        )
      result.get(ReassessmentPage(testRecordId, 1)) shouldBe
        Some(
          ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")), isAnswerCopiedFromPreviousAssessment = true)
        )
      result.get(ReassessmentPage(testRecordId, 2)) shouldBe
        Some(
          ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")), isAnswerCopiedFromPreviousAssessment = true)
        )
    }

    "should copy the old answers to the right position if they are in different order in the new categorisation" in {
      val category4                  =
        CategoryAssessment(
          "0",
          1,
          Seq(Certificate("Y199", "Y199", "Goods are not from warzone")),
          "theme description",
          Some("regulationUrl4")
        )
      val newCommodityCategorisation = CategorisationInfo(
        "1234567890",
        "BV",
        Some(validityEndDate),
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
        .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
        .success
        .value
        .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("Y903")))
        .success
        .value
        .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("Y903")))
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
      newUserAnswers.get(ReassessmentPage(testRecordId, 0)) mustBe
        Some(ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("Y903")), isAnswerCopiedFromPreviousAssessment = true))
      newUserAnswers.get(ReassessmentPage(testRecordId, 1)) mustBe
        Some(ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("Y903")), isAnswerCopiedFromPreviousAssessment = true))
      newUserAnswers.get(ReassessmentPage(testRecordId, 2)) mustBe
        Some(ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
      newUserAnswers.get(ReassessmentPage(testRecordId, 3)) mustBe
        Some(ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("Y903")), isAnswerCopiedFromPreviousAssessment = true))
    }

    "should move the old answers to the right position if only some are in the new categorisation" in {
      val oldUserAnswers = emptyUserAnswers
        .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
        .success
        .value
        .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
        .success
        .value
        .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("Y903")))
        .success
        .value
        .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("Y903")))
        .success
        .value

      val category4                  = CategoryAssessment(
        "0",
        1,
        Seq(Certificate("Y199", "Y199", "Goods are not from warzone")),
        "theme description",
        Some("regulationUrl4")
      )
      val newCommodityCategorisation = CategorisationInfo(
        "1234567890",
        "BV",
        Some(validityEndDate),
        Seq(category1, category4),
        Seq(category1, category4),
        Some("Weight, in kilograms"),
        0
      )
      val newUserAnswers             = categorisationService
        .updatingAnswersForRecategorisation(
          oldUserAnswers,
          testRecordId,
          categorisationInfo,
          newCommodityCategorisation
        )
        .success
        .value
      newUserAnswers.get(ReassessmentPage(testRecordId, 0)) mustBe
        Some(ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("Y903")), isAnswerCopiedFromPreviousAssessment = true))
      newUserAnswers.get(ReassessmentPage(testRecordId, 1)) mustBe
        Some(ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
      newUserAnswers.get(ReassessmentPage(testRecordId, 2)) mustBe None
    }

  }

  "reorderRecategorisationAnswers" - {
    "should reorder assessments so that answered ones come first" in {
      val categorisationInfo = CategorisationInfo(
        "1234567890",
        "BV",
        Some(validityEndDate),
        Seq(category1, category3, category2),
        Seq(category1, category3, category2),
        Some("Weight, in kilograms"),
        1
      )

      val userAnswers = emptyUserAnswers
        .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
        .success
        .value
        .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq.empty)))
        .success
        .value
        .set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
        .success
        .value
        .set(ReassessmentPage(testRecordId, 2), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq.empty)))
        .success
        .value

      val expectedCategoryInfo = CategorisationInfo(
        "1234567890",
        "BV",
        Some(validityEndDate),
        Seq(category1, category3, category2),
        Seq(category1, category2, category3),
        Some("Weight, in kilograms"),
        1
      )

      val updatedDate     = Instant.now()
      val expectedAnswers = emptyUserAnswers
        .set(LongerCategorisationDetailsQuery(testRecordId), expectedCategoryInfo)
        .success
        .value
        .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq.empty)))
        .success
        .value
        .set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq.empty)))
        .success
        .value
        .set(ReassessmentPage(testRecordId, 2), ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
        .success
        .value
        .copy(lastUpdated = updatedDate)

      categorisationService
        .reorderRecategorisationAnswers(userAnswers, testRecordId)
        .futureValue
        .copy(lastUpdated = updatedDate) mustEqual expectedAnswers
    }
  }

  "existsUnansweredCat1Questions" - {
    "should return true if there are unanswered Category 1 questions" in {
      val userAnswers = emptyUserAnswers
        .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
        .success
        .value
        .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
        .success
        .value
        .set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq.empty)))
        .success
        .value

      val result = categorisationService.existsUnansweredCat1Questions(userAnswers, testRecordId)
      result mustEqual true
    }

    "should return false if all Category 1 questions are answered" in {
      val userAnswers = emptyUserAnswers
        .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
        .success
        .value
        .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq.empty)))
        .success
        .value
        .set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq.empty)))
        .success
        .value

      val result = categorisationService.existsUnansweredCat1Questions(userAnswers, testRecordId)

      result mustEqual false
    }
  }
}
