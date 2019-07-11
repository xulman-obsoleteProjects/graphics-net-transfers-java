# SimViewer
![Example: TRAgen3D (C++) sends images to scenerygraphics/scenery (Java + Kotlin)](https://www.fi.muni.cz/~xulman/files/TRAgen-demo/EmbryoGen_earlyVersion.png)

# About
This is essentially a [Scenery](https://github.com/scenerygraphics/scenery) 'client'
that [ZeroMQ](https://github.com/zeromq/jeromq)-ish listens for and displays current
geometry of the simulated agents. The communication is based on own/proprietary
(and simple) network protocol.

One can find the simulator that feeds this display in [another repo](https://github.com/xulman/EmbryoGen).

# Installing and starting this client
```
git clone https://github.com/xulman/SimViewer.git
cd SimViewer/
mvn dependency:copy-dependencies package
cd target
java -cp "SimViewer-1.0-SNAPSHOT.jar:dependency/*" de.mpicbg.ulman.simviewer.StartUpScene
```
