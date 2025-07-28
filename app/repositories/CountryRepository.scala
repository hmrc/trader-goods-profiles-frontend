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

import config.FrontendAppConfig
import models.{Country, CountryCodeCache}
import org.mongodb.scala.model.*
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CountryRepository @Inject() (
  appConfig: FrontendAppConfig,
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[CountryCodeCache](
      collectionName = "country-code-cache",
      mongoComponent = mongoComponent,
      domainFormat = CountryCodeCache.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(86400, TimeUnit.SECONDS)
        ),
        IndexModel(
          Indexes.ascending("key"),
          IndexOptions().name("keyIdx")
        )
      ),
      replaceIndexes = true
    ) {

  private val cacheKey = appConfig.countryCacheKey

  def get(): Future[Option[CountryCodeCache]] =
    collection.find(Filters.equal("key", cacheKey)).headOption()

  def set(countries: Seq[Country]): Future[Unit] = {
    val cache = CountryCodeCache(cacheKey, countries, Instant.now())
    collection
      .replaceOne(
        filter = Filters.equal("key", cacheKey),
        replacement = cache,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
  }
}
