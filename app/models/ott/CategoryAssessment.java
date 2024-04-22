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
import models.ott.Exemption;
import models.ott.Measure;
import scala.collection.JavaConverters;
import scala.collection.immutable.List;

import java.util.ArrayList;
import java.util.Collection;

@JsonIgnoreProperties(ignoreUnknown = true)
@Type("category_assessment")
public class CategoryAssessment {
    @Id
    public String id;

    @Relationship("measures")
    private ArrayList<Measure> measures;

    @Relationship("exemptions")
    private ArrayList<Exemption> exemptions;


    public List<Measure> getMeasures() {
        return JavaConverters.asScalaBufferConverter(measures).asScala().toList();
    }

    public List<Exemption> getExemptions() {
        return JavaConverters.asScalaBuffer(exemptions).toList();
    }
}