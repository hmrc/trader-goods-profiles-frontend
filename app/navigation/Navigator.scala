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
import pages._
import models._
import queries.RecordCategorisationsQuery
import utils.Constants.firstAssessmentIndex

import scala.util.Try

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => UserAnswers => Call = {
    case ProfileSetupPage            => _ => routes.UkimsNumberController.onPageLoad(NormalMode)
    case UkimsNumberPage             => _ => routes.HasNirmsController.onPageLoad(NormalMode)
    case HasNirmsPage                => navigateFromHasNirms
    case NirmsNumberPage             => _ => routes.HasNiphlController.onPageLoad(NormalMode)
    case HasNiphlPage                => navigateFromHasNiphl
    case NiphlNumberPage             => _ => routes.CyaCreateProfileController.onPageLoad
    case CreateRecordStartPage       => _ => routes.TraderReferenceController.onPageLoad(NormalMode)
    case TraderReferencePage         => _ => routes.UseTraderReferenceController.onPageLoad(NormalMode)
    case UseTraderReferencePage      => navigateFromUseTraderReference
    case GoodsDescriptionPage        => _ => routes.CountryOfOriginController.onPageLoad(NormalMode)
    case CountryOfOriginPage         => _ => routes.CommodityCodeController.onPageLoad(NormalMode)
    case CommodityCodePage           => _ => routes.HasCorrectGoodsController.onPageLoad(NormalMode)
    case HasCorrectGoodsPage         => navigateFromHasCorrectGoods
    case p: AssessmentPage           => navigateFromAssessment(p)
    case p: HasSupplementaryUnitPage => navigateFromHasSupplementaryUnit(p.recordId)
    case p: SupplementaryUnitPage    => _ => routes.CyaCategorisationController.onPageLoad(p.recordId)
    case p: AdviceStartPage          => _ => routes.NameController.onPageLoad(NormalMode, p.recordId)
    case p: NamePage                 => _ => routes.EmailController.onPageLoad(NormalMode, p.recordId)
    case p: EmailPage                => _ => routes.CyaRequestAdviceController.onPageLoad(p.recordId)
    case p: CategoryGuidancePage     =>
      _ => routes.AssessmentController.onPageLoad(NormalMode, p.recordId, firstAssessmentIndex)
    case p: CyaCategorisationPage    =>
      _ => routes.CategorisationResultController.onPageLoad(p.recordId, Scenario.getScenario(p.categoryRecord))
    case RemoveGoodsRecordPage       => _ => routes.GoodsRecordsController.onPageLoad(1)
    case _                           => _ => routes.IndexController.onPageLoad

  }

  private def navigateFromUseTraderReference(answers: UserAnswers): Call =
    answers
      .get(UseTraderReferencePage)
      .map {
        case false => routes.GoodsDescriptionController.onPageLoad(NormalMode)
        case true  => routes.CountryOfOriginController.onPageLoad(NormalMode)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoods(answers: UserAnswers): Call =
    answers
      .get(HasCorrectGoodsPage)
      .map {
        case true  => routes.CyaCreateRecordController.onPageLoad
        case false => routes.CommodityCodeController.onPageLoad(NormalMode)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNirms(answers: UserAnswers): Call =
    answers
      .get(HasNirmsPage)
      .map {
        case true  => routes.NirmsNumberController.onPageLoad(NormalMode)
        case false => routes.HasNiphlController.onPageLoad(NormalMode)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNiphl(answers: UserAnswers): Call =
    answers
      .get(HasNiphlPage)
      .map {
        case true  => routes.NiphlNumberController.onPageLoad(NormalMode)
        case false => routes.CyaCreateProfileController.onPageLoad
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasSupplementaryUnit(recordId: String)(answers: UserAnswers): Call =
    answers
      .get(HasSupplementaryUnitPage(recordId))
      .map {
        case true  => routes.SupplementaryUnitController.onPageLoad(NormalMode, recordId)
        case false => routes.CyaCategorisationController.onPageLoad(recordId)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromAssessment(assessmentPage: AssessmentPage)(answers: UserAnswers): Call = {
    val recordId = assessmentPage.recordId

    for {
      recordQuery      <- answers.get(RecordCategorisationsQuery)
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
        routes.CyaCategorisationController.onPageLoad(recordId)
    }
  }.getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private val checkRouteMap: Page => UserAnswers => Call = {
    case UkimsNumberPage             => _ => routes.CyaCreateProfileController.onPageLoad
    case HasNirmsPage                => navigateFromHasNirmsCheck
    case NirmsNumberPage             => _ => routes.CyaCreateProfileController.onPageLoad
    case HasNiphlPage                => navigateFromHasNiphlCheck
    case NiphlNumberPage             => _ => routes.CyaCreateProfileController.onPageLoad
    case TraderReferencePage         => _ => routes.CyaCreateRecordController.onPageLoad
    case UseTraderReferencePage      => navigateFromUseTraderReferenceCheck
    case GoodsDescriptionPage        => _ => routes.CyaCreateRecordController.onPageLoad
    case CountryOfOriginPage         => _ => routes.CyaCreateRecordController.onPageLoad
    case CommodityCodePage           => _ => routes.HasCorrectGoodsController.onPageLoad(CheckMode)
    case HasCorrectGoodsPage         => navigateFromHasCorrectGoodsCheck
    case p: NamePage                 => _ => routes.CyaRequestAdviceController.onPageLoad(p.recordId)
    case p: EmailPage                => _ => routes.CyaRequestAdviceController.onPageLoad(p.recordId)
    case p: AssessmentPage           => navigateFromAssessmentCheck(p)
    case p: HasSupplementaryUnitPage => navigateFromHasSupplementaryUnitCheck(p.recordId)
    case p: SupplementaryUnitPage    => _ => routes.CyaCategorisationController.onPageLoad(p.recordId)
    case _                           => _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromHasNirmsCheck(answers: UserAnswers): Call =
    answers
      .get(HasNirmsPage)
      .map {
        case true  =>
          if (answers.isDefined(NirmsNumberPage)) {
            routes.CyaCreateProfileController.onPageLoad
          } else {
            routes.NirmsNumberController.onPageLoad(CheckMode)
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
            routes.NiphlNumberController.onPageLoad(CheckMode)
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
            routes.GoodsDescriptionController.onPageLoad(CheckMode)
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
            routes.CommodityCodeController.onPageLoad(CheckMode)
          }
        case false => routes.CommodityCodeController.onPageLoad(CheckMode)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromAssessmentCheck(assessmentPage: AssessmentPage)(answers: UserAnswers): Call = {
    val recordId = assessmentPage.recordId

    for {
      recordQuery      <- answers.get(RecordCategorisationsQuery)
      assessmentAnswer <- answers.get(assessmentPage)
    } yield assessmentAnswer match {
      case AssessmentAnswer.Exemption(_) =>
        val assessmentCount = Try {
          recordQuery.records(recordId).categoryAssessments.size
        }.getOrElse(0)

        if (assessmentPage.index + 1 < assessmentCount) {
          if (answers.isDefined(AssessmentPage(recordId, assessmentPage.index + 1))) {
            routes.CyaCategorisationController.onPageLoad(recordId)
          } else {
            routes.AssessmentController.onPageLoad(CheckMode, recordId, assessmentPage.index + 1)
          }
        } else {
          routes.CyaCategorisationController.onPageLoad(recordId)
        }
      case AssessmentAnswer.NoExemption  =>
        routes.CyaCategorisationController.onPageLoad(recordId)
    }
  }.getOrElse(routes.JourneyRecoveryController.onPageLoad())

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
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode  =>
      checkRouteMap(page)(userAnswers)
  }
}
