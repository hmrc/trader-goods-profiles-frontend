package generators

import org.scalacheck.Gen

trait NiphlsNumberGenerator extends Generators {

  def niphlsAlphaNumericGenerator(letterCount: Int, numberCount: Int): Gen[String] = {

    val letter = Gen.listOfN(letterCount, Gen.alphaChar).map(_.mkString)
    val numbers = Gen.listOfN(numberCount, Gen.numChar).map(_.mkString)

    for {
      letter <- letter
      numbers <- numbers
    } yield s"$letter$numbers"
  }

  def niphlsNumericGenerator(min: Int, max:Int): Gen[String] = {
    intsInRange(min, max)
  }
}
