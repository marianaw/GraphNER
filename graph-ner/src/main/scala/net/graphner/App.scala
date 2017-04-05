package net.graphner

import java.io._

import scala.io.Source
import cc.factorie._
import cc.factorie.app.nlp._
import cc.factorie.app.nlp.lexicon.StaticLexicons
import cc.factorie.app.nlp.ner.{LabeledBioConllNerTag, NerLexiconFeatures, NerTag, StaticLexiconFeatures}
import cc.factorie.la
import cc.factorie.optimize._
import cc.factorie.util._
import cc.factorie.variable.EnumDomain

import scala.collection.mutable.ArrayBuffer

/**
 * @author Mariana Vargas Vieyra
  * Advisor: Pablo Duboue
  * Co-advisor: Oscar Bustos
 */


object SentenceGraphNerTrainer extends HyperparameterMain{
  override def evaluateParameters(args: Array[String]): Double = {
    val opts = new GraphNerOpts
    opts.parse(args)

    val ner =
      new TweetGraphNER()(ModelProvider.empty,
        new StaticLexiconFeatures(new StaticLexicons(), "en")
      )

    val (trainDocs, testDocs) = if(opts.train.wasInvoked && opts.test.wasInvoked){
      ner.loadDocs(opts.train.value.getAbsolutePath) ->
        ner.loadDocs(opts.test.value.getAbsolutePath)
    } else{
      throw new IllegalArgumentException("You must provide --train and --test arguments.")
    }

    val ret = ner.train(trainDocs, testDocs, opts.learningRate.value, opts.delta.value)
    ret
  }
}

object SentenceGraphNER {
  def main(args : Array[String]) {
    val opts = new GraphNerOpts
    opts.parse(args)

    // Retrieve rate and delta from command line to AdaGrad:
    val rate = HyperParameter(opts.learningRate, new LogUniformDoubleSampler(1e-3, 1.0))
    val delta = HyperParameter(opts.delta, new LogUniformDoubleSampler(0.01, 0.1))

    // Create an executor:
    val exec = new QSubExecutor(10, "net.graphner.SentenceGraphNerTrainer")

    // Tuning hyperparameters:
    println("Tuning hyperparameters.")
    val optimizer = new HyperParameterSearcher(opts, Seq(rate, delta), exec.execute, 100, 90, 60)
    val result = optimizer.optimize()

    // Fitting model:
    println("Fitting...")
    exec.execute(opts.values.flatMap(_.unParse).toArray)
    println("Ready!")
  }
}


class GraphNerOpts extends cc.factorie.util.CmdOptions with SharedNLPCmdOptions
  with ModelProviderCmdOptions with DefaultCmdOptions{
  val saveModel = new CmdOption("save-model", "SentenceGraphNER.md", "FILE",
    "Filename where the model is already or will be saved.")
  val train = new CmdOption("train", new File(""), "File",
    "Filename with training set.")
  val test = new CmdOption("test", new File(""), "File",
    "Filename where the test data is.")
  val learningRate = new CmdOption("learning-rate", 0.15, "DOUBLE", "The learning rate for AdaGrad.")
  val delta = new CmdOption("delta", 0.06, "DOUBLE", "Learning delta for AdaGrad.")
}


object TwitterNerDomain extends EnumDomain {
  val O, PER, ORG, LOC, MISC = Value
  freeze()
}


class TwitterNerTag(token:Token, initialCategory:String) extends NerTag(token, initialCategory) with Serializable {
  def domain = TwitterNerDomain
}


class TweetGraphNER()(implicit mp: ModelProvider[Nothing], nerLexiconFeatures: NerLexiconFeatures)
extends GraphNER[TwitterNerTag](
  TwitterNerDomain,
  (t, s) => new TwitterNerTag(t, s),
  l => l.token,
  mp.provide,
  nerLexiconFeatures) with Serializable{

  def loadDocs(fileName: String): Seq[Document] = {
    val documents = new ArrayBuffer[Document]
    var document = new Document("").setName("TwitterDocs_" + documents.length + "_NER")
    var sentence = new Sentence(document)

    // Read the file and load a list of documents.
    for (line <- Source.fromFile(fileName).getLines()) {

      sentence = new Sentence(document)

      if (line.split(' ').length == 0) {

        // Add the document to our list of documents, but first get its content as a string:
        document.asSection.chainFreeze()
        documents += document

        // Create a fresh document to start loading new tweets:
        document = new Document("").setName("TwitterDocs_" + documents.length + "_NER")
        sentence = new Sentence(document)

        // We have to process a line which consists on a token and a label:
      } else {
        val fields = line.split(" ")
        assert(fields.length == 2)
        val word = fields(0)
        val label = fields(1)
        if (sentence.length > 0) {
          document.appendString(" ")
        }
        // Add this token to the sentence:
        val token = new Token(sentence, word)
        val tweetLabel = new TweetLabel(token, label)
        token.attr += tweetLabel
      }
    }
    println("Loaded " + documents.length + " documents.")
    documents
  }
}


class TweetLabel(token : cc.factorie.app.nlp.Token, initialCategory : scala.Predef.String){}