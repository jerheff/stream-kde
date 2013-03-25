package kdegt

import geotrellis.rest.WebRunner
import geotrellis.process.{Server,Catalog}

object Main {
	val server = Server("kde-server",
						Catalog.fromPath("data/catalog.json"))
						
	def main(args: Array[String]) = WebRunner.main(args)
}