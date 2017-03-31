package net.graphner

import java.io.File

import cc.factorie._
import cc.factorie.app.nlp.SharedNLPCmdOptions
import cc.factorie.la
import cc.factorie.optimize._
import cc.factorie.util._

/**
 * @author Mariana Vargas Vieyra
  * Advisor: Pablo Duboue
  * Co-advisor: Oscar Bustos
 */


object SentenceGraphNerTrainer extends HyperparameterMain{
  override def evaluateParameters(args: Array[String]): Double = {
    //TODO: Define this here. We need a method to load documents.
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


  }

}

class GraphNerOpts extends cc.factorie.util.CmdOptions with SharedNLPCmdOptions
  with ModelProviderCmdOptions with DefaultCmdOptions{
  val saveModel = new CmdOption("save-model", "SentenceGraphNER.md", "FILE",
    "Filename where the model is already or will be saved.")
  val train = new CmdOption("train", List.empty[File], "List[File]",
    "Filename(s) from which to read training data in CoNLL 2003 one-word-per-lineformat.")
  val test = new CmdOption("test", List.empty[File], "List[File]",
    "Filename(s) from which to read test data in CoNLL 2003 one-word-per-lineformat.")
  val learningRate = new CmdOption("learning-rate", 0.15, "DOUBLE", "The learning rate for AdaGrad.")
  val delta = new CmdOption("delta", 0.06, "DOUBLE", "Learning delta for AdaGrad.")
}
