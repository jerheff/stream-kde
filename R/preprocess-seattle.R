# reproject seattle 911 calls to wgs84 meters
library(lubridate)
library(sp)
library(rgdal)
library(spatstat)
library(plyr)

# data directory (no trailing slash)
directory = "/Users/jeremy/Documents/Development/github/stream-kde/R"

# read in raw 911 calls
calls.raw = read.csv(file.path(directory,"Seattle_Police_Department_911_Incident_Response.csv"),
                     stringsAsFactors=FALSE, comment.char = "")

str(calls.raw)
names(calls.raw)

# extract just date and locations
calls.simple = calls.raw[,c("Event.Clearance.Date","Longitude","Latitude")]
str(calls.simple)

# parse date and times
calls.simple$datetime = parse_date_time(calls.simple$Event.Clearance.Date, orders=c("mdY IMS p"))
calls.simple$Event.Clearance.Date = NULL

# show 10 sample rows
calls.simple[sample(nrow(calls.simple),10),]

# remove NAs
calls.clean = calls.simple[!is.na(calls.simple$datetime),]

# calls.clean all have POSIX dates, lon, lat

# fetch just the locations
calls.lonlat = calls.clean[,c("Longitude","Latitude")]

# create a point set
calls.sp.lonlat = SpatialPoints(calls.lonlat, proj4string=CRS("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs"))
summary(calls.sp.lonlat)

# project to spherical mercator EPSG:3857 http://spatialreference.org/ref/sr-org/6864/
calls.sp.meters = spTransform(calls.sp.lonlat, CRS("+proj=merc +lon_0=0 +k=1 +x_0=0 +y_0=0 +a=6378137 +b=6378137 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs "))
summary(calls.sp.meters)

# find bounding box
bb = bbox(calls.sp.meters)
summary(bb)

# same as data frame
calls.clean.projected = as.data.frame(calls.sp.meters)
str(calls.clean.projected)

# add date times
calls.clean.projected$datetime = calls.clean$datetime

# show ten samples
calls.clean.projected[sample(nrow(calls.clean.projected),10),]

# calls.clean.projected is ready for use
write.csv(calls.clean.projected, file = file.path(directory,"seattle_911_projected.csv"), row.names=FALSE)
