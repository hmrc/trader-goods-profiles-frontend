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

package models.ott

import models.ott.response._
import models.{AnsweredQuestions, TraderProfile, UserAnswers}
import pages.categorisation.{AssessmentPage, ReassessmentPage}
import play.api.libs.json.{Json, OFormat}
import utils.Constants.{NiphlCode, minimumLengthOfCommodityCode}
import models.ott.response.{ExemptionType => ResponseExemptionType}

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId, ZonedDateTime}

final case class CategorisationInfo(
  commodityCode: String,
  countryOfOrigin: String,
  comcodeEffectiveToDate: Option[Instant],
  categoryAssessments: Seq[CategoryAssessment],
  categoryAssessmentsThatNeedAnswers: Seq[CategoryAssessment],
  measurementUnit: Option[String],
  descendantCount: Int,
  longerCode: Boolean = false,
  isTraderNiphlAuthorised: Boolean = false,
  isTraderNirmsAuthorised: Boolean = false
) {

  def isAutoCategorisable: Boolean = categoryAssessments.exists(_.hasNoExemptions)

  def getAssessmentFromIndex(index: Int): Option[CategoryAssessment] =
    if (index + 1 > categoryAssessmentsThatNeedAnswers.size) None
    else Some(categoryAssessmentsThatNeedAnswers(index))

  def getAnswersForQuestions(
    userAnswers: UserAnswers,
    recordId: String
  ): Seq[AnsweredQuestions] =
    if (longerCode) getAnswersForReassessmentQuestions(userAnswers, recordId)
    else {
      categoryAssessmentsThatNeedAnswers.zipWithIndex.map { case (assessment, index) =>
        AnsweredQuestions(
          index,
          assessment,
          userAnswers.get(AssessmentPage(recordId, index))
        )
      }
    }

  private def getAnswersForReassessmentQuestions(userAnswers: UserAnswers, recordId: String): Seq[AnsweredQuestions] =
    categoryAssessmentsThatNeedAnswers.zipWithIndex.map { case (assessment, index) =>
      val answerOpt = userAnswers.get(ReassessmentPage(recordId, index))

      AnsweredQuestions(
        index,
        assessment,
        answerOpt.map(_.answer),
        reassessmentQuestion = true,
        wasCopiedFromInitialAssessment = answerOpt.exists(_.isAnswerCopiedFromPreviousAssessment)
      )
    }

  def getMinimalCommodityCode: String =
    commodityCode.reverse.dropWhile(_ == '0').reverse.padTo(minimumLengthOfCommodityCode, '0').mkString

  def isNiphlAssessment: Boolean =
    categoryAssessments.exists(_.isNiphlAnswer) &&
      categoryAssessments.exists(ass => ass.isCategory2 && ass.hasNoExemptions)

  def isNirmsAssessment: Boolean =
    categoryAssessments.exists(_.isNirmsAnswer)

  def isCommCodeExpired: Boolean = {
    val today: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS)
    comcodeEffectiveToDate.exists { effectiveToDate =>
      val effectiveDate: ZonedDateTime = effectiveToDate.atZone(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS)
      effectiveDate.isEqual(today)
    }
  }
}

object CategorisationInfo {

  def build(
    response: OttResponse,
    traderScheme: String,
    commodityCode: String,
    traderProfile: TraderProfile
  ): Either[Error, CategorisationInfo] = {

    val isTraderNiphlAuthorised                 = traderProfile.niphlNumber.isDefined
    val allAssessments: Seq[CategoryAssessment] = parseCategoryAssessments(response, isTraderNiphlAuthorised)

    // Use traderProfile's authorization flags directly
    val category1NeedingAnswers: Seq[CategoryAssessment] =
      allAssessments.category1ToAnswer(isTraderNiphlAuthorised = traderProfile.niphlNumber.isDefined)

    val category2NeedingAnswersRaw: Seq[CategoryAssessment] =
      allAssessments.category2ToAnswer(isTraderNirmsAuthorised = traderProfile.nirmsNumber.isDefined)

    val category2NeedingAnswers: Seq[CategoryAssessment] =
      if (category1NeedingAnswers.nonEmpty)
        category2NeedingAnswersRaw.filter(_.exemptions.isEmpty)
      else
        category2NeedingAnswersRaw

    val categoryAssessmentsThatNeedAnswers: Seq[CategoryAssessment] =
      if (category1NeedingAnswers.nonEmpty)
        (category1NeedingAnswers ++ category2NeedingAnswers).distinctBy(_.id)
      else
        category2NeedingAnswers.distinctBy(_.id)

    Right(
      CategorisationInfo(
        commodityCode = commodityCode,
        countryOfOrigin = traderScheme,
        comcodeEffectiveToDate = response.goodsNomenclature.validityEndDate,
        categoryAssessments = allAssessments,
        categoryAssessmentsThatNeedAnswers = categoryAssessmentsThatNeedAnswers,
        measurementUnit = response.goodsNomenclature.measurementUnit, // <-- use actual value
        descendantCount = response.descendents.size, // <-- use actual count
        isTraderNiphlAuthorised = traderProfile.niphlNumber.isDefined,
        isTraderNirmsAuthorised = traderProfile.nirmsNumber.isDefined
      )
    )

  }

  // Placeholder method, replace with your actual parsing logic for CategoryAssessments
  def parseCategoryAssessments(response: OttResponse, isTraderNiphlAuthorised: Boolean): Seq[CategoryAssessment] = {
    val categoryAssessmentResponses = response.includedElements.collect { case c: CategoryAssessmentResponse =>
      c
    }

    val themesById: Map[String, ThemeResponse] = response.includedElements.collect { case t: ThemeResponse =>
      t.id -> t
    }.toMap

    val certificatesById: Map[String, CertificateResponse] = response.includedElements.collect {
      case c: CertificateResponse => c.id -> c
    }.toMap

    val additionalCodesById: Map[String, AdditionalCodeResponse] = response.includedElements.collect {
      case a: AdditionalCodeResponse => a.id -> a
    }.toMap

    val legalActsByRegulationId: Map[String, LegalActResponse] = response.includedElements.collect {
      case l @ LegalActResponse(Some(regId), _, _) => regId -> l
    }.toMap

    categoryAssessmentResponses
      .map { assessmentResp =>
        val theme = themesById.getOrElse(
          assessmentResp.themeId,
          throw new RuntimeException(s"Missing theme for id ${assessmentResp.themeId}")
        )

        val exemptions = assessmentResp.exemptions
          .flatMap { exResp =>
            exResp.exemptionType match {
              case ResponseExemptionType.Certificate    =>
                certificatesById.get(exResp.id).map(c => Certificate(c.id, c.code, c.description))
              case ResponseExemptionType.AdditionalCode =>
                additionalCodesById.get(exResp.id).map(a => AdditionalCode(a.id, a.code, a.description))
              case _                                    =>
                None
            }
          }
          .filterNot(exemption =>
            !isTraderNiphlAuthorised && exemption.code == NiphlCode
          ) // <-- filter out NIPHL exemptions if trader not authorised

        val legalActUrl = legalActsByRegulationId.get(assessmentResp.regulationId).flatMap(_.regulationUrl)

        CategoryAssessment(
          id = assessmentResp.id,
          category = theme.category,
          exemptions = exemptions,
          themeDescription = theme.theme,
          regulationUrl = legalActUrl
        )
      }
      .sortBy(assessment => (assessment.category, assessment.exemptions.length))
  }

  implicit lazy val format: OFormat[CategorisationInfo] = Json.format
}
