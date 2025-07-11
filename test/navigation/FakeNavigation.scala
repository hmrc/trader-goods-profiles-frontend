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

import models.{Mode, UserAnswers}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.*
import play.api.mvc.Call
import services.CategorisationService

class FakeNavigator(desiredRoute: Call) extends Navigator {

  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call =
    desiredRoute

  override val normalRoutes: Page => UserAnswers => Call = _ => _ => desiredRoute
  override val checkRoutes: Page => UserAnswers => Call  = _ => _ => desiredRoute
}

class FakeNavigation(desiredRoute: Call) extends Navigation(mock[CategorisationService]) {
  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = desiredRoute
}

class FakeProfileNavigator(desiredRoute: Call) extends ProfileNavigator {
  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = desiredRoute
}

class FakeCategorisationNavigator(desiredRoute: Call) extends CategorisationNavigator(mock[CategorisationService]) {
  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = desiredRoute
}

class FakeGoodsProfileNavigator(desiredRoute: Call) extends GoodsProfileNavigator {
  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = desiredRoute
}

class FakeGoodsRecordNavigator(desiredRoute: Call) extends GoodsRecordNavigator {
  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = desiredRoute
}

class FakeDownloadNavigator(desiredRoute: Call) extends DownloadNavigator {
  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = desiredRoute
}

class FakeNewUkimsNavigator(desiredRoute: Call) extends NewUkimsNavigator {
  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = desiredRoute
}

class FakeAdviceNavigator(desiredRoute: Call) extends AdviceNavigator {
  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = desiredRoute
}
