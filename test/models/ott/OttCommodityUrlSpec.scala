/*
 * Copyright 2025 HM Revenue & Customs
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

package models.ott

import base.SpecBase
import config.FrontendAppConfig
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.Helpers.running

class OttCommodityUrlSpec extends SpecBase {

  private implicit val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  "OttCommodityUrlSpec" - {
    "paddedCommodityCode" - {
      "must return padded commodityCode when 6 digit code input" in {

        val commodityCode: String = "123456"
        OttCommodityUrl(commodityCode).paddedCommodityCode mustBe "1234560000"

      }

      "must return padded commodityCode when 8 digit code input" in {

        val commodityCode: String = "12345678"
        OttCommodityUrl(commodityCode).paddedCommodityCode mustBe "1234567800"

      }

      "must not pad commodityCode when 10 digit code input" in {

        val commodityCode: String = "1234567891"
        OttCommodityUrl(commodityCode).paddedCommodityCode mustBe "1234567891"

      }
    }

    "link must return correct url with commodity code attached" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      val appConfig = application.injector.instanceOf[FrontendAppConfig]

      running(application) {

        OttCommodityUrl("123456")(
          appConfig
        ).link mustBe "https://www.trade-tariff.service.gov.uk/xi/commodities/1234560000"
      }

    }
  }
}
