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

[![Join the chat at https://gitter.im/jerheff/stream-kde](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jerheff/stream-kde?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)