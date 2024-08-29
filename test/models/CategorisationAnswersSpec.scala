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

package models

import base.SpecBase
import base.TestConstants.testRecordId
import models.ott.CategorisationInfo
import org.scalatest.Inside.inside
import pages._
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}

class CategorisationAnswersSpec extends SpecBase {

  ".build" - {

    "for initial assessment" - {

      "must return a CategorisationAnswer when" - {

        "a NoExemption means the following assessment pages are unanswered" in {
          val answers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
            .success
            .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustBe Right(
            CategorisationAnswers(
              Seq(Some(AssessmentAnswer.Exemption), Some(AssessmentAnswer.NoExemption), None),
              None
            )
          )
        }

        "all assessments are answered Yes" in {

          val answers =
            userAnswersForCategorisation

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustEqual
            Right(
              CategorisationAnswers(
                Seq(
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption)
                ),
                None
              )
            )

        }

        "all assessments are answered" in {

          val answers =
            userAnswersForCategorisation

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustEqual
            Right(
              CategorisationAnswers(
                Seq(
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption)
                ),
                None
              )
            )

        }

        "and supplementary unit was asked for and the answer was no" in {

          val answers =
            userAnswersForCategorisation
              .set(HasSupplementaryUnitPage(testRecordId), false)
              .success
              .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustEqual
            Right(
              CategorisationAnswers(
                Seq(
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption)
                ),
                None
              )
            )

        }

