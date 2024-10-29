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

import base.SpecBase
import base.TestConstants.userAnswersId
import models._
import org.scalatest.BeforeAndAfterEach
import pages._
import play.api.http.Status.SEE_OTHER
import queries._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import controllers.routes
import controllers.profile.{routes => profileRoutes}

class NavigatorSpec extends SpecBase with BeforeAndAfterEach {

  private val navigator = new Navigator()

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, emptyUserAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "in Create Profile Journey" - {

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
            ) mustBe profileRoutes.UseExistingUkimsNumberController
              .onPageLoad()
          }

          "to UkimsNumberPage when no historic data" in {

            navigator.nextPage(
              ProfileSetupPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe profileRoutes.UkimsNumberController
              .onPageLoadCreate(NormalMode)
          }
        }

        "must go from UkimsNumberPage to HasNirmsPage" in {

          navigator.nextPage(UkimsNumberPage, NormalMode, emptyUserAnswers) mustBe profileRoutes.HasNirmsController
            .onPageLoadCreate(
              NormalMode
            )
        }

        "must go from UseExistingNirmsPage" - {

          "to HasNirmsPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(UseExistingUkimsNumberPage, true).success.value
            navigator.nextPage(UseExistingUkimsNumberPage, NormalMode, answers) mustBe profileRoutes.HasNirmsController
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
            ) mustBe profileRoutes.UkimsNumberController
              .onPageLoadCreate(
                NormalMode
              )
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              UseExistingUkimsNumberPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from HasNirmsPage" - {

          "to NirmsNumberPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, true).success.value
            navigator.nextPage(HasNirmsPage, NormalMode, answers) mustBe profileRoutes.NirmsNumberController
              .onPageLoadCreate(
                NormalMode
              )
          }

          "to HasNiphlPage when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, false).success.value
            navigator.nextPage(HasNirmsPage, NormalMode, answers) mustBe profileRoutes.HasNiphlController
              .onPageLoadCreate(
                NormalMode
              )
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNirmsPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NirmsNumberPage to HasNiphlPage" in {

          navigator.nextPage(NirmsNumberPage, NormalMode, emptyUserAnswers) mustBe profileRoutes.HasNiphlController
            .onPageLoadCreate(
              NormalMode
            )
        }

        "must go from HasNiphlPage" - {

          "to NiphlNumberPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, true).success.value
            navigator.nextPage(HasNiphlPage, NormalMode, answers) mustBe profileRoutes.NiphlNumberController
              .onPageLoadCreate(
                NormalMode
              )
          }

          "to CyaCreateProfile when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, false).success.value
            navigator.nextPage(HasNiphlPage, NormalMode, answers) mustBe profileRoutes.CyaCreateProfileController
              .onPageLoad()
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNiphlPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NiphlNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            NiphlNumberPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe profileRoutes.CyaCreateProfileController.onPageLoad()
        }

        "must go from CyaCreateProfile to CreateProfileSuccess" in {

          navigator.nextPage(
            CyaCreateProfilePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe profileRoutes.CreateProfileSuccessController.onPageLoad()
        }
      }

      "in Update Profile Journey" - {

        "must go from UkimsNumberUpdatePage to CyaMaintainProfilePage" in {

          navigator.nextPage(
            UkimsNumberUpdatePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadUkimsNumber()
        }

        "must go from HasNirmsUpdatePage" - {

          "to NirmsNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsUpdatePage, true).success.value
            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              answers
            ) mustBe profileRoutes.NirmsNumberController.onPageLoadUpdate(NormalMode)
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

            navigator.nextPage(HasNirmsUpdatePage, NormalMode, answers) mustBe profileRoutes.RemoveNirmsController
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
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(profileRoutes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(profileRoutes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              answers
            ) mustBe routes.JourneyRecoveryController
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
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }

          "to Cya NIRMS registered when user answered yes" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNirmsPage, true).success.value

            navigator.nextPage(
              RemoveNirmsPage,
              NormalMode,
              answers
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "to ProfilePage when answer is not present" in {

            navigator.nextPage(
              RemoveNirmsPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe profileRoutes.ProfileController.onPageLoad()
          }
        }

        "must go from NirmsNumberUpdatePage to CyaMaintainProfile" in {

          navigator.nextPage(
            NirmsNumberUpdatePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNirmsNumber()
        }

        "must go from CyaMaintainProfilePage to CyaMaintainProfile" in {

          navigator.nextPage(
            CyaMaintainProfilePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe profileRoutes.ProfileController.onPageLoad()
        }

        "must go from HasNiphlUpdatePage" - {

          "to NiphlNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlUpdatePage, true).success.value
            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              answers
            ) mustBe profileRoutes.NiphlNumberController.onPageLoadUpdate(NormalMode)
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

            navigator.nextPage(HasNiphlUpdatePage, NormalMode, answers) mustBe profileRoutes.RemoveNiphlController
              .onPageLoad()
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
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNiphl()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(profileRoutes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(profileRoutes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              answers
            ) mustBe routes.JourneyRecoveryController
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
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNiphlNumber()
          }

          "to Check Your Answers for NIPHL registered when user answered yes" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNiphlPage, true).success.value
            navigator.nextPage(
              RemoveNiphlPage,
              NormalMode,
              answers
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNiphl()
          }

          "to ProfilePage when answer is not present" in {

            navigator.nextPage(
              RemoveNiphlPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe profileRoutes.ProfileController.onPageLoad()
          }
        }

        "must go from NiphlNumberUpdatePage to ProfilePage" in {

          navigator.nextPage(
            NiphlNumberUpdatePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNiphlNumber()
        }

      }

      "in the new UKIMS number update journey" - {

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

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          emptyUserAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "in Create Profile Journey" - {
        "must go from UkimsNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            UkimsNumberPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe profileRoutes.CyaCreateProfileController.onPageLoad()
        }

        "must go from HasNirmsPage" - {

          "when answer is Yes" - {

            "to NirmsNumberPage when NirmsNumberPage is empty" in {

              val answers = UserAnswers(userAnswersId).set(HasNirmsPage, true).success.value
              navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe profileRoutes.NirmsNumberController
                .onPageLoadCreate(
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
              navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe profileRoutes.CyaCreateProfileController
                .onPageLoad()
            }
          }
          "to CyaCreateProfile when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, false).success.value
            navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe profileRoutes.CyaCreateProfileController
              .onPageLoad()
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNirmsPage,
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NirmsNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            NirmsNumberPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe profileRoutes.CyaCreateProfileController.onPageLoad()
        }

        "must go from HasNiphlPage" - {

          "when answer is Yes" - {

            "to NiphlNumberPage when NiphlNumberPage is empty" in {

              val answers = UserAnswers(userAnswersId).set(HasNiphlPage, true).success.value
              navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe profileRoutes.NiphlNumberController
                .onPageLoadCreate(
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
              navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe profileRoutes.CyaCreateProfileController
                .onPageLoad()
            }
          }

          "to CyaCreateProfile when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, false).success.value
            navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe profileRoutes.CyaCreateProfileController
              .onPageLoad()
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNiphlPage,
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NiphlNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            NiphlNumberPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe profileRoutes.CyaCreateProfileController.onPageLoad()
        }
      }

      "in Update Profile Journey" - {

        "must go from UkimsNumberUpdatePage to CyaMaintainProfilePage" in {

          navigator.nextPage(
            UkimsNumberUpdatePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadUkimsNumber()
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
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }

          "to CyaMaintainProfile when user answered No" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNirmsPage, false).success.value

            navigator.nextPage(
              RemoveNirmsPage,
              CheckMode,
              answers
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }

          "to Cya NIRMS registered when user answered yes" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNirmsPage, true).success.value

            navigator.nextPage(
              RemoveNirmsPage,
              CheckMode,
              answers
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "must go from NirmsNumberUpdatePage to CyaMaintainProfile" in {

            navigator.nextPage(
              NirmsNumberUpdatePage,
              CheckMode,
              emptyUserAnswers
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }
        }

        "must go from HasNirmsUpdatePage" - {

          "to NirmsNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsUpdatePage, true).success.value
            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              answers
            ) mustBe profileRoutes.NirmsNumberController.onPageLoadUpdate(CheckMode)
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
            navigator.nextPage(HasNirmsUpdatePage, CheckMode, answers) mustBe profileRoutes.RemoveNirmsController
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
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(profileRoutes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(profileRoutes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              answers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }
        }

        "must go from NirmsNumberUpdatePage to CyaMaintainProfile" in {

          navigator.nextPage(
            NirmsNumberUpdatePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNirmsNumber()
        }

        "must go from HasNiphlUpdatePage" - {

          "to NiphlNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlUpdatePage, true).success.value
            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              answers
            ) mustBe profileRoutes.NiphlNumberController.onPageLoadUpdate(CheckMode)
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

            navigator.nextPage(HasNiphlUpdatePage, CheckMode, answers) mustBe profileRoutes.RemoveNiphlController
              .onPageLoad()
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
            ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNiphl()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(profileRoutes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(profileRoutes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              answers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }
        }

        "must go from NiphlNumberUpdatePage to CyaMaintainProfile" in {

          navigator.nextPage(
            NiphlNumberUpdatePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe profileRoutes.CyaMaintainProfileController.onPageLoadNiphlNumber()
        }

        "must go from CyaMaintainProfilePage to CyaMaintainProfile" in {

          navigator.nextPage(
            CyaMaintainProfilePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe profileRoutes.ProfileController.onPageLoad()
        }
      }

    }

    ".journeyRecovery" - {

      "redirect to JourneyRecovery" - {

        "with no ContinueUrl if none supplied" in {
          val result = navigator.journeyRecovery()
          result.header.status mustEqual SEE_OTHER
          result.header.headers("Location") mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }

        "with ContinueUrl if one supplied" in {
          val redirectUrl = Some(RedirectUrl("/redirectUrl"))
          val result      = navigator.journeyRecovery(redirectUrl)
          result.header.status mustEqual SEE_OTHER
          result.header.headers("Location") mustEqual routes.JourneyRecoveryController.onPageLoad(redirectUrl).url
        }
      }
    }
  }
}
