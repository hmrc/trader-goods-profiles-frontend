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

package repositories

import base.TestConstants.userAnswersId
import config.FrontendAppConfig
import models.UserAnswers
import models.helper.{CategorisationJourney, CreateProfileJourney, CreateRecordJourney, RequestAdviceJourney, SupplementaryUnitUpdateJourney}
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, ZoneId}
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class SessionRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[UserAnswers]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant          = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val userAnswers = UserAnswers(userAnswersId, Json.obj("foo" -> "bar"), Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1

  protected override val repository = new SessionRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      val expectedResult = userAnswers copy (lastUpdated = instant)

      val setResult     = repository.set(userAnswers).futureValue
      val updatedRecord = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual expectedResult
    }
  }

  ".get" - {

    "when there is a record for this id" - {

      "must update the lastUpdated time and get the record" in {

        insert(userAnswers).futureValue

        val result         = repository.get(userAnswers.id).futureValue
        val expectedResult = userAnswers copy (lastUpdated = instant)

        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.get("id that does not exist").futureValue must not be defined
      }
    }
  }

  ".clear" - {

    "must remove a record" in {

      insert(userAnswers).futureValue

      val result = repository.clear(userAnswers.id).futureValue

      result mustEqual true
      repository.get(userAnswers.id).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist").futureValue

      result mustEqual true
    }
  }

  ".clearData" - {

    "must clear data for CreateProfileJourney" in {
      val journey = CreateProfileJourney
      insert(userAnswers).futureValue

      val result = repository.clearData(userAnswers.id, journey).futureValue

      result mustEqual true
      val updatedRecord = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value
      val expectedJson  = Json.obj("foo" -> "bar")
      updatedRecord.data mustEqual expectedJson
    }

    "must clear data for CreateRecordJourney" in {
      val journey = CreateRecordJourney
      insert(userAnswers).futureValue

      val result = repository.clearData(userAnswers.id, journey).futureValue

      result mustEqual true
      val updatedRecord = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value
      val expectedJson  = Json.obj("foo" -> "bar")
      updatedRecord.data mustEqual expectedJson
    }

    "must clear data for CategorisationJourney" in {
      val journey = CategorisationJourney
      insert(userAnswers).futureValue

      val result = repository.clearData(userAnswers.id, journey).futureValue

      result mustEqual true
      val updatedRecord = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value
      val expectedJson  = Json.obj("foo" -> "bar")
      updatedRecord.data mustEqual expectedJson
    }

    "must clear data for RequestAdviceJourney" in {
      val journey = RequestAdviceJourney
      insert(userAnswers).futureValue

      val result = repository.clearData(userAnswers.id, journey).futureValue

      result mustEqual true
      val updatedRecord = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value
      val expectedJson  = Json.obj("foo" -> "bar")
      updatedRecord.data mustEqual expectedJson
    }

    "must clear data for SupplementaryUnitUpdateJourney" in {
      val journey = SupplementaryUnitUpdateJourney
      insert(userAnswers).futureValue

      val result = repository.clearData(userAnswers.id, journey).futureValue

      result mustEqual true
      val updatedRecord = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value
      val expectedJson  = Json.obj("foo" -> "bar")
      updatedRecord.data mustEqual expectedJson
    }

    "must return true when there is no data to clear for any journey" in {
      val journey = CreateProfileJourney
      val result  = repository.clearData("nonexistent-id", journey).futureValue

      result mustEqual true
    }
  }

  ".keepAlive" - {

    "when there is a record for this id" - {

      "must update its lastUpdated to `now` and return true" in {

        insert(userAnswers).futureValue

        val result = repository.keepAlive(userAnswers.id).futureValue

        val expectedUpdatedAnswers = userAnswers copy (lastUpdated = instant)

        result mustEqual true
        val updatedAnswers = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value
        updatedAnswers mustEqual expectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {

      "must return true" in {

        repository.keepAlive("id that does not exist").futureValue mustEqual true
      }
    }
  }
}
