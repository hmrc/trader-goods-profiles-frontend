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

package models.ott.response

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, JsValue, Json}

class OttResponseSpec extends AnyFreeSpec with Matchers {

  "must parse valid JSON" in {

    val applicableCategoryAssessments: Seq[String] =
      Seq(
        "238dbab8cc5026c67757c7e05751f312",
        "305033007ad8ddeb3b88ca5083f0c3d8",
        "35b83129617060579e5793c173d0ce68",
        "5af7b526d996cfab04d4eb0d181b0a4d"
      )
    val descendantCategoryAssessments: Seq[String] =
      Seq("a67daf0acd83c3c0d5f4aba690f20ec1", "9b9c6e21f30a4e137f016e6dfb9b939c")

    val categoryAssessments: Seq[String] = applicableCategoryAssessments ++ descendantCategoryAssessments

    val result = json.validate[OttResponse]
    result mustBe a[JsSuccess[_]]

    result.get.categoryAssessments.map { assessment =>
      categoryAssessments.contains(assessment.id) mustBe true
    }
  }

  private lazy val json: JsValue =
    Json.parse("""
        |{
        |  "data": {
        |    "id": "54267",
        |    "type": "goods_nomenclature",
        |    "attributes": {
        |      "goods_nomenclature_sid": 54267,
        |      "goods_nomenclature_item_id": "9306210000",
        |      "description": "Cartridges",
        |      "formatted_description": "Cartridges",
        |      "validity_start_date": "1972-01-01T00:00:00.000Z",
        |      "validity_end_date": null,
        |      "description_plain": "Cartridges",
        |      "producline_suffix": "80",
        |      "parent_sid": 54266,
        |      "supplementary_measure_unit": "1000 items (1000 p/st)"
        |    },
        |    "relationships": {
        |      "applicable_category_assessments": {
        |        "data": [
        |          {
        |            "id": "238dbab8cc5026c67757c7e05751f312",
        |            "type": "category_assessment"
        |          },
        |          {
        |            "id": "305033007ad8ddeb3b88ca5083f0c3d8",
        |            "type": "category_assessment"
        |          },
        |          {
        |            "id": "35b83129617060579e5793c173d0ce68",
        |            "type": "category_assessment"
        |          },
        |          {
        |            "id": "5af7b526d996cfab04d4eb0d181b0a4d",
        |            "type": "category_assessment"
        |          }
        |        ]
        |      },
        |      "descendant_category_assessments": {
        |        "data": [
        |          {
        |            "id": "a67daf0acd83c3c0d5f4aba690f20ec1",
        |            "type": "category_assessment"
        |          },
        |          {
        |            "id": "9b9c6e21f30a4e137f016e6dfb9b939c",
        |            "type": "category_assessment"
        |          }
        |        ]
        |      },
        |      "ancestors": {
        |        "data": [
        |          {
        |            "id": "54234",
        |            "type": "goods_nomenclature"
        |          },
        |          {
        |            "id": "54264",
        |            "type": "goods_nomenclature"
        |          },
        |          {
        |            "id": "54266",
        |            "type": "goods_nomenclature"
        |          }
        |        ]
        |      },
        |      "descendants": {
        |        "data": [
        |          {
        |            "id": "72785",
        |            "type": "goods_nomenclature"
        |          },
        |          {
        |            "id": "94337",
        |            "type": "goods_nomenclature"
        |          },
        |          {
        |            "id": "94338",
        |            "type": "goods_nomenclature"
        |          }
        |        ]
        |      }
        |    }
        |  },
        |  "included": [
        |    {
        |      "id": "R0312100",
        |      "type": "legal_act",
        |      "attributes": {
        |        "validity_start_date": "2003-05-23T00:00:00.000Z",
        |        "validity_end_date": null,
        |        "officialjournal_number": "L 169",
        |        "officialjournal_page": 6,
        |        "published_date": "2003-07-08",
        |        "regulation_code": "R1210/03",
        |        "regulation_url": "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D169,YEAR_OJ%3D2003,PAGE_FIRST%3D0006\u0026DB_COLL_OJ=oj-l\u0026type=advanced\u0026lang=en",
        |        "description": "mes. 465, 467 - IQ",
        |        "role": 1
        |      }
        |    },
        |    {
        |      "id": "465",
        |      "type": "measure_type",
        |      "attributes": {
        |        "description": "Restriction on entry into free circulation",
        |        "measure_type_series_id": "B",
        |        "measure_component_applicable_code": 2,
        |        "order_number_capture_code": 2,
        |        "trade_movement_code": 0,
        |        "validity_end_date": null,
        |        "validity_start_date": "1972-01-01T00:00:00.000Z",
        |        "id": "465",
        |        "measure_type_series_description": "Entry into free circulation or exportation subject to conditions"
        |      }
        |    },
        |    {
        |      "id": "IQ",
        |      "type": "geographical_area",
        |      "attributes": {
        |        "id": "IQ",
        |        "description": "Iraq",
        |        "geographical_area_id": "IQ",
        |        "geographical_area_sid": 269
        |      }
        |    },
        |    {
        |      "id": "8392",
        |      "type": "additional_code",
        |      "attributes": {
        |        "code": "4061",
        |        "description": "Arms, between 50 and 100 years old, other than those covered by the additional codes 4008, 4010, 4011, 4013, 4023, 4040 - 4048",
        |        "formatted_description": "Arms, between 50 and 100 years old, other than those covered by the additional codes 4008, 4010, 4011, 4013, 4023, 4040 - 4048"
        |      }
        |    },
        |    {
        |      "id": "TM570",
        |      "type": "footnote",
        |      "attributes": {
        |        "code": "TM570",
        |        "description": "The following shall be prohibited:\u003cbr/\u003ea) the import of or the introduction into the territory of the Community of, and\u003cbr/\u003eb) the dealing in, Iraqi cultural property and other items of archaeological, historical, cultural, rare scientific and religious importance, if they have been illegally removed from locations in Iraq, in particular, if:\u003cbr/\u003ei) the items form an integral part of either the public collections listed in the inventories of Iraqi museums, archives or libraries' conservation collection, or the inventories of Iraqi religious institutions, or\u003cbr/\u003eii) there exists reasonable suspicion that the goods have been removed from Iraq without the consent of their legitimate owner or have been removed in breach of Iraq's laws and regulations.\u003cbr/\u003eThese prohibitions shall not apply if it is shown that either:\u003cbr/\u003ea) the cultural items were exported from Iraq prior to 6 August 1990; or\u003cbr/\u003eb) the cultural items are being returned to Iraqi institutions in accordance with the objective of safe return as set out in paragraph 7 of UNSC Resolution 1483 (2003).",
        |        "formatted_description": "The following shall be prohibited:\u003cbr/\u003ea) the import of or the introduction into the territory of the Community of, and\u003cbr/\u003eb) the dealing in, Iraqi cultural property and other items of archaeological, historical, cultural, rare scientific and religious importance, if they have been illegally removed from locations in Iraq, in particular, if:\u003cbr/\u003ei) the items form an integral part of either the public collections listed in the inventories of Iraqi museums, archives or libraries' conservation collection, or the inventories of Iraqi religious institutions, or\u003cbr/\u003eii) there exists reasonable suspicion that the goods have been removed from Iraq without the consent of their legitimate owner or have been removed in breach of Iraq's laws and regulations.\u003cbr/\u003eThese prohibitions shall not apply if it is shown that either:\u003cbr/\u003ea) the cultural items were exported from Iraq prior to 6 August 1990; or\u003cbr/\u003eb) the cultural items are being returned to Iraqi institutions in accordance with the objective of safe return as set out in paragraph 7 of UNSC Resolution 1483 (2003)."
        |      }
        |    },
        |    {
        |      "id": "2524368",
        |      "type": "measure",
        |      "attributes": {
        |        "effective_start_date": "2003-05-23T00:00:00.000Z",
        |        "effective_end_date": null
        |      },
        |      "relationships": {
        |        "measure_type": {
        |          "data": {
        |            "id": "465",
        |            "type": "measure_type"
        |          }
        |        },
        |        "footnotes": {
        |          "data": [
        |            {
        |              "id": "TM570",
        |              "type": "footnote"
        |            }
        |          ]
        |        }
        |      }
        |    },
        |    {
        |      "id": "1.1",
        |      "type": "theme",
        |      "attributes": {
        |        "section": "1.1",
        |        "theme": "restrictive measures in force based on Article 215 Treaty on the Functioning of the European Union, insofar as they relate to trade in goods between the Union and third countries;",
        |        "category": 1
        |      }
        |    },
        |    {
        |      "id": "238dbab8cc5026c67757c7e05751f312",
        |      "type": "category_assessment",
        |      "relationships": {
        |        "exemptions": {
        |          "data": [
        |            {
        |              "id": "8392",
        |              "type": "additional_code"
        |            }
        |          ]
        |        },
        |        "theme": {
        |          "data": {
        |            "id": "1.1",
        |            "type": "theme"
        |          }
        |        },
        |        "geographical_area": {
        |          "data": {
        |            "id": "IQ",
        |            "type": "geographical_area"
        |          }
        |        },
        |        "excluded_geographical_areas": {
        |          "data": []
        |        },
        |        "measure_type": {
        |          "data": {
        |            "id": "465",
        |            "type": "measure_type"
        |          }
        |        },
        |        "regulation": {
        |          "data": {
        |            "id": "R0312100",
        |            "type": "legal_act"
        |          }
        |        },
        |        "measures": {
        |          "data": [
        |            {
        |              "id": "2524368",
        |              "type": "measure"
        |            }
        |          ]
        |        }
        |      }
        |    },
        |    {
        |      "id": "8440",
        |      "type": "additional_code",
        |      "attributes": {
        |        "code": "4099",
        |        "description": "Other than those mentioned in Regulation (EC) no 1210/2003 (OJ L 169): no restrictions",
        |        "formatted_description": "Other than those mentioned in Regulation (EC) no 1210/2003 (OJ L 169): no restrictions"
        |      }
        |    },
        |    {
        |      "id": "2526744",
        |      "type": "measure",
        |      "attributes": {
        |        "effective_start_date": "2003-05-23T00:00:00.000Z",
        |        "effective_end_date": null
        |      },
        |      "relationships": {
        |        "measure_type": {
        |          "data": {
        |            "id": "465",
        |            "type": "measure_type"
        |          }
        |        },
        |        "footnotes": {
        |          "data": []
        |        }
        |      }
        |    },
        |    {
        |      "id": "305033007ad8ddeb3b88ca5083f0c3d8",
        |      "type": "category_assessment",
        |      "relationships": {
        |        "exemptions": {
        |          "data": [
        |            {
        |              "id": "8440",
        |              "type": "additional_code"
        |            }
        |          ]
        |        },
        |        "theme": {
        |          "data": {
        |            "id": "1.1",
        |            "type": "theme"
        |          }
        |        },
        |        "geographical_area": {
        |          "data": {
        |            "id": "IQ",
        |            "type": "geographical_area"
        |          }
        |        },
        |        "excluded_geographical_areas": {
        |          "data": []
        |        },
        |        "measure_type": {
        |          "data": {
        |            "id": "465",
        |            "type": "measure_type"
        |          }
        |        },
        |        "regulation": {
        |          "data": {
        |            "id": "R0312100",
        |            "type": "legal_act"
        |          }
        |        },
        |        "measures": {
        |          "data": [
        |            {
        |              "id": "2526744",
        |              "type": "measure"
        |            }
        |          ]
        |        }
        |      }
        |    },
        |    {
        |      "id": "R1600440",
        |      "type": "legal_act",
        |      "attributes": {
        |        "validity_start_date": "2016-01-20T00:00:00.000Z",
        |        "validity_end_date": null,
        |        "officialjournal_number": "L 12",
        |        "officialjournal_page": 1,
        |        "published_date": "2016-01-19",
        |        "regulation_code": "R0044/16",
        |        "regulation_url": "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D12,YEAR_OJ%3D2016,PAGE_FIRST%3D0001\u0026DB_COLL_OJ=oj-l\u0026type=advanced\u0026lang=en",
        |        "description": "COUNCIL REGULATION (EU) 2016/44 -  concerning restrictive measures in view of the situation in Libya and repealing Regulation (EU) No 204/2011",
        |        "role": 1
        |      }
        |    },
        |    {
        |      "id": "711",
        |      "type": "measure_type",
        |      "attributes": {
        |        "description": "Import control on restricted goods and technologies",
        |        "measure_type_series_id": "B",
        |        "measure_component_applicable_code": 2,
        |        "order_number_capture_code": 2,
        |        "trade_movement_code": 0,
        |        "validity_end_date": null,
        |        "validity_start_date": "2008-02-10T00:00:00.000Z",
        |        "id": "711",
        |        "measure_type_series_description": "Entry into free circulation or exportation subject to conditions"
        |      }
        |    },
        |    {
        |      "id": "LY",
        |      "type": "geographical_area",
        |      "attributes": {
        |        "id": "LY",
        |        "description": "Libya",
        |        "geographical_area_id": "LY",
        |        "geographical_area_sid": 57
        |      }
        |    },
        |    {
        |      "id": "Y920",
        |      "type": "certificate",
        |      "attributes": {
        |        "code": "Y920",
        |        "certificate_type_code": "Y",
        |        "certificate_code": "920",
        |        "description": "Goods other than those described in the footnotes linked to the measure",
        |        "formatted_description": "Goods other than those described in the footnotes linked to the measure"
        |      }
        |    },
        |    {
        |      "id": "CD995",
        |      "type": "footnote",
        |      "attributes": {
        |        "code": "CD995",
        |        "description": "If the declared goods are described in the footnotes linked to the measure, export/import is not allowed.",
        |        "formatted_description": "If the declared goods are described in the footnotes linked to the measure, export/import is not allowed."
        |      }
        |    },
        |    {
        |      "id": "TM612",
        |      "type": "footnote",
        |      "attributes": {
        |        "code": "TM612",
        |        "description": "Ammunition specially designed for the firearms listed in 1.1 and specially designed components therefor (see the annex to the regulation containing the list of equipment which might be used for internal repression).",
        |        "formatted_description": "Ammunition specially designed for the firearms listed in 1.1 and specially designed components therefor (see the annex to the regulation containing the list of equipment which might be used for internal repression)."
        |      }
        |    },
        |    {
        |      "id": "3474616",
        |      "type": "measure",
        |      "attributes": {
        |        "effective_start_date": "2016-01-20T00:00:00.000Z",
        |        "effective_end_date": null
        |      },
        |      "relationships": {
        |        "measure_type": {
        |          "data": {
        |            "id": "711",
        |            "type": "measure_type"
        |          }
        |        },
        |        "footnotes": {
        |          "data": [
        |            {
        |              "id": "CD995",
        |              "type": "footnote"
        |            },
        |            {
        |              "id": "TM612",
        |              "type": "footnote"
        |            }
        |          ]
        |        }
        |      }
        |    },
        |    {
        |      "id": "35b83129617060579e5793c173d0ce68",
        |      "type": "category_assessment",
        |      "relationships": {
        |        "exemptions": {
        |          "data": [
        |            {
        |              "id": "Y920",
        |              "type": "certificate"
        |            }
        |          ]
        |        },
        |        "theme": {
        |          "data": {
        |            "id": "1.1",
        |            "type": "theme"
        |          }
        |        },
        |        "geographical_area": {
        |          "data": {
        |            "id": "LY",
        |            "type": "geographical_area"
        |          }
        |        },
        |        "excluded_geographical_areas": {
        |          "data": []
        |        },
        |        "measure_type": {
        |          "data": {
        |            "id": "711",
        |            "type": "measure_type"
        |          }
        |        },
        |        "regulation": {
        |          "data": {
        |            "id": "R1600440",
        |            "type": "legal_act"
        |          }
        |        },
        |        "measures": {
        |          "data": [
        |            {
        |              "id": "3474616",
        |              "type": "measure"
        |            }
        |          ]
        |        }
        |      }
        |    },
        |    {
        |      "id": "D1405120",
        |      "type": "legal_act",
        |      "attributes": {
        |        "validity_start_date": "2014-08-01T00:00:00.000Z",
        |        "validity_end_date": "2015-07-31T00:00:00.000Z",
        |        "officialjournal_number": "L 229",
        |        "officialjournal_page": 13,
        |        "published_date": "2014-07-31",
        |        "regulation_code": "D0512/14",
        |        "regulation_url": "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D229,YEAR_OJ%3D2014,PAGE_FIRST%3D0013\u0026DB_COLL_OJ=oj-l\u0026type=advanced\u0026lang=en",
        |        "description": "COUNCIL DECISION 2014/512/CFSP of 31 July 2014 concerning restrictive measures in view of Russia's actions destabilising the situation in Ukraine",
        |        "role": 1
        |      }
        |    },
        |    {
        |      "id": "707",
        |      "type": "measure_type",
        |      "attributes": {
        |        "description": "Import control",
        |        "measure_type_series_id": "B",
        |        "measure_component_applicable_code": 0,
        |        "order_number_capture_code": 2,
        |        "trade_movement_code": 0,
        |        "validity_end_date": null,
        |        "validity_start_date": "2017-02-01T00:00:00.000Z",
        |        "id": "707",
        |        "measure_type_series_description": "Entry into free circulation or exportation subject to conditions"
        |      }
        |    },
        |    {
        |      "id": "RU",
        |      "type": "geographical_area",
        |      "attributes": {
        |        "id": "RU",
        |        "description": "Russian Federation",
        |        "geographical_area_id": "RU",
        |        "geographical_area_sid": 199
        |      }
        |    },
        |    {
        |      "id": "TM839",
        |      "type": "footnote",
        |      "attributes": {
        |        "code": "TM839",
        |        "description": "The import, purchase or transport of arms and related materiel of all types, including weapons and ammunition, military vehicles and equipment, paramilitary equipment, and spare parts therefor, from Russia by nationals of Member States or using their flag vessels or aircraft, shall be prohibited (COUNCIL DECISION 2014/512/CFSP of 31 July 2014).\u003cbr/\u003eThe prohibitions shall be without prejudice to the execution of contracts or agreements concluded before 1 August 2014, and to the provision of spare parts and services necessary to the maintenance and safety of existing capabilities within the Union.\u003cbr/\u003e",
        |        "formatted_description": "The import, purchase or transport of arms and related materiel of all types, including weapons and ammunition, military vehicles and equipment, paramilitary equipment, and spare parts therefor, from Russia by nationals of Member States or using their flag vessels or aircraft, shall be prohibited (COUNCIL DECISION 2014/512/CFSP of 31 July 2014).\u003cbr/\u003eThe prohibitions shall be without prejudice to the execution of contracts or agreements concluded before 1 August 2014, and to the provision of spare parts and services necessary to the maintenance and safety of existing capabilities within the Union.\u003cbr/\u003e"
        |      }
        |    },
        |    {
        |      "id": "3562481",
        |      "type": "measure",
        |      "attributes": {
        |        "effective_start_date": "2017-03-31T00:00:00.000Z",
        |        "effective_end_date": "2024-07-31T00:00:00.000Z"
        |      },
        |      "relationships": {
        |        "measure_type": {
        |          "data": {
        |            "id": "707",
        |            "type": "measure_type"
        |          }
        |        },
        |        "footnotes": {
        |          "data": [
        |            {
        |              "id": "TM839",
        |              "type": "footnote"
        |            }
        |          ]
        |        }
        |      }
        |    },
        |    {
        |      "id": "5af7b526d996cfab04d4eb0d181b0a4d",
        |      "type": "category_assessment",
        |      "relationships": {
        |        "exemptions": {
        |          "data": [
        |            {
        |              "id": "Y920",
        |              "type": "certificate"
        |            }
        |          ]
        |        },
        |        "theme": {
        |          "data": {
        |            "id": "1.1",
        |            "type": "theme"
        |          }
        |        },
        |        "geographical_area": {
        |          "data": {
        |            "id": "RU",
        |            "type": "geographical_area"
        |          }
        |        },
        |        "excluded_geographical_areas": {
        |          "data": []
        |        },
        |        "measure_type": {
        |          "data": {
        |            "id": "707",
        |            "type": "measure_type"
        |          }
        |        },
        |        "regulation": {
        |          "data": {
        |            "id": "D1405120",
        |            "type": "legal_act"
        |          }
        |        },
        |        "measures": {
        |          "data": [
        |            {
        |              "id": "3562481",
        |              "type": "measure"
        |            }
        |          ]
        |        }
        |      }
        |    },
        |    {
        |      "id": "R1406920",
        |      "type": "legal_act",
        |      "attributes": {
        |        "validity_start_date": "2014-06-25T00:00:00.000Z",
        |        "validity_end_date": null,
        |        "officialjournal_number": "L 183",
        |        "officialjournal_page": 9,
        |        "published_date": "2014-06-24",
        |        "regulation_code": "R0692/14",
        |        "regulation_url": "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D183,YEAR_OJ%3D2014,PAGE_FIRST%3D0009\u0026DB_COLL_OJ=oj-l\u0026type=advanced\u0026lang=en",
        |        "description": "Council Regulation (EU) No 692/2014 of 23 June 2014 concerning restrictions on the import into the Union of goods originating in Crimea or Sevastopol, in response to the illegal annexation of Crimea and Sevastopol",
        |        "role": 1
        |      }
        |    },
        |    {
        |      "id": "760",
        |      "type": "measure_type",
        |      "attributes": {
        |        "description": "Import control",
        |        "measure_type_series_id": "B",
        |        "measure_component_applicable_code": 2,
        |        "order_number_capture_code": 2,
        |        "trade_movement_code": 0,
        |        "validity_end_date": null,
        |        "validity_start_date": "2014-02-01T00:00:00.000Z",
        |        "id": "760",
        |        "measure_type_series_description": "Entry into free circulation or exportation subject to conditions"
        |      }
        |    },
        |    {
        |      "id": "UA",
        |      "type": "geographical_area",
        |      "attributes": {
        |        "id": "UA",
        |        "description": "Ukraine",
        |        "geographical_area_id": "UA",
        |        "geographical_area_sid": 388
        |      }
        |    },
        |    {
        |      "id": "Y997",
        |      "type": "certificate",
        |      "attributes": {
        |        "code": "Y997",
        |        "certificate_type_code": "Y",
        |        "certificate_code": "997",
        |        "description": "Goods not originating from or destined to Crimea or Sevastopol (Articles 2 and 2b.1 of Council Regulation (EU) No 692/2014)",
        |        "formatted_description": "Goods not originating from or destined to Crimea or Sevastopol (Articles 2 and 2b.1 of Council Regulation (EU) No 692/2014)"
        |      }
        |    },
        |    {
        |      "id": "CD967",
        |      "type": "footnote",
        |      "attributes": {
        |        "code": "CD967",
        |        "description": "I. According to Council Regulation (EU) No 692/2014 (OJ L183, p. 9) it shall be prohibited to import into European Union goods originating in Crimea or Sevastopol. The prohibition shall not apply in respect of: (a) the execution until 26 September 2014, of trade contracts concluded before 25 June 2014, or of ancillary contracts necessary for the execution of such contracts, provided that the natural or legal persons, entity or body seeking to perform the contract have notified, at least 10 working days in advance, the activity or transaction to the competent authority of the Member State in which they are established. (b) goods originating in Crimea or Sevastopol which have been made available to the Ukrainian authorities for examination, for which compliance with the conditions conferring entitlement to preferential origin has been verified and for which a certificate of origin has been issued in accordance with Regulation (EU) No 978/2012 and Regulation (EU) No 374/2014 or in accordance with the EU-Ukraine Association Agreement. II. According to the Council Regulation (EU) No 1351/2014 (OJ L365, p. 46), the export of goods and technologies suited for use in the sectors of transport; telecommunications; energy; prospection, exploitation and production of oil, gas and mineral resources is prohibited: (a) to any natural or legal person, entity or body in Crimea or Sevastopol, or (b) for use in Crimea or Sevastopol. The prohibitions shall be without prejudice to the execution until 21 March 2015 of an obligation arising from a contract concluded before 20 December 2014, or by ancillary contracts necessary for the execution of such contracts, provided that the competent authority has been informed at least five working days in advance. When related to the use in Crimea or Sevastopol, the prohibitions do not apply where there are no reasonable grounds to determine that the goods and technology or the services are to be used in Crimea or Sevastopol.",
        |        "formatted_description": "I. According to Council Regulation (EU) No 692/2014 (OJ L183, p. 9) it shall be prohibited to import into European Union goods originating in Crimea or Sevastopol. The prohibition shall not apply in respect of: (a) the execution until 26 September 2014, of trade contracts concluded before 25 June 2014, or of ancillary contracts necessary for the execution of such contracts, provided that the natural or legal persons, entity or body seeking to perform the contract have notified, at least 10 working days in advance, the activity or transaction to the competent authority of the Member State in which they are established. (b) goods originating in Crimea or Sevastopol which have been made available to the Ukrainian authorities for examination, for which compliance with the conditions conferring entitlement to preferential origin has been verified and for which a certificate of origin has been issued in accordance with Regulation (EU) No 978/2012 and Regulation (EU) No 374/2014 or in accordance with the EU-Ukraine Association Agreement. II. According to the Council Regulation (EU) No 1351/2014 (OJ L365, p. 46), the export of goods and technologies suited for use in the sectors of transport; telecommunications; energy; prospection, exploitation and production of oil, gas and mineral resources is prohibited: (a) to any natural or legal person, entity or body in Crimea or Sevastopol, or (b) for use in Crimea or Sevastopol. The prohibitions shall be without prejudice to the execution until 21 March 2015 of an obligation arising from a contract concluded before 20 December 2014, or by ancillary contracts necessary for the execution of such contracts, provided that the competent authority has been informed at least five working days in advance. When related to the use in Crimea or Sevastopol, the prohibitions do not apply where there are no reasonable grounds to determine that the goods and technology or the services are to be used in Crimea or Sevastopol."
        |      }
        |    },
        |    {
        |      "id": "3946020",
        |      "type": "measure",
        |      "attributes": {
        |        "effective_start_date": "2022-12-01T00:00:00.000Z",
        |        "effective_end_date": null
        |      },
        |      "relationships": {
        |        "measure_type": {
        |          "data": {
        |            "id": "760",
        |            "type": "measure_type"
        |          }
        |        },
        |        "footnotes": {
        |          "data": [
        |            {
        |              "id": "CD967",
        |              "type": "footnote"
        |            }
        |          ]
        |        }
        |      }
        |    },
        |    {
        |      "id": "a67daf0acd83c3c0d5f4aba690f20ec1",
        |      "type": "category_assessment",
        |      "relationships": {
        |        "exemptions": {
        |          "data": [
        |            {
        |              "id": "Y997",
        |              "type": "certificate"
        |            }
        |          ]
        |        },
        |        "theme": {
        |          "data": {
        |            "id": "1.1",
        |            "type": "theme"
        |          }
        |        },
        |        "geographical_area": {
        |          "data": {
        |            "id": "UA",
        |            "type": "geographical_area"
        |          }
        |        },
        |        "excluded_geographical_areas": {
        |          "data": []
        |        },
        |        "measure_type": {
        |          "data": {
        |            "id": "760",
        |            "type": "measure_type"
        |          }
        |        },
        |        "regulation": {
        |          "data": {
        |            "id": "R1406920",
        |            "type": "legal_act"
        |          }
        |        },
        |        "measures": {
        |          "data": [
        |            {
        |              "id": "3946020",
        |              "type": "measure"
        |            }
        |          ]
        |        }
        |      }
        |    },
        |    {
        |      "id": "R2202630",
        |      "type": "legal_act",
        |      "attributes": {
        |        "validity_start_date": "2022-02-26T00:00:00.000Z",
        |        "validity_end_date": null,
        |        "officialjournal_number": "L 42I",
        |        "officialjournal_page": 77,
        |        "published_date": "2022-02-24",
        |        "regulation_code": "R0263/22",
        |        "regulation_url": "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D42I,YEAR_OJ%3D2022,PAGE_FIRST%3D0077\u0026DB_COLL_OJ=oj-l\u0026type=advanced\u0026lang=en",
        |        "description": "concerning restrictive measures in response to the recognition of the non-government controlled areas of the Donetsk and Luhansk oblasts of Ukraine and the ordering of Russian armed forces into those areas",
        |        "role": 1
        |      }
        |    },
        |    {
        |      "id": "762",
        |      "type": "measure_type",
        |      "attributes": {
        |        "description": "Import control",
        |        "measure_type_series_id": "B",
        |        "measure_component_applicable_code": 0,
        |        "order_number_capture_code": 2,
        |        "trade_movement_code": 0,
        |        "validity_end_date": null,
        |        "validity_start_date": "2022-02-26T00:00:00.000Z",
        |        "id": "762",
        |        "measure_type_series_description": "Entry into free circulation or exportation subject to conditions"
        |      }
        |    },
        |    {
        |      "id": "Y984",
        |      "type": "certificate",
        |      "attributes": {
        |        "code": "Y984",
        |        "certificate_type_code": "Y",
        |        "certificate_code": "984",
        |        "description": "Goods not originating from or not destined for the non-government controlled areas of Ukraine in the oblasts of Donetsk, Kherson, Luhansk and Zaporizhzhia",
        |        "formatted_description": "Goods not originating from or not destined for the non-government controlled areas of Ukraine in the oblasts of Donetsk, Kherson, Luhansk and Zaporizhzhia"
        |      }
        |    },
        |    {
        |      "id": "CD860",
        |      "type": "footnote",
        |      "attributes": {
        |        "code": "CD860",
        |        "description": "According to Council Regulation (EU) 2022/263 (OJ L42I, p. 77): I. It shall be prohibited to import into the European Union goods originating in non-government controlled areas of the Donetsk, Kherson, Luhansk and Zaporizhzhia oblasts of Ukraine. The import prohibitions not apply in respect of: (a) the execution until 24 May 2022 of trade contracts concluded before 23 February 2022, or of ancillary contracts necessary for the execution of such contracts, provided that the natural or legal person, entity or body seeking to perform the contract has notified, at least 10 working days in advance, the activity or transaction to the competent authority of the Member State in which they are established; (b) goods originating in the specified territories which have been made available to the Ukrainian authorities for examination, for which compliance with the conditions conferring entitlement to preferential origin has been verified and for which a certificate of origin has been issued in accordance with the EU-Ukraine Association Agreement. II. It shall be prohibited to sell, supply, transfer or export goods and technology listed in Annex II to Council Regulation (EU) 2022/263: (a) to any natural or legal person, entity or body in the specified territories, or (b) for use in the specified territories. Annex II shall include certain goods and technologies suited for use in the following key sectors: (i) transport; (ii) telecommunications; (iii) energy; (iv) the prospecting, exploration and production of oil, gas and mineral resources. The prohibitions in point II above shall be without prejudice to the execution until 24 August 2022 of an obligation arising from a contract concluded before 23 February 2022, or from ancillary contracts necessary for the execution of such contracts, provided that the competent authority has been informed at least five working days in advance.",
        |        "formatted_description": "According to Council Regulation (EU) 2022/263 (OJ L42I, p. 77): I. It shall be prohibited to import into the European Union goods originating in non-government controlled areas of the Donetsk, Kherson, Luhansk and Zaporizhzhia oblasts of Ukraine. The import prohibitions not apply in respect of: (a) the execution until 24 May 2022 of trade contracts concluded before 23 February 2022, or of ancillary contracts necessary for the execution of such contracts, provided that the natural or legal person, entity or body seeking to perform the contract has notified, at least 10 working days in advance, the activity or transaction to the competent authority of the Member State in which they are established; (b) goods originating in the specified territories which have been made available to the Ukrainian authorities for examination, for which compliance with the conditions conferring entitlement to preferential origin has been verified and for which a certificate of origin has been issued in accordance with the EU-Ukraine Association Agreement. II. It shall be prohibited to sell, supply, transfer or export goods and technology listed in Annex II to Council Regulation (EU) 2022/263: (a) to any natural or legal person, entity or body in the specified territories, or (b) for use in the specified territories. Annex II shall include certain goods and technologies suited for use in the following key sectors: (i) transport; (ii) telecommunications; (iii) energy; (iv) the prospecting, exploration and production of oil, gas and mineral resources. The prohibitions in point II above shall be without prejudice to the execution until 24 August 2022 of an obligation arising from a contract concluded before 23 February 2022, or from ancillary contracts necessary for the execution of such contracts, provided that the competent authority has been informed at least five working days in advance."
        |      }
        |    },
        |    {
        |      "id": "3929246",
        |      "type": "measure",
        |      "attributes": {
        |        "effective_start_date": "2022-08-06T00:00:00.000Z",
        |        "effective_end_date": null
        |      },
        |      "relationships": {
        |        "measure_type": {
        |          "data": {
        |            "id": "762",
        |            "type": "measure_type"
        |          }
        |        },
        |        "footnotes": {
        |          "data": [
        |            {
        |              "id": "CD860",
        |              "type": "footnote"
        |            }
        |          ]
        |        }
        |      }
        |    },
        |    {
        |      "id": "9b9c6e21f30a4e137f016e6dfb9b939c",
        |      "type": "category_assessment",
        |      "relationships": {
        |        "exemptions": {
        |          "data": [
        |            {
        |              "id": "Y984",
        |              "type": "certificate"
        |            }
        |          ]
        |        },
        |        "theme": {
        |          "data": {
        |            "id": "1.1",
        |            "type": "theme"
        |          }
        |        },
        |        "geographical_area": {
        |          "data": {
        |            "id": "UA",
        |            "type": "geographical_area"
        |          }
        |        },
        |        "excluded_geographical_areas": {
        |          "data": []
        |        },
        |        "measure_type": {
        |          "data": {
        |            "id": "762",
        |            "type": "measure_type"
        |          }
        |        },
        |        "regulation": {
        |          "data": {
        |            "id": "R2202630",
        |            "type": "legal_act"
        |          }
        |        },
        |        "measures": {
        |          "data": [
        |            {
        |              "id": "3929246",
        |              "type": "measure"
        |            }
        |          ]
        |        }
        |      }
        |    },
        |    {
        |      "id": "54234",
        |      "type": "goods_nomenclature",
        |      "attributes": {
        |        "goods_nomenclature_sid": 54234,
        |        "goods_nomenclature_item_id": "9300000000",
        |        "description": "ARMS AND AMMUNITION; PARTS AND ACCESSORIES THEREOF",
        |        "number_indents": 0,
        |        "producline_suffix": "80",
        |        "validity_start_date": "1971-12-31T00:00:00.000Z",
        |        "validity_end_date": null,
        |        "parent_sid": null
        |      }
        |    },
        |    {
        |      "id": "54264",
        |      "type": "goods_nomenclature",
        |      "attributes": {
        |        "goods_nomenclature_sid": 54264,
        |        "goods_nomenclature_item_id": "9306000000",
        |        "description": "Bombs, grenades, torpedoes, mines, missiles and similar munitions of war and parts thereof; cartridges and other ammunition and projectiles and parts thereof, including shot and cartridge wads",
        |        "number_indents": 0,
        |        "producline_suffix": "80",
        |        "validity_start_date": "1972-01-01T00:00:00.000Z",
        |        "validity_end_date": null,
        |        "parent_sid": 54234
        |      }
        |    },
        |    {
        |      "id": "54266",
        |      "type": "goods_nomenclature",
        |      "attributes": {
        |        "goods_nomenclature_sid": 54266,
        |        "goods_nomenclature_item_id": "9306210000",
        |        "description": "Shotgun cartridges and parts thereof; air gun pellets",
        |        "number_indents": 1,
        |        "producline_suffix": "10",
        |        "validity_start_date": "1972-01-01T00:00:00.000Z",
        |        "validity_end_date": null,
        |        "parent_sid": 54264
        |      }
        |    }
        |  ]
        |}
        |""".stripMargin)
}
