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

package base

import base.TestConstants.{testRecordId, userAnswersId}
import controllers.actions._
import models.ott.response.{GoodsNomenclatureResponse, OttResponse}
import models.ott.{AdditionalCode, CategorisationInfo, CategoryAssessment, Certificate}
import models.router.responses.GetGoodsRecordResponse
import models.{AssessmentAnswer, Commodity, RecordCategorisations, UserAnswers}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages._
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import queries.{CommodityQuery, MeasurementQuery, RecordCategorisationsQuery}

import java.time.Instant

trait SpecBase
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience {

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  def fullProfileUserAnswers: UserAnswers = UserAnswers(userAnswersId)
    .set(UkimsNumberPage, "1")
    .success
    .value
    .set(HasNirmsPage, true)
    .success
    .value
    .set(NirmsNumberPage, "2")
    .success
    .value
    .set(HasNiphlPage, true)
    .success
    .value
    .set(NiphlNumberPage, "3")
    .success
    .value

  def mandatoryProfileUserAnswers: UserAnswers = UserAnswers(userAnswersId)
    .set(UkimsNumberPage, "1")
    .success
    .value
    .set(HasNirmsPage, false)
    .success
    .value
    .set(HasNiphlPage, false)
    .success
    .value

  def validityStartDate: Instant = Instant.parse("2007-12-03T10:15:30.00Z")

  def testCommodity: Commodity = Commodity("1234567890", List("test"), validityStartDate, None)

  def testAuditOttResponse: OttResponse = OttResponse(
    GoodsNomenclatureResponse("test", "1234567890", None, Instant.EPOCH, None, List("test")),
    Seq(),
    Seq(),
    Seq()
  )

  def fullRecordUserAnswers: UserAnswers =
    UserAnswers(userAnswersId)
      .set(TraderReferencePage, "123")
      .success
      .value
      .set(CommodityCodePage, testCommodity.commodityCode)
      .success
      .value
      .set(CountryOfOriginPage, "1")
      .success
      .value
      .set(UseTraderReferencePage, false)
      .success
      .value
      .set(HasCorrectGoodsPage, true)
      .success
      .value
      .set(GoodsDescriptionPage, "DESCRIPTION")
      .success
      .value
      .set(CommodityQuery, testCommodity)
      .success
      .value

  def mandatoryRecordUserAnswers: UserAnswers =
    UserAnswers(userAnswersId)
      .set(TraderReferencePage, "123")
      .success
      .value
      .set(CommodityCodePage, testCommodity.commodityCode)
      .success
      .value
      .set(CountryOfOriginPage, "1")
      .success
      .value
      .set(UseTraderReferencePage, true)
      .success
      .value
      .set(HasCorrectGoodsPage, true)
      .success
      .value
      .set(CommodityQuery, testCommodity)
      .success
      .value

  def mandatoryAdviceUserAnswers: UserAnswers =
    UserAnswers(userAnswersId)
      .set(NamePage(testRecordId), "Firstname Lastname")
      .success
      .value
      .set(EmailPage(testRecordId), "test@test.com")
      .success
      .value

  def mandatorySupplementaryUserAnswers: UserAnswers =
    UserAnswers(userAnswersId)
      .set(HasSupplementaryUnitUpdatePage(testRecordId), true)
      .success
      .value
      .set(SupplementaryUnitUpdatePage(testRecordId), "100")
      .success
      .value
      .set(MeasurementQuery(testRecordId), "litres")
      .success
      .value

  lazy val category1: CategoryAssessment =
    CategoryAssessment("1azbfb-1-dfsdaf-2", 1, Seq(Certificate("Y994", "Y994", "Goods are not from warzone")))

  lazy val category2: CategoryAssessment =
    CategoryAssessment("2nghjghg4-fsdff4-hfgdhfg", 1, Seq(AdditionalCode("NC123", "NC123", "Not required")))

  lazy val category3: CategoryAssessment = CategoryAssessment(
    "3fsdfsdf-r234fds-bfgbdfg",
    2,
    Seq(
      Certificate("Y737", "Y737", "Goods not containing ivory"),
      Certificate("X812", "X812", "Goods not containing seal products")
    )
  )

  lazy val categoryQuery: CategorisationInfo = CategorisationInfo(
    "1234567890",
    Seq(category1, category2, category3),
    Some("Weight, in kilograms"),
    0,
    Some("1234567890")
  )

  private lazy val categoryQueryWithEmptyMeasurementUnit: CategorisationInfo = CategorisationInfo(
    "1234567890",
    Seq(category1, category2, category3),
    None,
    0
  )

  lazy val recordCategorisations: RecordCategorisations = RecordCategorisations(
    Map(testRecordId -> categoryQuery)
  )

  lazy val recordCategorisationsEmptyMeasurementUnit: RecordCategorisations = RecordCategorisations(
    Map(testRecordId -> categoryQueryWithEmptyMeasurementUnit)
  )

  lazy val userAnswersForCategorisation: UserAnswers = emptyUserAnswers
    .set(RecordCategorisationsQuery, recordCategorisations)
    .success
    .value
    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("Y994"))
    .success
    .value
    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption("NC123"))
    .success
    .value
    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption("X812"))
    .success
    .value

  private lazy val categoryQueryNoAssessments: CategorisationInfo = CategorisationInfo(
    "1234567890",
    Seq(),
    Some("Weight, in kilograms"),
    0
  )

  lazy val recordCategorisationsNoAssessments: RecordCategorisations = RecordCategorisations(
    Map(testRecordId -> categoryQueryNoAssessments)
  )

  lazy val uaForCategorisationStandardNoAssessments: UserAnswers = emptyUserAnswers
    .set(RecordCategorisationsQuery, recordCategorisationsNoAssessments)
    .success
    .value

  lazy val category1NoExemptions: CategoryAssessment =
    CategoryAssessment("1azbfb-1-dfsdaf-2", 1, Seq())

  private lazy val categoryQueryNoExemptions: CategorisationInfo = CategorisationInfo(
    "1234567890",
    Seq(category1NoExemptions),
    Some("Weight, in kilograms"),
    0
  )

  lazy val recordCategorisationsNoExemptions: RecordCategorisations = RecordCategorisations(
    Map(testRecordId -> categoryQueryNoExemptions)
  )

  lazy val uaForCategorisationCategory1NoExemptions: UserAnswers = emptyUserAnswers
    .set(RecordCategorisationsQuery, recordCategorisationsNoExemptions)
    .success
    .value

  def goodsRecordResponse(createdDateTime: Instant, updatedDateTime: Instant): GetGoodsRecordResponse =
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "10410100",
      "BAN0010011",
      "1234567",
      "Not requested",
      "Organic bananas",
      "UK",
      1,
      None,
      None,
      None,
      Instant.now(),
      None,
      1,
      active = true,
      toReview = true,
      None,
      "Not ready",
      None,
      None,
      None,
      createdDateTime,
      updatedDateTime
    )

  def goodsRecordResponseWithSupplementaryUnit(
    createdDateTime: Instant,
    updatedDateTime: Instant
  ): GetGoodsRecordResponse =
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "10410100",
      "BAN0010011",
      "1234567",
      "Not requested",
      "Organic bananas",
      "UK",
      1,
      None,
      Some(1234.0),
      Some("grams"),
      Instant.now(),
      None,
      1,
      active = true,
      toReview = true,
      None,
      "Not ready",
      None,
      None,
      None,
      createdDateTime,
      updatedDateTime
    )

  def goodsRecordResponseWithOutSupplementaryUnit(
    createdDateTime: Instant,
    updatedDateTime: Instant
  ): GetGoodsRecordResponse =
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "10410100",
      "BAN0010011",
      "1234567",
      "Not requested",
      "Organic bananas",
      "UK",
      1,
      None,
      Some(0.0),
      Some("grams"),
      Instant.now(),
      None,
      1,
      active = true,
      toReview = true,
      None,
      "Not ready",
      None,
      None,
      None,
      createdDateTime,
      updatedDateTime
    )

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalOrCreateAction].to[FakeDataRetrievalOrCreateAction],
        bind[ProfileCheckAction].to[ProfileCheckActionImpl],
        bind[ProfileAuthenticateAction].to[ProfileAuthenticateActionImpl],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
}
