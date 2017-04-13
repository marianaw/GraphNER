package net.graphner

import java.io.InputStream

import cc.factorie.Factorie.BinaryFeatureVectorVariable
import cc.factorie.app.nlp.coref.CorefFeatures.True
import cc.factorie.app.nlp.{Document, DocumentAnnotator, Sentence, Token}
import cc.factorie.app.nlp.ner.{NerLexiconFeatures, NerTag}
import cc.factorie.infer.{BP, InferByBPLoopy}
import cc.factorie.optimize.{AdaGrad, LikelihoodExample, Trainer}
import cc.factorie.variable._

import scala.reflect.ClassTag

/**
  * Created by mariana on 4/1/17.
  */
abstract class GraphNER [L <: NerTag](val labelDomain: CategoricalDomain[String],
                                      val newLabel: (Token, String) => L,
                                      labelToToken: L => Token,
                                      modelIs: InputStream=null,
                                      nerLexiconFeatures: NerLexiconFeatures)(implicit m: ClassTag[L]) extends
  DocumentAnnotator{
  /*
  This class is an interface that process the documents, extract features, and trains a GraphModel.
   */
  // String representation of token's label:
  override def tokenAnnotationString(token: Token): String = token.attr[L].categoryValue

  // Sequence of class tags:
  val postAttrs = Seq(m.runtimeClass)

  // Sequence of Sentences.
  val prereqAttrs = Seq(classOf[Sentence])

  // Features:
  object GraphNerFeatureDomain extends CategoricalVectorDomain[String]
  class GraphNerFeatures(val token: Token) extends BinaryFeatureVectorVariable[String] {
    def domain = GraphNerFeatureDomain
    override def skipNonCategories: Boolean = true
  }

  // The model:
  class GraphNerModel[Features <: CategoricalVectorVar[String]: ClassTag](
                                                                         featuresDomain: CategoricalVectorDomain[String],
                                                                         labelToFeatures: L => Features,
                                                                         labelToToken: L => Token,
                                                                         tokenToLabel: Token => L
                                                                         ) extends GraphModel[L, Features, Token](
    labelDomain, featuresDomain, labelToFeatures, labelToToken, tokenToLabel
  )

  val model = new GraphNerModel[GraphNerFeatures](
    GraphNerFeatureDomain,
    l => labelToToken(l).attr[GraphNerFeatures],
    labelToToken,
    t => t.attr[L]
  )

  // Process the document: here we perform inference with the model.
  override def process(document: Document): Document = document

  def train(trainDocs: Seq[Document], testDocs: Seq[Document], rate: Double=0.2, delta: Double=0.07): Double = {
    val optimizer = new AdaGrad(rate = rate, delta = delta)

    def evaluate(): Unit ={
      trainDocs.foreach(d => {
        features(d, (t:Token) => t.attr[GraphNerFeatures])
      })
    }

    val examples =
      trainDocs.map(sentence => new LikelihoodExample(sentence.tokens.map(_.attr[L]), model, InferByBPLoopy))
    Trainer.onlineTrain(model.parameters, examples, optimizer = optimizer, evaluate = evaluate)

    // TODO: evaluate here testDocs.
    if (testDocs.nonEmpty){
      val variables = testDocs.flatMap(_.tokens.map(_.attr[L with LabeledMutableCategoricalVar[String]]))
      testDocs.foreach(d =>{
        features(d, (t:Token) => t.attr[GraphNerFeatures])
        BP.inferLoopyTreewiseMax(variables, model).setToMaximize(null)
      })
      HammingObjective.accuracy(variables = variables)
    }
    1.0 // Dummy return for now.
  }

  def features(document: Document, f: Token => CategoricalVectorVar[String]): Unit = {
    val tokenSequence = document.tokens.toIndexedSeq
    nerLexiconFeatures.addLexiconFeatures(tokenSequence, f)

    for (token <- document.tokens){
      val features = f(token)
      val word = token.string
      val lowerWord = word.toLowerCase()
      features += s"WORD=$lowerWord"
      if (token.isPunctuation) features += "PUNCTUATION"
      if (token.isCapitalized) features += "CAPITALIZED"
      if (token.isDigits) features += "DIGITS"
    }
  }

}
