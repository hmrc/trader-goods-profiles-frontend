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

import base.TestConstants.{NiphlsCode, testRecordId, userAnswersId}
import controllers.actions._
import models.ott._
import models.ott.response.{GoodsNomenclatureResponse, OttResponse}
import models.router.responses.GetGoodsRecordResponse
import models.{AssessmentAnswer, Commodity, UserAnswers}
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
import queries.{CategorisationDetailsQuery, CommodityQuery, MeasurementQuery}

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

  def testShorterCommodityQuery: Commodity = Commodity("1742900000", List("test"), validityStartDate, None)

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
      .set(SupplementaryUnitUpdatePage(testRecordId), "1234567890.123456")
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

  lazy val category1Niphl: CategoryAssessment =
    CategoryAssessment("1azbfb-1-dfsdaf-3", 1, Seq(Certificate(NiphlsCode, "Y994", "Goods are not from warzone")))

  lazy val categorisationInfo: CategorisationInfo = CategorisationInfo(
    "1234567890",
    Seq(category1, category2, category3),
    Seq(category1, category2, category3),
    Some("Weight, in kilograms"),
    1
  )

  lazy val categorisationInfoWithEmptyMeasurementUnit: CategorisationInfo = CategorisationInfo(
    "1234567890",
    Seq(category1, category2, category3),
    Seq(category1, category2, category3),
    None,
    1
  )

  lazy val userAnswersForCategorisation: UserAnswers = emptyUserAnswers
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

  lazy val category1NoExemptions: CategoryAssessment =
    CategoryAssessment("1azbfb-1-dfsdaf-2", 1, Seq())

  lazy val category2NoExemptions: CategoryAssessment =
    CategoryAssessment("1azbfb-1-dfsdaf-2", 2, Seq())

  def goodsRecordResponse(
    createdDateTime: Instant = Instant.now,
    updatedDateTime: Instant = Instant.now
  ): GetGoodsRecordResponse =
    GetGoodsRecordResponse(
      "1",
      "10410100",
      "10410100",
      "BAN0010011",
      "12345678",
      "Not requested",
      "Organic bananas",
      "GB",
      Some(1),
      None,
      None,
      None,
      Instant.now(),
      None,
      1,
      active = true,
      toReview = false,
      None,
      "Not ready",
      None,
      None,
      None,
      createdDateTime,
      updatedDateTime
    )

  val recordForTestingSummaryRows: GetGoodsRecordResponse = goodsRecordResponse(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId)

  def toReviewGoodsRecordResponse(
    createdDateTime: Instant,
    updatedDateTime: Instant,
    reviewReason: String
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
      Some(1),
      None,
      None,
      None,
      Instant.now(),
      None,
      1,
      active = true,
      toReview = true,
      reviewReason = Some(reviewReason),
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
      Some(1),
      None,
      Some(1234567890.123456),
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
      Some(1),
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
        bind[ProfileAuthenticateAction].to[FakeProfileAuthenticateAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
}
