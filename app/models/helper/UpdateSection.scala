package models.helper

sealed trait UpdateSection

case object CategorisationUpdate extends UpdateSection {
  override def toString: String = "categorisation"
}

case object GoodsDetailsUpdate extends UpdateSection {
  override def toString: String = "goodsDetails"
}