package testingCode

import java.io.{File, PrintStream}

import mlpipeline.{TFIDF, CoreNLP}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.Pipeline
import org.apache.spark.mllib.clustering.GaussianMixture
import org.apache.spark.ml.feature.{MinMaxScaler, Word2Vec, StopWordsRemover, Tokenizer}
import org.apache.spark.mllib.classification.{NaiveBayesModel, NaiveBayes}
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.{Matrix, Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by Manikanta on 7/30/2016.
  */
object EM_NV {

  def main(args: Array[String]) {

    System.setProperty("hadoop.home.dir", "C:\\Users\\Manikanta\\Documents\\UMKC Subjects\\PB\\hadoopforspark");

    // Configuration
    val sparkConf = new SparkConf().setAppName("iHearWOrd2Vec").setMaster("local[*]").set("spark.driver.memory","2g").set("spark.executor.memory","2g")

    val sc = new SparkContext(sparkConf)

    //val spark = SQLContext.getOrCreate(sc)


    val spark = SparkSession.builder.appName("iHearWOrd2Vec").master("local[*]").getOrCreate()
    import spark.implicits._


    // Turn off Info Logger for Consolexxx
    Logger.getLogger("org").setLevel(Level.OFF);
    Logger.getLogger("akka").setLevel(Level.OFF);



    // Read the file into RDD[String]
    val inputfilepath="C:\\Users\\Manikanta\\Documents\\UMKC Subjects\\KDM\\Project Files\\bbcsport-fulltext\\bbcsport"
    val tfidffilepath="C:\\Users\\Manikanta\\Documents\\UMKC Subjects\\KDM\\Project Files\\tfidfoutput2"

    //val inputfilepath="C:\\Users\\Manikanta\\Documents\\UMKC Subjects\\KDM\\Project Files\\bbcsport-fulltext\\bbcsport\\cricket\\*"
    val rddWords =sc.wholeTextFiles(inputfilepath+"\\*",2000)
    val text: RDD[(String, String)] = rddWords.map { case (file, text) => (file,CoreNLP.returnLemma(text.
      replaceAll("[^a-zA-Z\\s:]", " ")
      .replaceAll("\"\"[\\p{Punct}&&[^.]]\"\"", " ")
      .replaceAll(","," ")
      .replaceAll("\"\"\\b\\p{IsLetter}{1,2}\\b\"\""," "))) }


    //Creating DataFrame from RDD
    val sentenceData = spark.createDataFrame(text).toDF("labels", "sentence")
    //Tokenizer
    val tokenizer = new Tokenizer().setInputCol("sentence").setOutputCol("words")
    val wordsData = tokenizer.transform(sentenceData)

    //Stop Word Remover
    val remover = new StopWordsRemover()
      .setInputCol("words")
      .setOutputCol("filteredWords")
    val processedWordData = remover.transform(wordsData)


    //Word2Vec Model Generation
    val word2Vec = new Word2Vec()
      .setInputCol("filteredWords")
      .setOutputCol("result")
      .setVectorSize(100)
      .setMinCount(0)

    println("word2vec model is done")

    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, remover, word2Vec))
    val model= word2Vec.fit(processedWordData)
    //word2Vec.save("data/word2vec")
    val model3=model.transform(processedWordData)

    //model.save("data/word2vecmodel")
    println("saved word2vec model")


    val dirs: Array[String] = getListOfSubDirectories(inputfilepath)

    var toptfidfwords: Array[(String, Double)]=Array()
    val y =dirs.foreach(f => {

      val folderpath = inputfilepath + "\\" + f + "\\*"

      val rddWords2 =sc.wholeTextFiles(folderpath)
      val text2: RDD[(String, String)] = rddWords2.map { case (file, text) => (file,CoreNLP.returnLemma(text.
        replaceAll("[^a-zA-Z\\s:]", " ")
        .replaceAll("\"\"[\\p{Punct}&&[^.]]\"\"", " ")
        .replaceAll(","," ")
        .replaceAll("\"\"\\b\\p{IsLetter}{1,2}\\b\"\""," "))) }


      //Creating DataFrame from RDD
      val sentenceData2 = spark.createDataFrame(text2).toDF("labels", "sentence")
      //Tokenizer
      val tokenizer2 = new Tokenizer().setInputCol("sentence").setOutputCol("words")
      val wordsData2 = tokenizer2.transform(sentenceData2)

      //Stop Word Remover
      val remover2 = new StopWordsRemover()
        .setInputCol("words")
        .setOutputCol("filteredWords")
      val processedWordData2 = remover2.transform(wordsData2)
      val x=TFIDF.getTopTFIDFWords(sc, processedWordData2.select("filteredWords").rdd)
      toptfidfwords=toptfidfwords ++ x
    })

    println("toptfidf words for all classes done")
    //toptfidfwords.foreach(f=>println(f))

    val topwordterms =toptfidfwords.map(f=>f._1)
    //sc.parallelize(topwordterms)
    //sc.broadcast(topwordterms)
    val vec: DataFrame =model.getVectors

    /*
    val scaler = new MinMaxScaler()
      .setInputCol("vector")
      .setOutputCol("vector2")
    val vec_scaled2=scaler.fit(vec)
    val vec_scaled=vec_scaled2.transform(vec)
    */


    val vec2=vec.select("vector","word")
    val vec_3 =  vec2.filter(vec("word").isin(topwordterms.toList:_*))
    val vec3=vec_3.rdd
    vec3.coalesce(1,true).saveAsTextFile("data/wordvector2")


    //kMeans to get 40 categories
    // Load and parse the data for kMeans Input
    val x3=vec_3.select("vector").rdd.map{ f=>f.mkString(" ")}
    //x3.foreach(f=>println(f))
    val x4 =x3.map{ f=>f.replace("[","").replace("]","").split(",")}
    val x5: RDD[Array[Double]] =x4.map{ f=>f.map(ff=>ff.toDouble)}
    val x6=x3.map{ f=>f.replace("[","").replace("]","").split(",")}
    val vecdata =x5.map{ f=>Vectors.dense(f)}

    //val vecdata: RDD[scala.Vector[Double]] = x5.map{ f => f.toVector}
    val worddata: RDD[String] =vec_3.select("word").rdd.map{ f=>f.mkString(" ")}
    //val x2 =vec3.collect().map{case Row(vector:Vector,word:String)=>(vector,word)}
    //x2.foreach(f=>println(f))
    val vec4 =vec3.map{case Row(vector:Vector,word:String)=>(vector,word)}
    //val vecdata: RDD[Vector] =vec4.map{ f=>f._1}
    //val worddata =vec4.map{ f=>f._2}


    // Cluster the data into 40 classes using EM

    val gmm = new GaussianMixture().setK(100).run(vecdata)
    gmm.save(sc,"data/EMModel")
    val mapClusterIndices =gmm.predict(vecdata)

    val x: RDD[((String, Vector), Int)] =worddata.zip(vecdata).zip(mapClusterIndices)

    val writer = new PrintStream("data/somclusters3.txt")
    x.collect.foreach { f =>
      writer.println(f._1._1.toString+"\t"+f._1._2.toString.replace("[","").replace("]","").replace(",","\t")+"\t"+f._2.toString)
    }

    val nvinput=x.map{f=>new LabeledPoint(f._2,f._1._2)}
    //nvinput.foreach(f=>println(f))
    nvinput.saveAsTextFile("data/nvinput")

    // Split data into training (70%) and test (30%).
    val splits = nvinput.randomSplit(Array(0.8, 0.2), seed = 11L)
    val training = splits(0)
    val test = splits(1)

    val model2 = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")

    val predictionAndLabel = test.map(p => (model2.predict(p.features), p.label))
    predictionAndLabel
    val accuracy = 1.0 * predictionAndLabel.filter(x => x._1 == x._2).count() / test.count()

    //predictionAndLabel.foreach{f=>println(f)}

    println("accuracy of naive bayes model: "+accuracy)
    //predictionAndLabel.foreach(f=>println(f))
    val metrics=new MulticlassMetrics(predictionAndLabel)

    val confmatrix: Matrix =metrics.confusionMatrix
    println("ConfusionMatrix: ")
    confmatrix.rowIter.foreach(f=>println(f))
    val wghtprecision: Double =metrics.weightedPrecision
    val wghtrecall: Double =metrics.weightedRecall
    val wghtfmeasure: Double =metrics.weightedFMeasure
    println("Accuracy: "+metrics.accuracy)
    println("Weighted Precision: "+wghtprecision)
    println("Weighted Recall: "+wghtrecall)
    println("Weighted FMeasure: "+wghtfmeasure)


    // Save and load model
    model2.save(sc, "data/myNaiveBayesModel")
    val sameModel = NaiveBayesModel.load(sc, "data/myNaiveBayesModel")

    spark.stop()
    sc.stop()
  }

  def getListOfSubDirectories(directoryName: String): Array[String] = {
    new File(directoryName).listFiles.filter(_.isDirectory).map(_.getName)
  }
}
