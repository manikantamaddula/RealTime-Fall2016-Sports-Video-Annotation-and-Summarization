package testingCode

import org.apache.spark.mllib.linalg.{Matrices, Matrix=>M}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.DoubleType
import org.apache.spark.{SparkContext, SparkConf}
import org.encog.mathutil.matrices.Matrix
import org.encog.ml.data.MLData
import org.encog.ml.data.basic.{BasicMLData, BasicMLDataSet}
import org.encog.neural.som.SOM

/**
  * Created by Manikanta on 7/16/2016.
  */
object testSOM {

  def main (args: Array[String]) {


    System.setProperty("hadoop.home.dir", "C:\\Users\\Manikanta\\Documents\\UMKC Subjects\\PB\\hadoopforspark");

    // Configuration
    val sparkConf = new SparkConf().setAppName("SparkTFIDF").setMaster("local[*]")

    val sc = new SparkContext(sparkConf)




    val som = new SOM(100, 10 * 10)
    val line: RDD[String] =sc.textFile("data/matrix/part-00000")

    val rddDouble: RDD[Double] =line.map{ f=>f.toDouble}
    val arrayDouble: Array[Double] =rddDouble.collect()
    val z3:M=Matrices.dense(100,100,arrayDouble)

    var settingMatrix: Matrix =new Matrix(100,100)

    var i=0
    var j=0
    for (i<-0 to 99) {
      for (j<-0 to 99) {
        settingMatrix.set(i,j,arrayDouble(i*100+j))
      }
    }

    som.setWeights(settingMatrix)
    settingMatrix.getArrayCopy.foreach(f=>println(f.mkString(" ")))

    val vec: Array[Double] =Array(-0.002318885875865817,-0.0016161918174475431,0.005026868544518948,-0.003222254104912281,0.002812860533595085,-0.004936268087476492,0.0016152642201632261,-0.004826554097235203,-0.0061429343186318874,-0.0018723004031926394,0.0043474468402564526,0.005843369290232658,-6.607855902984738E-5,0.0035057137720286846,0.001049016253091395,-0.00210741744376719,0.009751328267157078,0.004728510044515133,0.00242304103448987,0.0014926530420780182,0.0029419988859444857,-0.004532755818217993,0.003471034811809659,0.005149769596755505,0.001359925838187337,-5.00059686601162E-4,-0.0031095724552869797,0.0034735286608338356,-0.004110150970518589,0.001762122381478548,0.00195495318621397,0.0028523446526378393,-0.0015129032544791698,0.004173237830400467,0.0014073082711547613,-0.0034462492913007736,0.0057474467903375626,-0.0018739175284281373,0.00432105828076601,-0.0023509107995778322,-0.0012046551564708352,-0.004835975356400013,-0.002830341225489974,-0.0027272882871329784,-8.418192155659199E-4,-0.0010212960187345743,-0.0027190030086785555,0.008267904631793499,-0.00267863180488348,0.0022885873913764954,0.004119629971683025,-1.0990211740136147E-4,-0.0011547636240720749,0.00462132366374135,0.004286947660148144,-0.0034278561361134052,0.003251047106459737,-0.005384539254009724,-0.0024642099160701036,0.005250409245491028,-0.0010719583369791508,-0.0033982074819505215,-0.0033677173778414726,7.340917363762856E-4,-6.091180257499218E-4,0.0029777665622532368,0.004060288891196251,-0.0016049935948103666,-0.0052984110079705715,0.001772780204191804,0.00483733369037509,-0.002542949514463544,-0.004558074288070202,0.003970053046941757,0.0023930994793772697,0.0020161771681159735,-0.003333479631692171,0.0037249233573675156,0.003284332575276494,0.00532353762537241,0.001566633116453886,0.001464181230403483,0.0028718700632452965,-0.0014304008800536394,-0.005123061593621969,-0.003997082822024822,3.514293348416686E-5,0.004706786014139652,0.0028571076691150665,0.0023187834303826094,-8.901339024305344E-4,-7.846471853554249E-4,0.001531924121081829,0.002379113342612982,-0.0039027193561196327,-0.004683106206357479,0.005689842626452446,-0.0017251920653507113,-0.002450451022014022,0.0010101486695930362)
    val dataset:MLData =new BasicMLData(vec)
    println("Classification:")
    println(convertToXY(som.classify(dataset)))



    def convertToXY(pos: Int): (Int, Int) = {
      val x = Math.floor(pos / 10).toInt
      val y = pos - (10 * x)
      (x, y)
    }


  }
  }
