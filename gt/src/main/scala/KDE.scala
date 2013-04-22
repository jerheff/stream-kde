package kdegt

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.{GET, Path, PathParam}
import javax.ws.rs.core.{Response,Context}

import geotrellis._
import geotrellis.data.ColorRamps._
import geotrellis.raster.op.VerticalFlip
import geotrellis.raster.op._
@Path("/kde")
class KDE {
	@GET
	def get(@Context req:HttpServletRequest) = {
    print("Received request.")
		val loadRaster:Op[Raster] =
			io.LoadRaster("overallkde", RasterExtent( Extent(-13630000, 6014000, -13604000, 6071000), 25, 25, 1040, 2280 ))

    val removeZeros:Op[Raster] = loadRaster.map { r:Raster => r.mapDouble { z => if (z < 45) Double.NaN else z } }

    val rasterOp = removeZeros
    
    val ramp = HeatmapBlueToYellowToRedSpectrum.interpolate(100).alphaGradient(128,255)
    println(ramp.toArray.mkString(","))
 
		val pngOp:Op[Array[Byte]] = 
			io.SimpleRenderPng(rasterOp, ramp)
    print("Serving png.")
			
		try {
			val img:Array[Byte] = Main.server.run(pngOp)
      val path = "/var/www/stream-kde/current.png"
      val fos = new java.io.FileOutputStream(new java.io.File(path))
      fos.write(img)
      fos.close()

			Response.ok(img)
					.`type`("image/png")
					.build()
		} catch {
			case e:Throwable => Response.ok(s"Error: $e")
										.`type`("text/plain")
										.build()
		}
	}
}
