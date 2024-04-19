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

package models.ott.util;

import com.github.jasminb.jsonapi.ResourceConverter;
import models.*;
import models.ott.*;

import java.nio.charset.StandardCharsets;

public class JsonApiModelConverter {

    private static final ResourceConverter resourceConverter = new ResourceConverter(
            GoodsNomenclature.class,
            CategoryAssessment.class,
            Measure.class,
            MeasureType.class,
            Certificate.class,
            AdditionalCode.class,
            GeographicalArea.class,
            ReferencedGoodsNomenclature.class,
            CategoryAssessmentListing.class
    );

    public static GoodsNomenclature convert(String jsonString) {
        return resourceConverter.readDocument(jsonString.getBytes(StandardCharsets.UTF_8), GoodsNomenclature.class).get();
    }

}
