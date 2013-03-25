package com.azavea.jheffner.kde

import spark.streaming.{Seconds, Minutes, StreamingContext}
import StreamingContext._
import spark.SparkContext._

import geotrellis._
//import geotrellis.process.Server
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis.data.ColorRamps._

// import geotrellis.rest.WebRunner

/**
 * Generates KDE ARG files for serving via GeoTrellis
 *
 */

object KDE {
	// Point class represents event
	class Point(xc: Double, yc: Double) extends Serializable {
		val x: Double = xc
		val y: Double = yc

		override def toString(): String = "Point: (" + x + ", " + y + ")"	

		def isContainedIn(extent: SpatialExtent): Boolean = {
			extent.containsPoint(this)
		}

		def returnCellFrom(extent: SpatialExtent): Cell = {
			extent.returnCellForPoint(this)
		}
	}

	// SpatialExtent represents the raster covering
	class SpatialExtent(minX: Double, minY: Double, maxX: Double, maxY: Double, cellSize: Double)  extends Serializable {
		require(minX < maxX)
		require(minY < maxY)
		require(cellSize > 0)
	
		val xLength = maxX - minX
		val yLength = maxY - minY

		require(xLength >= cellSize)
		require(yLength >= cellSize)

		val columns: Int = (xLength / cellSize).toInt
		val rows: Int = (yLength / cellSize).toInt

		def containsX(x: Double): Boolean = x >= minX && x <= maxX
		def containsY(y: Double): Boolean = y >= minY && y <= maxY    

		def containsPoint(p: Point): Boolean = {
			this.containsX(p.x) && this.containsY(p.y)
		}

		def containsCell(c: Cell): Boolean = {
			c.row >= 0 && c.row < rows && c.col >= 0 && c.col < columns
		}

		def returnCellForPoint(p: Point): Cell = {
			new Cell(((maxY - p.y) / cellSize).toInt, ((maxX - p.x) / cellSize).toInt)
		}

		override def toString(): String = "Extent: (" + minX + ", " + minY + ") to (" + maxX + ", " + maxY + ") Cellsize: " + cellSize
	}	

	// Cell represents one cell within the raster covering
	class Cell(rowc: Int, colc: Int) extends Serializable {
		val row: Int = rowc
		val col: Int = colc
	
		override def toString(): String = "Cell: (" + row + ", " + col + ")"
	
		override def equals(that: Any): Boolean = that match {
			case cell: Cell => row == cell.row && col == cell.col
			case _ => false
		}
	
		override def hashCode: Int = {
			row * 41 + col
		}
	  	
	
		def + (that: Cell): Cell = {
			new Cell(row + that.row, col + that.col)
		}
	
		def isContainedIn(extent: SpatialExtent): Boolean = {
			extent.containsCell(this)
		}
	
		def applyKernel(k: Kernel): List[(Cell, Double)] = {
			for { 
				o <- k.kernelOffsets()
				val (cell, value) = o
				} yield (this + cell, value)
		}
	}

	// SparseRaster represents a raster as non-zero cell values in a map of cell indices to values
	class SparseRaster(cellvaluesc: Set[(Cell,Double)], extentc: SpatialExtent) extends Serializable {
		val cellmap: Map[Cell,Double] = cellvaluesc.toMap
		val extent: SpatialExtent = extentc
	
		override def toString(): String = "SparseRaster with extent: " + extent.toString + " and cells " + cellmap.toString()
	
		override def equals(that: Any): Boolean = that match {
			case sr: SparseRaster => false // TODO
			case _ => false
		}
	
		override def hashCode: Int = {
			(cellmap.hashCode + 41) * extent.hashCode
		}
	
		def valueForCell(c: Cell): Double = {
			cellmap.get(c) match {
				case Some(value) => value
				case None => 0
			}
		}
	
		def asDoubleArray(): Array[Double] = {
			{ for(r <- 1 to extent.rows; c <- 1 to extent.columns) yield this.valueForCell(new Cell(r,c)) }.toArray
		}
	
		def asFloatArray(): Array[Float] = {
			{ for(r <- 1 to extent.rows; c <- 1 to extent.columns) yield this.valueForCell(new Cell(r,c)).toFloat }.toArray
		}
}

	// Kernel represents the kernel split used in the KDE
	class Kernel(cellSize: Double, kernel: String, bandWidth: Double) extends Serializable {
		override def toString(): String = "Kernel: " + kernel + " with bandwidth: " + bandWidth
	
