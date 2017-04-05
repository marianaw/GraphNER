package net.graphner

import java.io.InputStream

import cc.factorie.app.nlp.{Document, DocumentAnnotator, Token}
import cc.factorie.app.nlp.ner.{NerLexiconFeatures, NerTag}
import cc.factorie.infer.BP
import cc.factorie.optimize.{AdaGrad, LikelihoodExample, Trainer}
import cc.factorie.variable.CategoricalDomain

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

  // The model:
  val model = ???

  // Process the document: here we perform inference with the model.
  override def process(document: Document): Document = ???  // TODO!!!

  def train(trainDocs: Seq[Document], testDocs: Seq[Document], rate: Double=0.2, delta: Double=0.07): Double = {
    val optimizer = new AdaGrad()
    val examples = trainDocs.map(sentence => new LikelihoodExample(sentence.tokens.map(_.attr[L]), model))  // FIXME
    Trainer.onlineTrain(model.parameters, examples, optimizer = optimizer)

    // TODO: evaluate here testDocs.
  }
}
