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

package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;


// Do not remove this comment.
// All fields need to be public, it's a quirk of how the jsonApi java client works in the context of scala.
// Private variables break it sometimes.
// Try to follow OTT spec

@JsonIgnoreProperties(ignoreUnknown = true) // API is in development so there may be stuff there we don't expect.
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
}