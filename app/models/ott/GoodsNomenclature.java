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

package models.ott;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import scala.collection.JavaConverters;
import scala.collection.immutable.List;

import java.util.ArrayList;


// The OTT API is still in development.
// We need @JsonIgnoreProperties in case we get data we don't expect.
@JsonIgnoreProperties(ignoreUnknown = true)
@Type("goods_nomenclature")
public class GoodsNomenclature {
    @Id
    public String id;
    public String goods_nomenclature_item_id;
    public String parent_sid;
    public String description;
    public Integer number_indents;
    public String productline_suffix;
    public String validity_start_date;
    public String validity_end_date;

    @Relationship("applicable_category_assessments")
    private ArrayList<CategoryAssessment> applicable_category_assessments;

    public List<CategoryAssessment> getCategoryAssessments() {
        return JavaConverters.asScalaBuffer(applicable_category_assessments).toList();
    }
}