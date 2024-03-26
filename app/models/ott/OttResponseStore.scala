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

package models.ott

import play.api.libs.json._

class OttResponseStore(json: JsValue) {

  val models = (json \ "data").as[OttDataModel] +: (json \ "included").as[Seq[OttDataModel]]

  def getRoot: OttDataModel = models(0)
  def getIncluded: Seq[OttDataModel] = models.slice(1, models.length)
  def getById(id: String): OttDataModel = getIncluded.find(_.id == id).get

  def getApplicableCategoryAssessments: Seq[OttDataModel] = {
    getRoot
      .relationships.get
      .get("applicable_category_assessments").get.asInstanceOf[JsObject].value
      .get("data").get.asInstanceOf[JsArray].value.toSeq
      .map {modelJson => modelJson.as[OttDataModel]}
      .map(ref => getById(ref.id))
  }

  def getMeasuresForAssessment(assesment: OttDataModel): Seq[OttDataModel] = {
    assesment.relationships
      .get("measures").as[JsObject].value
      .get("data").get.as[JsArray].as[Seq[OttDataModel]]
      .map(model => getById(model.id))
  }

}
