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

import base.TestConstants._
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
import pages.advice.{EmailPage, NamePage}
import pages.categorisation.{AssessmentPage, HasSupplementaryUnitUpdatePage}
import pages.goodsRecord.{CommodityCodePage, CountryOfOriginPage, GoodsDescriptionPage, TraderReferencePage}
import pages.profile._
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import queries.{CategorisationDetailsQuery, CommodityQuery, MeasurementQuery}

import java.time.{Instant, LocalDate, ZoneId}

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

  def validityEndDate: Instant = Instant.parse("2008-12-03T10:15:30.00Z")

  def testCommodity: Commodity = Commodity("1234567890", List("test"), validityStartDate, None)

  def testShorterCommodityQuery: Commodity = Commodity("1742900000", List("test"), validityStartDate, None)

  val lockedRecord: GetGoodsRecordResponse = goodsRecordResponse(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(adviceStatus = requested)

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
      .set(GoodsDescriptionPage, "DESCRIPTION")
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
    CategoryAssessment(
      "1azbfb-1-dfsdaf-2",
      1,
      Seq(Certificate("Y994", "Y994", "Goods are not from warzone")),
      "measure description",
      Some(
        "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D88,YEAR_OJ%3D2012,PAGE_FIRST%3D0001&DB_COLL_OJ=oj-l&type=advanced&lang=en"
      )
    )

  lazy val category2: CategoryAssessment =
    CategoryAssessment(
      "2nghjghg4-fsdff4-hfgdhfg",
      1,
      Seq(AdditionalCode("NC123", "NC123", "Not required")),
      "measure description",
      Some(
        "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D224,YEAR_OJ%3D2017,PAGE_FIRST%3D0001&DB_COLL_OJ=oj-l&type=advanced&lang=en"
      )
    )

  lazy val category3: CategoryAssessment = CategoryAssessment(
    "3fsdfsdf-r234fds-bfgbdfg",
    2,
    Seq(
      Certificate("Y737", "Y737", "Goods not containing ivory"),
      Certificate("X812", "X812", "Goods not containing seal products")
    ),
    "measure description",
    Some(
      "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D229,YEAR_OJ%3D2014,PAGE_FIRST%3D0013&DB_COLL_OJ=oj-l&type=advanced&lang=en"
    )
  )

  lazy val category1Niphl: CategoryAssessment =
    CategoryAssessment(
      "1azbfb-1-dfsdaf-3",
      1,
      Seq(OtherExemption(NiphlCode, "Y994", "Goods are not from warzone")),
      "measure description",
      Some(
        "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D183,YEAR_OJ%3D2014,PAGE_FIRST%3D0009&DB_COLL_OJ=oj-l&type=advanced&lang=en"
      )
    )

  lazy val category2Nirms: CategoryAssessment =
    CategoryAssessment(
      "1azbfb-1-dfsdaf-3",
      2,
      Seq(OtherExemption(NirmsCode, "Y990", "Nirms description")),
      "measure description",
      Some(
        "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D42I,YEAR_OJ%3D2022,PAGE_FIRST%3D0077&DB_COLL_OJ=oj-l&type=advanced&lang=en"
      )
    )

  lazy val categorisationInfo: CategorisationInfo = CategorisationInfo(
    "1234567890",
    "BV",
    Some(validityEndDate),
    Seq(category1, category2, category3),
    Seq(category1, category2, category3),
    Some("Weight, in kilograms"),
    1
  )

  lazy val categorisationInfoWithThreeCat1: CategorisationInfo = CategorisationInfo(
    "1234567890",
    "BV",
    Some(validityEndDate),
    Seq(category1, category1, category1, category2),
    Seq(category1, category1, category1, category2),
    Some("Weight, in kilograms"),
    1
  )

  val today: Instant                                                      = LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant
  lazy val categorisationInfoWithExpiredCommodityCode: CategorisationInfo = CategorisationInfo(
    "1234567890",
    "BV",
    Some(today),
    Seq(category1, category2, category3),
    Seq(category1, category2, category3),
    Some("Weight, in kilograms"),
    1
  )

  lazy val categorisationInfoWithEmptyCatAssessThatNeedAnswersWithExpiredCommodityCode: CategorisationInfo =
    CategorisationInfo(
      "1234567890",
      "BV",
      Some(today),
      Seq(category1, category2, category3),
      Seq.empty,
      None,
      1
    )

  lazy val categorisationInfoWithEmptyMeasurementUnit: CategorisationInfo = CategorisationInfo(
    "1234567890",
    "BV",
    Some(validityEndDate),
    Seq(category1, category2, category3),
    Seq(category1, category2, category3),
    None,
    1
  )

  lazy val userAnswersForCategorisation: UserAnswers = emptyUserAnswers
    .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
    .success
    .value
    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
    .success
    .value
    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
    .success
    .value
    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
    .success
    .value

  lazy val category1NoExemptions: CategoryAssessment =
    CategoryAssessment(
      "1azbfb-1-dfsdaf-2",
      1,
      Seq(),
      "measure description",
      Some(
        "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D190,YEAR_OJ%3D2006,PAGE_FIRST%3D0001&DB_COLL_OJ=oj-l&type=advanced&lang=en"
      )
    )

  lazy val category2NoExemptions: CategoryAssessment =
    CategoryAssessment(
      "1azbfb-1-dfsdaf-2",
      2,
      Seq(),
      "measure description",
      Some(
        "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D190,YEAR_OJ%3D2006,PAGE_FIRST%3D0001&DB_COLL_OJ=oj-l&type=advanced&lang=en"
      )
    )

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

  val recordForTestingSummaryRowsWithAdviceProvided: GetGoodsRecordResponse = goodsRecordResponse(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId).copy(adviceStatus = "Advice Provided")

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
        bind[EoriCheckAction].to[EoriCheckActionImpl],
        bind[ProfileAuthenticateAction].to[FakeProfileAuthenticateAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
}
