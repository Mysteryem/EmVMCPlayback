## About

A small program I wrote to record and play back VMC (Virtual Motion Capture) messages since the OSC (Open Sound Control)
recording programs I tried either couldn't keep up with the amount of messages or didn't support all the required data
types. This program is definitely not user friendly, sorry, and I'm not going to be offering any support for it.

## Running

To run, either build the jar and add javaosc to the classpath or build the shaded jar and then:


### Recording

`java -jar EmVMCPlayback-1.0-SNAPSHOT-shaded.jar record --file=<output file> --port=<port to listen on> --duration=<recording duration in seconds>`

### Playing

`java -jar EmVMCPlayback-1.0-SNAPSHOT-shaded.jar play --file=<input file> --port=<port to send to>`

### Arguments
All argument names are case insensitive.
Single character flag arguments can be combined into a single argument, e.g. `-to`
- `--port=<port>`, the port to listen to or send to
- `--file=<filename/path>`, the file to read from or save to
- `--duration=<seconds>`, how long to record for
- `--address=<address/hostname>`, address/hostname to send to, defaults to localhost when absent
- `-t, --replaceVMCTiming`, enable to replace VMC timing messages, in VSeeFace, this prevents glitching when looping and time goes back to the start
- `-o, --osc`, enable to allow all OSC messages to be recorded or played back instead of only VMC messages
- `-b, --filterBodyAndHeadMovement`, enable to filter out body and head movement from VMC messages, maintaining eye movement and blendshape clips; useful for isolating face tracking

## Bugs:
1. Full relative file paths don't work (no ../myrecording.bin.gz)
1. Probably won't make directories for output files if they don't already exist
1. The maven-shade-plugin doesn't seem to copy in the necessary license files of the libraries meaning the shaded jars
cannot be distributed as is