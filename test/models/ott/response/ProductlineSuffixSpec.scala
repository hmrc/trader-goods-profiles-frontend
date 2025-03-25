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
import play.api.libs.json.Json

class ProductlineSuffixSpec extends AnyFreeSpec with Matchers {

  "must parse valid JSON" in {
    val expected = ProductlineSuffix("80")

    val result = Json.fromJson[ProductlineSuffix](json)

    result.get mustBe expected
  }

  private lazy val json = Json.parse(
    """
      |{
      |  "data": {
      |    "id": "106662",
      |    "type": "goods_nomenclature",
      |    "attributes": {
      |      "goods_nomenclature_item_id": "2404120000",
      |      "parent_sid": "106659",
      |      "description": "Other, containing nicotine",
      |      "number_indents": 2,
      |      "productline_suffix": "80",
      |      "validity_start_date": "2022-01-01T00:00:00.000Z",
      |      "validity_end_date": null,
      |      "supplementary_measure_unit": "1000 items (1000 p/st)"
      |    },
      |    "relationships": {
      |      "applicable_category_assessments": {
      |        "data": [
      |          {
      |            "id": "123456cd",
      |            "type": "category_assessment"
      |          }
      |        ]
      |      },
      |      "descendant_category_assessments": {
      |        "data": [
      |          {
      |            "id": "abcd1234",
      |            "type": "category_assessment"
      |          },
      |          {
      |            "id": "c5678def",
      |            "type": "category_assessment"
      |          }
      |        ]
      |      },
      |      "ancestors": {
      |        "data": [
      |          {
      |            "id": "35246",
      |            "type": "goods_nomenclature"
      |          },
      |          {
      |            "id": "106659",
      |            "type": "goods_nomenclature"
      |          }
      |        ]
      |      },
      |      "descendants": {
      |        "data": [
      |          {
      |            "id": "106676",
      |            "type": "goods_nomenclature"
      |          },
      |          {
      |            "id": "107156",
      |            "type": "goods_nomenclature"
      |          }
      |        ]
      |      },
      |      "licences": {
      |        "data": [
      |          {
      |            "id": "L100",
      |            "type": "certificate"
      |          }
      |        ]
      |      }
      |    }
      |  },
      |  "included": [
      |    {
      |      "id": "123456cd",
      |      "type": "category_assessments",
      |      "relationships": {
      |        "geographical_area": {
      |          "data": {
      |            "id": "1011",
      |            "type": "geographical_area"
      |          }
      |        },
      |        "excluded_geographical_areas": {
      |          "data": []
      |        },
      |        "theme": {
      |          "data": {
      |            "id": "1.1",
      |            "type": "theme"
      |          }
      |        },
      |        "exemptions": {
      |          "data": [
      |            {
      |              "id": "Y069",
      |              "type": "certificate"
      |            }
      |          ]
      |        },
      |        "measures": {
      |          "data": [
      |            {
      |              "id": "3871194",
      |              "type": "measure"
      |            }
      |          ]
      |        },
      |        "measure_type": {
      |          "data": {
      |            "id": "714",
      |            "type": "measure_type"
      |          }
      |        },
      |        "regulation": {
      |          "data": {
      |            "id": "D1908540",
      |            "type": "legal_act"
      |          }
      |        }
      |      }
      |    },
      |    {
      |      "id": "abcd1234",
      |      "type": "category_assessments",
      |      "relationships": {
      |        "geographical_area": {
      |          "data": {
      |            "id": "1011",
      |            "type": "geographical_area"
      |          }
      |        },
      |        "excluded_geographical_areas": {
      |          "data": []
      |        },
      |        "theme": {
      |          "data": {
      |            "id": "2.1",
      |            "type": "theme"
      |          }
      |        },
      |        "exemptions": {
      |          "data": [
      |            {
      |              "id": "L135",
      |              "type": "certificate"
      |            },
      |            {
      |              "id": "3200",
      |              "type": "additional_code"
      |            }
      |          ]
      |        },
      |        "measures": [
      |          {
      |            "data": [
      |              {
      |                "id": "3871192",
      |                "type": "measure"
      |              }
      |            ]
      |          },
      |          {
      |            "data": [
      |              {
      |                "id": "3871247",
      |                "type": "measure"
      |              }
      |            ]
      |          }
      |        ],
      |        "measure_type": {
      |          "data": {
      |            "id": "475",
      |            "type": "measure_type"
      |          }
      |        },
      |        "regulation": {
      |          "data": {
      |            "id": "D1908540",
      |            "type": "legal_act"
      |          }
      |        }
      |      }
      |    },
      |    {
      |      "id": "c5678def",
      |      "type": "category_assessment",
      |      "relationships": {
      |        "geographical_area": {
      |          "data": {
      |            "id": "1011",
      |            "type": "geographical_area"
      |          }
      |        },
      |        "excluded_geographical_areas": {
      |          "data": []
      |        },
      |        "theme": {
      |          "data": {
      |            "id": "2.1",
      |            "type": "theme"
      |          }
      |        },
      |        "exemptions": [
      |          {
      |            "id": "3200",
      |            "type": "additional_code"
      |          },
      |          {
      |            "id": "3249",
      |            "type": "additional_code"
      |          }
      |        ],
      |        "measures": [
      |          {
      |            "data": [
      |              {
      |                "id": "3871193",
      |                "type": "measure"
      |              },
      |              {
      |                "id": "3871248",
      |                "type": "measure"
      |              }
      |            ]
      |          }
      |        ],
      |        "measure_type": {
      |          "data": {
      |            "id": "475",
      |            "type": "measure_type"
      |          }
      |        },
      |        "regulation": {
      |          "data": {
      |            "id": "D1908540",
      |            "type": "legal_act"
      |          }
      |        }
      |      }
      |    },
      |    {
      |      "id": "1011",
      |      "type": "geographical_area",
      |      "attributes": {
      |        "description": "ERGA OMNES",
      |        "geographical_area_id": "1011"
      |      }
      |    },
      |    {
      |      "id": "1.1",
      |      "type": "theme",
      |      "attributes": {
      |        "theme": "Sanctions",
      |        "category": 1
      |      }
      |    },
      |    {
      |      "id": "2.1",
      |      "type": "theme",
      |      "attributes": {
      |        "theme": "Drug precursors",
      |        "category": 2
      |      }
      |    },
      |    {
      |      "id": "L135",
      |      "type": "certificate",
      |      "attributes": {
      |        "certificate_type_code": "L",
      |        "certificate_code": "135",
      |        "code": "L135",
      |        "description": "Import authorisation (precursors) issued by the competent authorities of the Member State where the importer is established",
      |        "formatted_description": "Import authorisation (precursors) issued by the competent authorities of the Member State where the importer is established"
      |      }
      |    },
      |    {
      |      "id": "3200",
      |      "type": "additional_code",
      |      "attributes": {
      |        "additional_code_type_id": "3",
      |        "additional_code": "200",
      |        "code": "3200",
      |        "description": "Mixtures of scheduled substances listed in the Annex to Regulation (EC) No 111/2005 that can be used for the illicit manufacture of narcotic drugs or psychotropic substances",
      |        "formatted_description": "Mixtures of scheduled substances listed in the Annex to Regulation (EC) No 111/2005 that can be used for the illicit manufacture of narcotic drugs or psychotropic substances"
      |      }
      |    },
      |    {
      |      "id": "Y069",
      |      "type": "certificate",
      |      "attributes": {
      |        "certificate_type_code": "Y",
      |        "certificate_code": "069",
      |        "code": "Y069",
      |        "description": "Goods not consigned from Iran",
      |        "formatted_description": "Goods not consigned from Iran"
      |      }
      |    },
      |    {
      |      "id": "3871194",
      |      "type": "measure",
      |      "attributes": {
      |        "goods_nomenclature_item_id": "2404120000",
      |        "goods_nomenclature_sid": "106662",
      |        "effective_start_date": "2022-01-01T00:00:00.000Z",
      |        "effective_end_date": null
      |      }
      |    },
      |    {
      |      "id": "3871192",
      |      "type": "measure",
      |      "attributes": {
      |        "goods_nomenclature_item_id": "2404120010",
      |        "goods_nomenclature_sid": "106676",
      |        "effective_start_date": "2022-01-01T00:00:00.000Z",
      |        "effective_end_date": null
      |      },
      |      "relationships": {
      |        "footnotes": {
      |          "data": [
      |            {
      |              "id": "CD437",
      |              "type": "footnote"
      |            }
      |          ]
      |        }
      |      }
      |    },
      |    {
      |      "id": "3871193",
      |      "type": "measure",
      |      "attributes": {
      |        "goods_nomenclature_item_id": "2404120010",
      |        "goods_nomenclature_sid": "106676",
      |        "effective_start_date": "2022-01-01T00:00:00.000Z",
      |        "effective_end_date": null
      |      },
      |      "relationships": {
      |        "footnotes": {
      |          "data": [
      |            {
      |              "id": "TM135",
      |              "type": "footnote"
      |            }
      |          ]
      |        }
      |      }
      |    },
      |    {
      |      "id": "3871247",
      |      "type": "measure",
      |      "attributes": {
      |        "goods_nomenclature_item_id": "2404120090",
      |        "goods_nomenclature_sid": "107156",
      |        "effective_start_date": "2022-01-01T00:00:00.000Z",
      |        "effective_end_date": null
      |      },
      |      "relationships": {
      |        "footnotes": {
      |          "data": [
      |            {
      |              "id": "CD437",
      |              "type": "footnote"
      |            },
      |            {
      |              "id": "TM135",
      |              "type": "footnote"
      |            }
      |          ]
      |        }
      |      }
      |    },
      |    {
      |      "id": "3871248",
      |      "type": "measure",
      |      "attributes": {
      |        "effective_start_date": "2022-01-01T00:00:00.000Z",
      |        "effective_end_date": null
      |      },
      |      "relationships": {
      |        "footnotes": {
      |          "data": [
      |            {
      |              "id": "TM135",
      |              "type": "footnote"
      |            }
      |          ]
      |        }
      |      }
      |    },
      |    {
      |      "id": "35246",
      |      "type": "goods_nomenclature",
      |      "attributes": {
      |        "goods_nomenclature_item_id": "2400000000",
      |        "parent_sid": null,
      |        "description": "Tobacco and manufactured tobacco substitutes; products, whether or not containing nicotine, intended for inhalation without combustion; other nicotine containing products intended for the intake of nicotine into the human body",
      |        "number_indents": 0,
      |        "productline_suffix": "80",
      |        "validity_start_date": "1971-12-31T00:00:00.000Z",
      |        "validity_end_date": null,
      |        "supplementary_measure_unit": null
      |      },
      |      "relationships": {
      |        "licences": {
      |          "data": []
      |        }
      |      }
      |    },
      |    {
      |      "id": "106659",
      |      "type": "goods_nomenclature",
      |      "attributes": {
      |        "goods_nomenclature_item_id": "2404000000",
      |        "parent_sid": 35246,
      |        "description": "Products containing tobacco, reconstituted tobacco, nicotine, or tobacco or nicotine substitutes, intended for inhalation without combustion; other nicotine containing products intended for the intake of nicotine into the human body",
      |        "number_indents": 0,
      |        "productline_suffix": "80",
      |        "validity_start_date": "2022-01-01T00:00:00.000Z",
      |        "validity_end_date": null,
      |        "supplementary_measure_unit": null
      |      },
      |      "relationships": {
      |        "licences": {
      |          "data": []
      |        }
      |      }
      |    },
      |    {
      |      "id": "106676",
      |      "type": "goods_nomenclature",
      |      "attributes": {
      |        "goods_nomenclature_item_id": "2404120010",
      |        "parent_sid": 106662,
      |        "description": "Cartridges and refills, filled, for electronic cigarettes, preparations for use in cartridges and refills for electronic cigarettes",
      |        "number_indents": 3,
      |        "productline_suffix": "80",
      |        "validity_start_date": "2022-01-01T00:00:00.000Z",
      |        "validity_end_date": null,
      |        "supplementary_measure_unit": "1000 items (1000 p/st)"
      |      },
      |      "relationships": {
      |        "licences": {
      |          "data": []
      |        }
      |      }
      |    },
      |    {
      |      "id": "107156",
      |      "type": "goods_nomenclature",
      |      "attributes": {
      |        "goods_nomenclature_item_id": "2404120090",
      |        "parent_sid": 106662,
      |        "description": "Other",
      |        "number_indents": 3,
      |        "productline_suffix": "80",
      |        "validity_start_date": "2022-01-01T00:00:00.000Z",
      |        "validity_end_date": null,
      |        "supplementary_measure_unit": "1000 items (1000 p/st)"
      |      },
      |      "relationships": {
      |        "licences": {
      |          "data": []
      |        }
      |      }
      |    },
      |    {
      |      "id": "475",
      |      "type": "measure_type",
      |      "attributes": {
      |        "description": "Restriction on entry into free circulation",
      |        "measure_type_series_description": "Entry into free circulation or exportation subject to conditions",
      |        "validity_start_date": "1972-01-01T00:00:00.000Z",
      |        "validity_end_date": null,
      |        "measure_type_series_id": "B",
      |        "trade_movement_code": 0
      |      }
      |    },
      |    {
      |      "id": "714",
      |      "type": "measure_type",
      |      "attributes": {
      |        "description": "Import control",
      |        "measure_type_series_description": "Entry into free circulation or exportation subject to conditions",
      |        "validity_start_date": "2017-02-01 00:00:00.000",
      |        "validity_end_date": null,
      |        "measure_type_series_id": "B",
      |        "trade_movement_code": 0
      |      }
      |    },
      |    {
      |      "id": "CD437",
      |      "type": "footnote",
      |      "attributes": {
      |        "code": "CD437",
      |        "description": "\"Import authorization\" and \"Specific import requirements\" - see Articles 20-25 of Regulation (EC) No 111/05 (OJ L 22) implemented by Regulation (EC) No 2015/1011 (OJ L 162)."
      |      }
      |    },
      |    {
      |      "id": "TM135",
      |      "type": "footnote",
      |      "attributes": {
      |        "code": "TM135",
      |        "description": "The surveillance does not apply to mixtures and natural products which contain scheduled substances and which are compounded in such a way that the scheduled substances cannot be easily used or extracted by readily applicable or economically viable means, to medicinal products as defined in point 2 of Article 1 of Directive 2001/83/EC of the European Parliament and of the Council and to veterinary medicinal products as defined in point 2 of Article 1 of Directive 2001/82/EC of the European Parliament and of the Council."
      |      }
      |    },
      |    {
      |      "id": "L100",
      |      "type": "certificate",
      |      "attributes": {
      |        "certificate_type_code": "L",
      |        "certificate_code": "100",
      |        "code": "L100",
      |        "description": "Import licence \"controlled substances\" (ozone), issued by the Commission",
      |        "formatted_description": "Import licence \"controlled substances\" (ozone), issued by the Commission"
      |      }
      |    },
      |    {
      |      "id": "D1908540",
      |      "type": "legal_act",
      |      "attributes": {
      |        "validity_start_date": "2019-09-20T00:00:00.000Z",
      |        "validity_end_date": null,
      |        "officialjournal_number": "L 147",
      |        "officialjournal_page": 1,
      |        "published_date": "2019-06-05",
      |        "regulation_code": "D0854/19",
      |        "regulation_url": "http://eur-lex.europa.eu/search.html?whOJ=NO_OJ%3D147,YEAR_OJ%3D2019,PAGE_FIRST%3D0001&DB_COLL_OJ=oj-l&type=advanced&lang=en",
      |        "description": null
      |      }
      |    }
      |  ]
      |}
      |""".stripMargin
  )
}
