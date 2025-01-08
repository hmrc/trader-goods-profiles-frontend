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
import models._
import pages._
import pages.profile._
import play.api.mvc.Call
import queries._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import controllers.profile.niphl.routes._
import controllers.profile.nirms.routes._
import controllers.profile.ukims.routes._
import pages.profile.niphl.{HasNiphlPage, HasNiphlUpdatePage, NiphlNumberPage, NiphlNumberUpdatePage, RemoveNiphlPage}
import pages.profile.nirms.{HasNirmsPage, HasNirmsUpdatePage, NirmsNumberPage, NirmsNumberUpdatePage, RemoveNirmsPage}
import pages.profile.ukims.{UkimsNumberPage, UkimsNumberUpdatePage, UseExistingUkimsNumberPage}

import javax.inject.{Inject, Singleton}

@Singleton
class ProfileNavigator @Inject() extends Navigator {

  val normalRoutes: Page => UserAnswers => Call = {
    case ProfileSetupPage           => navigateFromProfileSetUp
    case UseExistingUkimsNumberPage => navigateFromUseExistingUkimsNumber
    case UkimsNumberPage            => _ => HasNirmsController.onPageLoadCreate(NormalMode)
    case HasNirmsPage               => navigateFromHasNirms
    case NirmsNumberPage            => _ => HasNiphlController.onPageLoadCreate(NormalMode)
    case HasNiphlPage               => navigateFromHasNiphl
    case NiphlNumberPage            => _ => controllers.profile.routes.CyaCreateProfileController.onPageLoad()
    case UkimsNumberUpdatePage      => _ => controllers.profile.routes.CyaMaintainProfileController.onPageLoadUkimsNumber()
    case HasNirmsUpdatePage         => answers => navigateFromHasNirmsUpdate(answers, NormalMode)
    case NirmsNumberUpdatePage      => _ => controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
    case RemoveNirmsPage            => navigateFromRemoveNirmsPage
    case HasNiphlUpdatePage         => userAnswers => navigateFromHasNiphlUpdate(userAnswers, NormalMode)
    case NiphlNumberUpdatePage      => _ => controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphlNumber()
    case RemoveNiphlPage            => navigateFromRemoveNiphlPage
    case CyaMaintainProfilePage     => _ => controllers.profile.routes.ProfileController.onPageLoad()
    case CyaCreateProfilePage       => _ => controllers.profile.routes.CreateProfileSuccessController.onPageLoad()
    case _                          => _ => routes.IndexController.onPageLoad()
  }

  val checkRoutes: Page => UserAnswers => Call = {
    case CyaMaintainProfilePage => _ => controllers.profile.routes.ProfileController.onPageLoad()
    case UkimsNumberPage        => _ => controllers.profile.routes.CyaCreateProfileController.onPageLoad()
    case HasNirmsPage           => navigateFromHasNirmsCheck
    case NirmsNumberPage        => _ => controllers.profile.routes.CyaCreateProfileController.onPageLoad()
    case NirmsNumberUpdatePage  => _ => controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
    case RemoveNirmsPage        => navigateFromRemoveNirmsPage
    case HasNirmsUpdatePage     => answers => navigateFromHasNirmsUpdate(answers, CheckMode)
    case HasNiphlPage           => navigateFromHasNiphlCheck
    case NiphlNumberPage        => _ => controllers.profile.routes.CyaCreateProfileController.onPageLoad()
    case HasNiphlUpdatePage     => answers => navigateFromHasNiphlUpdate(answers, CheckMode)
    case NiphlNumberUpdatePage  => _ => controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphlNumber()
    case UkimsNumberUpdatePage  => _ => controllers.profile.routes.CyaMaintainProfileController.onPageLoadUkimsNumber()
    case _                      => _ => controllers.problem.routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromProfileSetUp(answers: UserAnswers): Call =
    answers
      .get(HistoricProfileDataQuery) match {
      case Some(_) =>
        UseExistingUkimsNumberController.onPageLoad()
      case None    => UkimsNumberController.onPageLoadCreate(NormalMode)
    }

  private def navigateFromUseExistingUkimsNumber(answers: UserAnswers): Call =
    answers
      .get(UseExistingUkimsNumberPage)
      .map {
        case true  => HasNirmsController.onPageLoadCreate(NormalMode)
        case false => UkimsNumberController.onPageLoadCreate(NormalMode)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNirms(answers: UserAnswers): Call =
    answers
      .get(HasNirmsPage)
      .map {
        case true  => NirmsNumberController.onPageLoadCreate(NormalMode)
        case false => HasNiphlController.onPageLoadCreate(NormalMode)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromRemoveNirmsPage(answers: UserAnswers): Call =
    answers
      .get(RemoveNirmsPage)
      .map {
        case false => controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
        case true  => controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirms()
      }
      .getOrElse(controllers.profile.routes.ProfileController.onPageLoad())

  private def navigateFromRemoveNiphlPage(answers: UserAnswers): Call =
    answers
      .get(RemoveNiphlPage)
      .map {
        case false => controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphlNumber()
        case true  => controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphl()
      }
      .getOrElse(controllers.profile.routes.ProfileController.onPageLoad())

  private def navigateFromHasNirmsUpdate(answers: UserAnswers, mode: Mode): Call = {
    val continueUrl = RedirectUrl(controllers.profile.routes.ProfileController.onPageLoad().url)
    answers
      .get(HasNirmsUpdatePage)
      .map {
        case true  => NirmsNumberController.onPageLoadUpdate(mode)
        case false =>
          answers
            .get(TraderProfileQuery)
            .map { userProfile =>
              if (userProfile.nirmsNumber.isDefined) {
                RemoveNirmsController.onPageLoad()
              } else {
                controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirms()
              }
            }
            .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasNiphl(answers: UserAnswers): Call =
    answers
      .get(HasNiphlPage)
      .map {
        case true  => NiphlNumberController.onPageLoadCreate(NormalMode)
        case false => controllers.profile.routes.CyaCreateProfileController.onPageLoad()
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNiphlUpdate(answers: UserAnswers, mode: Mode): Call = {
    val continueUrl = RedirectUrl(controllers.profile.routes.ProfileController.onPageLoad().url)
    answers
      .get(HasNiphlUpdatePage)
      .map {
        case true  => NiphlNumberController.onPageLoadUpdate(mode)
        case false =>
          answers
            .get(TraderProfileQuery)
            .map { userProfile =>
              if (userProfile.niphlNumber.isDefined) {
                RemoveNiphlController.onPageLoad()
              } else {
                controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphl()
              }
            }
            .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasNirmsCheck(answers: UserAnswers): Call =
    answers
      .get(HasNirmsPage)
      .map {
        case true  =>
          if (answers.isDefined(NirmsNumberPage)) {
            controllers.profile.routes.CyaCreateProfileController.onPageLoad()
          } else {
            NirmsNumberController.onPageLoadCreate(CheckMode)
          }
        case false => controllers.profile.routes.CyaCreateProfileController.onPageLoad()
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNiphlCheck(answers: UserAnswers): Call =
    answers
      .get(HasNiphlPage)
      .map {
        case true  =>
          if (answers.isDefined(NiphlNumberPage)) {
            controllers.profile.routes.CyaCreateProfileController.onPageLoad()
          } else {
            NiphlNumberController.onPageLoadCreate(CheckMode)
          }
        case false => controllers.profile.routes.CyaCreateProfileController.onPageLoad()
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

}
