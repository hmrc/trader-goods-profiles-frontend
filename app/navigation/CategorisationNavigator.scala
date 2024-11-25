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
import pages.categorisation._
import play.api.mvc.Call
import queries.{CategorisationDetailsQuery, HasLongComCodeQuery, LongerCategorisationDetailsQuery}
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants._

import javax.inject.{Inject, Singleton}

@Singleton
class CategorisationNavigator @Inject() (categorisationService: CategorisationService) extends Navigator {

  val normalRoutes: Page => UserAnswers => Call = {
    case p: AssessmentPage => navigateFromAssessment(p)

    case p: CategoryGuidancePage     =>
      _ =>
        controllers.categorisation.routes.AssessmentController.onPageLoad(NormalMode, p.recordId, firstAssessmentNumber)
    case p: HasSupplementaryUnitPage => navigateFromHasSupplementaryUnit(p.recordId)

    case p: SupplementaryUnitPage =>
      _ => controllers.categorisation.routes.CyaCategorisationController.onPageLoad(p.recordId)
    case p: CyaCategorisationPage => navigateFromCyaCategorisationPage(p)

    case p: HasSupplementaryUnitUpdatePage  => answers => navigateFromHasSupplementaryUnitUpdatePage(answers, p.recordId)
    case p: SupplementaryUnitUpdatePage     =>
      _ => controllers.categorisation.routes.CyaSupplementaryUnitController.onPageLoad(p.recordId)
    case p: CyaSupplementaryUnitPage        =>
      _ => controllers.goodsRecord.routes.SingleRecordController.onPageLoad(p.recordId)
    case p: RecategorisationPreparationPage => navigateFromReassessmentPrep(p)
    case p: ReassessmentPage                => navigateFromReassessment(p)
    case p: LongerCommodityCodePage         =>
      _ => routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(NormalMode, p.recordId)
    case p: CategorisationPreparationPage   =>
      answers => navigateFromCategorisationPreparationPage(answers, p.recordId)

    case _ => _ => routes.IndexController.onPageLoad()
  }