        "and supplementary unit was asked for and the answer was yes and it was supplied" in {

          val answers =
            userAnswersForCategorisation
              .set(HasSupplementaryUnitPage(testRecordId), true)
              .success
              .value
              .set(SupplementaryUnitPage(testRecordId), "42.0")
              .success
              .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustEqual
            Right(
              CategorisationAnswers(
                Seq(
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption)
                ),
                Some("42.0")
              )
            )
        }

        "all category 1 are answered and category 2 have no exemptions" in {

          val categoryQuery = CategorisationInfo(
            "1234567890",
            Some(validityEndDate),
            Seq(category1, category2, category3.copy(exemptions = Seq.empty)),
            Seq(category1, category2),
            Some("Weight, in kilograms"),
            0
          )

          val answers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categoryQuery)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustEqual Right(
            CategorisationAnswers(
              Seq(
                Some(AssessmentAnswer.Exemption),
                Some(AssessmentAnswer.Exemption)
              ),
              None
            )
          )
        }

        "there are no questions to be answered but supplementary unit is asked for and answer is no" in {
          val catInfo = CategorisationInfo(
            "1234567890",
            Seq(category2NoExemptions),
            Seq.empty,
            Some("kg"),
            0
          )

          val answers =
            emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), catInfo)
              .success
              .value
              .set(HasSupplementaryUnitPage(testRecordId), false)
              .success
              .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustEqual
            Right(
              CategorisationAnswers(
                Seq.empty,
                None
              )
            )

        }

        "there are no questions to be answered but supplementary unit is asked for and answer is yes" in {
          val catInfo = CategorisationInfo(
            "1234567890",
            Seq(category2NoExemptions),
            Seq.empty,
            Some("kg"),
            0
          )

          val answers =
            emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), catInfo)
              .success
              .value
              .set(HasSupplementaryUnitPage(testRecordId), true)
              .success
              .value
              .set(SupplementaryUnitPage(testRecordId), "312")
              .success
              .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustEqual
            Right(
              CategorisationAnswers(
                Seq.empty,
                Some("312")
              )
            )

        }

      }

      "must return errors" - {
        "when the user said they have a supplementary unit but it is missing" in {

          val answers =
            userAnswersForCategorisation
              .set(HasSupplementaryUnitPage(testRecordId), true)
              .success
              .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only PageMissing(SupplementaryUnitPage(testRecordId))
          }
        }

        "when the user has a supplementary unit without being asked about it " in {

          val answers =
            userAnswersForCategorisation
              .set(SupplementaryUnitPage(testRecordId), "42.0")
              .success
              .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only UnexpectedPage(SupplementaryUnitPage(testRecordId))
          }
        }

        "when no questions are answered" in {

          val answers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only MissingAssessmentAnswers(AssessmentPage(testRecordId, 0))
          }
        }

        "when additional assessments have been answered after a NoExemption" in {

          val answers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption)
            .success
            .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only UnexpectedNoExemption(AssessmentPage(testRecordId, 1))
          }
        }

        "when you have not finished answering assessments" in {

          val answers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only MissingAssessmentAnswers(CategorisationDetailsQuery(testRecordId))
          }
        }

        "when no answers for the record Id" in {

          val answers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value

          val result = CategorisationAnswers.build(answers, "differentId")

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only NoCategorisationDetailsForRecordId(
              CategorisationDetailsQuery("differentId"),
              "differentId"
            )
          }
        }

      }
    }

    "for longer commodity reassessment" - {

      val longerCommodityBaseAnswers = emptyUserAnswers
        .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
        .success
        .value
        .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
        .success
        .value
        .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
        .success
        .value
        .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
        .success
        .value
        .set(
          LongerCategorisationDetailsQuery(testRecordId),
          categorisationInfo.copy(commodityCode = "1234567890", longerCode = true)
        )
        .success
        .value

      "must return a CategorisationAnswer when" - {

        "a NoExemption means the following assessment pages are unanswered" in {
          val answers = longerCommodityBaseAnswers
            .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
            .success
            .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustBe Right(
            CategorisationAnswers(Seq(Some(AssessmentAnswer.NoExemption), None, None), None)
          )
        }

        "all assessments are answered Yes" in {

          val answers = longerCommodityBaseAnswers
            .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value
            .set(ReassessmentPage(testRecordId, 2), AssessmentAnswer.Exemption)
            .success
            .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustEqual
            Right(
              CategorisationAnswers(
                Seq(
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption)
                ),
                None
              )
            )

        }

        "all assessments are answered" in {

          val answers =
            longerCommodityBaseAnswers
              .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
              .success
              .value
              .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
              .success
              .value
              .set(ReassessmentPage(testRecordId, 2), AssessmentAnswer.Exemption)
              .success
              .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustEqual
            Right(
              CategorisationAnswers(
                Seq(
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption)
                ),
                None
              )
            )

        }

        "and supplementary unit was asked for and the answer was no" in {

          val answers =
            longerCommodityBaseAnswers
              .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
              .success
              .value
              .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
              .success
              .value
              .set(ReassessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
              .success
              .value
              .set(HasSupplementaryUnitPage(testRecordId), false)
              .success
              .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustEqual
            Right(
              CategorisationAnswers(
                Seq(
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.NoExemption)
                ),
                None
              )
            )

        }

        "and supplementary unit was asked for and the answer was yes and it was supplied" in {

          val answers =
            longerCommodityBaseAnswers
              .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
              .success
              .value
              .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
              .success
              .value
              .set(ReassessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
              .success
              .value
              .set(HasSupplementaryUnitPage(testRecordId), true)
              .success
              .value
              .set(SupplementaryUnitPage(testRecordId), "42.0")
              .success
              .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          result mustEqual
            Right(
              CategorisationAnswers(
                Seq(
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.Exemption),
                  Some(AssessmentAnswer.NoExemption)
                ),
                Some("42.0")
              )
            )
        }

      }

      "must return errors" - {
        "when the user said they have a supplementary unit but it is missing" in {

          val answers =
            longerCommodityBaseAnswers
              .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
              .success
              .value
              .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
              .success
              .value
              .set(ReassessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
              .success
              .value
              .set(HasSupplementaryUnitPage(testRecordId), true)
              .success
              .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only PageMissing(SupplementaryUnitPage(testRecordId))
          }
        }

        "when the user has a supplementary unit without being asked about it " in {

          val answers =
            longerCommodityBaseAnswers
              .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
              .success
              .value
              .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
              .success
              .value
              .set(ReassessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
              .success
              .value
              .set(SupplementaryUnitPage(testRecordId), "42.0")
              .success
              .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only UnexpectedPage(SupplementaryUnitPage(testRecordId))
          }
        }

        "when no questions are answered" in {

          val answers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
            .success
            .value
            .set(
              LongerCategorisationDetailsQuery(testRecordId),
              categorisationInfo.copy(commodityCode = "1234567890", longerCode = true)
            )
            .success
            .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only MissingAssessmentAnswers(ReassessmentPage(testRecordId, 0))
          }
        }

        "when additional assessments have been answered after a NoExemption" in {

          val answers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
            .success
            .value
            .set(
              LongerCategorisationDetailsQuery(testRecordId),
              categorisationInfo.copy(commodityCode = "1234567890", longerCode = true)
            )
            .success
            .value
            .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
            .success
            .value
            .set(ReassessmentPage(testRecordId, 2), AssessmentAnswer.Exemption)
            .success
            .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only UnexpectedNoExemption(ReassessmentPage(testRecordId, 1))
          }
        }

        "when you have not finished answering assessments" in {

          val answers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
            .success
            .value
            .set(
              LongerCategorisationDetailsQuery(testRecordId),
              categorisationInfo.copy(commodityCode = "1234567890", longerCode = true)
            )
            .success
            .value
            .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only MissingAssessmentAnswers(
              LongerCategorisationDetailsQuery(testRecordId)
            )
          }
        }

        "when you have unanswered reassessment questions" in {

          val answers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
            .success
            .value
            .set(
              LongerCategorisationDetailsQuery(testRecordId),
              categorisationInfo.copy(commodityCode = "1234567890", longerCode = true)
            )
            .success
            .value
            .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.NotAnsweredYet)
            .success
            .value
            .set(ReassessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
            .success
            .value

          val result = CategorisationAnswers.build(answers, testRecordId)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only MissingAssessmentAnswers(
              ReassessmentPage(testRecordId, 1)
            )
          }
        }

        "when no answers for the record Id" in {

          val answers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
            .success
            .value
            .set(
              LongerCategorisationDetailsQuery(testRecordId),
              categorisationInfo.copy(commodityCode = "1234567890", longerCode = true)
            )
            .success
            .value
            .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value
            .set(ReassessmentPage(testRecordId, 2), AssessmentAnswer.Exemption)
            .success
            .value

          val result = CategorisationAnswers.build(answers, "differentId")

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only NoCategorisationDetailsForRecordId(
              CategorisationDetailsQuery("differentId"),
              "differentId"
            )
          }
        }

      }
    }
  }

}
