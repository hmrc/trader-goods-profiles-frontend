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

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import models.GoodsRecordsPagination.firstPage
import pages._
import models._
import queries.RecordCategorisationsQuery
import pages._
import play.api.mvc.Call
import queries.{CategorisationDetailsQuery, CategorisationDetailsQuery2}
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.firstAssessmentIndex

import scala.util.Try

@Singleton
//TODO decide if actually needed cat service injected here?
class Navigator @Inject() (categorisationService: CategorisationService) {
  private val normalRoutes: Page => UserAnswers => Call = {
    case ProfileSetupPage                          => _ => routes.UkimsNumberController.onPageLoadCreate(NormalMode)
    case UkimsNumberPage                           => _ => routes.HasNirmsController.onPageLoadCreate(NormalMode)
    case HasNirmsPage                              => navigateFromHasNirms
    case NirmsNumberPage                           => _ => routes.HasNiphlController.onPageLoadCreate(NormalMode)
    case HasNiphlPage                              => navigateFromHasNiphl
    case NiphlNumberPage                           => _ => routes.CyaCreateProfileController.onPageLoad
    case UkimsNumberUpdatePage                     => _ => routes.ProfileController.onPageLoad()
    case HasNirmsUpdatePage                        => navigateFromHasNirmsUpdate
    case NirmsNumberUpdatePage                     => _ => routes.ProfileController.onPageLoad()
    case RemoveNirmsPage                           => _ => routes.ProfileController.onPageLoad()
    case HasNiphlUpdatePage                        => navigateFromHasNiphlUpdate
    case NiphlNumberUpdatePage                     => _ => routes.ProfileController.onPageLoad()
    case RemoveNiphlPage                           => _ => routes.ProfileController.onPageLoad()
    case CreateRecordStartPage                     => _ => routes.TraderReferenceController.onPageLoadCreate(NormalMode)
    case TraderReferencePage                       => _ => routes.UseTraderReferenceController.onPageLoad(NormalMode)
    case p: TraderReferenceUpdatePage              => _ => routes.CyaUpdateRecordController.onPageLoadTraderReference(p.recordId)
    case UseTraderReferencePage                    => navigateFromUseTraderReference
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
    case p: CyaCategorisationPage                  =>
      _ => routes.CategorisationResultController.onPageLoad(p.recordId, Scenario.getScenario(p.categoryRecord))
    case RemoveGoodsRecordPage                     => _ => routes.GoodsRecordsController.onPageLoad(firstPage)
    case p: LongerCommodityCodePage                =>
      _ => navigateFromLongerCommodityCode(p.recordId, p.shouldRedirectToCya)
    case p: HasCorrectGoodsLongerCommodityCodePage =>
      navigateFromHasCorrectGoodsLongerCommodityCode(p.recordId, p.needToRecategorise)
    case p: HasGoodsDescriptionChangePage          => answers => navigateFromHasGoodsDescriptionChangePage(answers, p.recordId)
    case p: HasCountryOfOriginChangePage           => answers => navigateFromHasCountryOfOriginChangePage(answers, p.recordId)
    case p: HasCommodityCodeChangePage             => answers => navigateFromHasCommodityCodeChangePage(answers, p.recordId)
    case p: HasSupplementaryUnitUpdatePage         => answers => navigateFromHasSupplementaryUnitUpdatePage(answers, p.recordId)
    case p: SupplementaryUnitUpdatePage            => _ => routes.CyaSupplementaryUnitController.onPageLoad(p.recordId)
    case p: ReviewReasonPage                       => _ => routes.SingleRecordController.onPageLoad(p.recordId)
    case p: CategorisationPreparationPage          => _ => routes.CategoryGuidanceController.onPageLoad2(p.recordId)
    case p: CategoryGuidancePage =>
      _ => routes.AssessmentController.onPageLoad2(NormalMode, p.recordId, firstAssessmentIndex)
    case p: AssessmentPage2                        => navigateFromAssessment2(p)
    case p: CyaCategorisationPage2 => navigateFromCyaCategorisationPage(p)
    case _                                         => _ => routes.IndexController.onPageLoad
  }

  private def navigateFromHasCommodityCodeChangePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasCommodityCodeChangePage(recordId))
      .map {
        case false => routes.SingleRecordController.onPageLoad(recordId)
        case true  => routes.CommodityCodeController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasCountryOfOriginChangePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasCountryOfOriginChangePage(recordId))
      .map {
        case false => routes.SingleRecordController.onPageLoad(recordId)
        case true  => routes.CountryOfOriginController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasSupplementaryUnitUpdatePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasSupplementaryUnitUpdatePage(recordId))
      .map {
        case false => routes.CyaSupplementaryUnitController.onPageLoad(recordId)
        case true  => routes.SupplementaryUnitController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasGoodsDescriptionChangePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasGoodsDescriptionChangePage(recordId))
      .map {
        case false => routes.SingleRecordController.onPageLoad(recordId)
        case true  => routes.GoodsDescriptionController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromUseTraderReference(answers: UserAnswers): Call =
    answers
      .get(UseTraderReferencePage)
      .map {
        case false => routes.GoodsDescriptionController.onPageLoadCreate(NormalMode)
        case true  => routes.CountryOfOriginController.onPageLoadCreate(NormalMode)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoods(answers: UserAnswers): Call =
    answers
      .get(HasCorrectGoodsPage)
      .map {
        case true  => routes.CyaCreateRecordController.onPageLoad
        case false => routes.CommodityCodeController.onPageLoadCreate(NormalMode)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoodsLongerCommodityCode(recordId: String, needToRecategorise: Boolean)(
    answers: UserAnswers
  ): Call = {
    for {
      recordCategorisations <- answers.get(RecordCategorisationsQuery)
      categorisationInfo    <- recordCategorisations.records.get(recordId)
      assessmentAnswer      <- answers
                                 .get(HasCorrectGoodsLongerCommodityCodePage(recordId))
    } yield
      if (assessmentAnswer) {
        if (needToRecategorise) {
          routes.AssessmentController.onPageLoad(NormalMode, recordId, firstAssessmentIndex)
        } else {
          val supplementaryUnitAnswer = answers.get(HasSupplementaryUnitPage(recordId))
          if (categorisationInfo.measurementUnit.isDefined && supplementaryUnitAnswer.isEmpty) {
            routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId)
          } else {
            routes.CyaCategorisationController.onPageLoad(recordId)
          }
        }
      } else {
        routes.LongerCommodityCodeController.onPageLoad(NormalMode, recordId)
      }
  }.getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromLongerCommodityCode(
    recordId: String,
    shouldRedirectToCya: Boolean,
    mode: Mode = NormalMode
  ): Call =
    if (shouldRedirectToCya) {
      routes.CyaCategorisationController.onPageLoad(recordId)
    } else {
      routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(mode, recordId)
    }

  private def navigateFromHasCorrectGoodsUpdate(answers: UserAnswers, recordId: String): Call =
    answers
      .get(HasCorrectGoodsCommodityCodeUpdatePage(recordId))
      .map {
        case true  => routes.CyaUpdateRecordController.onPageLoadCommodityCode(recordId)
        case false => routes.CommodityCodeController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNirms(answers: UserAnswers): Call =
    answers
      .get(HasNirmsPage)
      .map {
        case true  => routes.NirmsNumberController.onPageLoadCreate(NormalMode)
        case false => routes.HasNiphlController.onPageLoadCreate(NormalMode)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNirmsUpdate(answers: UserAnswers): Call = {
    val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)
    answers
      .get(HasNirmsUpdatePage)
      .map {
        case true  => routes.NirmsNumberController.onPageLoadUpdate
        case false => routes.RemoveNirmsController.onPageLoad()
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasNiphl(answers: UserAnswers): Call =
    answers
      .get(HasNiphlPage)
      .map {
        case true  => routes.NiphlNumberController.onPageLoadCreate(NormalMode)
        case false => routes.CyaCreateProfileController.onPageLoad
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNiphlUpdate(answers: UserAnswers): Call = {
    val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)
    answers
      .get(HasNiphlUpdatePage)
      .map {
        case true  => routes.NiphlNumberController.onPageLoadUpdate
        case false => routes.RemoveNiphlController.onPageLoad()
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasSupplementaryUnit(recordId: String)(answers: UserAnswers): Call =
    answers
      .get(HasSupplementaryUnitPage(recordId))
      .map {
        case true  => routes.SupplementaryUnitController.onPageLoad(NormalMode, recordId)
        case false => routes.CyaCategorisationController.onPageLoad(recordId)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromAssessment2(assessmentPage: AssessmentPage2)(answers: UserAnswers): Call = {
    val recordId  = assessmentPage.recordId
    val nextIndex = assessmentPage.index + 1

    {
      for {
        categorisationInfo <- answers.get(CategorisationDetailsQuery2(recordId))
        assessmentCount     = categorisationInfo.categoryAssessmentsThatNeedAnswers.size
        assessmentAnswer   <- answers.get(assessmentPage)
      } yield assessmentAnswer match {
        case AssessmentAnswer2.Exemption if nextIndex < assessmentCount =>
          routes.AssessmentController.onPageLoad2(NormalMode, recordId, nextIndex)
        case _                                                          =>
          routes.CyaCategorisationController.onPageLoad2(recordId)
      }
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())
  }

  private def navigateFromAssessment(assessmentPage: AssessmentPage)(answers: UserAnswers): Call = {
    if (!assessmentPage.shouldRedirectToCya) {
      val recordId = assessmentPage.recordId

      for {
        recordQuery      <- answers.get(RecordCategorisationsQuery)
        record           <- recordQuery.records.get(recordId)
        assessmentAnswer <- answers.get(assessmentPage)
      } yield assessmentAnswer match {
        case AssessmentAnswer.Exemption(_) =>
          val assessmentCount = Try {
            recordQuery.records(recordId).categoryAssessments.size
          }.getOrElse(0)

          if (assessmentPage.index + 1 < assessmentCount) {
            routes.AssessmentController.onPageLoad(NormalMode, recordId, assessmentPage.index + 1)
          } else {
            routes.CyaCategorisationController.onPageLoad(recordId)
          }
        case AssessmentAnswer.NoExemption  =>
          record.categoryAssessments(assessmentPage.index).category match {
            case 2
                if commodityCodeSansTrailingZeros(record.commodityCode).length <= 6 &&
                  record.descendantCount != 0 =>
              routes.LongerCommodityCodeController.onPageLoad(NormalMode, recordId)
            case 2 if record.measurementUnit.isDefined =>
              routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId)
            case _                                     =>
              routes.CyaCategorisationController.onPageLoad(recordId)
          }
      }
    } else {
      Some(routes.CyaCategorisationController.onPageLoad(assessmentPage.recordId))
    }
  }.getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromCyaCategorisationPage(page: CyaCategorisationPage2)(answers: UserAnswers): Call = {
    (for {
      categorisationInfo <- answers.get(CategorisationDetailsQuery2(page.recordId))
      scenario = categorisationService.calculateResult(categorisationInfo, answers, page.recordId)
    } yield {
      routes.CategorisationResultController.onPageLoad2(page.recordId, scenario)
    }).getOrElse(routes.JourneyRecoveryController.onPageLoad(
      Some(RedirectUrl(routes.CategorisationPreparationController.startCategorisation(page.recordId).url))
    ))

  }
  private val checkRouteMap: Page => UserAnswers => Call = {
    case UkimsNumberPage                           => _ => routes.CyaCreateProfileController.onPageLoad
    case HasNirmsPage                              => navigateFromHasNirmsCheck
    case NirmsNumberPage                           => _ => routes.CyaCreateProfileController.onPageLoad
    case HasNiphlPage                              => navigateFromHasNiphlCheck
    case NiphlNumberPage                           => _ => routes.CyaCreateProfileController.onPageLoad
    case TraderReferencePage                       => _ => routes.CyaCreateRecordController.onPageLoad
    case p: TraderReferenceUpdatePage              => _ => routes.CyaUpdateRecordController.onPageLoadTraderReference(p.recordId)
    case UseTraderReferencePage                    => navigateFromUseTraderReferenceCheck
    case GoodsDescriptionPage                      => _ => routes.CyaCreateRecordController.onPageLoad
    case p: GoodsDescriptionUpdatePage             => _ => routes.CyaUpdateRecordController.onPageLoadGoodsDescription(p.recordId)
    case CountryOfOriginPage                       => _ => routes.CyaCreateRecordController.onPageLoad
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
      _ => navigateFromLongerCommodityCode(p.recordId, p.shouldRedirectToCya, CheckMode)
    case p: HasCorrectGoodsLongerCommodityCodePage =>
      navigateFromHasCorrectGoodsLongerCommodityCodeCheck(p.recordId, p.needToRecategorise)
    case p: AssessmentPage2 => navigateFromAssessmentCheck2(p)
    case _                                         => _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromHasNirmsCheck(answers: UserAnswers): Call =
    answers
      .get(HasNirmsPage)
      .map {
        case true  =>
          if (answers.isDefined(NirmsNumberPage)) {
            routes.CyaCreateProfileController.onPageLoad
          } else {
            routes.NirmsNumberController.onPageLoadCreate(CheckMode)
          }
        case false => routes.CyaCreateProfileController.onPageLoad
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNiphlCheck(answers: UserAnswers): Call =
    answers
      .get(HasNiphlPage)
      .map {
        case true  =>
          if (answers.isDefined(NiphlNumberPage)) {
            routes.CyaCreateProfileController.onPageLoad
          } else {
            routes.NiphlNumberController.onPageLoadCreate(CheckMode)
          }
        case false => routes.CyaCreateProfileController.onPageLoad
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromUseTraderReferenceCheck(answers: UserAnswers): Call =
    answers
      .get(UseTraderReferencePage)
      .map {
        case false =>
          if (answers.isDefined(GoodsDescriptionPage)) {
            routes.CyaCreateRecordController.onPageLoad
          } else {
            routes.GoodsDescriptionController.onPageLoadCreate(CheckMode)
          }
        case true  => routes.CyaCreateRecordController.onPageLoad
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoodsCheck(answers: UserAnswers): Call =
    answers
      .get(HasCorrectGoodsPage)
      .map {
        case true  =>
          if (answers.isDefined(CommodityCodePage)) {
            routes.CyaCreateRecordController.onPageLoad
          } else {
            routes.CommodityCodeController.onPageLoadCreate(CheckMode)
          }
        case false => routes.CommodityCodeController.onPageLoadCreate(CheckMode)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoodsLongerCommodityCodeCheck(recordId: String, needToRecategorise: Boolean)(
    answers: UserAnswers
  ): Call = {
    for {
      recordCategorisations <- answers.get(RecordCategorisationsQuery)
      categorisationInfo    <- recordCategorisations.records.get(recordId)
      assessmentAnswer      <- answers
                                 .get(HasCorrectGoodsLongerCommodityCodePage(recordId))
    } yield
      if (assessmentAnswer) {
        if (needToRecategorise) {
          routes.AssessmentController.onPageLoad(CheckMode, recordId, firstAssessmentIndex)
        } else {
          val supplementaryUnitAnswer = answers.get(HasSupplementaryUnitPage(recordId))
          if (categorisationInfo.measurementUnit.isDefined && supplementaryUnitAnswer.isEmpty) {
            routes.HasSupplementaryUnitController.onPageLoad(CheckMode, recordId)
          } else {
            routes.CyaCategorisationController.onPageLoad(recordId)
          }
        }
      } else {
        routes.LongerCommodityCodeController.onPageLoad(CheckMode, recordId)
      }
  }.getOrElse(routes.JourneyRecoveryController.onPageLoad())

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
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromAssessmentCheck2(assessmentPage: AssessmentPage2)(answers: UserAnswers): Call = {
    val recordId = assessmentPage.recordId
    val nextIndex = assessmentPage.index + 1

    {
      for {
        categorisationInfo <- answers.get(CategorisationDetailsQuery2(recordId))
        assessmentCount = categorisationInfo.categoryAssessmentsThatNeedAnswers.size
        assessmentAnswer <- answers.get(assessmentPage)
        nextAnswer = answers.get(AssessmentPage2(recordId, nextIndex))
      } yield assessmentAnswer match {
        case AssessmentAnswer2.Exemption if nextIndex < assessmentCount && nextAnswer.isEmpty =>
          routes.AssessmentController.onPageLoad2(CheckMode, recordId, nextIndex)

        case _ =>
          routes.CyaCategorisationController.onPageLoad2(recordId)
      }
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())
  }

  private def navigateFromAssessmentCheck(assessmentPage: AssessmentPage)(answers: UserAnswers): Call = {
    val recordId = assessmentPage.recordId

    for {
      recordQuery        <- answers.get(RecordCategorisationsQuery)
      categorisationInfo <- recordQuery.records.get(recordId)
      assessmentAnswer   <- answers.get(assessmentPage)
    } yield assessmentAnswer match {
      case AssessmentAnswer.Exemption(_) =>
        val assessmentCount = Try {
          recordQuery.records(recordId).categoryAssessments.size
        }.getOrElse(0)

        if (assessmentPage.index + 1 < assessmentCount) {
          val nextAnswer = answers.get(AssessmentPage(recordId, assessmentPage.index + 1))

          nextAnswer match {
            case Some(_) if !categorisationInfo.areThereAnyNonAnsweredQuestions(recordId, answers) =>
              routes.CyaCategorisationController.onPageLoad(recordId)
            case _                                                                                 =>
              routes.AssessmentController.onPageLoad(CheckMode, recordId, assessmentPage.index + 1)

          }
        } else {
          routes.CyaCategorisationController.onPageLoad(recordId)
        }
      case AssessmentAnswer.NoExemption  =>
        categorisationInfo.categoryAssessments(assessmentPage.index).category match {
          case 2
              if commodityCodeSansTrailingZeros(categorisationInfo.commodityCode).length <= 6 &&
                categorisationInfo.descendantCount != 0 =>
            routes.LongerCommodityCodeController.onPageLoad(CheckMode, recordId)
          case 2 if categorisationInfo.measurementUnit.isDefined =>
            routes.HasSupplementaryUnitController.onPageLoad(CheckMode, recordId)
          case _                                                 =>
            routes.CyaCategorisationController.onPageLoad(recordId)
        }
    }
  }.getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasSupplementaryUnitCheck(recordId: String)(answers: UserAnswers): Call       =
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
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())
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
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode  =>
      checkRouteMap(page)(userAnswers)
  }

  private def commodityCodeSansTrailingZeros(commodityCode: String): String =
    commodityCode.reverse.dropWhile(x => x == '0').reverse
}
