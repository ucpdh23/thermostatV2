servant
=======


Servant is a system though to provide some basic functionality and generic operations to a person. 
Providing a simple interfaces (for example via xmpp) the human can communicate with the system, to 
request actions system must do, provide new information, etc...

In any case, although it could be more complicated, the implementation tries to be generic enough to
provide a full bidirectional communication. In some aspect, the requested action's nature could require
some monitoring operations (zookeeper nodes, servers, ...). This special action could provokes a final human
intervention (to shutdown the server, send an email, ...) so, in this cases, system can ask the human for something.


At first, this system is split in some pieces:
- command, which provides a generic CLI-shell system. Using a classic Command pattern, commands can be appended
into the system to provide more functionality to the system.
- communication, which manages all the communication system. 
- intelligence, a kind of IA generic architecture to provide a more natural language interface.


Remainder work:
- Unit test
- Concept of application context and session.
- Concept of automatic operations (for example rss monitoring)

Future work:
- Implement a real IA system, to provided an automatic learning system that suggests things from common behaviour.


Some generic ideas regarding the future of the system
=====================================================

http://conceptnet5.media.mit.edu/web/c/en/saxophone
http://conceptnet5.media.mit.edu/data/5.1/c/en/download
https://github.com/commonsense 
https://github.com/commonsense/conceptnet5/wiki/API
http://nlp.stanford.edu/software/index.shtml
http://www.igniterealtime.org/builds/smack/docs/3.1.0/javadoc/org/jivesoftware/smack/XMPPConnection.html#login%28java.lang.String,%20java.lang.String%29
http://code.google.com/p/google-voice-java/wiki/ApplicationGallery

http://community.igniterealtime.org/thread/26681

http://www.oracle.com/technetwork/java/javase/emb7u10-readme-1881013.html
http://www.raspbmc.com/wiki/user/os-x-linux-installation/
http://reviews.cnet.co.uk/desktops/how-to-turn-your-raspberry-pi-into-an-xbmc-media-centre-50008599/
http://elinux.org/RPi_Easy_SD_Card_Setup
http://www.raspberrypi.org/downloads

https://github.com/ucpdh23

Based on Vertx 2.0 template project