		def kernelOffsets(): List[(Cell, Double)] = this.kernel match {
			case "rook" => List(
							(new Cell(0,0), 1.0 / 5),
							(new Cell(-1,0), 1.0 / 5),
							(new Cell(1,0), 1.0 / 5),
							(new Cell(0,1), 1.0 / 5),
							(new Cell(0,-1), 1.0 / 5))
			case _ => List((new Cell(0,0), 1))
		}
  }

	
	def main(args: Array[String]) {
	
		/*  prove that the classes work
	
		val myextent = new SpatialExtent(10,0,20,5,1)
		val cell1 = new Cell(1,1)
		println("Cell1 is in extent?" + cell1.isContainedIn(myextent))
		val cell2 = new Cell(7000,1)
		println("Cell2 is in extent?" + cell2.isContainedIn(myextent))
		println("does cell1 equal cell2? " + cell1 == cell2)
		val cell3 = new Cell(7000,1)
		println("does cell2 equal cell3? " + cell2 == cell3)
		val mykernel = new Kernel(1, "rook", 1)
		println("mykernel offsets: " + mykernel.kernelOffsets())
		println("Apply kernel to cell1: " + cell1.applyKernel(mykernel))
	
		val mycells = Set((cell1, 1.0))
	
	
		val rasterarray: Array[Float] = {new SparseRaster(mycells.toSet, myextent)}.asFloatArray
		println("created rasterarray of length " + rasterarray.length)
		println(rasterarray.deep.toString)
	
		val rasterdata: RasterData = FloatArrayRasterData({new SparseRaster(mycells.toSet, myextent)}.asFloatArray, myextent.columns, myextent.rows)
		println("created rasterdata")
		println(rasterdata)

		val myraster: Raster = Raster(FloatArrayRasterData({new SparseRaster(mycells.toSet, myextent)}.asFloatArray, myextent.columns, myextent.rows), 
									RasterExtent(Extent(10, 0, 20, 5), 1, 1, myextent.columns, myextent.rows))
	
		println("created myraster")
		println(myraster.asciiDraw())
	
	
		val myargWriter: Option[ArgWriter] = 
			rasterdata.asArray map { arrayRasterData => new ArgWriter(arrayRasterData.getType) }
		println("created myargwriter")
	
		myargWriter match {
			case Some(myargWriter) => myargWriter.write("/Users/jeremy/Documents/Business/Azavea/Scala/Spark/kde/rasters/testkde.arg", myraster, "testkde")
			case _ => sys.error("Could not write raster")
		}
		
		val point1 = new Point(0, -89.5)
		println("point1 is in cell " + myextent.returnCellForPoint(point1))
		*/

		// begin main logic
	
		if (args.length < 3) {
			System.err.println("Usage: <master> <extent> <cellsize>")
			System.exit(1)
		}

		// Setup config
		val Array(master, extentString, cellSizeString) = args.slice(0, 3)

		// extract bounding box from extent string arg
		val Array(minX, minY, maxX, maxY) = extentString.split(",").map(n => n.toDouble)

		// duration of batches coming into the stream
		val streamBatchDuration = 5 // seconds

		// overall stream duration to measure
		val streamOverallDuration = 60*60*6 // 6 hours in seconds

	    // size of cells (width and height)
		val cellSize = cellSizeString.toDouble // map units
	
		// create our extent
		val extent = new SpatialExtent(minX, minY, maxX, maxY, cellSize)
	
		// create our kernel
		val kernel = new Kernel(1, "rook", 1)
	
		// raster data directory 
		val rasterDirectory = "/Users/jeremy/Documents/Development/github/stream-kde/spark/rasters/"
	
		// hdfs checkpoint directory
		val hdfsDirectory = "/Users/jeremy/Documents/Development/github/stream-kde/spark/hdfs"
	
		// PREP WORK DONE
	
	
		// set config
		System.setProperty("spark.ui.port", "8080")
		System.setProperty("spark.cleaner.ttl", (streamOverallDuration + 60).toString)
	
		val ssc = new StreamingContext(master, "KDE", Seconds(streamBatchDuration), 
			                           System.getenv("SPARK_HOME"), Seq(System.getenv("SPARK_JAR")))

		// setup checkpointing
		ssc.checkpoint(hdfsDirectory)
	
		// broadcast variables to our workers
		val broadcastExtent = ssc.sparkContext.broadcast(extent)
		val broadcastKernel = ssc.sparkContext.broadcast(kernel)

		
		// get event strings from a socket
		val stream = ssc.socketTextStream("localhost", 8081)
	
		// convert to points within extent
		val pointStream = stream.map(s => s.split(",") match {
											case Array(x, y) => new Point(x.toDouble, y.toDouble)})
								.filter(p => p.isContainedIn(broadcastExtent.value))
	
		// convert to kde slices
		val kdeStream = pointStream.flatMap(p => p.returnCellFrom(broadcastExtent.value).applyKernel(broadcastKernel.value))
								   .reduceByKey(_ + _)
							       .filter(cv => cv match { 
										case (c, v) => c.isContainedIn(broadcastExtent.value)
										})
	
		// assemble into sliding window
		val overallKDE = kdeStream.reduceByKeyAndWindow(_ + _, _ - _, Seconds(streamOverallDuration), Seconds(streamBatchDuration))
		overallKDE.checkpoint(Seconds(5*streamBatchDuration))

	
		// with each result from the sliding window:
		overallKDE.foreach(rdd => {
		
			// collect cell values
			val cells = rdd.collect()
		
			// generate a raster
			val raster: Raster = Raster(FloatArrayRasterData({new SparseRaster(cells.toSet, extent)}.asFloatArray, extent.columns, extent.rows), 
										RasterExtent(Extent(minX, minY, maxX, maxY), cellSize, cellSize, extent.columns, extent.rows))
		
			// write an arg for further use
			val argWriter = ArgWriter(TypeFloat)
			argWriter.write(rasterDirectory + "overallkde.arg", raster, "overallkde")
		
		})
		
	
		// start the process
		ssc.start()
	
	}
}