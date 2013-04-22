###
# Replays a data set via a socket
###

library(lubridate)
library(plyr)

# data directory (no trailing slash)
directory = "/home/jmarcus/projects/github/stream-kde/R"

# data file
file = "seattle_911_projected.csv"

socket.port = 8081


###
# READ DATA
###

events = read.csv(file.path(directory, file), stringsAsFactors=FALSE, comment.char = "")

###
# TRANSFORM
###

# parse datetimes
events$datetime.parsed = parse_date_time(events$datetime, orders=c("Ymd hms"))

# create a numeric datetime field to manipulate
events$datetime.number = as.double(events$datetime.parsed)

calls.clean.datetime.number.min = min(events$datetime.number)

# sort the dataframe by the datetime number
events.sorted = arrange(events, datetime.number)
head(events.sorted)

###
# FUNCTIONS
###

# the logic is to send over the wire any events between the time.cursor and the fake present

# define a function that returns the fake current time
# offset points the time in the past, scale can speed up replay
fakenow <- function(real.start, fake.start, scale=1.0) {
  seconds.since.start = as.double(now()) - real.start
  scaled.seconds.since.start = scale * seconds.since.start
  return(fake.start + scaled.seconds.since.start)
}

# define a function that formats an event
formatevent <- function(row) {
  x = events.sorted[row,"Longitude"]
  y = events.sorted[row,"Latitude"]
  return(paste0(x, ",", y))
}

# define a function to determine what rows need to be sent.
currentevents <- function(last.sent.time, current.time) {
  new.event.rows = NULL
  new.event.rows = which(events.sorted$datetime.number > last.sent.time & events.sorted$datetime.number <= current.time)
  return(new.event.rows)
}

# define a function that replays events onto a socket
eventserver <- function(real.start, fake.start, scale=1.0) {
  
  # count the number of events sent
  number.events.sent = 0
  
  while(time.cursor <= data.end) {
    # create a socket
    print("Waiting for connection...")
    eventsocket = make.socket(host="localhost", port=socket.port, server=TRUE) # waits for a connection at this point
    on.exit(close.socket(eventsocket))
    # connected
    print("Socket connected.")
    socket.connected = TRUE
    
    while(time.cursor <= data.end & socket.connected == TRUE) {
      print("In Socket loop.")
      
      # fetch the new current time
      new.time = fakenow(real.start, fake.start, scale)
      
      print("new time is")
      print(new.time)
      
      print("time cursor is")
      print(time.cursor)
      
      # fetch the row numbers that need to be sent
      new.event.rows = currentevents(time.cursor,new.time)
      # print(new.event.rows)
      
      if(length(new.event.rows)>0) {
        print("sending events")
        # we have new events to sent
        events.string = NULL
        
        for (row.number in new.event.rows) {
          # get the formated string and build up our overall string
          events.string = paste0(events.string, formatevent(row.number), "\n", collapse="")
        }
        
        # we now have the string to send, store written length (-1 means failure)
        write.length = write.socket(eventsocket, events.string)
        if(write.length == -1) {
          # write failed
          socket.connected = FALSE
          break
        } else {
          number.events.sent = number.events.sent + length(new.event.rows)
          print(paste0("Total events sent: ", number.events.sent, collapse=""))
        }
        
      } else {
        print("length(new.event.rows) == 0")
      }
      # update time.cursor to new time
      time.cursor = new.time
      
      # sleep a bit
      Sys.sleep(0.25)
      
    }
    
  }
  close.socket(eventsocket)
}

runevents <- function() {
  # capture the current time as a datetime number
  real.start = as.double(now())
  
  # determine the datetime number for the date we want to consider now
  fake.start = as.double(parse_date_time("2011-01-01 12:00:00", c("ymdhms")))
  
  # determine the datetime number of the last event
  #data.end = max(events.sorted$datetime.number)
  
  # set scale
  scale = 20000.0
  
  # set row cursor, events before (and including) this time have been replayed
  time.cursor = fakenow(real.start, fake.start, scale)
  
  # serve stream
  eventserver(real.start, fake.start, scale)
}
###
# START STREAM
###

# capture the current time as a datetime number
real.start = as.double(now() + 30)

# determine the datetime number for the date we want to consider now
fake.start = as.double(parse_date_time("2011-01-01 12:00:00", c("ymdhms")))

# determine the datetime number of the last event
#data.end = max(events.sorted$datetime.number)

# set scale
scale = 20000.0

# set row cursor, events before (and including) this time have been replayed
time.cursor = fakenow(real.start, fake.start, scale)

# serve stream
eventserver(real.start, fake.start, scale)