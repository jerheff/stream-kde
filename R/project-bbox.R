library(sp)
library(rgdal)

# spark bounding box
bb.spark = "-13630000,6014000,-13604000,6071000"

bb.spark.split = as.numeric(strsplit(bb.spark, ",")[[1]])
names(bb.spark.split) = c("minX", "minY", "maxX", "maxY")

point.min = c(long=bb.spark.split["minX"], lat=bb.spark.split["minY"])
point.max = c(long=bb.spark.split["maxX"], lat=bb.spark.split["maxY"])
points = rbind(point.min, point.max)

sp.webmerc = SpatialPoints(points, proj4string=CRS("+proj=merc +lon_0=0 +lat_ts=0.0 +k=1 +x_0=0 +y_0=0 +a=6378137 +b=6378137 +units=m +wktext +nadgrids=@null +no_defs"))
sp.longlat = spTransform(sp.webmerc, CRS("+proj=longlat +datum=WGS84 +no_defs"))