  val checkRoutes: Page => UserAnswers => Call = {
    case p: AssessmentPage                  => navigateFromAssessmentCheck(p)
    case p: HasSupplementaryUnitPage        => navigateFromHasSupplementaryUnitCheck(p.recordId)
    case p: SupplementaryUnitPage           =>
      _ => controllers.categorisation.routes.CyaCategorisationController.onPageLoad(p.recordId)
    case p: HasSupplementaryUnitUpdatePage  =>
      navigateFromHasSupplementaryUnitUpdateCheck(p.recordId)
    case p: SupplementaryUnitUpdatePage     =>
      _ => controllers.categorisation.routes.CyaSupplementaryUnitController.onPageLoad(p.recordId)
    case p: LongerCommodityCodePage         =>
      _ => routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(CheckMode, p.recordId)
    case p: ReassessmentPage                => navigateFromReassessmentCheck(p)
    case p: RecategorisationPreparationPage => navigateFromReassessmentPrepCheck(p)

    case _ => _ => controllers.problem.routes.JourneyRecoveryController.onPageLoad()
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

  private def navigateFromHasSupplementaryUnitCheck(recordId: String)(answers: UserAnswers): Call =
    answers
      .get(HasSupplementaryUnitPage(recordId))
      .map {
        case true  =>
          if (answers.isDefined(SupplementaryUnitPage(recordId))) {
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(recordId)
          } else {
            controllers.categorisation.routes.SupplementaryUnitController.onPageLoad(CheckMode, recordId)
          }
        case false => controllers.categorisation.routes.CyaCategorisationController.onPageLoad(recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasSupplementaryUnitUpdateCheck(recordId: String)(answers: UserAnswers): Call =
    answers
      .get(HasSupplementaryUnitUpdatePage(recordId))
      .map {
        case true  =>
          if (answers.isDefined(SupplementaryUnitUpdatePage(recordId))) {
            controllers.categorisation.routes.CyaSupplementaryUnitController.onPageLoad(recordId)
          } else {
            controllers.categorisation.routes.SupplementaryUnitController.onPageLoadUpdate(CheckMode, recordId)
          }
        case false => controllers.categorisation.routes.CyaSupplementaryUnitController.onPageLoad(recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromReassessmentPrepCheck(
    reasessmentPrep: RecategorisationPreparationPage
  )(answers: UserAnswers): Call = {
    val recordId = reasessmentPrep.recordId

    answers.get(LongerCategorisationDetailsQuery(recordId)) match {
      case Some(_) if categorisationService.existsUnansweredCat1Questions(answers, recordId) =>
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
        } else
          scenario match {
            case StandardGoodsNoAssessmentsScenario | Category1NoExemptionsScenario =>
              controllers.categorisation.routes.CategorisationResultController.onPageLoad(recordId, scenario)
            case _                                                                  => controllers.categorisation.routes.CyaCategorisationController.onPageLoad(recordId)
          }

      case None => controllers.problem.routes.JourneyRecoveryController.onPageLoad()

    }
  }

  private def navigateFromAssessmentCheck(assessmentPage: AssessmentPage)(answers: UserAnswers): Call = {
    val recordId   = assessmentPage.recordId
    val nextIndex  = assessmentPage.index + 1
    val nextNumber = nextIndex + 1

    {
      for {
        hasLongComCode     <- answers.get(HasLongComCodeQuery(recordId))
        categorisationInfo <- answers.get(CategorisationDetailsQuery(recordId))
        assessmentCount     = categorisationInfo.categoryAssessmentsThatNeedAnswers.size
        assessmentQuestion <- categorisationInfo.getAssessmentFromIndex(assessmentPage.index)
        assessmentAnswer   <- answers.get(assessmentPage)
        nextAnswer          = answers.get(AssessmentPage(recordId, nextIndex))
      } yield assessmentAnswer match {
        case AssessmentAnswer.Exemption(_) if nextIndex < assessmentCount && answerIsEmpty(nextAnswer) =>
          controllers.categorisation.routes.AssessmentController.onPageLoad(CheckMode, recordId, nextNumber)
        case AssessmentAnswer.Exemption(_)
            if shouldGoToLongerCommodityCodeWhenCategory1(
              categorisationInfo,
              assessmentQuestion,
              hasLongComCode
            ) =>
          controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(CheckMode, recordId)
        case AssessmentAnswer.NoExemption
            if shouldGoToLongerCommodityCodeWhenCategory2(
              categorisationInfo,
              assessmentQuestion,
              hasLongComCode
            ) =>
          controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(CheckMode, recordId)
        case AssessmentAnswer.NoExemption
            if shouldGoToSupplementaryUnitCheck(answers, categorisationInfo, assessmentQuestion, recordId) =>
          controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(CheckMode, recordId)
        case _                                                                                         =>
          controllers.categorisation.routes.CyaCategorisationController.onPageLoad(recordId)
      }
    }.getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
  }

  private def shouldGoToSupplementaryUnitCheck(
    userAnswers: UserAnswers,
    categorisationInfo: CategorisationInfo,
    assessmentQuestion: CategoryAssessment,
    recordId: String
  ) =
    assessmentQuestion.category == Category2AsInt && categorisationInfo.measurementUnit.isDefined &&
      userAnswers.get(HasSupplementaryUnitPage(recordId)).isEmpty

  private def answerIsEmpty(nextAnswer: Option[AssessmentAnswer]) =
    nextAnswer.isEmpty || nextAnswer.contains(AssessmentAnswer.NotAnsweredYet)

  private def navigateFromCategorisationPreparationPage(
    answers: UserAnswers,
    recordId: String
  ): Call =
    (for {
      hasLongComCode <- answers.get(HasLongComCodeQuery(recordId))
      catInfo        <- answers.get(CategorisationDetailsQuery(recordId))
    } yield
      if (catInfo.isCommCodeExpired) {
        controllers.problem.routes.ExpiredCommodityCodeController.onPageLoad(recordId)
      } else if (catInfo.categoryAssessmentsThatNeedAnswers.nonEmpty) {
        controllers.categorisation.routes.CategoryGuidanceController.onPageLoad(recordId)
      } else {
        val scenario = categorisationService.calculateResult(catInfo, answers, recordId)
        if (shouldGoToLongerCommodityCodeFromPrepPage(catInfo, scenario, hasLongComCode)) {
          controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, recordId)
        } else if (shouldGoToSupplementaryUnitFromPrepPage(catInfo, scenario)) {
          controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId)
        } else {
          controllers.categorisation.routes.CategorisationResultController.onPageLoad(recordId, scenario)
        }
      }).getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def shouldGoToLongerCommodityCodeFromPrepPage(
    catInfo: CategorisationInfo,
    scenario: Scenario,
    hasLongComCode: Boolean
  ): Boolean =
    !hasLongComCode &&
      catInfo.getMinimalCommodityCode.length == minimumLengthOfCommodityCode &&
      catInfo.descendantCount != 0 &&
      getResultAsInt(scenario) == Category2AsInt

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

  private def reassessmentAnswerIsEmpty(nextAnswer: Option[ReassessmentAnswer]) =
    nextAnswer.isEmpty || nextAnswer.exists(reassessment => reassessment.answer == AssessmentAnswer.NotAnsweredYet)

  private def navigateFromReassessmentPrep(
    reasessmentPrep: RecategorisationPreparationPage
  )(answers: UserAnswers): Call = {
    val recordId = reasessmentPrep.recordId

    answers.get(LongerCategorisationDetailsQuery(recordId)) match {
      case Some(_) if categorisationService.existsUnansweredCat1Questions(answers, recordId) =>
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
        } else
          scenario match {
            case StandardGoodsNoAssessmentsScenario | Category1NoExemptionsScenario =>
              controllers.categorisation.routes.CategorisationResultController.onPageLoad(recordId, scenario)
            case _                                                                  => controllers.categorisation.routes.CyaCategorisationController.onPageLoad(recordId)
          }

      case None => controllers.problem.routes.JourneyRecoveryController.onPageLoad()

    }
  }

  private def navigateFromHasSupplementaryUnitUpdatePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasSupplementaryUnitUpdatePage(recordId))
      .map {
        case false => controllers.categorisation.routes.CyaSupplementaryUnitController.onPageLoad(recordId)
        case true  =>
          controllers.categorisation.routes.SupplementaryUnitController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromCyaCategorisationPage(page: CyaCategorisationPage)(answers: UserAnswers): Call =
    (for {
      categorisationInfo <- getCategorisationInfoFromUA(answers, page.recordId)
      scenario            = categorisationService.calculateResult(categorisationInfo, answers, page.recordId)
    } yield controllers.categorisation.routes.CategorisationResultController.onPageLoad(page.recordId, scenario))
      .getOrElse(
        controllers.problem.routes.JourneyRecoveryController.onPageLoad(
          Some(
            RedirectUrl(
              controllers.categorisation.routes.CategorisationPreparationController
                .startCategorisation(page.recordId)
                .url
            )
          )
        )
      )

  private def getCategorisationInfoFromUA(userAnswers: UserAnswers, recordId: String) = {
    val longerDetails = userAnswers.get(LongerCategorisationDetailsQuery(recordId))
    longerDetails match {
      case Some(_) => longerDetails
      case _       => userAnswers.get(CategorisationDetailsQuery(recordId))
    }
  }

  private def navigateFromHasSupplementaryUnit(recordId: String)(answers: UserAnswers): Call =
    answers
      .get(HasSupplementaryUnitPage(recordId))
      .map {
        case true  => controllers.categorisation.routes.SupplementaryUnitController.onPageLoad(NormalMode, recordId)
        case false => controllers.categorisation.routes.CyaCategorisationController.onPageLoad(recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def shouldGoToSupplementaryUnit(
    categorisationInfo: CategorisationInfo,
    assessmentQuestion: CategoryAssessment
  ) =
    assessmentQuestion.category == Category2AsInt && categorisationInfo.measurementUnit.isDefined

  private def shouldGoToSupplementaryUnitFromPrepPage(catInfo: CategorisationInfo, scenario: Scenario) =
    catInfo.measurementUnit.isDefined && getResultAsInt(scenario) == Category2AsInt

  private def shouldGoToLongerCommodityCodeWhenCategory2(
    categorisationInfo: CategorisationInfo,
    assessmentQuestion: CategoryAssessment,
    hasLongComCode: Boolean
  ): Boolean =
    !hasLongComCode &&
      categorisationInfo.getMinimalCommodityCode.length == 6 &&
      categorisationInfo.descendantCount != 0 &&
      assessmentQuestion.category == Category2AsInt

  private def shouldGoToLongerCommodityCodeWhenCategory1(
    categorisationInfo: CategorisationInfo,
    assessmentQuestion: CategoryAssessment,
    hasLongComCode: Boolean
  ): Boolean =
    !hasLongComCode &&
      categorisationInfo.getMinimalCommodityCode.length == minimumLengthOfCommodityCode &&
      categorisationInfo.descendantCount != 0 &&
      assessmentQuestion.category == Category1AsInt &&
      !categorisationInfo.categoryAssessmentsThatNeedAnswers.exists(_.isCategory2) &&
      categorisationInfo.categoryAssessments.exists(_.isCategory2)

  private def navigateFromAssessment(assessmentPage: AssessmentPage)(answers: UserAnswers): Call = {
    val recordId   = assessmentPage.recordId
    val nextIndex  = assessmentPage.index + 1
    val nextNumber = nextIndex + 1

    {
      for {
        hasLongComCode     <- answers.get(HasLongComCodeQuery(recordId))
        categorisationInfo <- answers.get(CategorisationDetailsQuery(recordId))
        assessmentCount     = categorisationInfo.categoryAssessmentsThatNeedAnswers.size
        assessmentQuestion <- categorisationInfo.getAssessmentFromIndex(assessmentPage.index)
        assessmentAnswer   <- answers.get(assessmentPage)
      } yield assessmentAnswer match {
        case AssessmentAnswer.Exemption(_) if nextIndex < assessmentCount                                        =>
          controllers.categorisation.routes.AssessmentController.onPageLoad(NormalMode, recordId, nextNumber)
        case AssessmentAnswer.Exemption(_)
            if shouldGoToLongerCommodityCodeWhenCategory1(
              categorisationInfo,
              assessmentQuestion,
              hasLongComCode
            ) =>
          controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, recordId)
        case AssessmentAnswer.NoExemption
            if shouldGoToLongerCommodityCodeWhenCategory2(
              categorisationInfo,
              assessmentQuestion,
              hasLongComCode
            ) =>
          controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, recordId)
        case AssessmentAnswer.NoExemption if shouldGoToSupplementaryUnit(categorisationInfo, assessmentQuestion) =>
          controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId)
        case _                                                                                                   =>
          controllers.categorisation.routes.CyaCategorisationController.onPageLoad(recordId)
      }
    }.getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
  }

}
