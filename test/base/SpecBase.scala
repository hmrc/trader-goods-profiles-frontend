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

import base.TestConstants.userAnswersId
import controllers.actions._
import models.ott.{AdditionalCode, CategorisationInfo, CategoryAssessment, Certificate}
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
import queries.{CategorisationQuery, CommodityQuery}

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

  def testCommodity: Commodity = Commodity("1234567890", "test", validityStartDate, None)

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
      .set(NamePage, "123")
      .success
      .value
      .set(EmailPage, "654321")
      .success
      .value

  lazy val category1: CategoryAssessment =
    CategoryAssessment("1", 1, Seq(Certificate("Y994", "Y994", "Goods are not from warzone")))

  lazy val category2: CategoryAssessment =
    CategoryAssessment("2", 1, Seq(AdditionalCode("NC123", "NC123", "Not required")))

  lazy val category3: CategoryAssessment = CategoryAssessment(
    "3",
    2,
    Seq(
      Certificate("Y737", "Y737", "Goods not containing ivory"),
      Certificate("X812", "X812", "Goods not containing seal products")
    )
  )

  lazy val categoryQuery: CategorisationInfo = CategorisationInfo(
    "1234567890",
    Seq(category1, category2, category3)
  )

  lazy val userAnswersForCategorisationCya: UserAnswers = emptyUserAnswers
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

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalOrCreateAction].to[FakeDataRetrievalOrCreateAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
}
