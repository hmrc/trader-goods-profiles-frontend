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

package navigation

import controllers.routes
import models.Scenario.getResultAsInt
import models._
import models.ott.{CategorisationInfo, CategoryAssessment}
import pages._
import pages.categorisation.{HasSupplementaryUnitPage, ReassessmentPage}
import pages.download.RequestDataPage
import pages.goodsRecord.{CommodityCodePage, CommodityCodeUpdatePage}
import play.api.mvc.Call
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery, LongerCommodityQuery}
import services.CategorisationService
import utils.Constants._

import javax.inject.{Inject, Singleton}

@Singleton
class Navigation @Inject() (categorisationService: CategorisationService) extends Navigator {

  val normalRoutes: Page => UserAnswers => Call = {
    case p: LongerCommodityCodePage                =>
      _ => controllers.commodityCodeResult.routes.LongerCommodityCodeController.onPageLoad(NormalMode, p.recordId)
    case HasCorrectGoodsPage                       => answers => navigateFromHasCorrectGoods(answers)
    case p: HasCorrectGoodsCommodityCodeUpdatePage => answers => navigateFromHasCorrectGoodsUpdate(answers, p.recordId)
    case p: HasCorrectGoodsLongerCommodityCodePage =>
      answers => navigateFromHasCorrectGoodsLongerCommodityCode(p.recordId, answers, NormalMode)
    case p: ReviewReasonPage                       => _ => controllers.goodsRecord.routes.SingleRecordController.onPageLoad(p.recordId)
    case p: RecategorisationPreparationPage        => navigateFromReassessmentPrep(p)
    case RequestDataPage                           => _ => controllers.download.routes.DownloadRequestSuccessController.onPageLoad()
    case _                                         => _ => routes.IndexController.onPageLoad()
  }

