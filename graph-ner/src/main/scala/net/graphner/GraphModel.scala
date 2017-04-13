package net.graphner

import cc.factorie.Factorie.{Model, Parameters}
import cc.factorie.app.chain.Observation
import cc.factorie.model.Factor
import cc.factorie.variable._

import scala.reflect.ClassTag

/**
  * Created by mariana on 4/6/17.
  */
class GraphModel[Label <: MutableDiscreteVar, Features <: CategoricalVectorVar[String], Token <: Observation[Token]]
(
val labelDomain: CategoricalDomain[String],
val featuresDomain: CategoricalVectorDomain[String],
val labelToFeatures: Label => Features,
val labelToToken: Label => Token,
val tokenToLabel: Token => Label
) (implicit lm: ClassTag[Label], fm: ClassTag[Features], tm: ClassTag[Token])
extends Model with Parameters{
  override def factors(d: Diff): Iterable[Factor] = {
    super.factors(d)
  }

  override def factors(variables: Iterable[Var]): Iterable[Factor] = ???

}
