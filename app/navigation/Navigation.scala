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
import models.GoodsRecordsPagination.firstPage
import models.Scenario.getResultAsInt
import models._
import models.ott.{CategorisationInfo, CategoryAssessment}
import pages._
import pages.categorisation.CategoryGuidancePage
import play.api.mvc.Call
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery, LongerCommodityQuery}
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants._

import javax.inject.{Inject, Singleton}

@Singleton
class Navigation @Inject() (categorisationService: CategorisationService) extends Navigator {

  val normalRoutes: Page => UserAnswers => Call = {
    case CreateRecordStartPage                     => _ => routes.TraderReferenceController.onPageLoadCreate(NormalMode)
    case TraderReferencePage                       => _ => routes.GoodsDescriptionController.onPageLoadCreate(NormalMode)
    case p: TraderReferenceUpdatePage              => _ => routes.CyaUpdateRecordController.onPageLoadTraderReference(p.recordId)
    case GoodsDescriptionPage                      => _ => routes.CountryOfOriginController.onPageLoadCreate(NormalMode)
    case p: GoodsDescriptionUpdatePage             => _ => routes.CyaUpdateRecordController.onPageLoadGoodsDescription(p.recordId)
    case CountryOfOriginPage                       => _ => routes.CommodityCodeController.onPageLoadCreate(NormalMode)
    case p: CountryOfOriginUpdatePage              => _ => routes.CyaUpdateRecordController.onPageLoadCountryOfOrigin(p.recordId)
    case CommodityCodePage                         => _ => routes.HasCorrectGoodsController.onPageLoadCreate(NormalMode)
    case p: CommodityCodeUpdatePage                => _ => routes.HasCorrectGoodsController.onPageLoadUpdate(NormalMode, p.recordId)
    case HasCorrectGoodsPage                       => answers => navigateFromHasCorrectGoods(answers)
    case p: HasCorrectGoodsCommodityCodeUpdatePage => answers => navigateFromHasCorrectGoodsUpdate(answers, p.recordId)
    case p: AssessmentPage                         => navigateFromAssessment(p)
    case p: HasSupplementaryUnitPage               => navigateFromHasSupplementaryUnit(p.recordId)
    case p: SupplementaryUnitPage                  => _ => routes.CyaCategorisationController.onPageLoad(p.recordId)
    case p: AdviceStartPage                        => _ => routes.NameController.onPageLoad(NormalMode, p.recordId)
    case p: NamePage                               => _ => routes.EmailController.onPageLoad(NormalMode, p.recordId)
    case p: EmailPage                              => _ => routes.CyaRequestAdviceController.onPageLoad(p.recordId)
    case p: CyaCategorisationPage                  => navigateFromCyaCategorisationPage(p)
    case RemoveGoodsRecordPage                     => _ => routes.GoodsRecordsController.onPageLoad(firstPage)
    case p: LongerCommodityCodePage                =>
      _ => routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(NormalMode, p.recordId)
    case p: HasCorrectGoodsLongerCommodityCodePage =>
      answers => navigateFromHasCorrectGoodsLongerCommodityCode(p.recordId, answers, NormalMode)
    case p: HasGoodsDescriptionChangePage          => answers => navigateFromHasGoodsDescriptionChangePage(answers, p.recordId)
    case p: HasCountryOfOriginChangePage           => answers => navigateFromHasCountryOfOriginChangePage(answers, p.recordId)
    case p: HasCommodityCodeChangePage             => answers => navigateFromHasCommodityCodeChangePage(answers, p.recordId)
    case p: HasSupplementaryUnitUpdatePage         => answers => navigateFromHasSupplementaryUnitUpdatePage(answers, p.recordId)
    case p: SupplementaryUnitUpdatePage            => _ => routes.CyaSupplementaryUnitController.onPageLoad(p.recordId)
    case p: ReviewReasonPage                       => _ => routes.SingleRecordController.onPageLoad(p.recordId)
    case p: CategorisationPreparationPage          => answers => navigateFromCategorisationPreparationPage(answers, p.recordId)
    case p: CategoryGuidancePage                   =>
      _ => routes.AssessmentController.onPageLoad(NormalMode, p.recordId, firstAssessmentNumber)
    case p: ReassessmentPage                       => navigateFromReassessment(p)
    case p: RecategorisationPreparationPage        => navigateFromReassessmentPrep(p)
    case p: WithdrawAdviceStartPage                => answers => navigateFromWithdrawAdviceStartPage(answers, p.recordId)
    case p: ReasonForWithdrawAdvicePage            => _ => routes.WithdrawAdviceSuccessController.onPageLoad(p.recordId)
    case p: CyaCreateRecordPage                    => _ => routes.CreateRecordSuccessController.onPageLoad(p.recordId)
    case p: CyaRequestAdvicePage                   => _ => routes.AdviceSuccessController.onPageLoad(p.recordId)
    case p: CyaUpdateRecordPage                    => _ => routes.SingleRecordController.onPageLoad(p.recordId)
    case p: CyaSupplementaryUnitPage               => _ => routes.SingleRecordController.onPageLoad(p.recordId)
    case PreviousMovementRecordsPage               => _ => routes.GoodsRecordsController.onPageLoad(firstPage)
    case RequestDataPage                           => _ => routes.DownloadRequestSuccessController.onPageLoad()
    case _                                         => _ => routes.IndexController.onPageLoad()
  }

