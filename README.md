## About

A small program I wrote to record and play back VMC (Virtual Motion Capture) messages since the OSC (Open Sound Control)
recording programs I tried either couldn't keep up with the amount of messages or didn't support all the required data
types. This program is definitely not user friendly, sorry, and I'm not going to be offering any support for it.

## Running

To run, either build the jar and add javaosc to the classpath or build the shaded jar and then:


### Recording

`java -jar EmVMCPlayback-1.0-SNAPSHOT-shaded.jar record <output file> <port to listen on> <recording duration in seconds>`

### Playing

`java -jar EmVMCPlayback-1.0-SNAPSHOT-shaded.jar play <input file> <port to listen on>`

## Bugs:
1. Full relative file paths don't work (no ../myrecording.bin.gz)
1. Probably won't make directories for output files if they don't already exist
1. VMC playback seems to glitch when looping (e.g. brief T-pose in VSeeFace), could be because VMC has its own 
timekeeping message
1. log4j hasn't been set up
1. The maven-shade-plugin doesn't seem to copy in the necessary license files of the libraries meaning the shaded jars
cannot be distributed