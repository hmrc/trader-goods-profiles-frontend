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
import models.ott.{CategorisationInfo, CategoryAssessment, Certificate}
import models.{Commodity, RecordCategorisations, UserAnswers}
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
import queries.{CommodityQuery, RecordCategorisationsQuery}

import java.time.Instant

trait SpecBase
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience {

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  private val assessment1 = CategoryAssessment("assessmentId1", 1, Seq(Certificate("1", "code", "description")))
  private val assessment2 = CategoryAssessment("assessmentId2", 1, Seq(Certificate("1", "code", "description")))
  private val assessment3 = CategoryAssessment("assessmentId3", 2, Seq(Certificate("1", "code", "description")))
  private val assessment4 = CategoryAssessment("assessmentId4", 2, Seq(Certificate("1", "code", "description")))

  private val categorisationInfo: CategorisationInfo =
    CategorisationInfo("123", Seq(assessment1, assessment2, assessment3, assessment4))

  val recordCategorisations: RecordCategorisations =
    RecordCategorisations(records = Map(testRecordId -> categorisationInfo))

  def fullProfileUserAnswers: UserAnswers          = UserAnswers(userAnswersId)
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

  def mandatoryAssessmentAnswers: UserAnswers =
    UserAnswers(userAnswersId)
      .set(HasSupplementaryUnitPage, false)
      .success
      .value
      .set(RecordCategorisationsQuery, recordCategorisations)
      .success
      .value

  def fullAssessmentAnswers: UserAnswers =
    UserAnswers(userAnswersId)
      .set(HasSupplementaryUnitPage, false)
      .success
      .value
      .set(SupplementaryUnitPage, 1)
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
