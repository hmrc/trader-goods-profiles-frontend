package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form

class CommodityCodeFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("commodityCode.error.required")
        .verifying(maxLength(100, "commodityCode.error.length"))
    )
}
