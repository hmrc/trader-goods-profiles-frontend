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

package navigation.profile

import controllers.routes
import controllers.profile.{routes => profileRoutes}
import models._
import pages._
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import queries._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () {
  private val normalRoutes: Page => UserAnswers => Call = {
    case ProfileSetupPage           => navigateFromProfileSetUp
    case UseExistingUkimsNumberPage => navigateFromUseExistingUkimsNumber
    case UkimsNumberPage            => _ => profileRoutes.HasNirmsController.onPageLoadCreate(NormalMode)
    case HasNirmsPage               => navigateFromHasNirms
    case NirmsNumberPage            => _ => profileRoutes.HasNiphlController.onPageLoadCreate(NormalMode)
    case HasNiphlPage               => navigateFromHasNiphl
    case NiphlNumberPage            => _ => profileRoutes.CyaCreateProfileController.onPageLoad()
    case UkimsNumberUpdatePage      => _ => profileRoutes.CyaMaintainProfileController.onPageLoadUkimsNumber()
    case HasNirmsUpdatePage         => answers => navigateFromHasNirmsUpdate(answers, NormalMode)
    case NirmsNumberUpdatePage      => _ => profileRoutes.CyaMaintainProfileController.onPageLoadNirmsNumber()
    case RemoveNirmsPage            => navigateFromRemoveNirmsPage
    case HasNiphlUpdatePage         => userAnswers => navigateFromHasNiphlUpdate(userAnswers, NormalMode)
    case NiphlNumberUpdatePage      => _ => profileRoutes.CyaMaintainProfileController.onPageLoadNiphlNumber()
    case RemoveNiphlPage            => navigateFromRemoveNiphlPage
    case CyaMaintainProfilePage     => _ => profileRoutes.ProfileController.onPageLoad()
    case CyaCreateProfilePage       => _ => profileRoutes.CreateProfileSuccessController.onPageLoad()
    case _                          => _ => routes.IndexController.onPageLoad()
  }

  private def navigateFromUseExistingUkimsNumber(answers: UserAnswers): Call =
    answers
      .get(UseExistingUkimsNumberPage)
      .map {
        case true  => profileRoutes.HasNirmsController.onPageLoadCreate(NormalMode)
        case false => profileRoutes.UkimsNumberController.onPageLoadCreate(NormalMode)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNirms(answers: UserAnswers): Call =
    answers
      .get(HasNirmsPage)
      .map {
        case true  => profileRoutes.NirmsNumberController.onPageLoadCreate(NormalMode)
        case false => profileRoutes.HasNiphlController.onPageLoadCreate(NormalMode)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromRemoveNirmsPage(answers: UserAnswers): Call =
    answers
      .get(RemoveNirmsPage)
      .map {
        case false => profileRoutes.CyaMaintainProfileController.onPageLoadNirmsNumber()
        case true  => profileRoutes.CyaMaintainProfileController.onPageLoadNirms()
      }
      .getOrElse(profileRoutes.ProfileController.onPageLoad())

  private def navigateFromRemoveNiphlPage(answers: UserAnswers): Call =
    answers
      .get(RemoveNiphlPage)
      .map {
        case false => profileRoutes.CyaMaintainProfileController.onPageLoadNiphlNumber()
        case true  => profileRoutes.CyaMaintainProfileController.onPageLoadNiphl()
      }
      .getOrElse(profileRoutes.ProfileController.onPageLoad())

  private def navigateFromHasNirmsUpdate(answers: UserAnswers, mode: Mode): Call = {
    val continueUrl = RedirectUrl(profileRoutes.ProfileController.onPageLoad().url)
    answers
      .get(HasNirmsUpdatePage)
      .map {
        case true  => profileRoutes.NirmsNumberController.onPageLoadUpdate(mode)
        case false =>
          answers
            .get(TraderProfileQuery)
            .map { userProfile =>
              if (userProfile.nirmsNumber.isDefined) {
                profileRoutes.RemoveNirmsController.onPageLoad()
              } else {
                profileRoutes.CyaMaintainProfileController.onPageLoadNirms()
              }
            }
            .getOrElse(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasNiphl(answers: UserAnswers): Call =
    answers
      .get(HasNiphlPage)
      .map {
        case true  => profileRoutes.NiphlNumberController.onPageLoadCreate(NormalMode)
        case false => profileRoutes.CyaCreateProfileController.onPageLoad()
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNiphlUpdate(answers: UserAnswers, mode: Mode): Call = {
    val continueUrl = RedirectUrl(profileRoutes.ProfileController.onPageLoad().url)
    answers
      .get(HasNiphlUpdatePage)
      .map {
        case true  => profileRoutes.NiphlNumberController.onPageLoadUpdate(mode)
        case false =>
          answers
            .get(TraderProfileQuery)
            .map { userProfile =>
              if (userProfile.niphlNumber.isDefined) {
                profileRoutes.RemoveNiphlController.onPageLoad()
              } else {
                profileRoutes.CyaMaintainProfileController.onPageLoadNiphl()
              }
            }
            .getOrElse(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case CyaMaintainProfilePage => _ => profileRoutes.ProfileController.onPageLoad()
    case UkimsNumberPage        => _ => profileRoutes.CyaCreateProfileController.onPageLoad()
    case HasNirmsPage           => navigateFromHasNirmsCheck
    case NirmsNumberPage        => _ => profileRoutes.CyaCreateProfileController.onPageLoad()
    case NirmsNumberUpdatePage  => _ => profileRoutes.CyaMaintainProfileController.onPageLoadNirmsNumber()
    case RemoveNirmsPage        => navigateFromRemoveNirmsPage
    case HasNirmsUpdatePage     => answers => navigateFromHasNirmsUpdate(answers, CheckMode)
    case HasNiphlPage           => navigateFromHasNiphlCheck
    case NiphlNumberPage        => _ => profileRoutes.CyaCreateProfileController.onPageLoad()
    case HasNiphlUpdatePage     => answers => navigateFromHasNiphlUpdate(answers, CheckMode)
    case NiphlNumberUpdatePage  => _ => profileRoutes.CyaMaintainProfileController.onPageLoadNiphlNumber()
    case UkimsNumberUpdatePage  => _ => profileRoutes.CyaMaintainProfileController.onPageLoadUkimsNumber()
    case _                      => _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromHasNirmsCheck(answers: UserAnswers): Call =
    answers
      .get(HasNirmsPage)
      .map {
        case true  =>
          if (answers.isDefined(NirmsNumberPage)) {
            profileRoutes.CyaCreateProfileController.onPageLoad()
          } else {
            profileRoutes.NirmsNumberController.onPageLoadCreate(CheckMode)
          }
        case false => profileRoutes.CyaCreateProfileController.onPageLoad()
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasNiphlCheck(answers: UserAnswers): Call =
    answers
      .get(HasNiphlPage)
      .map {
        case true  =>
          if (answers.isDefined(NiphlNumberPage)) {
            profileRoutes.CyaCreateProfileController.onPageLoad()
          } else {
            profileRoutes.NiphlNumberController.onPageLoadCreate(CheckMode)
          }
        case false => profileRoutes.CyaCreateProfileController.onPageLoad()
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromProfileSetUp(answers: UserAnswers): Call =
    answers
      .get(HistoricProfileDataQuery) match {
      case Some(_) =>
        profileRoutes.UseExistingUkimsNumberController.onPageLoad()
      case None    => profileRoutes.UkimsNumberController.onPageLoadCreate(NormalMode)
    }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode  =>
      checkRouteMap(page)(userAnswers)
  }

  def journeyRecovery(continueUrl: Option[RedirectUrl] = None): Result = Redirect(
    routes.JourneyRecoveryController.onPageLoad(continueUrl)
  )
}
