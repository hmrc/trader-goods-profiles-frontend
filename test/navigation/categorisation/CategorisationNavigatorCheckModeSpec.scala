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

package navigation.categorisation

import base.SpecBase
import base.TestConstants.{testRecordId, userAnswersId}
import controllers.routes
import models._
import models.ott.{CategorisationInfo, CategoryAssessment}
import navigation.CategorisationNavigator
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages._
import pages.categorisation._
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery, LongerCommodityQuery}
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants
import utils.Constants.firstAssessmentNumber

import java.time.Instant

class CategorisationNavigatorCheckModeSpec extends SpecBase with BeforeAndAfterEach {

  private val categorisationService = mock[CategorisationService]

  private val navigator = new CategorisationNavigator(categorisationService)

  private val recordId    = "dummyRecordId"
  private val userAnswers = UserAnswers(recordId)

  "CategorisationNavigator" - {}
}