  val checkRoutes: Page => UserAnswers => Call = {
    case p: LongerCommodityCodePage                =>
      _ => controllers.commodityCodeResult.routes.LongerCommodityCodeController.onPageLoad(CheckMode, p.recordId)
    case HasCorrectGoodsPage                       => answers => navigateFromHasCorrectGoodsCheck(answers)
    case p: HasCorrectGoodsCommodityCodeUpdatePage =>
      answers => navigateFromHasCorrectGoodsUpdateCheck(answers, p.recordId)
    case p: HasCorrectGoodsLongerCommodityCodePage =>
      answers => navigateFromHasCorrectGoodsLongerCommodityCode(p.recordId, answers, CheckMode)
    case p: RecategorisationPreparationPage        => navigateFromReassessmentPrepCheck(p)
    case _                                         => _ => controllers.problem.routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromHasCorrectGoods(answers: UserAnswers): Call =
    answers
      .get(HasCorrectGoodsPage)
      .map {
        case true  => controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
        case false => controllers.goodsRecord.commodityCode.routes.CreateCommodityCodeController.onPageLoad(NormalMode)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoodsCheck(answers: UserAnswers): Call =
    answers
      .get(HasCorrectGoodsPage)
      .map {
        case true  =>
          if (answers.isDefined(CommodityCodePage)) {
            controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
          } else {
            controllers.goodsRecord.commodityCode.routes.CreateCommodityCodeController.onPageLoad(CheckMode)
          }
        case false => controllers.goodsRecord.commodityCode.routes.CreateCommodityCodeController.onPageLoad(CheckMode)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoodsUpdate(answers: UserAnswers, recordId: String): Call =
    answers
      .get(HasCorrectGoodsCommodityCodeUpdatePage(recordId))
      .map {
        case true  => controllers.goodsRecord.commodityCode.routes.UpdatedCommodityCodeController.onPageLoad(recordId)
        case false =>
          controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController.onPageLoad(NormalMode, recordId)
      }
      .getOrElse(controllers.goodsRecord.commodityCode.routes.UpdatedCommodityCodeController.onPageLoad(recordId))

  private def navigateFromHasCorrectGoodsUpdateCheck(answers: UserAnswers, recordId: String): Call =
    answers
      .get(HasCorrectGoodsCommodityCodeUpdatePage(recordId))
      .map {
        case true  =>
          if (answers.isDefined(CommodityCodeUpdatePage(recordId))) {
            controllers.goodsRecord.commodityCode.routes.CommodityCodeCyaController.onPageLoad(recordId)
          } else {
            controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController.onPageLoad(CheckMode, recordId)
          }
        case false =>
          controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController.onPageLoad(CheckMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoodsLongerCommodityCode(
    recordId: String,
    answers: UserAnswers,
    mode: Mode
  ): Call = {
    for {
      categorisationInfo <- answers.get(CategorisationDetailsQuery(recordId))
      commodity          <- answers.get(LongerCommodityQuery(recordId))
    } yield
      if (
        categorisationInfo.commodityCode == getShortenedCommodityCode(commodity)
        && !commodity.commodityCode.endsWith("0000")
      ) {
        controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(mode, recordId)
      } else {
        answers.get(HasCorrectGoodsLongerCommodityCodePage(recordId)) match {
          case Some(true)  =>
            controllers.categorisation.routes.CategorisationPreparationController
              .startLongerCategorisation(mode, recordId)
          case Some(false) => controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(mode, recordId)
          case None        => controllers.problem.routes.JourneyRecoveryController.onPageLoad()
        }
      }
  }.getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def getShortenedCommodityCode(commodity: Commodity) =
    commodity.commodityCode.reverse
      .dropWhile(char => char == '0')
      .reverse
      .padTo(minimumLengthOfCommodityCode, '0')
      .mkString

  private def shouldGoToSupplementaryUnitFromPrepPage(catInfo: CategorisationInfo, scenario: Scenario) =
    catInfo.measurementUnit.isDefined && getResultAsInt(scenario) == Category2AsInt

  private def navigateFromReassessmentPrep(
    reasessmentPrep: RecategorisationPreparationPage
  )(answers: UserAnswers): Call = {
    val recordId = reasessmentPrep.recordId

    answers.get(LongerCategorisationDetailsQuery(recordId)) match {
      case Some(catInfo) if catInfo.categoryAssessmentsThatNeedAnswers.nonEmpty =>
        val firstAnswer = answers.get(ReassessmentPage(recordId, firstAssessmentIndex))
        if (reassessmentAnswerIsEmpty(firstAnswer) || !firstAnswer.get.isAnswerCopiedFromPreviousAssessment) {
          controllers.categorisation.routes.AssessmentController
            .onPageLoadReassessment(NormalMode, recordId, firstAssessmentNumber)
        } else {
          navigateFromReassessment(ReassessmentPage(recordId, firstAssessmentIndex))(answers)
        }

      case Some(catInfo) =>
        val scenario =
          categorisationService.calculateResult(catInfo, answers, recordId)
        if (shouldGoToSupplementaryUnitFromPrepPage(catInfo, scenario)) {
          controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId)
        } else {
          controllers.categorisation.routes.CategorisationResultController.onPageLoad(recordId, scenario)
        }

      case None => controllers.problem.routes.JourneyRecoveryController.onPageLoad()

    }
  }

  private def navigateFromReassessment(assessmentPage: ReassessmentPage)(answers: UserAnswers): Call = {
    val recordId   = assessmentPage.recordId
    val nextIndex  = assessmentPage.index + 1
    val nextNumber = nextIndex + 1

    {
      for {
        categorisationInfo <- answers.get(LongerCategorisationDetailsQuery(recordId))
        assessmentCount     = categorisationInfo.categoryAssessmentsThatNeedAnswers.size
        assessmentQuestion <- categorisationInfo.getAssessmentFromIndex(assessmentPage.index)
        nextAnswer          = answers.get(ReassessmentPage(recordId, nextIndex))
        assessmentAnswer   <- answers.get(assessmentPage)
      } yield assessmentAnswer.answer match {
        case AssessmentAnswer.Exemption(_)
            if nextIndex < assessmentCount && (reassessmentAnswerIsEmpty(nextAnswer) || !nextAnswer.exists(
              _.isAnswerCopiedFromPreviousAssessment
            )) =>
          controllers.categorisation.routes.AssessmentController
            .onPageLoadReassessment(NormalMode, recordId, nextNumber)
        case AssessmentAnswer.Exemption(_) if nextIndex < assessmentCount                                        =>
          navigateFromReassessment(ReassessmentPage(recordId, nextIndex))(answers)
        case AssessmentAnswer.NoExemption if shouldGoToSupplementaryUnit(categorisationInfo, assessmentQuestion) =>
          controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId)
        case _                                                                                                   =>
          controllers.categorisation.routes.CyaCategorisationController.onPageLoad(recordId)
      }
    }.getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
  }

  private def shouldGoToSupplementaryUnit(
    categorisationInfo: CategorisationInfo,
    assessmentQuestion: CategoryAssessment
  ) =
    assessmentQuestion.category == Category2AsInt && categorisationInfo.measurementUnit.isDefined

  private def navigateFromReassessmentPrepCheck(
    reasessmentPrep: RecategorisationPreparationPage
  )(answers: UserAnswers): Call = {
    val recordId = reasessmentPrep.recordId

    answers.get(LongerCategorisationDetailsQuery(recordId)) match {
      case Some(catInfo) if catInfo.categoryAssessmentsThatNeedAnswers.nonEmpty =>
        val firstAnswer = answers.get(ReassessmentPage(recordId, firstAssessmentIndex))
        if (reassessmentAnswerIsEmpty(firstAnswer) || !firstAnswer.get.isAnswerCopiedFromPreviousAssessment) {
          controllers.categorisation.routes.AssessmentController
            .onPageLoadReassessment(CheckMode, recordId, firstAssessmentNumber)
        } else {
          navigateFromReassessmentCheck(ReassessmentPage(recordId, firstAssessmentIndex))(answers)
        }

      case Some(catInfo) =>
        val scenario =
          categorisationService.calculateResult(catInfo, answers, recordId)
        if (shouldGoToSupplementaryUnitFromPrepPage(catInfo, scenario)) {
          controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(CheckMode, recordId)
        } else {
          controllers.categorisation.routes.CategorisationResultController.onPageLoad(recordId, scenario)
        }
      case None          => controllers.problem.routes.JourneyRecoveryController.onPageLoad()

    }
  }

  private def navigateFromReassessmentCheck(assessmentPage: ReassessmentPage)(answers: UserAnswers): Call = {
    val recordId   = assessmentPage.recordId
    val nextIndex  = assessmentPage.index + 1
    val nextNumber = nextIndex + 1

    for {
      categorisationInfo <- answers.get(LongerCategorisationDetailsQuery(recordId))
      assessmentCount     = categorisationInfo.categoryAssessmentsThatNeedAnswers.size
      assessmentQuestion <- categorisationInfo.getAssessmentFromIndex(assessmentPage.index)
      assessmentAnswer   <- answers.get(assessmentPage)
      nextAnswer          = answers.get(ReassessmentPage(recordId, nextIndex))
    } yield assessmentAnswer.answer match {
      case AssessmentAnswer.Exemption(_) if nextIndex < assessmentCount && reassessmentAnswerIsEmpty(nextAnswer) =>
        controllers.categorisation.routes.AssessmentController.onPageLoadReassessment(CheckMode, recordId, nextNumber)
      case AssessmentAnswer.Exemption(_) if nextIndex < assessmentCount                                          =>
        navigateFromReassessmentCheck(ReassessmentPage(recordId, nextIndex))(answers)
      case AssessmentAnswer.NoExemption
          if shouldGoToSupplementaryUnitCheck(answers, categorisationInfo, assessmentQuestion, recordId) =>
        controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(CheckMode, recordId)
      case _                                                                                                     =>
        controllers.categorisation.routes.CyaCategorisationController.onPageLoad(recordId)
    }
  } getOrElse controllers.problem.routes.JourneyRecoveryController.onPageLoad()

  private def shouldGoToSupplementaryUnitCheck(
    userAnswers: UserAnswers,
    categorisationInfo: CategorisationInfo,
    assessmentQuestion: CategoryAssessment,
    recordId: String
  ) =
    assessmentQuestion.category == Category2AsInt && categorisationInfo.measurementUnit.isDefined &&
      userAnswers.get(HasSupplementaryUnitPage(recordId)).isEmpty

  private def reassessmentAnswerIsEmpty(nextAnswer: Option[ReassessmentAnswer]) =
    nextAnswer.isEmpty || nextAnswer.exists(reassessment => reassessment.answer == AssessmentAnswer.NotAnsweredYet)

}
