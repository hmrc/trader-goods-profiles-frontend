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

import base.SpecBase
import base.TestConstants.userAnswersId
import controllers.routes
import models._
import org.scalatest.BeforeAndAfterEach
import pages._
import pages.profile._
import play.api.http.Status.SEE_OTHER
import queries._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

class ProfileNavigatorSpec extends SpecBase with BeforeAndAfterEach {

  private val navigator = new ProfileNavigator()

  "ProfileNavigator" - {

    "when in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, emptyUserAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "within the Create Profile Journey" - {

        "must go from ProfileSetupPage" - {

          "to UseExistingUkimsNumber when historic data" in {

            val userAnswers = emptyUserAnswers
              .set(
                HistoricProfileDataQuery,
                HistoricProfileData("GB123456789", "GB123456789", Some("XIUKIMS1234567890"), None, None)
              )
              .success
              .value

            navigator.nextPage(
              ProfileSetupPage,
              NormalMode,
              userAnswers
            ) mustBe controllers.profile.routes.UseExistingUkimsNumberController
              .onPageLoad()
          }

          "to UkimsNumberPage when no historic data" in {

            navigator.nextPage(
              ProfileSetupPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe controllers.profile.routes.UkimsNumberController
              .onPageLoadCreate(NormalMode)
          }
        }

        "must go from UkimsNumberPage to HasNirmsPage" in {

          navigator.nextPage(
            UkimsNumberPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.HasNirmsController
            .onPageLoadCreate(
              NormalMode
            )
        }

        "must go from UseExistingNirmsPage" - {

          "to HasNirmsPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(UseExistingUkimsNumberPage, true).success.value
            navigator.nextPage(
              UseExistingUkimsNumberPage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.HasNirmsController
              .onPageLoadCreate(
                NormalMode
              )
          }

          "to UkimsNumberController when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(UseExistingUkimsNumberPage, false).success.value
            navigator.nextPage(
              UseExistingUkimsNumberPage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.UkimsNumberController
              .onPageLoadCreate(
                NormalMode
              )
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              UseExistingUkimsNumberPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from HasNirmsPage" - {

          "to NirmsNumberPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, true).success.value
            navigator.nextPage(
              HasNirmsPage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.NirmsNumberController.onPageLoadCreate(
              NormalMode
            )
          }

          "to HasNiphlPage when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, false).success.value
            navigator.nextPage(HasNirmsPage, NormalMode, answers) mustBe controllers.profile.routes.HasNiphlController
              .onPageLoadCreate(
                NormalMode
              )
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNirmsPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NirmsNumberPage to HasNiphlPage" in {

          navigator.nextPage(
            NirmsNumberPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.HasNiphlController
            .onPageLoadCreate(
              NormalMode
            )
        }

        "must go from HasNiphlPage" - {

          "to NiphlNumberPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, true).success.value
            navigator.nextPage(
              HasNiphlPage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.NiphlNumberController.onPageLoadCreate(
              NormalMode
            )
          }

          "to CyaCreateProfile when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, false).success.value
            navigator.nextPage(
              HasNiphlPage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.CyaCreateProfileController.onPageLoad()
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNiphlPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NiphlNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            NiphlNumberPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.CyaCreateProfileController.onPageLoad()
        }

        "must go from CyaCreateProfile to CreateProfileSuccess" in {

          navigator.nextPage(
            CyaCreateProfilePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.CreateProfileSuccessController.onPageLoad()
        }
      }

      "within the Update Profile Journey" - {

        "must go from UkimsNumberUpdatePage to CyaMaintainProfilePage" in {

          navigator.nextPage(
            UkimsNumberUpdatePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadUkimsNumber()
        }

        "must go from HasNirmsUpdatePage" - {

          "to NirmsNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsUpdatePage, true).success.value
            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.NirmsNumberController.onPageLoadUpdate(NormalMode)
          }

          "to RemoveNirmsPage when answer is No and Nirms number is associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value
              .set(
                TraderProfileQuery,
                TraderProfile("actorId", "ukims", Some("nirms"), Some("niphl"), eoriChanged = false)
              )
              .success
              .value

            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.RemoveNirmsController
              .onPageLoad()
          }

          "to RemoveNirmsPage when answer is No and Nirms number is not associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value
              .set(TraderProfileQuery, TraderProfile("actorId", "ukims", None, Some("niphl"), eoriChanged = false))
              .success
              .value

            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(controllers.profile.routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(controllers.profile.routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              answers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }
        }

        "must go from RemoveNirmsPage" - {

          "to CyaMaintainProfile when user answered No" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNirmsPage, false).success.value

            navigator.nextPage(
              RemoveNirmsPage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }

          "to Cya NIRMS registered when user answered yes" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNirmsPage, true).success.value

            navigator.nextPage(
              RemoveNirmsPage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "to ProfilePage when answer is not present" in {

            navigator.nextPage(
              RemoveNirmsPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe controllers.profile.routes.ProfileController.onPageLoad()
          }
        }

        "must go from NirmsNumberUpdatePage to CyaMaintainProfile" in {

          navigator.nextPage(
            NirmsNumberUpdatePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
        }

        "must go from CyaMaintainProfilePage to CyaMaintainProfile" in {

          navigator.nextPage(
            CyaMaintainProfilePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.ProfileController.onPageLoad()
        }

        "must go from HasNiphlUpdatePage" - {

          "to NiphlNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlUpdatePage, true).success.value
            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.NiphlNumberController.onPageLoadUpdate(NormalMode)
          }

          "to RemoveNiphlPage when answer is No and Niphl number is associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value
              .set(
                TraderProfileQuery,
                TraderProfile("actorId", "ukims", Some("nirms"), Some("niphl"), eoriChanged = false)
              )
              .success
              .value

            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.RemoveNiphlController.onPageLoad()
          }

          "to RemoveNiphlPage when answer is No and Niphl number is not associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value
              .set(TraderProfileQuery, TraderProfile("actorId", "ukims", Some("nirms"), None, eoriChanged = false))
              .success
              .value

            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphl()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(controllers.profile.routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(controllers.profile.routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              answers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }
        }

        "must go from RemoveNiphlPage" - {
          "to Check Your Answers for Niphls Number when user answered No" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNiphlPage, false).success.value
            navigator.nextPage(
              RemoveNiphlPage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphlNumber()
          }

          "to Check Your Answers for NIPHL registered when user answered yes" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNiphlPage, true).success.value
            navigator.nextPage(
              RemoveNiphlPage,
              NormalMode,
              answers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphl()
          }

          "to ProfilePage when answer is not present" in {

            navigator.nextPage(
              RemoveNiphlPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe controllers.profile.routes.ProfileController.onPageLoad()
          }
        }

        "must go from NiphlNumberUpdatePage to ProfilePage" in {

          navigator.nextPage(
            NiphlNumberUpdatePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphlNumber()
        }

      }

      "within the new UKIMS number update journey" - {

        "must go from CyaNewUkimsNumberPage to ???" in {

          // TODO Needs to be updated according to navigation TGP-2700
          navigator.nextPage(
            CyaNewUkimsNumberPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.IndexController.onPageLoad()
        }
      }
    }

    "when in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          emptyUserAnswers
        ) mustBe controllers.problem.routes.JourneyRecoveryController.onPageLoad()
      }

      "within the  Create Profile Journey" - {
        "must go from UkimsNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            UkimsNumberPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.CyaCreateProfileController.onPageLoad()
        }

        "must go from HasNirmsPage" - {

          "when answer is Yes" - {

            "to NirmsNumberPage when NirmsNumberPage is empty" in {

              val answers = UserAnswers(userAnswersId).set(HasNirmsPage, true).success.value
              navigator.nextPage(
                HasNirmsPage,
                CheckMode,
                answers
              ) mustBe controllers.profile.routes.NirmsNumberController.onPageLoadCreate(
                CheckMode
              )
            }

            "to CyaCreateProfile when NirmsNumberPage is answered" in {

              val answers =
                UserAnswers(userAnswersId)
                  .set(HasNirmsPage, true)
                  .success
                  .value
                  .set(NirmsNumberPage, "1234")
                  .success
                  .value
              navigator.nextPage(
                HasNirmsPage,
                CheckMode,
                answers
              ) mustBe controllers.profile.routes.CyaCreateProfileController.onPageLoad()
            }
          }
          "to CyaCreateProfile when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, false).success.value
            navigator.nextPage(
              HasNirmsPage,
              CheckMode,
              answers
            ) mustBe controllers.profile.routes.CyaCreateProfileController.onPageLoad()
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNirmsPage,
              CheckMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NirmsNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            NirmsNumberPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.CyaCreateProfileController.onPageLoad()
        }

        "must go from HasNiphlPage" - {

          "when answer is Yes" - {

            "to NiphlNumberPage when NiphlNumberPage is empty" in {

              val answers = UserAnswers(userAnswersId).set(HasNiphlPage, true).success.value
              navigator.nextPage(
                HasNiphlPage,
                CheckMode,
                answers
              ) mustBe controllers.profile.routes.NiphlNumberController.onPageLoadCreate(
                CheckMode
              )
            }

            "to CyaCreateProfile when NiphlNumberPage is answered" in {

              val answers =
                UserAnswers(userAnswersId)
                  .set(HasNiphlPage, true)
                  .success
                  .value
                  .set(NiphlNumberPage, "1234")
                  .success
                  .value
              navigator.nextPage(
                HasNiphlPage,
                CheckMode,
                answers
              ) mustBe controllers.profile.routes.CyaCreateProfileController.onPageLoad()
            }
          }

          "to CyaCreateProfile when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, false).success.value
            navigator.nextPage(
              HasNiphlPage,
              CheckMode,
              answers
            ) mustBe controllers.profile.routes.CyaCreateProfileController.onPageLoad()
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNiphlPage,
              CheckMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NiphlNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            NiphlNumberPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.CyaCreateProfileController.onPageLoad()
        }
      }

      "within the Update Profile Journey" - {

        "must go from UkimsNumberUpdatePage to CyaMaintainProfilePage" in {

          navigator.nextPage(
            UkimsNumberUpdatePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadUkimsNumber()
        }

        "must go from RemoveNirmsPage" - {

          "to CyaMaintainProfile when user answered No and NimrsNumberUpdate is defined" in {

            val answers = UserAnswers(userAnswersId)
              .set(RemoveNirmsPage, false)
              .success
              .value
              .set(NirmsNumberUpdatePage, "some nirms")
              .success
              .value

            navigator.nextPage(
              RemoveNirmsPage,
              CheckMode,
              answers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }

          "to CyaMaintainProfile when user answered No" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNirmsPage, false).success.value

            navigator.nextPage(
              RemoveNirmsPage,
              CheckMode,
              answers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }

          "to Cya NIRMS registered when user answered yes" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNirmsPage, true).success.value

            navigator.nextPage(
              RemoveNirmsPage,
              CheckMode,
              answers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "must go from NirmsNumberUpdatePage to CyaMaintainProfile" in {

            navigator.nextPage(
              NirmsNumberUpdatePage,
              CheckMode,
              emptyUserAnswers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }
        }

        "must go from HasNirmsUpdatePage" - {

          "to NirmsNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsUpdatePage, true).success.value
            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              answers
            ) mustBe controllers.profile.routes.NirmsNumberController.onPageLoadUpdate(CheckMode)
          }

          "to RemoveNirmsPage when answer is No and Nirms number is associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value
              .set(
                TraderProfileQuery,
                TraderProfile("actorId", "ukims", Some("nirms"), Some("niphl"), eoriChanged = false)
              )
              .success
              .value
            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              answers
            ) mustBe controllers.profile.routes.RemoveNirmsController
              .onPageLoad()
          }

          "to RemoveNirmsPage when answer is No and Nirms number is not associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value
              .set(TraderProfileQuery, TraderProfile("actorId", "ukims", None, Some("niphl"), eoriChanged = false))
              .success
              .value

            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              answers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(controllers.profile.routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(controllers.profile.routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              answers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }
        }

        "must go from NirmsNumberUpdatePage to CyaMaintainProfile" in {

          navigator.nextPage(
            NirmsNumberUpdatePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
        }

        "must go from HasNiphlUpdatePage" - {

          "to NiphlNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlUpdatePage, true).success.value
            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              answers
            ) mustBe controllers.profile.routes.NiphlNumberController.onPageLoadUpdate(CheckMode)
          }

          "to RemoveNiphlPage when answer is No and Niphl number is associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value
              .set(
                TraderProfileQuery,
                TraderProfile("actorId", "ukims", Some("nirms"), Some("niphl"), eoriChanged = false)
              )
              .success
              .value

            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              answers
            ) mustBe controllers.profile.routes.RemoveNiphlController.onPageLoad()
          }

          "to RemoveNiphlPage when answer is No and Niphl number is not associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value
              .set(TraderProfileQuery, TraderProfile("actorId", "ukims", Some("nirms"), None, eoriChanged = false))
              .success
              .value

            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              answers
            ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphl()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(controllers.profile.routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(controllers.profile.routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              answers
            ) mustBe controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }
        }

        "must go from NiphlNumberUpdatePage to CyaMaintainProfile" in {

          navigator.nextPage(
            NiphlNumberUpdatePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.CyaMaintainProfileController.onPageLoadNiphlNumber()
        }

        "must go from CyaMaintainProfilePage to CyaMaintainProfile" in {

          navigator.nextPage(
            CyaMaintainProfilePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.profile.routes.ProfileController.onPageLoad()
        }
      }
    }

    ".journeyRecovery" - {

      "redirect to JourneyRecovery" - {

        "with no ContinueUrl if none supplied" in {
          val result = navigator.journeyRecovery()
          result.header.status mustEqual SEE_OTHER
          result.header
            .headers("Location") mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

        "with ContinueUrl if one supplied" in {
          val redirectUrl = Some(RedirectUrl("/redirectUrl"))
          val result      = navigator.journeyRecovery(redirectUrl)
          result.header.status mustEqual SEE_OTHER
          result.header.headers("Location") mustEqual controllers.problem.routes.JourneyRecoveryController
            .onPageLoad(redirectUrl)
            .url
        }
      }
    }
  }
}