  val checkRoutes: Page => UserAnswers => Call = {
    case TraderReferencePage                       => _ => routes.CyaCreateRecordController.onPageLoad()
    case p: TraderReferenceUpdatePage              => _ => routes.CyaUpdateRecordController.onPageLoadTraderReference(p.recordId)
    case GoodsDescriptionPage                      => _ => routes.CyaCreateRecordController.onPageLoad()
    case p: GoodsDescriptionUpdatePage             => _ => routes.CyaUpdateRecordController.onPageLoadGoodsDescription(p.recordId)
    case CountryOfOriginPage                       => _ => routes.CyaCreateRecordController.onPageLoad()
    case p: CountryOfOriginUpdatePage              => _ => routes.CyaUpdateRecordController.onPageLoadCountryOfOrigin(p.recordId)
    case CommodityCodePage                         => _ => routes.HasCorrectGoodsController.onPageLoadCreate(CheckMode)
    case p: CommodityCodeUpdatePage                => _ => routes.HasCorrectGoodsController.onPageLoadUpdate(CheckMode, p.recordId)
    case HasCorrectGoodsPage                       => answers => navigateFromHasCorrectGoodsCheck(answers)
    case p: HasCorrectGoodsCommodityCodeUpdatePage =>
      answers => navigateFromHasCorrectGoodsUpdateCheck(answers, p.recordId)
    case p: NamePage                               => _ => routes.CyaRequestAdviceController.onPageLoad(p.recordId)
    case p: EmailPage                              => _ => routes.CyaRequestAdviceController.onPageLoad(p.recordId)
    case p: AssessmentPage                         => navigateFromAssessmentCheck(p)
    case p: HasSupplementaryUnitPage               => navigateFromHasSupplementaryUnitCheck(p.recordId)
    case p: SupplementaryUnitPage                  => _ => routes.CyaCategorisationController.onPageLoad(p.recordId)
    case p: HasSupplementaryUnitUpdatePage         =>
      navigateFromHasSupplementaryUnitUpdateCheck(p.recordId)
    case p: SupplementaryUnitUpdatePage            => _ => routes.CyaSupplementaryUnitController.onPageLoad(p.recordId)
    case p: LongerCommodityCodePage                =>
      _ => routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(CheckMode, p.recordId)
    case p: HasCorrectGoodsLongerCommodityCodePage =>
      answers => navigateFromHasCorrectGoodsLongerCommodityCode(p.recordId, answers, CheckMode)
    case p: ReassessmentPage                       => navigateFromReassessmentCheck(p)
    case p: RecategorisationPreparationPage        => navigateFromReassessmentPrepCheck(p)
    case _                                         => _ => controllers.problem.routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromWithdrawAdviceStartPage(answers: UserAnswers, recordId: String): Call = {

    val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(WithdrawAdviceStartPage(recordId))
      .map {
        case false => routes.SingleRecordController.onPageLoad(recordId)
        case true  => routes.ReasonForWithdrawAdviceController.onPageLoad(recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasCommodityCodeChangePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasCommodityCodeChangePage(recordId))
      .map {
        case false => routes.SingleRecordController.onPageLoad(recordId)
        case true  => routes.CommodityCodeController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasCountryOfOriginChangePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasCountryOfOriginChangePage(recordId))
      .map {
        case false => routes.SingleRecordController.onPageLoad(recordId)
        case true  => routes.CountryOfOriginController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasSupplementaryUnitUpdatePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasSupplementaryUnitUpdatePage(recordId))
      .map {
        case false => routes.CyaSupplementaryUnitController.onPageLoad(recordId)
        case true  => routes.SupplementaryUnitController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasGoodsDescriptionChangePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasGoodsDescriptionChangePage(recordId))
      .map {
        case false => routes.SingleRecordController.onPageLoad(recordId)
        case true  => routes.GoodsDescriptionController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasCorrectGoods(answers: UserAnswers): Call =
    answers
      .get(HasCorrectGoodsPage)
      .map {
        case true  => routes.CyaCreateRecordController.onPageLoad()
        case false => routes.CommodityCodeController.onPageLoadCreate(NormalMode)
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
      if (categorisationInfo.commodityCode == getShortenedCommodityCode(commodity)) {
        routes.LongerCommodityCodeController.onPageLoad(mode, recordId)
      } else {
        answers.get(HasCorrectGoodsLongerCommodityCodePage(recordId)) match {
          case Some(true)  => routes.CategorisationPreparationController.startLongerCategorisation(mode, recordId)
          case Some(false) => routes.LongerCommodityCodeController.onPageLoad(mode, recordId)
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

  private def navigateFromHasCorrectGoodsUpdate(answers: UserAnswers, recordId: String): Call =
    answers
      .get(HasCorrectGoodsCommodityCodeUpdatePage(recordId))
      .map {
        case true  => routes.CyaUpdateRecordController.onPageLoadCommodityCode(recordId)
        case false => routes.CommodityCodeController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasSupplementaryUnit(recordId: String)(answers: UserAnswers): Call =
    answers
      .get(HasSupplementaryUnitPage(recordId))
      .map {
        case true  => routes.SupplementaryUnitController.onPageLoad(NormalMode, recordId)
        case false => routes.CyaCategorisationController.onPageLoad(recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromCategorisationPreparationPage(answers: UserAnswers, recordId: String): Call =
    answers.get(CategorisationDetailsQuery(recordId)) match {
      case Some(catInfo) if catInfo.isCommCodeExpired                           =>
        controllers.problem.routes.ExpiredCommodityCodeController.onPageLoad(recordId)
      case Some(catInfo) if catInfo.categoryAssessmentsThatNeedAnswers.nonEmpty =>
        controllers.categorisation.routes.CategoryGuidanceController.onPageLoad(recordId)

      case Some(catInfo) =>
        val scenario = categorisationService.calculateResult(catInfo, answers, recordId)

        if (shouldGoToLongerCommodityCodeFromPrepPage(catInfo, scenario)) {
          routes.LongerCommodityCodeController.onPageLoad(NormalMode, recordId)
        } else if (shouldGoToSupplementaryUnitFromPrepPage(catInfo, scenario)) {
          routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId)
        } else {
          routes.CategorisationResultController.onPageLoad(recordId, scenario)
        }

      case None => controllers.problem.routes.JourneyRecoveryController.onPageLoad()
    }

  private def shouldGoToLongerCommodityCodeFromPrepPage(catInfo: CategorisationInfo, scenario: Scenario) =
    catInfo.getMinimalCommodityCode.length == minimumLengthOfCommodityCode && catInfo.descendantCount != 0 &&
      getResultAsInt(scenario) == Category2AsInt

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
          routes.AssessmentController.onPageLoadReassessment(NormalMode, recordId, firstAssessmentNumber)
        } else {
          navigateFromReassessment(ReassessmentPage(recordId, firstAssessmentIndex))(answers)
        }

      case Some(catInfo) =>
        val scenario = categorisationService.calculateResult(catInfo, answers, recordId)
        if (shouldGoToSupplementaryUnitFromPrepPage(catInfo, scenario)) {
          routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId)
        } else {
          routes.CategorisationResultController.onPageLoad(recordId, scenario)
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
          routes.AssessmentController.onPageLoadReassessment(NormalMode, recordId, nextNumber)
        case AssessmentAnswer.Exemption(_) if nextIndex < assessmentCount                                        =>
          navigateFromReassessment(ReassessmentPage(recordId, nextIndex))(answers)
        case AssessmentAnswer.NoExemption if shouldGoToSupplementaryUnit(categorisationInfo, assessmentQuestion) =>
          routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId)
        case _                                                                                                   =>
          routes.CyaCategorisationController.onPageLoad(recordId)
      }
    }.getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
  }

  private def navigateFromAssessment(assessmentPage: AssessmentPage)(answers: UserAnswers): Call = {
    val recordId   = assessmentPage.recordId
    val nextIndex  = assessmentPage.index + 1
    val nextNumber = nextIndex + 1

    {
      for {
        categorisationInfo <- answers.get(CategorisationDetailsQuery(recordId))
        assessmentCount     = categorisationInfo.categoryAssessmentsThatNeedAnswers.size
        assessmentQuestion <- categorisationInfo.getAssessmentFromIndex(assessmentPage.index)
        assessmentAnswer   <- answers.get(assessmentPage)
      } yield assessmentAnswer match {
        case AssessmentAnswer.Exemption(_) if nextIndex < assessmentCount                                        =>
          routes.AssessmentController.onPageLoad(NormalMode, recordId, nextNumber)
        case AssessmentAnswer.Exemption(_)
            if shouldGoToLongerCommodityCodeWhenCategory1(categorisationInfo, assessmentQuestion) =>
          routes.LongerCommodityCodeController.onPageLoad(NormalMode, recordId)
        case AssessmentAnswer.NoExemption
            if shouldGoToLongerCommodityCodeWhenCategory2(categorisationInfo, assessmentQuestion) =>
          routes.LongerCommodityCodeController.onPageLoad(NormalMode, recordId)
        case AssessmentAnswer.NoExemption if shouldGoToSupplementaryUnit(categorisationInfo, assessmentQuestion) =>
          routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId)
        case _                                                                                                   =>
          routes.CyaCategorisationController.onPageLoad(recordId)
      }
    }.getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
  }

  private def shouldGoToLongerCommodityCodeWhenCategory2(
    categorisationInfo: CategorisationInfo,
    assessmentQuestion: CategoryAssessment
  ) =
    categorisationInfo.getMinimalCommodityCode.length == 6 && categorisationInfo.descendantCount != 0 && assessmentQuestion.category == Category2AsInt

  private def shouldGoToLongerCommodityCodeWhenCategory1(
    categorisationInfo: CategorisationInfo,
    assessmentQuestion: CategoryAssessment
  ) =
    categorisationInfo.getMinimalCommodityCode.length == minimumLengthOfCommodityCode && categorisationInfo.descendantCount != 0 && assessmentQuestion.category == Category1AsInt && !categorisationInfo.categoryAssessmentsThatNeedAnswers
      .exists(_.isCategory2) && categorisationInfo.categoryAssessments.exists(_.isCategory2)

  private def shouldGoToSupplementaryUnit(
    categorisationInfo: CategorisationInfo,
    assessmentQuestion: CategoryAssessment
  ) =
    assessmentQuestion.category == Category2AsInt && categorisationInfo.measurementUnit.isDefined

  private def navigateFromCyaCategorisationPage(page: CyaCategorisationPage)(answers: UserAnswers): Call =
    (for {
      categorisationInfo <- getCategorisationInfoFromUA(answers, page.recordId)
      scenario            = categorisationService.calculateResult(categorisationInfo, answers, page.recordId)
    } yield routes.CategorisationResultController.onPageLoad(page.recordId, scenario))
      .getOrElse(
        controllers.problem.routes.JourneyRecoveryController.onPageLoad(
          Some(RedirectUrl(routes.CategorisationPreparationController.startCategorisation(page.recordId).url))
        )
      )

  private def getCategorisationInfoFromUA(userAnswers: UserAnswers, recordId: String) = {
    val longerDetails = userAnswers.get(LongerCategorisationDetailsQuery(recordId))
    longerDetails match {
      case Some(_) => longerDetails
      case _       => userAnswers.get(CategorisationDetailsQuery(recordId))
    }
  }

  private def navigateFromHasCorrectGoodsCheck(answers: UserAnswers): Call =
    answers
      .get(HasCorrectGoodsPage)
      .map {
        case true  =>
          if (answers.isDefined(CommodityCodePage)) {
            routes.CyaCreateRecordController.onPageLoad()
          } else {
            routes.CommodityCodeController.onPageLoadCreate(CheckMode)
          }
        case false => routes.CommodityCodeController.onPageLoadCreate(CheckMode)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoodsUpdateCheck(answers: UserAnswers, recordId: String): Call =
    answers
      .get(HasCorrectGoodsCommodityCodeUpdatePage(recordId))
      .map {
        case true  =>
          if (answers.isDefined(CommodityCodeUpdatePage(recordId))) {
            routes.CyaUpdateRecordController.onPageLoadCommodityCode(recordId)
          } else {
            routes.CommodityCodeController.onPageLoadUpdate(CheckMode, recordId)
          }
        case false => routes.CommodityCodeController.onPageLoadUpdate(CheckMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromReassessmentPrepCheck(
    reasessmentPrep: RecategorisationPreparationPage
  )(answers: UserAnswers): Call = {
    val recordId = reasessmentPrep.recordId

    answers.get(LongerCategorisationDetailsQuery(recordId)) match {
      case Some(catInfo) if catInfo.categoryAssessmentsThatNeedAnswers.nonEmpty =>
        val firstAnswer = answers.get(ReassessmentPage(recordId, firstAssessmentIndex))
        if (reassessmentAnswerIsEmpty(firstAnswer) || !firstAnswer.get.isAnswerCopiedFromPreviousAssessment) {
          routes.AssessmentController.onPageLoadReassessment(CheckMode, recordId, firstAssessmentNumber)
        } else {
          navigateFromReassessmentCheck(ReassessmentPage(recordId, firstAssessmentIndex))(answers)
        }

      case Some(catInfo) =>
        val scenario = categorisationService.calculateResult(catInfo, answers, recordId)
        if (shouldGoToSupplementaryUnitFromPrepPage(catInfo, scenario)) {
          routes.HasSupplementaryUnitController.onPageLoad(CheckMode, recordId)
        } else {
          routes.CategorisationResultController.onPageLoad(recordId, scenario)
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
        routes.AssessmentController.onPageLoadReassessment(CheckMode, recordId, nextNumber)
      case AssessmentAnswer.Exemption(_) if nextIndex < assessmentCount                                          =>
        navigateFromReassessmentCheck(ReassessmentPage(recordId, nextIndex))(answers)
      case AssessmentAnswer.NoExemption
          if shouldGoToSupplementaryUnitCheck(answers, categorisationInfo, assessmentQuestion, recordId) =>
        routes.HasSupplementaryUnitController.onPageLoad(CheckMode, recordId)
      case _                                                                                                     =>
        routes.CyaCategorisationController.onPageLoad(recordId)
    }
  } getOrElse controllers.problem.routes.JourneyRecoveryController.onPageLoad()

  private def navigateFromAssessmentCheck(assessmentPage: AssessmentPage)(answers: UserAnswers): Call = {
    val recordId   = assessmentPage.recordId
    val nextIndex  = assessmentPage.index + 1
    val nextNumber = nextIndex + 1

    {
      for {
        categorisationInfo <- answers.get(CategorisationDetailsQuery(recordId))
        assessmentCount     = categorisationInfo.categoryAssessmentsThatNeedAnswers.size
        assessmentQuestion <- categorisationInfo.getAssessmentFromIndex(assessmentPage.index)
        assessmentAnswer   <- answers.get(assessmentPage)
        nextAnswer          = answers.get(AssessmentPage(recordId, nextIndex))
      } yield assessmentAnswer match {
        case AssessmentAnswer.Exemption(_) if nextIndex < assessmentCount && answerIsEmpty(nextAnswer) =>
          routes.AssessmentController.onPageLoad(CheckMode, recordId, nextNumber)
        case AssessmentAnswer.Exemption(_)
            if shouldGoToLongerCommodityCodeWhenCategory1(categorisationInfo, assessmentQuestion) =>
          routes.LongerCommodityCodeController.onPageLoad(CheckMode, recordId)
        case AssessmentAnswer.NoExemption
            if shouldGoToLongerCommodityCodeWhenCategory2(categorisationInfo, assessmentQuestion) =>
          routes.LongerCommodityCodeController.onPageLoad(CheckMode, recordId)
        case AssessmentAnswer.NoExemption
            if shouldGoToSupplementaryUnitCheck(answers, categorisationInfo, assessmentQuestion, recordId) =>
          routes.HasSupplementaryUnitController.onPageLoad(CheckMode, recordId)
        case _                                                                                         =>
          routes.CyaCategorisationController.onPageLoad(recordId)
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

  private def navigateFromHasSupplementaryUnitUpdateCheck(recordId: String)(answers: UserAnswers): Call =
    answers
      .get(HasSupplementaryUnitUpdatePage(recordId))
      .map {
        case true  =>
          if (answers.isDefined(SupplementaryUnitUpdatePage(recordId))) {
            routes.CyaSupplementaryUnitController.onPageLoad(recordId)
          } else {
            routes.SupplementaryUnitController.onPageLoadUpdate(CheckMode, recordId)
          }
        case false => routes.CyaSupplementaryUnitController.onPageLoad(recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasSupplementaryUnitCheck(recordId: String)(answers: UserAnswers): Call =
    answers
      .get(HasSupplementaryUnitPage(recordId))
      .map {
        case true  =>
          if (answers.isDefined(SupplementaryUnitPage(recordId))) {
            routes.CyaCategorisationController.onPageLoad(recordId)
          } else {
            routes.SupplementaryUnitController.onPageLoad(CheckMode, recordId)
          }
        case false => routes.CyaCategorisationController.onPageLoad(recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def answerIsEmpty(nextAnswer: Option[AssessmentAnswer]) =
    nextAnswer.isEmpty || nextAnswer.contains(AssessmentAnswer.NotAnsweredYet)

  private def reassessmentAnswerIsEmpty(nextAnswer: Option[ReassessmentAnswer]) =
    nextAnswer.isEmpty || nextAnswer.exists(reassessment => reassessment.answer == AssessmentAnswer.NotAnsweredYet)

}