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

package utils

import base.TestConstants.{testEori, testRecordId}
import models.GoodsRecordsPagination
import models.router.responses.{Assessment, Condition, GetGoodsRecordResponse, GetRecordsResponse}
import play.api.libs.json.{JsValue, Json}

import java.time.Instant
import scala.language.postfixOps

trait GetRecordsResponseUtil {

  def getMultipleRecordResponseData: JsValue =
    Json.parse(s"""
         |{
         |"goodsItemRecords":
         |[
         |  {
         |    "eori": "$testEori",
         |    "actorId": "GB1234567890",
         |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
         |    "traderRef": "BAN001001",
         |    "comcode": "10410100",
         |    "adviceStatus": "Not requested",
         |    "goodsDescription": "Organic bananas",
         |    "countryOfOrigin": "EC",
         |    "category": 3,
         |    "assessments": [
         |      {
         |        "assessmentId": "abc123",
         |        "primaryCategory": 1,
         |        "condition": {
         |          "type": "abc123",
         |          "conditionId": "Y923",
         |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
         |          "conditionTraderText": "Excluded product"
         |        }
         |      }
         |    ],
         |    "supplementaryUnit": 500,
         |    "measurementUnit": "square meters(m^2)",
         |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
         |    "comcodeEffectiveToDate": "",
         |    "version": 1,
         |    "active": true,
         |    "toReview": false,
         |    "reviewReason": null,
         |    "declarable": "IMMI declarable",
         |    "ukimsNumber": "XIUKIM47699357400020231115081800",
         |    "nirmsNumber": "RMS-GB-123456",
         |    "niphlNumber": "6 S12345",
         |    "locked": false,
         |    "createdDateTime": "2024-11-18T23:20:19Z",
         |    "updatedDateTime": "2024-11-18T23:20:19Z"
         |  },
         |    {
         |    "eori": "$testEori",
         |    "actorId": "GB1234567890",
         |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
         |    "traderRef": "BAN001001",
         |    "comcode": "10410100",
         |    "adviceStatus": "Not requested",
         |    "goodsDescription": "Organic bananas",
         |    "countryOfOrigin": "EC",
         |    "category": 3,
         |    "assessments": [
         |      {
         |        "assessmentId": "abc123",
         |        "primaryCategory": 1,
         |        "condition": {
         |          "type": "abc123",
         |          "conditionId": "Y923",
         |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
         |          "conditionTraderText": "Excluded product"
         |        }
         |      }
         |    ],
         |    "supplementaryUnit": 500,
         |    "measurementUnit": "square meters(m^2)",
         |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
         |    "comcodeEffectiveToDate": "",
         |    "version": 1,
         |    "active": true,
         |    "toReview": false,
         |    "reviewReason": null,
         |    "declarable": "IMMI declarable",
         |    "ukimsNumber": "XIUKIM47699357400020231115081800",
         |    "nirmsNumber": "RMS-GB-123456",
         |    "niphlNumber": "6 S12345",
         |    "locked": false,
         |    "createdDateTime": "2024-11-18T23:20:19Z",
         |    "updatedDateTime": "2024-11-18T23:20:19Z"
         |  }
         |],
         |"pagination":
         | {
         |   "totalRecords": 2,
         |   "currentPage": 0,
         |   "totalPages": 1,
         |   "nextPage": null,
         |   "prevPage": null
         | }
         |}
         |""".stripMargin)

  def getEmptyResponseData: JsValue =
    Json.parse(s"""
                  |{
                  |"goodsItemRecords":
                  |[
                  |],
                  |"pagination":
                  | {
                  |   "totalRecords": 2,
                  |   "currentPage": 0,
                  |   "totalPages": 1,
                  |   "nextPage": null,
                  |   "prevPage": null
                  | }
                  |}
                  |""".stripMargin)

  val mockAssessment: Assessment = Assessment(
    assessmentId = Some("abc123"),
    primaryCategory = Some(1),
    condition = Some(
      Condition(
        `type` = Some("abc123"),
        conditionId = Some("Y923"),
        conditionDescription =
          Some("Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law"),
        conditionTraderText = Some("Excluded product")
      )
    )
  )

  val mockGoodsItemRecords: GetGoodsRecordResponse = GetGoodsRecordResponse(
    recordId = "c89e1d92-129e-47c3-aa37-3569f21133aa",
    traderRef = "BAN001001",
    commodityCode = "11063010",
    declarable = "Not requested",
    goodsDescription = "Organic bananas",
    countryOfOrigin = "EC",
    createdDateTime = Instant.parse("2024-10-12T16:12:34Z"),
    updatedDateTime = Instant.parse("2024-10-12T16:12:34Z")
  )

  val mockPagination: GoodsRecordsPagination = GoodsRecordsPagination(
    totalRecords = 15,
    currentPage = 1,
    totalPages = 2,
    nextPage = Some(0),
    prevPage = Some(0)
  )

  val mockGetRecordsResponse: GetRecordsResponse = GetRecordsResponse(
    goodsItemRecords = Seq(mockGoodsItemRecords),
    pagination = mockPagination
  )

  val mockGetRecordsResponseOption: Option[GetRecordsResponse] = Some(
    GetRecordsResponse(
      goodsItemRecords = Seq(mockGoodsItemRecords),
      pagination = mockPagination
    )
  )

  val mockGetRecordsEmpty: Option[GetRecordsResponse] = Some(
    GetRecordsResponse(
      goodsItemRecords = Seq.empty,
      pagination = mockPagination
    )
  )

}
