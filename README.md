Kernel Density Estimation on Point Streams

Data flow:
* R
  * Project from lat/long to web mercator
  * Serve events on a socket 
* spark
  * Catch event stream from socket
  * Generate ARGs for KDE slices
* gt
  * Serve final KDE to browser